package com.servoy.extensions.aiplugin;

import com.servoy.j2db.plugins.IClientPluginAccess;

public class ProviderLoader
{
	private static final String CHAT_PACKAGE = "com.servoy.extensions.aiplugin.chat.";
	private static final String EMBEDDING_PACKAGE = "com.servoy.extensions.aiplugin.embedding.";

	public static boolean isAvailable(String markerClassName)
	{
		try
		{
			Class.forName(markerClassName);
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T createChatBuilder(String builderSimpleName, IClientPluginAccess access)
	{
		try
		{
			Class< ? > clazz = Class.forName(CHAT_PACKAGE + builderSimpleName);
			return (T)clazz.getDeclaredConstructor(IClientPluginAccess.class).newInstance(access);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException("Provider class not found: " + builderSimpleName, e);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to instantiate provider: " + builderSimpleName, e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T createEmbeddingBuilder(String builderSimpleName, AIProvider provider)
	{
		try
		{
			Class< ? > clazz = Class.forName(EMBEDDING_PACKAGE + builderSimpleName);
			return (T)clazz.getDeclaredConstructor(AIProvider.class).newInstance(provider);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException("Provider class not found: " + builderSimpleName, e);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to instantiate provider: " + builderSimpleName, e);
		}
	}

	public static void ensureAvailable(String markerClass, String providerName, String artifactName)
	{
		if (!isAvailable(markerClass))
		{
			throw new RuntimeException(
				providerName + " provider not installed. " +
					"Download 'servoy-ai-provider-" + artifactName + "' from the Servoy AI Plugin releases " +
					"and place the JARs in your Servoy plugins/ai/ directory.");
		}
	}
}
