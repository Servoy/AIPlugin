package com.servoy.extensions.aiplugin;

import java.rmi.Remote;

import com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreFactory;

public interface AiPluginService extends Remote {
	String AIPLUGIN_SERVICE = "servoy.aiPluginService";

	ServoyEmbeddingStoreFactory servoyEmbeddingStoreFactory();
}
