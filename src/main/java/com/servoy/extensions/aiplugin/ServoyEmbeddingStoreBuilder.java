package com.servoy.extensions.aiplugin;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore;
import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStoreFactory;
import com.servoy.j2db.plugins.IServerAccess;

public class ServoyEmbeddingStoreBuilder {

	private final IServerAccess serverAccess;
	private String clientId;
	private String serverName;
	private String sourceTableName;
	private String tableName;
	private boolean dropTableFirst = false;
	private boolean createTable = false;
	private int dimension = 0;
	private boolean addText = false;

	public ServoyEmbeddingStoreBuilder(IServerAccess serverAccess) {
		this.serverAccess = serverAccess;
	}

	public ServoyEmbeddingStoreBuilder serverName(String serverName) {
		this.serverName = serverName;
		return this;
	}

	public ServoyEmbeddingStoreBuilder sourceTableName(String sourceTableName) {
		this.sourceTableName = sourceTableName;
		return this;
	}

	public ServoyEmbeddingStoreBuilder clientId(String clientId) {
		this.clientId = clientId;
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

	public ServoyEmbeddingStoreBuilder addText(boolean addText) {
		this.addText = addText;
		return this;
	}

	public ServoyEmbeddingStore build() throws Exception {
		return ServoyEmbeddingStoreFactory.createStore(serverAccess, clientId, serverName, sourceTableName, tableName,
				dropTableFirst, createTable, dimension, addText);
	}
}
