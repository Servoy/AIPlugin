package com.servoy.extensions.aiplugin;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.util.List;

import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * EmbeddingModel provides methods to generate embeddings for text and manage
 * embedding stores. It supports in-memory and Servoy (pgvector) embedding
 * stores.
 */
@ServoyDocumented
public class EmbeddingModel implements IJavaScriptType {

	/**
	 * The embedding model used for generating embeddings.
	 */
	private final DimensionAwareEmbeddingModel model;
	/**
	 * The client plugin access instance for Servoy scripting context.
	 */
	private final AIProvider provider;

	/**
	 * Constructs an EmbeddingModel with the given embedding model and plugin
	 * access.
	 *
	 * @param model    The embedding model to use.
	 * @param provider ai plugin remote service.
	 */
	public EmbeddingModel(DimensionAwareEmbeddingModel model, AIProvider provider) {
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
	 * the foundset.sort(vectorColumn, embeddingModel.embed("text"), maxRows);
	 *
	 * @param text The text string to create embeddings for.
	 * @return The embedding as a float array, or null if input is empty.
	 */
	@JSFunction
	public float[] embedding(String text) {
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
	public NativePromise embedding(String[] texts) {
		return provider.async((texts == null || texts.length == 0) ? null : () -> {
			List<TextSegment> segments = stream(texts).map(TextSegment::textSegment).collect(toList());
			Response<List<Embedding>> embeddings = model.embedAll(segments);
			List<Embedding> content = embeddings.content();
			return content.stream().map(Embedding::vector).toArray(float[][]::new);
		});
	}

	/**
	 * Creates an in-memory embedding store for storing and retrieving embeddings.
	 *
	 * @return An EmbeddingStore backed by an in-memory store.
	 */
	@JSFunction
	public EmbeddingStore createInMemoryStore() {
		return new EmbeddingStore(provider, new InMemoryEmbeddingStore<>(), model);
	}

	/**
	 * Creates a builder for servoy embedding stores.
	 *
	 * @return ServoyEmbeddingStoreBuilder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder createServoyEmbeddingStoreBuilder() {
		return new ServoyEmbeddingStoreBuilder(provider, model);
	}

}