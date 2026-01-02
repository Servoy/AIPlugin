package com.servoy.extensions.aiplugin.server;

import static com.servoy.j2db.dataprocessing.BufferedDataSetInternal.createBufferedDataSet;
import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.ServoyException;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class ServoyEmbeddingStore implements EmbeddingStore<TextSegment>, SupportsTransaction<ServoyEmbeddingStore> {
	public static final String EMBEDDING_ID_COLUMN = "embedding_id";
	public static final String EMBEDDING_COLUMN = "embedding";
	public static final String TEXT_COLUMN = "text";

	private final IServerAccess serverAccess;
	private final String clientId;
	private final TableModel tableModel;
	private final String transactionId;

	ServoyEmbeddingStore(IServerAccess serverAccess, String clientId, String transactionId, TableModel tableModel) {
		this.serverAccess = ensureNotNull(serverAccess, "serverAccess");
		this.clientId = ensureNotNull(clientId, "clientId");
		this.transactionId = transactionId;
		this.tableModel = ensureNotNull(tableModel, "tableModel");
	}

	public ServoyEmbeddingStore withTransactionId(String transactionId) {
		return Objects.equals(transactionId, this.transactionId) ? this
				: new ServoyEmbeddingStore(serverAccess, clientId, transactionId, tableModel);
	}

	@Override
	public String add(Embedding embedding) {
		return addSingle(randomUUID(), embedding, null);
	}

	@Override
	public void add(String id, Embedding embedding) {
		addSingle(id, embedding, null);
	}

	@Override
	public String add(Embedding embedding, TextSegment textSegment) {
		return addSingle(randomUUID(), embedding, textSegment);
	}

	private String addSingle(String id, Embedding embedding, TextSegment textSegment) {
		addAll(List.of(id), List.of(embedding), textSegment == null ? null : List.of(textSegment));
		return id;
	}

	@Override
	public List<String> addAll(List<Embedding> embeddings) {
		List<String> ids = generateIds(embeddings.size());
		addAll(ids, embeddings, null);
		return ids;
	}

	@Override
	public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
		ensureTrue(ids.size() == embeddings.size(), "ids and embeddings differ in size");
		ensureTrue(embedded == null || ids.size() == embedded.size(), "ids and embedded differ in size");
		if (ids.isEmpty()) {
			return;
		}

		try {
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

			var dataSet = createBufferedDataSet(columnNames.toArray(String[]::new),
					columnTypes.toArray(ColumnType[]::new), rows, false);
			if (!tableModel.wasCreated()) {
				var oldSourceIdsDataset = createBufferedDataSet(metaDataColumnNames.toArray(String[]::new),
						metaDataColumnTypes.toArray(ColumnType[]::new), oldSourceIds, false);
				serverAccess.deleteFromDataSet(clientId, tableModel.serverName(), tableModel.tableName(), transactionId,
						oldSourceIdsDataset);
			}
			serverAccess.insertDataSet(clientId, tableModel.serverName(), tableModel.tableName(), transactionId,
					dataSet);
		} catch (ServoyException e) {
			throw new RuntimeException(e);
		}
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

	@Override
	public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
		return null;
	}

}
