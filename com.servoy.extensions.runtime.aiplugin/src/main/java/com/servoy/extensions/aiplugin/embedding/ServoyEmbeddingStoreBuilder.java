package com.servoy.extensions.aiplugin.embedding;

import static com.servoy.base.persistence.IBaseColumn.PK_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.USER_ROWID_COLUMN;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.AIProvider;
import com.servoy.extensions.aiplugin.server.MetaDataKey;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Debug;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;

@ServoyDocumented
public class ServoyEmbeddingStoreBuilder implements IJavaScriptType {
	/**
	 * The ai provider plugin.
	 */
	private final AIProvider provider;
	private final DimensionAwareEmbeddingModel model;

	private boolean recreate = false;
	private boolean addText = false;
	private String dataSource;
	private String serverName;
	private String tableName;
	private final List<MetaDataKey> metaDataKeys = new ArrayList<>();

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
	 * Create a metaKey adder for adding a meta data key to this builder instance.
	 * Use the `add` method to add the key to this builder instance.
	 *
	 * @return A meta data key adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataKeyAdder metaKey() {
		return new EmbeddingMetaDataKeyAdder(this);
	}

	/**
	 * Convenience method to add a TEXT meta data key to this builder. This is
	 * equivalent to metaKey().name(name).add()
	 *
	 * @param name meta data key name.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder metaKey(String name) {
		new EmbeddingMetaDataKeyAdder(this).name(name).add();
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
	 * Sets the serverName to store the embeddings in. Defaults to same server as
	 * `dataSource` server.
	 *
	 * @param serverName serverName.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder serverName(String serverName) {
		this.serverName = serverName;
		return this;
	}

	/**
	 * Creates a Servoy embedding store for the specified table name.
	 * <p>
	 * The meta data columns are based on the specified meta data keys.
	 * Alternatively, when a dataSource is specified, the key columns of that table
	 * are used as meta data columns.
	 *
	 * @return An EmbeddingStore backed by a servoy store, or null if creation
	 *         fails.
	 */
	@JSFunction
	public EmbeddingStore build() {
		try {
			if (dataSource == null) {
				ensureNotBlank(serverName, "either a dataSource or serverName (with metaDataKeys) must be specified");
				ensureTrue(!metaDataKeys.isEmpty(),
						"either a dataSource or serverName (with metaDataKeys) must be specified");
			} else if (metaDataKeys.isEmpty()) {
				metaDataKeys.addAll(getSourceTableMetaDataKeys(dataSource));
			}

			String remoteServerName = provider.getDatabaseManager()
					.getSwitchedToServerName(serverName == null ? getDataSourceServerName(dataSource) : serverName);
			dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> embeddingStore = provider.getAiPluginService()
					.servoyEmbeddingStoreFactory().create(provider.getClientID(), remoteServerName, metaDataKeys,
							tableName, recreate, true, model.dimension(), addText);
			return new EmbeddingStore(provider, embeddingStore, model);
		} catch (Exception e) {
			Debug.error(e);
		}
		return null;
	}

	private List<MetaDataKey> getSourceTableMetaDataKeys(String dataSource) throws RepositoryException {
		var sourceTable = ensureNotNull(provider.getDatabaseManager().getTable(dataSource),
				"Cannot find source table %s", dataSource);
		var sourcePkColumns = sourceTable.getRowIdentColumns();
		ensureTrue(!sourcePkColumns.isEmpty(),
				"Cannot work without PK column on source table " + sourceTable.getName());
		return sourcePkColumns.stream().map(sourcePkColumn -> new MetaDataKey(sourcePkColumn.getSQLName(),
				sourcePkColumn.getColumnType(), sourcePkColumn.getFlags() & ~(PK_COLUMN | USER_ROWID_COLUMN), false))
				.toList();
	}

	ServoyEmbeddingStoreBuilder addMetaDataKey(MetaDataKey metaDataKey) {
		metaDataKeys.add(metaDataKey);
		return this;
	}
}