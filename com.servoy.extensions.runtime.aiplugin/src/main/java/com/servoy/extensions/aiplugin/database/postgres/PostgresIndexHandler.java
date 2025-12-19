package com.servoy.extensions.aiplugin.database.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;

public class PostgresIndexHandler {

	public static boolean createEmbeddingIndex(Connection connection, ITable table, String indexName, Column column,
			int embeddingListSize) throws SQLException {

		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE INDEX IF NOT EXISTS \"" + indexName + "\"" + " ON  " + qualifiedName(table)
					+ " USING ivfflat (\"" + column.getSQLName() + "\" vector_cosine_ops)" + " WITH (lists = "
					+ embeddingListSize + ")");
		}
		return true;
	}

	private static String qualifiedName(ITable table) {
		StringBuilder sb = new StringBuilder();
		if (table.getCatalog() != null && !table.getCatalog().isEmpty()) {
			sb.append('"').append(table.getCatalog()).append("\".");
		}
		if (table.getSchema() != null && !table.getSchema().isEmpty()) {
			sb.append('"').append(table.getSchema()).append("\".");
		}
		sb.append('"').append(table.getSQLName()).append('"');
		return sb.toString();
	}
}
