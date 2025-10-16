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

@ServoyDocumented(scriptingName = "EmbeddingClient")
public class EmbeddingClient {
	// TODO should i shutdown this somewhere?
	static ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
	
	private final DimensionAwareEmbeddingModel model;
	private final IClientPluginAccess access;

	public EmbeddingClient(DimensionAwareEmbeddingModel model, IClientPluginAccess access) {
		this.model = model;
		this.access = access;
	}

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
	
	@JSFunction
	public EmbeddingStore createInMemoryStore() {
		InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
		return new EmbeddingStore(embeddingStore, model, access);
	}
	
	@JSFunction
	public EmbeddingStore createPgVectorStore(String serverName, String tableName) {
		try {
			DataSource dataSource = ((IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName)).getDataSource();
			PgVectorEmbeddingStore embeddingStore = PgVectorEmbeddingStore.datasourceBuilder().datasource(dataSource).table(tableName).dimension(model.dimension()).createTable(true).build();
			return new EmbeddingStore(embeddingStore, model, access);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
