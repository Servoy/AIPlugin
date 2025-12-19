package com.servoy.extensions.aiplugin;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore;
import com.servoy.j2db.plugins.IServerAccess;

public class ServoyEmbeddingStoreFactory {

	private final IServerAccess serverAccess;

	public ServoyEmbeddingStoreFactory(IServerAccess serverAccess) {
		this.serverAccess = serverAccess;
	}

	public ServoyEmbeddingStore create(String clientId, String serverName, String sourceTableName, String tableName,
			boolean dropTableFirst, boolean createTable, int dimension, boolean addText) throws Exception {
		return com.servoy.extensions.aiplugin.server.ServoyEmbeddingStoreFactory.createStore(serverAccess, clientId,
				serverName, sourceTableName, tableName, dropTableFirst, createTable, dimension, addText);
	}
}
