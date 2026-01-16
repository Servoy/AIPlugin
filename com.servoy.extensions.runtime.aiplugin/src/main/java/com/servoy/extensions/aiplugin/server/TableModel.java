package com.servoy.extensions.aiplugin.server;

import java.util.List;
import java.util.Map;

import com.servoy.extensions.aiplugin.embedding.MetaDataKey;
import com.servoy.j2db.query.ColumnType;

record TableModel(String serverName, String tableName, Map<String, ColumnType> columnTypes,
		List<MetaDataKey> metaDataKeys, boolean wasCreated) {
}
