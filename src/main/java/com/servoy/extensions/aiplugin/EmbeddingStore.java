package com.servoy.extensions.aiplugin;

import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.pdf.ApachePdfBoxDocumentParser;
import com.servoy.extensions.aiplugin.server.SupportsTransaction;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderResult;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IScriptable;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

/**
 * EmbeddingStore provides methods to embed text data and perform similarity
 * search using embeddings. It supports asynchronous embedding and blocking
 * search, and can be backed by various embedding store implementations.
 */
@ServoyDocumented
public class EmbeddingStore implements IScriptable, IJavaScriptType {

	/**
	 * The client plugin access instance for Servoy scripting context.
	 */
	private final AIProvider provider;

	/**
	 * The underlying embedding store implementation.
	 */
	private final dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> embeddingStore;
	/**
	 * The embedding model used for generating embeddings.
	 */
	private final DimensionAwareEmbeddingModel model;

	/**
	 * Constructs an EmbeddingStore with the given embedding store, model, and
	 * plugin access.
	 *
	 * @param provider       The plugin store implementation to use.
	 * @param embeddingStore The embedding store implementation to use.
	 * @param model          The embedding model to use.
	 */
	public EmbeddingStore(AIProvider provider,
			dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> embeddingStore,
			DimensionAwareEmbeddingModel model) {
		this.provider = provider;
		this.embeddingStore = embeddingStore;
		this.model = model;
	}

	dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> getEmbeddingStore(String transactionID) {
		return (embeddingStore instanceof SupportsTransaction<?> txStore) ? txStore.withTransactionId(transactionID)
				: embeddingStore;
	}

	/**
	 * Generates embeddings for all records in the specified foundSet for the given
	 * textColumns and stores them in the specified vector column.
	 *
	 * @param foundSet    records in the foundSet are embedded
	 * @param textColumns columns of the foundSet to embed
	 * @return A Promise resolving with the given foundSet when embeddings are
	 *         stored, or rejects on error.
	 */
	@JSFunction
	public NativePromise embedAll(IFoundSet foundSet, String... textColumns) {
		return provider.async(() -> {
			int[] selectedIndexes = foundSet.getSelectedIndexes();
			try {
				IQueryBuilder query = foundSet.getQuery();
				query.sort().clear();
				IQueryBuilderResult result = query.result().clear();
				for (String textColumn : textColumns) {
					result.add(textColumn);
				}
				query.result().addPk();

				String transactionID = provider.getDatabaseManager()
						.getTransactionID(getDataSourceServerName(foundSet.getDataSource()));
				var segmentStore = getEmbeddingStore(transactionID);

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
						segmentStore.addAll(embeddings.content(), segments);
					});

					return true; // process all dataset chunks
				});
			} finally {
				foundSet.setSelectedIndexes(selectedIndexes);
			}
			return foundSet;
		});
	}

	/**
	 * Asynchronously embeds an array of text data with optional metadata and stores
	 * the results.
	 *
	 * @param data     The array of text strings to embed.
	 * @param metaData An array of metadata maps, one for each text string (may be
	 *                 null).
	 * @return A Promise resolving to the store
	 */
	@JSFunction
	public NativePromise embed(String[] data, Map<String, Object>[] metaData) {
		return provider.async(() -> {
			if (data != null && data.length > 0) {
				var segments = new ArrayList<TextSegment>();
				for (int i = 0; i < data.length; i++) {
					if (metaData != null && i < metaData.length && metaData[i] != null) {
						Metadata md = Metadata.from(metaData[i]);
						segments.add(TextSegment.textSegment(data[i], md));
					} else {
						segments.add(TextSegment.textSegment(data[i]));
					}
				}

				Response<List<Embedding>> embeddings = model.embedAll(segments);
				List<Embedding> content = embeddings.content();
				embeddingStore.addAll(content, segments);
			}
			return this;
		});
	}

	/**
	 * Embeds a PDF document by splitting it into paragraphs of specified size and
	 * overlap The text segments are stored in the embedding store and will get
	 * metadata from the pdf and if possible also the name of the PDF is set if that
	 * is known.
	 *
	 * @param pdfSource             This can be JSFile or String represeting a file
	 *                              path, or byte[] representing the PDF content.
	 * @param maxSegmentSizeInChars How many characters per segment we can have.
	 * @param maxOverlapSizeInChars How many characters of overlap between segments.
	 * @return A Promise resolving to the store
	 */
	@JSFunction
	public NativePromise embed(Object pdfSource, int maxSegmentSizeInChars, int maxOverlapSizeInChars) {
		return embed(pdfSource, maxSegmentSizeInChars, maxOverlapSizeInChars, null);
	}

	/**
	 * Embeds a PDF document by splitting it into paragraphs of specified size and
	 * overlap, with metadata. The text segments are stored in the embedding store
	 * and will get metadata from the pdf and if possible also the name of the PDF
	 * is set if that is known. Also additional metadata can be provided that will
	 * be attached to each segment.
	 *
	 * @param pdfSource             This can be JSFile or String represeting a file
	 *                              path, or byte[] representing the PDF content.
	 * @param maxSegmentSizeInChars How many characters per segment we can have.
	 * @param maxOverlapSizeInChars How many characters of overlap between segments.
	 * @param metaData              Metadata to attach to each segment.
	 * @return A Promise resolving to the store
	 */
	@JSFunction
	public NativePromise embed(Object pdfSource, int maxSegmentSizeInChars, int maxOverlapSizeInChars,
			Map<String, Object> metaData) {
		return provider.async(() -> {
			ApachePdfBoxDocumentParser parser = new ApachePdfBoxDocumentParser(true);
			Document document = parser.parse(pdfSource);
			if (metaData != null) {
				document.metadata().putAll(metaData);
			}

			DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(maxSegmentSizeInChars,
					maxOverlapSizeInChars);
			List<TextSegment> segments = splitter.split(document);
			Response<List<Embedding>> embeddings = model.embedAll(segments);
			List<Embedding> content = embeddings.content();
			embeddingStore.addAll(content, segments);
			return this;
		});
	}

	/**
	 * Performs a blocking similarity search for the given text, returning the best
	 * matches from the store.
	 *
	 * @param text       The query text to search for.
	 * @param maxResults The maximum number of results to return.
	 * @return An array of SearchResult objects representing the best matches.
	 */
	@JSFunction
	public SearchResult[] search(String text, int maxResults) {
		Embedding queryEmbedding = model.embed(text).content();
		EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding)
				.maxResults(maxResults).build();
		List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
		return matches.stream()
				.map(match -> new SearchResult(match.score(), match.embedded().text(), match.embedded().metadata()))
				.toArray(SearchResult[]::new);
	}
}