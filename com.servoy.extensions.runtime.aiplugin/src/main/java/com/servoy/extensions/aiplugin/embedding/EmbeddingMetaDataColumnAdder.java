package com.servoy.extensions.aiplugin.embedding;

import static com.servoy.base.persistence.IBaseColumn.NORMAL_COLUMN;
import static com.servoy.j2db.persistence.IColumnTypes.TEXT;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.server.MetaDataKey;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ColumnType;

/**
 * Builder like class to add a meta data column to a ServoyEmbeddingStoreBuilder.
 */
@ServoyDocumented
public class EmbeddingMetaDataColumnAdder {
	private final ServoyEmbeddingStoreBuilder servoyEmbeddingStoreBuilder;

	private String name;
	private ColumnType columnType = ColumnType.getColumnType(TEXT);
	private boolean allowNull = false;
	private int flags = NORMAL_COLUMN;

	public EmbeddingMetaDataColumnAdder(ServoyEmbeddingStoreBuilder servoyEmbeddingStoreBuilder) {
		this.servoyEmbeddingStoreBuilder = servoyEmbeddingStoreBuilder;
	}

	/**
	 * Sets the name of the meta data column to add.
	 *
	 * @param name meta data column name.
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataColumnAdder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the column type of the meta data column to add.
	 *
	 * @param columnType meta data column type (default JSColumn.TEXT).
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataColumnAdder columnType(int columnType) {
		this.columnType = ColumnType.getColumnType(columnType);
		return this;
	}

	/**
	 * Sets the allow null option of the meta data column to add.
	 *
	 * @param allowNull meta data column allow null option (default false).
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataColumnAdder allowNull(boolean allowNull) {
		this.allowNull = allowNull;
		return this;
	}

	/**
	 * Sets a flag for the meta data column to add, for example
	 * JSColumn.TENANT_COLUMN or JSColumn.UUID_COLUMN.
	 *
	 * @param flag flag to set for the meta data column.
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataColumnAdder flag(int flag) {
		this.flags |= flag;
		return this;
	}

	/**
	 * Adds the meta data column to the ServoyEmbeddingStoreBuilder and returns it.
	 *
	 * @return The ServoyEmbeddingStoreBuilder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder add() {
		return servoyEmbeddingStoreBuilder
				.addMetaDataKey(new MetaDataKey(ensureNotNull(name, "name"), columnType, flags, allowNull));
	}

}
