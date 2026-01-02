package com.servoy.extensions.aiplugin.server;

import static com.servoy.base.persistence.IBaseColumn.NATIVE_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.UUID_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.VECTOR_COLUMN;
import static com.servoy.extensions.aiplugin.database.DatabaseHandler.DATABASE_HANDLER;
import static com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.EMBEDDING_COLUMN;
import static com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.EMBEDDING_ID_COLUMN;
import static com.servoy.extensions.aiplugin.server.ServoyEmbeddingStore.TEXT_COLUMN;
import static com.servoy.j2db.persistence.IColumnTypes.MEDIA;
import static com.servoy.j2db.persistence.IColumnTypes.TEXT;
import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;
import static java.lang.Integer.parseInt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Debug;

import dev.langchain4j.data.segment.TextSegment;

public class ServoyEmbeddingStoreServer {

	public static dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> createStore(IServerAccess serverAccess,
			String clientId, String serverName, List<MetaDataKey> metaDataKeys, String tableName,
			boolean dropTableFirst, boolean createTable, int dimension, boolean addText) throws Exception {
		ensureNotNull(clientId, "clientId");
		ensureNotNull(serverName, "serverName");
		ensureNotNull(metaDataKeys, "metaDataKeys");
		ensureNotNull(tableName, "tableName");
		if (createTable)
			ensureGreaterThanZero(dimension, "dimension");

		var tableModel = initializeStore(serverAccess, serverName, metaDataKeys, tableName, dropTableFirst, createTable,
				dimension, addText);
		return new ServoyEmbeddingStore(serverAccess, clientId, null, tableModel);
	}

	private static TableModel initializeStore(IServerAccess serverAccess, String serverName,
			List<MetaDataKey> metaDataKeys, String tableName, boolean dropTableFirst, boolean createTable,
			int dimension, boolean addText) throws Exception {
		var server = (IServerInternal) ensureNotNull(serverAccess.getDBServer(serverName, true, true),
				"Cannot find server %s", serverName);

		var table = server.getTable(tableName);
		ensureTrue(table != null || createTable,
				"Cannot find embeddings table " + tableName + " in server " + serverName);

		if (table != null && dropTableFirst) {
			server.removeTable(table);
			table = null;
		}

		boolean wasCreated = false;
		if (table == null) {
			table = createTable(tableName, serverAccess, server, metaDataKeys, dimension, addText);
			wasCreated = true;
		}

		return verifyTable(table, metaDataKeys, addText, wasCreated);
	}

	private static ITable createTable(String tableName, IServerAccess serverAccess, IServerInternal server,
			List<MetaDataKey> metaDataKeys, int dimension, boolean addText) throws RepositoryException, SQLException {
		var table = server.createNewTable(DummyValidator.INSTANCE, tableName);

		// Embedding PK
		var pkColumn = table.createNewColumn(DummyValidator.INSTANCE, EMBEDDING_ID_COLUMN,
				ColumnType.getInstance(MEDIA, 16, 0), false, true);
		pkColumn.setFlag(NATIVE_COLUMN, true);
		pkColumn.setFlag(UUID_COLUMN, true);

		var metaDataColumns = new ArrayList<Column>();
		for (var metaDataKey : metaDataKeys) {
			var metaDataColumn = table.createNewColumn(DummyValidator.INSTANCE, metaDataKey.name(),
					metaDataKey.columnType(), metaDataKey.allowNull(), false);
			metaDataColumn.setFlags(metaDataKey.flags());
			metaDataColumns.add(metaDataColumn);
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

		// Index on metadata columns
		server.createIndex(table, "_sv_embedding_meta_" + tableName, metaDataColumns.toArray(Column[]::new), false);

		// Index on embedding columns
		try {
			int embeddingListSize = parseInt(
					serverAccess.getSettings().getProperty("servoy.aiplugin.embedding_list_size", "500"));
			if (!DATABASE_HANDLER.createEmbeddingIndex(server.getConnection(), table, "_sv_embedding_" + tableName,
					embeddingColumn, embeddingListSize)) {
				// no native support, try Servoy index
				server.createIndex(table, "_sv_embedding_" + tableName, new Column[] { embeddingColumn }, false);
			}
		} catch (Exception e) {
			Debug.log("Failed to create index on embedding -- continuing without index", e);
		}

		return table;
	}

	private static TableModel verifyTable(ITable table, List<MetaDataKey> metaDataKeys, boolean addText,
			boolean wasCreated) {
		var columnTypes = new HashMap<String, ColumnType>();

		var pkColumn = ensureNotNull(table.getColumn(EMBEDDING_ID_COLUMN),
				"Table not usable as for embedding store: Missing pk column: %s", EMBEDDING_ID_COLUMN);
		ensureTrue(pkColumn.isDatabasePK(),
				"Table not usable as for embedding store: Column is not PK: " + EMBEDDING_ID_COLUMN);
		ensureTrue(pkColumn.hasFlag(UUID_COLUMN),
				"Table not usable as for embedding store: Column is not UUID: " + EMBEDDING_ID_COLUMN);
		columnTypes.put(EMBEDDING_ID_COLUMN, pkColumn.getColumnType());

		for (var metaDataKey : metaDataKeys) {
			var column = ensureNotNull(table.getColumn(metaDataKey.name()),
					"Table not usable as for embedding store: Missing reference column %s", metaDataKey.name());
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

		return new TableModel(table.getServerName(), table.getName(), columnTypes, metaDataKeys, wasCreated);
	}
}
