package com.servoy.extensions.aiplugin.embedding;

import java.util.List;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStoreServerImpl;
import com.servoy.j2db.plugins.IServerAccess;

public class ServoyEmbeddingStoreFactory {

	private final IServerAccess serverAccess;

	public ServoyEmbeddingStoreFactory(IServerAccess serverAccess) {
		this.serverAccess = serverAccess;
	}

	public ServoyEmbeddingStoreServer create(String remoteServerName, List<MetaDataKey> metaDataKeys, String tableName,
			boolean dropTableFirst, boolean createTable, int dimension, boolean addText) throws Exception {
		return ServoyEmbeddingStoreServerImpl.createStore(serverAccess, remoteServerName, metaDataKeys, tableName,
				dropTableFirst, createTable, dimension, addText);
	}
}
