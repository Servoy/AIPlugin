package com.servoy.extensions.aiplugin.embedding;

import java.util.List;

import com.servoy.extensions.aiplugin.server.MetaDataKey;
import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStoreServer;
import com.servoy.j2db.plugins.IServerAccess;

import dev.langchain4j.data.segment.TextSegment;

public class ServoyEmbeddingStoreFactory {

	private final IServerAccess serverAccess;

	public ServoyEmbeddingStoreFactory(IServerAccess serverAccess) {
		this.serverAccess = serverAccess;
	}

	public dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> create(String clientId, String serverName,
			List<MetaDataKey> metaDataKeys, String tableName, boolean dropTableFirst, boolean createTable,
			int dimension, boolean addText) throws Exception {

		return ServoyEmbeddingStoreServer.createStore(serverAccess, clientId, serverName, metaDataKeys, tableName,
				dropTableFirst, createTable, dimension, addText);
	}
}
