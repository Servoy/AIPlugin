package com.servoy.extensions.aiplugin.embedding;

import static com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer.EMBEDDING_COLUMN;
import static com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer.EMBEDDING_ID_COLUMN;
import static com.servoy.extensions.aiplugin.embedding.ServoyEmbeddingStoreServer.TEXT_COLUMN;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;
import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;
import static java.lang.Float.parseFloat;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;

import com.servoy.extensions.aiplugin.AIProvider;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.impl.QBColumn;
import com.servoy.j2db.querybuilder.impl.QBVectorColumnBase;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class ServoyEmbeddingStore implements EmbeddingStore<TextSegment> {

	private final AIProvider provider;
	private final String dataSource;
	private final ServoyEmbeddingStoreServer servoyEmbeddingStoreServer;

	public ServoyEmbeddingStore(AIProvider provider, String dataSource,
			ServoyEmbeddingStoreServer servoyEmbeddingStoreServer) {
		this.provider = provider;
		this.dataSource = dataSource;
		this.servoyEmbeddingStoreServer = servoyEmbeddingStoreServer;
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
			String transactionId = provider.getDatabaseManager().getTransactionID(getDataSourceServerName(dataSource));
			servoyEmbeddingStoreServer.addEmbeddings(provider.getClientID(), transactionId, ids, embeddings, embedded);
		} catch (ServoyException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
		try {
			var metaDataColumnInfo = getMetaDataColumnInfo();
			List<Column> metaDataColumns = metaDataColumnInfo.getLeft();
			boolean hasText = metaDataColumnInfo.getRight();

			var query = provider.getDatabaseManager().getQueryFactory().createSelect(dataSource);
			QBVectorColumnBase embeddingColumn = (QBVectorColumnBase) query.getColumn(EMBEDDING_COLUMN);
			var vectorScore = embeddingColumn.vector_score(request.queryEmbedding().vector());

			query.result().add(query.getColumn(EMBEDDING_ID_COLUMN));
			query.result().add(embeddingColumn);
			if (hasText) {
				query.result().add(query.getColumn(TEXT_COLUMN));
			}
			query.result().add(vectorScore, "_sv_score");
			for (Column column : metaDataColumns) {
				query.result().add(query.getColumn(column.getName()));
			}
			query.where().add(vectorScore.min_score(request.minScore()));
			query.sort().add(((QBColumn) vectorScore).desc());

			var dataSet = provider.getDatabaseManager().getDataSetByQuery(query, request.maxResults());
			var filter = request.filter();
			List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
			for (int row = 0; row < dataSet.getRowCount(); row++) {
				var embedded = getEmbedded(metaDataColumns, hasText, dataSet, row);
				if (filter == null || filter.test(embedded.metadata())) {
					Double score = getValue(dataSet, row, "_sv_score");
					String embeddingId = getValue(dataSet, row, EMBEDDING_ID_COLUMN);
					Object embedding = getValue(dataSet, row, EMBEDDING_COLUMN);
					matches.add(
							new EmbeddingMatch<>(score, embeddingId, new Embedding(toFloatArray(embedding)), embedded));
				}
			}
			return new EmbeddingSearchResult<>(matches);
		} catch (ServoyException e) {
			throw new RuntimeException(e);
		}
	}

	private static float[] toFloatArray(Object value) {
		if (value instanceof float[] floats) {
			return floats;
		}
		if (value instanceof String string) {
			return parseFloatArray(string);
		}
		throw new IllegalArgumentException(
				"Unsupported embedding value type: " + (value == null ? "null" : value.getClass().getName()));
	}

	private static float[] parseFloatArray(String string) {
		String floatString = string.trim();
		if (floatString.startsWith("[") && floatString.endsWith("]")) {
			floatString = floatString.substring(1, floatString.length() - 1);
		}
		if (floatString.isEmpty()) {
			return new float[0];
		}
		String[] split = floatString.split(",");
		float[] floats = new float[split.length];
		for (int i = 0; i < split.length; i++) {
			floats[i] = parseFloat(split[i].trim());
		}
		return floats;
	}

	private static TextSegment getEmbedded(List<Column> metaDataColumns, boolean hasText, IDataSet dataSet, int row) {
		String text = hasText ? getValue(dataSet, row, TEXT_COLUMN) : "text not stored";
		var metaData = metaDataColumns.stream()
				.map(column -> new Pair<>(column.getName(), getValue(dataSet, row, column.getName())))
				.filter(pair -> pair.getRight() != null).collect(toMap(Pair::getLeft, Pair::getRight));
		return TextSegment.from(text, Metadata.from(metaData));
	}

	private Pair<List<Column>, Boolean> getMetaDataColumnInfo() throws RepositoryException {
		var table = ensureNotNull(provider.getDatabaseManager().getTable(dataSource),
				"Cannot find table " + dataSource);
		var metaDataColumns = table.getColumns().stream()
				.filter(column -> !EMBEDDING_ID_COLUMN.equals(column.getName())
						&& !EMBEDDING_COLUMN.equals(column.getName()) && !TEXT_COLUMN.equals(column.getName()))
				.toList();
		var hasText = table.getColumns().stream().anyMatch(column -> TEXT_COLUMN.equals(column.getName()));
		return new Pair<>(metaDataColumns, hasText);
	}

	private static <T> T getValue(IDataSet dataSet, int row, String columnName) {
		int index = asList(dataSet.getColumnNames()).indexOf(columnName);
		return (T) dataSet.getRow(row)[index];
	}
}
