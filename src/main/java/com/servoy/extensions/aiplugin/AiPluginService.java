package com.servoy.extensions.aiplugin;

import java.rmi.Remote;

public interface AiPluginService extends Remote {
	String AIPLUGIN_SERVICE = "servoy.aiPluginService";

	ServoyEmbeddingStoreFactory embeddingStoreFactory();
}
