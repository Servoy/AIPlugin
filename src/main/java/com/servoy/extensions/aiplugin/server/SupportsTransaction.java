package com.servoy.extensions.aiplugin.server;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface SupportsTransaction<T extends EmbeddingStore<TextSegment>> {

	T withTransactionId(String transactionId);
}
