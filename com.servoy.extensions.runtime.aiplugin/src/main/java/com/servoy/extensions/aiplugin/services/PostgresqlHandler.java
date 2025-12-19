package com.servoy.extensions.aiplugin.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.pgvector.PGvector;
import com.servoy.base.query.TypeInfo;
import com.servoy.j2db.server.extensions.PostgresqlConnectionInitializer;
import com.servoy.j2db.server.extensions.PreparedStatementParameterHandler;

public class PostgresqlHandler implements PostgresqlConnectionInitializer, PreparedStatementParameterHandler {

	@Override
	public void initialize(Connection connection) throws SQLException {
		PGvector.registerTypes(connection);
	}

	@Override
	public boolean setParameter(PreparedStatement ps, int paramIndex, TypeInfo typeInfo, Object qd)
			throws SQLException {
		if ("vector".equalsIgnoreCase(typeInfo.getNativeTypename())) {
			// nulls are handled inside servoy itself
			float[] floats = null;
			if (qd instanceof float[] flarray) {
				floats = flarray;
			} else if (qd instanceof Object[] array) {
				float[] flarray = new float[array.length];
				for (int i = 0; i < array.length; i++) {
					if (array[i] instanceof Number number) {
						flarray[i] = number.floatValue();
					} else {
						flarray = null;
						break;
					}
				}
				floats = flarray;
			}
			if (floats != null) {
				PGvector vector = new PGvector(floats);
				ps.setObject(paramIndex, vector);
				return true;
			}
		}
		return false;
	}
}
