package com.servoy.extensions.aiplugin;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.util.DataSourceUtils;

public class ServoyEmbeddingStoreBuilder {

    private final IServerAccess serverAccess;
    private String source;
    private String tableName;
    private boolean dropTableFirst = false;
    private boolean createTable = false;
    private int dimension = 0;

    public ServoyEmbeddingStoreBuilder(IServerAccess serverAccess) {
        this.serverAccess = serverAccess;
        validate(serverAccess != null, "serverAccess should not be null");
    }

    public ServoyEmbeddingStoreBuilder source(String dataSource) {
        this.source = dataSource;
        return this;
    }

    public ServoyEmbeddingStoreBuilder tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public ServoyEmbeddingStoreBuilder dropTableFirst(boolean dropTableFirst) {
        this.dropTableFirst = dropTableFirst;
        return this;
    }

    public ServoyEmbeddingStoreBuilder createTable(boolean createTable) {
        this.createTable = createTable;
        return this;
    }

    public ServoyEmbeddingStoreBuilder dimension(int dimension) {
        this.dimension = dimension;
        return this;
    }

    public ServoyEmbeddingStore build() {
        validate(DataSourceUtils.isDatasourceUri(source), "source should be a valid Servoy data source");
        validate(tableName != null, "tableName should not be null");
        validate(!createTable || dimension <= 0, "dimension should be specified when createTable is true");
        return new ServoyEmbeddingStore(serverAccess, source, tableName, dropTableFirst, createTable, dimension);
    }

    private static void validate(boolean b, String message) {
        if (!b) throw new IllegalArgumentException(message);
    }
}
