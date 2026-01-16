package com.servoy.extensions.aiplugin.embedding;

import static com.servoy.base.persistence.IBaseColumn.PK_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.USER_ROWID_COLUMN;
import static com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer.EMBEDDING_COLUMN;
import static com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer.EMBEDDING_ID_COLUMN;
import static com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer.TEXT_COLUMN;
import static com.servoy.j2db.util.DataSourceUtils.createDBTableDataSource;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;
import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.AIProvider;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.Debug;

import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;

@ServoyDocumented
public class ServoyEmbeddingStoreBuilder implements IJavaScriptType {
	/**
	 * The ai provider plugin.
	 */
	private final AIProvider provider;
	private final DimensionAwareEmbeddingModel model;

	private boolean recreate = false;
	private Boolean addText = null;
	private String dataSource;
	private String serverName;
	private String tableName;
	private List<MetaDataKey> metaDataKeys = null;

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
	 * Create a meta data column adder for adding a meta data column to this builder
	 * instance. Use the `add` method to add the column to this builder instance.
	 *
	 * @return A meta data column adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataColumnAdder metaDataColumn() {
		return new EmbeddingMetaDataColumnAdder(this);
	}

	/**
	 * Convenience method to add a TEXT meta data column to this builder. This is
	 * equivalent to metaDataColumn().name(name).add()
	 *
	 * @param name meta data column name.
	 * @return This builder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder metaDataColumn(String name) {
		new EmbeddingMetaDataColumnAdder(this).name(name).add();
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
	 * The meta data columns are based on the specified meta data columns.
	 * Alternatively, when a dataSource is specified, the key columns of that table
	 * are used as meta data columns.
	 *
	 * @return An EmbeddingStore backed by a servoy store, or null if creation
	 *         fails.
	 */
	@JSFunction
	public EmbeddingStore build() {
		try {
			deriveOptionsFromExistingTable();

			String localServerName = ensureNotBlank(
					serverName == null ? getDataSourceServerName(dataSource) : serverName,
					"either a dataSource or serverName (with metaDataColumns) must be specified");
			if (dataSource == null) {
				ensureTrue(metaDataKeys != null,
						"either a dataSource or serverName (with metaDataColumns) must be specified");
			} else if (metaDataKeys == null) {
				metaDataKeys = getSourceTableMetaDataKeys(dataSource);
			}

			String remoteServerName = provider.getDatabaseManager().getSwitchedToServerName(localServerName);
			ServoyEmbeddingStoreServer servoyEmbeddingStoreServer = provider.getAiPluginService()
					.servoyEmbeddingStoreFactory().create(remoteServerName, metaDataKeys, tableName, recreate, true,
							model.dimension(), TRUE.equals(addText));
			return new EmbeddingStore(provider, new ServoyEmbeddingStore(provider,
					createDBTableDataSource(localServerName, tableName), servoyEmbeddingStoreServer), model);
		} catch (Exception e) {
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Check if the embeddings table already exists, then apply all options that
	 * have not been set yet in this builder.
	 *
	 * @throws RepositoryException
	 */
	private void deriveOptionsFromExistingTable() throws RepositoryException {
		if (metaDataKeys != null && addText != null) {
			// all options we can derive have already been set
			return;
		}

		if (serverName == null) {
			serverName = getDataSourceServerName(dataSource);
		}
		if (serverName != null) {
			ITable existingTable = provider.getDatabaseManager()
					.getTable(createDBTableDataSource(serverName, tableName));
			if (existingTable != null) {
				List<Column> metaDataColumns = new ArrayList<>();
				boolean hasText = false;
				// Derive options from existing table
				for (Column column : existingTable.getColumns()) {
					switch (column.getName()) {
					case EMBEDDING_ID_COLUMN:
					case EMBEDDING_COLUMN:
						break;

					case TEXT_COLUMN:
						hasText = true;
						break;

					default: // not a fixed column, must be a meta data column
						metaDataColumns.add(column);
					}
				}
				if (metaDataKeys == null) {
					// metaDataKeys were not set yet, we use the columns we found in the existing
					// table
					metaDataKeys = metaDataColumns.stream().map(column -> new MetaDataKey(column.getSQLName(),
							column.getColumnType(), column.getFlags(), column.getAllowNull())).toList();
				}
				if (addText == null) {
					// addText option was not set yet
					addText = hasText;
				}
			}
		}
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
		if (metaDataKeys == null) {
			metaDataKeys = new ArrayList<>();
		}
		metaDataKeys.add(metaDataKey);
		return this;
	}
}