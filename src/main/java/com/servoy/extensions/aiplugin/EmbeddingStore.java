package com.servoy.extensions.aiplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IScriptable;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

/**
 * EmbeddingStore provides methods to embed text data and perform similarity search using embeddings.
 * It supports asynchronous embedding and blocking search, and can be backed by various embedding store implementations.
 */
@ServoyDocumented(scriptingName = "EmbeddingStore")
public class EmbeddingStore implements IScriptable, IJavaScriptType{

	/**
	 * The underlying embedding store implementation.
	 */
	private final dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> embeddingStore;
	/**
	 * The embedding model used for generating embeddings.
	 */
	private final DimensionAwareEmbeddingModel model;
	/**
	 * The client plugin access instance for Servoy scripting context.
	 */
	private final IClientPluginAccess access;
	/**
	 * Tracks the number of ongoing embedding operations.
	 */
	private final AtomicInteger processing = new AtomicInteger(0);

	/**
	 * Constructs an EmbeddingStore with the given embedding store, model, and plugin access.
	 * @param embeddingStore The embedding store implementation to use.
	 * @param model The embedding model to use.
	 * @param access The client plugin access instance.
	 */
	public EmbeddingStore(dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> embeddingStore, DimensionAwareEmbeddingModel model,
			IClientPluginAccess access) {
				this.embeddingStore = embeddingStore;
				this.model = model;
				this.access = access;
	}

	/**
	 * Asynchronously embeds an array of text data using the configured model and stores the results.
	 * @param data The array of text strings to embed.
	 */
	@JSFunction
	public void embed(String[] data) {
		embed(data, null);
	}
	
	/**
	 * Asynchronously embeds an array of text data with optional metadata and stores the results.
	 * @param data The array of text strings to embed.
	 * @param metaData An array of metadata maps, one for each text string (may be null).
	 */
	@JSFunction
	public void embed(String[] data, Map<String, Object>[] metaData) {
		if (data != null &&  data.length > 0) {
			processing.incrementAndGet();
			EmbeddingClient.virtualThreadExecutor.submit(() -> {
				try {
					List<TextSegment> segments = new ArrayList<>();
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
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				finally {
					processing.decrementAndGet();
					synchronized (processing) {
						processing.notifyAll();
					}
				}
			});
		}
	}
	
	/**
	 * Performs a blocking similarity search for the given text, returning the best matches from the store.
	 * Waits for all ongoing embedding operations to complete before searching.
	 * @param text The query text to search for.
	 * @param maxResults The maximum number of results to return.
	 * @return An array of SearchResult objects representing the best matches.
	 */
	@JSFunction
	public SearchResult[] search(String text, int maxResults) {
		while (processing.get() > 0) {
			synchronized (processing) {
				try {
					processing.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
        Embedding queryEmbedding = model.embed(text).content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
        
        if (matches.size() > 0)	{
        	List<SearchResult> collect = matches.stream().map(m -> new SearchResult(m.score(), m.embedded().text(), m.embedded().metadata())).collect(Collectors.toList());
        	return collect.toArray(new SearchResult[collect.size()]);
        }
        return new SearchResult[0];
	}

}