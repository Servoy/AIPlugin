package com.servoy.extensions.aiplugin.server;

import com.servoy.j2db.plugins.IServerAccess;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

import java.util.List;

public class ServoyEmbeddingStore implements dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> {
    private final IServerAccess serverAccess;
    private final String source;
    private final String tableName;
    private final boolean dropTableFirst;
    private final boolean createTable;
    private final int dimension;

    public ServoyEmbeddingStore(IServerAccess serverAccess, String source, String tableName, boolean dropTableFirst, boolean createTable, int dimension) {
        this.serverAccess = serverAccess;
        this.source = source;
        this.tableName = tableName;
        this.dropTableFirst = dropTableFirst;
        this.createTable = createTable;
        this.dimension = dimension;
    }

    @Override
    public String add(Embedding embedding) {
        return "";
    }

    @Override
    public void add(String id, Embedding embedding) {

    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        return "";
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return List.of();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        return null;
    }
}
