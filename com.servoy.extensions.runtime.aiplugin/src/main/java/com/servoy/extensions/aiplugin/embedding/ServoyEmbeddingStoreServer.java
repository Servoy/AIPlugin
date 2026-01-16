package com.servoy.extensions.aiplugin.embedding;

import java.util.List;

import com.servoy.j2db.util.ServoyException;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

public interface ServoyEmbeddingStoreServer {
	String EMBEDDING_ID_COLUMN = "embedding_id";
	String EMBEDDING_COLUMN = "embedding";
	String TEXT_COLUMN = "text";

	void addEmbeddings(String clientId, String transactionId, List<String> ids, List<Embedding> embeddings,
			List<TextSegment> embedded) throws ServoyException;
}
