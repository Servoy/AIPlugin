package com.servoy.extensions.aiplugin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.Deferred;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

/**
 * EmbeddingClient provides methods to generate embeddings for text and manage embedding stores.
 * It supports in-memory and PostgreSQL (pgvector) embedding stores.
 */
@ServoyDocumented(scriptingName = "EmbeddingClient")
public class EmbeddingClient {
	// TODO should i shutdown this somewhere?
	/**
	 * Executor for running embedding operations asynchronously using virtual threads.
	 */
	static ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
	
	/**
	 * The embedding model used for generating embeddings.
	 */
	private final DimensionAwareEmbeddingModel model;
	/**
	 * The client plugin access instance for Servoy scripting context.
	 */
	private final IClientPluginAccess access;

	/**
	 * Constructs an EmbeddingClient with the given embedding model and plugin access.
	 * @param model The embedding model to use.
	 * @param access The client plugin access instance.
	 */
	public EmbeddingClient(DimensionAwareEmbeddingModel model, IClientPluginAccess access) {
		this.model = model;
		this.access = access;
	}
	/**
	 * Gets the dimension of the embeddings produced by the model.
	 * This can be used when createing a vector column in a database, to use as the "size" of the vector.
	 * 
	 * @return The embeddings model dimension.
	 */
	@JSFunction
	public int getDimension() {
		return model.dimension();
	}

	/**
	 * Generates embeddings for an array of text strings asynchronously.
	 * @param texts The array of text strings to embed.
	 * @return A Promise resolving to a float array of embeddings, or null if input is empty.
	 */
	@JSFunction
	public NativePromise embed(String[] texts) {
		Deferred deferred = new Deferred(access);
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
	 * Creates an in-memory embedding store for storing and retrieving embeddings.
	 * @return An EmbeddingStore backed by an in-memory store.
	 */
	@JSFunction
	public EmbeddingStore createInMemoryStore() {
		InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
		return new EmbeddingStore(embeddingStore, model, access);
	}
	
	/**
	 * Creates a PostgreSQL (pgvector) embedding store using the specified server and table.
	 * This will drop an existing table if it exists.
	 * 
	 * @param serverName The name of the Servoy database server.
	 * @param tableName The name of the table to use for storing embeddings.
	 * @return An EmbeddingStore backed by a pgvector store, or null if creation fails.
	 */
	@JSFunction
	public EmbeddingStore createPgVectorStore(String serverName, String tableName) {
		return openOrCreate(serverName, tableName, true);
	}
	
	/**
	 * Opens a PostgreSQL (pgvector) embedding store using the specified server and table.
	 * This will not drop an existing table.
	 * 
	 * @param serverName The name of the Servoy database server.
	 * @param tableName The name of the table to use for storing embeddings.
	 * @return An EmbeddingStore backed by a pgvector store, or null if creation fails.
	 */
	@JSFunction
	public EmbeddingStore openPgVectorStore(String serverName, String tableName) {
		return openOrCreate(serverName, tableName, false);
	}

	/**
	 * @param serverName
	 * @param tableName
	 */
	private EmbeddingStore openOrCreate(String serverName, String tableName, boolean dropTableFirst) {
		try {
			DataSource dataSource = ((IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName)).getDataSource();
			PgVectorEmbeddingStore embeddingStore = PgVectorEmbeddingStore.datasourceBuilder().datasource(dataSource).dropTableFirst(dropTableFirst).table(tableName).dimension(model.dimension()).createTable(true).build();
			return new EmbeddingStore(embeddingStore, model, access);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}