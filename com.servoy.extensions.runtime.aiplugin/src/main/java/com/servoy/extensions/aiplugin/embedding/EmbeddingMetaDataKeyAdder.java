package com.servoy.extensions.aiplugin.embedding;

import static com.servoy.base.persistence.IBaseColumn.NORMAL_COLUMN;
import static com.servoy.j2db.persistence.IColumnTypes.TEXT;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.extensions.aiplugin.server.MetaDataKey;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ColumnType;

/**
 * Builder like class to add a MetaDataKey to a ServoyEmbeddingStoreBuilder.
 */
@ServoyDocumented
public class EmbeddingMetaDataKeyAdder {
	private final ServoyEmbeddingStoreBuilder servoyEmbeddingStoreBuilder;

	private String name;
	private ColumnType columnType = ColumnType.getColumnType(TEXT);
	private boolean allowNull = false;
	private int flags = NORMAL_COLUMN;

	public EmbeddingMetaDataKeyAdder(ServoyEmbeddingStoreBuilder servoyEmbeddingStoreBuilder) {
		this.servoyEmbeddingStoreBuilder = servoyEmbeddingStoreBuilder;
	}

	/**
	 * Sets the name of the meta data key to add.
	 *
	 * @param name meta data key name.
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataKeyAdder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Sets the column type of the meta data key to add.
	 *
	 * @param columnType meta data key column type (default JSColumn.TEXT).
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataKeyAdder columnType(int columnType) {
		this.columnType = ColumnType.getColumnType(columnType);
		return this;
	}

	/**
	 * Sets the allow null option of the meta data key to add.
	 *
	 * @param allowNull meta data key column allow null option (default false).
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataKeyAdder allowNull(boolean allowNull) {
		this.allowNull = allowNull;
		return this;
	}

	/**
	 * Sets a flag for the meta data key to add, for example JSColumn.TENANT_COLUMN
	 * or JSColumn.UUID_COLUMN.
	 *
	 * @param flag flag to set for the meta data key column.
	 * @return This adder instance.
	 */
	@JSFunction
	public EmbeddingMetaDataKeyAdder flag(int flag) {
		this.flags |= flag;
		return this;
	}

	/**
	 * Adds the meta data key to the ServoyEmbeddingStoreBuilder and returns it.
	 *
	 * @return The ServoyEmbeddingStoreBuilder instance.
	 */
	@JSFunction
	public ServoyEmbeddingStoreBuilder add() {
		return servoyEmbeddingStoreBuilder
				.addMetaDataKey(new MetaDataKey(ensureNotNull(name, "name"), columnType, flags, allowNull));
	}

}
