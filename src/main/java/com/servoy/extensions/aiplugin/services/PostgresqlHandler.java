package com.servoy.extensions.aiplugin.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.pgvector.PGvector;
import com.servoy.base.query.TypeInfo;
import com.servoy.j2db.dataprocessing.ValueFactory.NullValue;
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
		if ( "vector".equalsIgnoreCase(typeInfo.getNativeTypename())) 
		{
			if (qd instanceof float[] flarray) 
			{
				PGvector vector = new PGvector(flarray);
				ps.setObject(paramIndex, vector);
			}
			else if (qd instanceof NullValue) {
				ps.setNull(paramIndex, Types.OTHER);
			}
			return true;
		}
		return false;
	}

}
