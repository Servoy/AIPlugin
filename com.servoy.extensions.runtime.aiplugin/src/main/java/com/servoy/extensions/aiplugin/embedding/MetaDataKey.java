package com.servoy.extensions.aiplugin.embedding;

import com.servoy.j2db.query.ColumnType;

public record MetaDataKey(String name, ColumnType columnType, int flags, boolean allowNull) {
}