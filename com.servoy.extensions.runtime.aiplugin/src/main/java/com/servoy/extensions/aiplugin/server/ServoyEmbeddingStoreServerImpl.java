package com.servoy.extensions.aiplugin.server;

import static com.servoy.base.persistence.IBaseColumn.NATIVE_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.UUID_COLUMN;
import static com.servoy.base.persistence.IBaseColumn.VECTOR_COLUMN;
import static com.servoy.extensions.aiplugin.database.DatabaseHandler.DATABASE_HANDLER;
import static com.servoy.j2db.dataprocessing.BufferedDataSetInternal.createBufferedDataSet;
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

import com.servoy.extensions.aiplugin.embedding.MetaDataKey;
import com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

public class ServoyEmbeddingStoreServerImpl implements ServoyEmbeddingStoreServer {
	private final IServerAccess serverAccess;
	private final TableModel tableModel;

	private ServoyEmbeddingStoreServerImpl(IServerAccess serverAccess, TableModel tableModel) {
		this.serverAccess = serverAccess;
		this.tableModel = tableModel;
	}

	public static ServoyEmbeddingStoreServer createStore(IServerAccess serverAccess, String remoteServerName,
			List<MetaDataKey> metaDataKeys, String tableName, boolean dropTableFirst, boolean createTable,
			int dimension, boolean addText) throws Exception {
		ensureNotNull(remoteServerName, "remoteServerName");
		ensureNotNull(metaDataKeys, "metaDataKeys");
		ensureNotNull(tableName, "tableName");
		if (createTable) {
			ensureGreaterThanZero(dimension, "dimension");
		}

		var tableModel = initializeStore(serverAccess, remoteServerName, metaDataKeys, tableName, dropTableFirst,
				createTable, dimension, addText);
		return new ServoyEmbeddingStoreServerImpl(serverAccess, tableModel);
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

	@Override
	public void addEmbeddings(String clientId, String transactionId, List<String> ids, List<Embedding> embeddings,
			List<TextSegment> embedded) throws ServoyException {
		boolean hasText = tableModel.columnTypes().containsKey(TEXT_COLUMN);
		var columnNames = new ArrayList<String>();
		var metaDataColumnNames = new ArrayList<String>();
		var columnTypes = new ArrayList<ColumnType>();
		var metaDataColumnTypes = new ArrayList<ColumnType>();

		// Embedding columns
		addColumn(EMBEDDING_ID_COLUMN, columnNames, columnTypes);
		addColumn(EMBEDDING_COLUMN, columnNames, columnTypes);

		// Meta Data columns
		tableModel.metaDataKeys().forEach(metaDataKey -> {
			addColumn(metaDataKey.name(), columnNames, columnTypes);
			addColumn(metaDataKey.name(), metaDataColumnNames, metaDataColumnTypes);
		});

		// Text column
		if (hasText) {
			addColumn(TEXT_COLUMN, columnNames, columnTypes);
		}

		// Rows
		var rows = new ArrayList<Object[]>();
		var oldSourceIds = new ArrayList<Object[]>();
		for (int i = 0; i < ids.size(); i++) {
			var id = ids.get(i);
			var embedding = embeddings.get(i);
			TextSegment textSegment;
			Metadata metadata;
			if (embedded == null) {
				textSegment = null;
				metadata = null;
			} else {
				textSegment = embedded.get(i);
				metadata = textSegment.metadata();
			}

			var row = new ArrayList<>();
			var metaDataValues = new ArrayList<>();
			row.add(id);
			row.add(embedding.vector());
			tableModel.metaDataKeys().forEach(metaDataKey -> {
				Object metadataValue = getMetadataValue(metadata, metaDataKey.name());
				metaDataValues.add(metadataValue);
				row.add(metadataValue);
			});

			if (hasText) {
				row.add(textSegment == null ? null : textSegment.text());
			}

			rows.add(row.toArray());
			oldSourceIds.add(metaDataValues.toArray());
		}

		var dataSet = createBufferedDataSet(columnNames.toArray(String[]::new), columnTypes.toArray(ColumnType[]::new),
				rows, false);
		if (!tableModel.wasCreated()) {
			var oldSourceIdsDataset = createBufferedDataSet(metaDataColumnNames.toArray(String[]::new),
					metaDataColumnTypes.toArray(ColumnType[]::new), oldSourceIds, false);
			serverAccess.deleteFromDataSet(clientId, tableModel.serverName(), tableModel.tableName(), transactionId,
					oldSourceIdsDataset);
		}
		serverAccess.insertDataSet(clientId, tableModel.serverName(), tableModel.tableName(), transactionId, dataSet);
	}

	private void addColumn(String columnName, ArrayList<String> columnNames, ArrayList<ColumnType> columnTypes) {
		columnNames.add(columnName);
		columnTypes.add(tableModel.columnTypes().get(columnName));
	}

	private Object getMetadataValue(Metadata metadata, String columnName) {
		if (metadata == null || !metadata.containsKey(columnName)) {
			return null;
		}
		var columnType = ensureNotNull(tableModel.columnTypes().get(columnName), "Missing column %s", columnName);
		return switch (Column.mapToDefaultType(columnType)) {
		case IColumnTypes.TEXT, IColumnTypes.DATETIME -> metadata.getString(columnName);
		case IColumnTypes.NUMBER -> metadata.getDouble(columnName);
		case IColumnTypes.INTEGER -> metadata.getLong(columnName);
		case IColumnTypes.MEDIA -> metadata.getUUID(columnName);
		default -> throw new IllegalArgumentException(
				"Could not get metadata value for column " + columnName + ", type " + columnType);
		};
	}
}
