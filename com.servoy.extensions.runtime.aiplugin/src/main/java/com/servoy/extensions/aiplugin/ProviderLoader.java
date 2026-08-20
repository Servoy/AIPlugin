package com.servoy.extensions.aiplugin;

public class ProviderLoader
{
	private static boolean isAvailable(String markerClassName)
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
