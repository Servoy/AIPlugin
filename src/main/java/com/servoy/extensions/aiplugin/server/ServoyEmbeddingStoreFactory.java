package com.servoy.extensions.aiplugin.server;

import static com.servoy.base.persistence.IBaseColumn.NATIVE_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.PK_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.USER_ROWID_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.UUID_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.VECTOR_COLUMN;
import static com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.EMBEDDING_COLUMN;
import static com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.EMBEDDING_ID_COLUMN;
import static com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.TEXT_COLUMN;
import static com.servoy.j2db.persistence.IColumnTypes.MEDIA;
import static com.servoy.j2db.persistence.IColumnTypes.TEXT;
import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.TableModel;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.DataSourceUtils;

public class ServoyEmbeddingStoreFactory {

	public static ServoyEmbeddingStore createStore(IServerAccess serverAccess, String clientId, String source,
			String tableName, boolean dropTableFirst, boolean createTable, int dimension, boolean addText)
			throws Exception {
		ensureNotNull(clientId, "clientId");
		ensureTrue(DataSourceUtils.isDatasourceUri(source), "source should be a valid Servoy data source");
		ensureNotNull(tableName, "tableName");
		if (createTable)
			ensureGreaterThanZero(dimension, "dimension");

		var tableModel = initializeStore(serverAccess, source, tableName, dropTableFirst, createTable, dimension,
				addText);
		return new ServoyEmbeddingStore(serverAccess, clientId, null, tableModel);
	}

	private static TableModel initializeStore(IServerAccess serverAccess, String source, String tableName,
			boolean dropTableFirst, boolean createTable, int dimension, boolean addText) throws Exception {
		var serverName = DataSourceUtils.getDataSourceServerName(source);
		var server = (IServerInternal) ensureNotNull(serverAccess.getDBServer(serverName, true, true),
				"Cannot find server from source %s", source);

		var sourceTable = ensureNotNull(server.getTable(DataSourceUtils.getDataSourceTableName(source)),
				"Cannot find source table %s", source);
		var sourcePkColumns = sourceTable.getRowIdentColumns();
		ensureTrue(!sourcePkColumns.isEmpty(), "Cannot work without PK column on source table " + source);

		var table = server.getTable(tableName);
		ensureTrue(table != null || createTable, "Cannot find source table " + tableName + " in server " + serverName);

		if (table != null && dropTableFirst) {
			server.removeTable(table);
			table = null;
		}

		if (table == null) {
			table = createTable(tableName, server, sourcePkColumns, dimension, addText);
		}

		return verifyTable(table, sourcePkColumns, addText);
	}

	private static ITable createTable(String tableName, IServerInternal server, List<Column> sourcePkColumns,
			int dimension, boolean addText) throws RepositoryException, SQLException {
		var table = server.createNewTable(DummyValidator.INSTANCE, tableName);

		// Embedding PK
		var pkColumn = table.createNewColumn(DummyValidator.INSTANCE, EMBEDDING_ID_COLUMN,
				ColumnType.getInstance(MEDIA, 16, 0), false, true);
		pkColumn.setFlag(NATIVE_COLUMN, true);
		pkColumn.setFlag(UUID_COLUMN, true);

		// Source pks
		var sourceRefColumns = new ArrayList<Column>();
		for (var sourcePkColumn : sourcePkColumns) {
			var sourceRefColumn = table.createNewColumn(DummyValidator.INSTANCE, sourcePkColumn.getSQLName(),
					sourcePkColumn.getColumnType(), false, false);
			sourceRefColumn.setFlag(sourcePkColumn.getFlags(), true);
			sourceRefColumn.setFlag(PK_COLUMN, false);
			sourceRefColumn.setFlag(USER_ROWID_COLUMN, false);

			sourceRefColumns.add(sourcePkColumn);
		}

		// Embedding
		var embeddingColumn = table.createNewColumn(DummyValidator.INSTANCE, EMBEDDING_COLUMN,
				ColumnType.getInstance(MEDIA, dimension, 0), false);
		embeddingColumn.setFlag(NATIVE_COLUMN, true);
		embeddingColumn.setFlag(VECTOR_COLUMN, true);

		// Text
		if (addText) {
			table.createNewColumn(DummyValidator.INSTANCE, TEXT_COLUMN, ColumnType.getColumnType(TEXT), true);
		}

		// Actually create the table
		server.syncTableObjWithDB(table, false, false);

		// Index on source ref
		server.createIndex(table, "_sv_embedding_meta_" + tableName , sourceRefColumns.toArray(Column[]::new), false);

		return table;
	}

	private static TableModel verifyTable(ITable table, List<Column> sourcePkColumns, boolean addText) {
		var columnTypes = new HashMap<String, ColumnType>();

		var pkColumn = ensureNotNull(table.getColumn(EMBEDDING_ID_COLUMN),
				"Table not usable as for embedding store: Missing pk column: %s", EMBEDDING_ID_COLUMN);
		ensureTrue(pkColumn.isDatabasePK(),
				"Table not usable as for embedding store: Column is not PK: " + EMBEDDING_ID_COLUMN);
		ensureTrue(pkColumn.hasFlag(UUID_COLUMN),
				"Table not usable as for embedding storee: Column is not UUID: " + EMBEDDING_ID_COLUMN);
		columnTypes.put(EMBEDDING_ID_COLUMN, pkColumn.getColumnType());

		var sourceColumns = new ArrayList<String>();
		for (var sourcePkColumn : sourcePkColumns) {
			var column = ensureNotNull(table.getColumn(sourcePkColumn.getName()),
					"Table not usable as for embedding store: Missing reference column %s", sourcePkColumn.getName());
			sourceColumns.add(column.getName());
			columnTypes.put(column.getName(), column.getColumnType());
		}

		var embeddingColumn = ensureNotNull(table.getColumn(EMBEDDING_COLUMN),
				"Table not usable as for embedding store: Missing embedding column: %d", EMBEDDING_COLUMN);
		ensureTrue(embeddingColumn.hasFlag(VECTOR_COLUMN),
				"Table not usable as for embedding store: Embedding column not a vector: " + EMBEDDING_COLUMN);
		columnTypes.put(EMBEDDING_COLUMN, embeddingColumn.getColumnType());

		if (addText) {
			var textColumn = ensureNotNull(table.getColumn(TEXT_COLUMN),
					"Table not usable as for embedding store: Missing text column: %s", TEXT_COLUMN);
			ensureTrue(textColumn.getDataProviderType() == TEXT,
					"Table not usable as for embedding store: Text column is not a text column");
			columnTypes.put(TEXT_COLUMN, textColumn.getColumnType());
		}

		return new TableModel(table.getServerName(), table.getName(), columnTypes, sourceColumns);
	}
}
