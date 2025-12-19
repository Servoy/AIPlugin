package com.servoy.extensions.aiplugin.database.postgres;

import java.sql.Connection;
import java.sql.SQLException;

import com.pgvector.PGvector;

public class PostgresqlConnectionInitializer
		implements com.servoy.j2db.server.extensions.PostgresqlConnectionInitializer {

	@Override
	public void initialize(Connection connection) throws SQLException {
		PGvector.registerTypes(connection);
	}
}
