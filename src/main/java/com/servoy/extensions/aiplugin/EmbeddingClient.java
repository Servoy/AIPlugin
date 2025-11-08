package com.servoy.extensions.aiplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.servoy.j2db.util.Debug;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderResult;
import com.servoy.j2db.scripting.Deferred;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * EmbeddingClient provides methods to generate embeddings for text and manage
 * embedding stores. It supports in-memory and PostgreSQL (pgvector) embedding
 * stores.
 */
@ServoyDocumented(scriptingName = "EmbeddingClient")
public class EmbeddingClient {
	// TODO should i shutdown this somewhere?
	/**
	 * Executor for running embedding operations asynchronously using virtual
	 * threads.
	 */
	static ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

	/**
	 * The embedding model used for generating embeddings.
	 */
	private final DimensionAwareEmbeddingModel model;
	/**
	 * The client plugin access instance for Servoy scripting context.
	 */
	private final AIProvider provider;

	/**
	 * Constructs an EmbeddingClient with the given embedding model and plugin
	 * access.
	 *
	 * @param model    The embedding model to use.
	 * @param provider ai plugin remote service.
	 */
	public EmbeddingClient(DimensionAwareEmbeddingModel model, AIProvider provider) {
		this.model = model;
		this.provider = provider;
	}

	/**
	 * Gets the dimension of the embeddings produced by the model. This can be used
	 * when createing a vector column in a database, to use as the "size" of the
	 * vector.
	 *
	 * @return The embeddings model dimension.
	 */
	@JSFunction
	public int getDimension() {
		return model.dimension();
	}

	/**
	 * Generates an embedding for a single text string this can be used to use in
	 * the foundset.sort(vectorColumn, embeddingClient.embed("text"), maxRows);
	 *
	 * @param text The text string to create embeddings for.
	 * @return The embedding as a float array, or null if input is empty.
	 */
	@JSFunction
	public float[] embed(String text) {
		if (text == null || text.trim().isEmpty())
			return null;

		TextSegment segment = TextSegment.textSegment(text);
		Response<Embedding> embedding = model.embed(segment);
		return embedding.content().vector();
	}

	/**
	 * Generates embeddings for an array of text strings asynchronously.
	 *
	 * @param texts The array of text strings to embed.
	 * @return A Promise resolving to a float array of embeddings, or null if input
	 *         is empty.
	 */
	@JSFunction
	public NativePromise embed(String[] texts) {
		Deferred deferred = new Deferred(provider.getApplication());
		if (texts == null || texts.length == 0) {
			deferred.resolve(null);
		} else {
			virtualThreadExecutor.submit(() -> {
				try {
					List<TextSegment> segments = Arrays.asList(texts).stream().map(t -> TextSegment.textSegment(t))
							.collect(Collectors.toList());
					Response<List<Embedding>> embeddings = model.embedAll(segments);
					List<Embedding> content = embeddings.content();
					List<float[]> collect = content.stream().map(e -> e.vector()).collect(Collectors.toList());
					deferred.resolve(collect.toArray(new float[collect.size()][]));

				} catch (Exception ex) {
					deferred.reject(ex);
				}
			});
		}
		return deferred.getPromise();
	}

	/**
	 * Generates embeddings for all records in the specified foundset for the given
	 * textColumns and stores them in the specified vector column.
	 *
	 * @param foundset
	 * @param embeddingStore
	 * @param textColumns
	 * @return A Promise resolving with the given foundset when embeddings are
	 *         stored, or rejects on error.
	 */
	@JSFunction
	public NativePromise embedAll(IFoundSet foundset, EmbeddingStore embeddingStore, String... textColumns) {
		Deferred deferred = new Deferred(provider.getApplication());
		virtualThreadExecutor.submit(() -> {
			try {
				IQueryBuilder query = foundset.getQuery();
				query.sort().clear();
				IQueryBuilderResult result = query.result().clear();
				for (String textColumn : textColumns) {
					result.add(textColumn);
				}
				query.result().addPk();

				provider.getDatabaseManager().loadDataSetsByQuery(query, 0, 100, (dataSet) -> {
					var segments = new ArrayList<TextSegment>(dataSet.getRowCount() * textColumns.length);
					dataSet.getRows().forEach((row) -> {
						var metaData = new HashMap<String, Object>();
						// pk column names
						for (int i = textColumns.length; i < dataSet.getColumnCount(); i++) {
							metaData.put(dataSet.getColumnNames()[i], row[i]);
						}

						// text columns
						for (int i = 0; i < textColumns.length; i++) {
							if (row[i] != null) {
								segments.add(TextSegment.textSegment(row[i].toString(), Metadata.from(metaData)));
							}
						}

						Response<List<Embedding>> embeddings = model.embedAll(segments);
						embeddingStore.getEmbeddingStore().addAll(embeddings.content(), segments);
					});

					return true;
				});
				deferred.resolve(foundset);
			} catch (Exception ex) {
				deferred.reject(ex);
			}
		});
		return deferred.getPromise();
	}

	/**
	 * Creates an in-memory embedding store for storing and retrieving embeddings.
	 *
	 * @return An EmbeddingStore backed by an in-memory store.
	 */
	@JSFunction
	public EmbeddingStore createInMemoryStore() {
		InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
		return new EmbeddingStore(embeddingStore, model);
	}

	/**
     * Creates a Servoy embedding store for the specified source table, the emmbeddings will be saved in the specified
     * table in the same server. This will not drop an existing table.
	 *
	 * @param dataSource The source table.
	 * @param tableName  The name of the table to use for storing embeddings.
	 * @return An EmbeddingStore backed by a servoy store, or null if creation
	 *         fails.
	 */
	@JSFunction
	public EmbeddingStore createServoyEmbeddingStore(String dataSource, String tableName) {
		return openOrCreate(dataSource, tableName, true);
	}

	/**
	 * Opens a Servoy embedding store for the specified source table, the emmbeddings will be saved in the specified
     * table in ghe same server. This will not drop an existing table.
	 *
     * @param dataSource The source table.
     * @param tableName  The name of the table to use for storing embeddings.
     * @return An EmbeddingStore backed by a servoy store, or null if creation
     *         fails.
	 */
	@JSFunction
	public EmbeddingStore openServoyEmbeddingStore(String dataSource, String tableName) {
		return openOrCreate(dataSource, tableName, false);
	}

	private EmbeddingStore openOrCreate(String dataSource, String tableName, boolean dropTableFirst) {
		try {
			ServoyEmbeddingStore embeddingStore = provider.getAiPluginService().embeddingStoreBuilder()
					.clientId(provider.getClientID()).source(dataSource).tableName(tableName)
					.dropTableFirst(dropTableFirst).createTable(true).dimension(model.dimension()).build();
			return new EmbeddingStore(embeddingStore, model);
		} catch (Exception e) {
            Debug.error(e);
		}
		return null;
	}

}