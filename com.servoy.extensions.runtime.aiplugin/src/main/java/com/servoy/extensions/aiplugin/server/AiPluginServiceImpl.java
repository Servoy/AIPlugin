package com.servoy.extensions.aiplugin.server;

import com.servoy.extensions.aiplugin.AiPluginService;
import com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreFactory;
import com.servoy.j2db.plugins.IServerAccess;

public class AiPluginServiceImpl implements AiPluginService {
	private final IServerAccess serverAccess;

	public AiPluginServiceImpl(IServerAccess serverAccess) {
		this.serverAccess = serverAccess;
	}

	@Override
	public ServoyEmbeddingStoreFactory servoyEmbeddingStoreFactory() {
		return new ServoyEmbeddingStoreFactory(serverAccess);
	}
}
