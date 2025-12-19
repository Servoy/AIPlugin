package com.servoy.extensions.aiplugin.database.postgres;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.pgvector.PGvector;
import com.servoy.base.query.TypeInfo;

public class PostgresPreparedStatementParameterHandler {
	static public boolean setParameter(PreparedStatement ps, int paramIndex, TypeInfo typeInfo, Object qd)
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
