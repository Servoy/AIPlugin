package com.servoy.extensions.aiplugin;

import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceTableName;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Debug;

import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;

public class ServoyEmbeddingStoreBuilder implements IJavaScriptType{
	/**
	 * The ai provider plugin.
	 */
	private final AIProvider provider;
	private final DimensionAwareEmbeddingModel model;

	private boolean recreate = false;
	private boolean addText = false;
	private String dataSource;
	private String tableName;

	/**
	 * Constructs a GeminiEmbeddingModelBuilder with the given plugin access.
	 *
	 * @param provider ai provider plugin.
	 * @param model    embedding model.
	 */
	public ServoyEmbeddingStoreBuilder(AIProvider provider, DimensionAwareEmbeddingModel model) {
		this.provider = provider;
		this.model = model;
	}

	/**
	 * Sets the recreate option (remove persistent storage) before opening the
	 * store.
	 *
	 * @param recreate recreate option.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder recreate(boolean recreate) {
		this.recreate = recreate;
		return this;
	}

	/**
	 * Sets the addText option (store original text).
	 *
	 * @param addText addText option.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder addText(boolean addText) {
		this.addText = addText;
		return this;
	}

	/**
	 * Sets the dataSource to read data from.
	 *
	 * @param dataSource dataSource.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder dataSource(String dataSource) {
		this.dataSource = dataSource;
		return this;
	}

	/**
	 * Sets the tableName to store the embeddings in.
	 *
	 * @param tableName tableName.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder tableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	/**
	 * Creates a Servoy embedding store for the specified source table, the
	 * emmbeddings will be saved in the specified table in the same server.
	 *
	 * @return An EmbeddingStore backed by a servoy store, or null if creation
	 *         fails.
	 */
	@JSFunction
	public EmbeddingStore build() {
		try {
			String serverName = getDataSourceServerName(dataSource);
			String sourceTableName = getDataSourceTableName(dataSource);
			String remoteServerName = provider.getDatabaseManager().getSwitchedToServerName(serverName);

			ServoyEmbeddingStore embeddingStore = provider.getAiPluginService().embeddingStoreFactory().create(
					provider.getClientID(), remoteServerName, sourceTableName, tableName, recreate, true,
					model.dimension(), addText);
			return new EmbeddingStore(provider, embeddingStore, model);
		} catch (Exception e) {
			Debug.error(e);
		}
		return null;
	}
}