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
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

@ServoyDocumented(scriptingName = "EmbeddingStore")
public class EmbeddingStore implements IScriptable, IJavaScriptType{

	private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
	private final DimensionAwareEmbeddingModel model;
	private final IClientPluginAccess access;
	
	private final AtomicInteger processing = new AtomicInteger(0);

	public EmbeddingStore(InMemoryEmbeddingStore<TextSegment> embeddingStore, DimensionAwareEmbeddingModel model,
			IClientPluginAccess access) {
				this.embeddingStore = embeddingStore;
				this.model = model;
				this.access = access;
	}

	@JSFunction
	public void embed(String[] data) {
		embed(data, null);
	}
	
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
