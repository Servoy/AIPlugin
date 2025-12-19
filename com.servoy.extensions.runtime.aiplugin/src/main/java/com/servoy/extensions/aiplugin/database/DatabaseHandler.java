package com.servoy.extensions.aiplugin.database;

import static com.servoy.extensions.aiplugin.database.DatabaseHandler.DatabasedProduct.PostgreSQL;
import static com.servoy.extensions.aiplugin.database.DatabaseHandler.DatabasedProduct.detectDatabaseProduct;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.servoy.base.query.TypeInfo;
import com.servoy.extensions.aiplugin.database.postgres.PostgresIndexHandler;
import com.servoy.extensions.aiplugin.database.postgres.PostgresPreparedStatementParameterHandler;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.server.extensions.PreparedStatementParameterHandler;

public class DatabaseHandler implements PreparedStatementParameterHandler {

	public static DatabaseHandler DATABASE_HANDLER = new DatabaseHandler();

	enum DatabasedProduct {
		PostgreSQL, Unknown;

		static DatabasedProduct detectDatabaseProduct(Connection connection) throws SQLException {
			String databaseProductName = connection.getMetaData().getDatabaseProductName();
			if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
				return PostgreSQL;
			}

			return Unknown;
		}
	}

	@Override
	public boolean setParameter(PreparedStatement ps, int paramIndex, TypeInfo typeInfo, Object qd)
			throws SQLException {

		if (DatabasedProduct.detectDatabaseProduct(ps.getConnection()) == PostgreSQL) {
			return PostgresPreparedStatementParameterHandler.setParameter(ps, paramIndex, typeInfo, qd);
		}

		return false;
	}

	public boolean createEmbeddingIndex(Connection connection, ITable table, String indexName, Column column,
			int embeddingListSize) throws SQLException {
		if (detectDatabaseProduct(connection) == PostgreSQL) {
			return PostgresIndexHandler.createEmbeddingIndex(connection, table, indexName, column, embeddingListSize);
		}

		return false;
	}

}
