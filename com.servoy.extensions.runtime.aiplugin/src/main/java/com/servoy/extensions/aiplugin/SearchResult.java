package com.servoy.extensions.aiplugin;

import java.util.Map;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;

import dev.langchain4j.data.document.Metadata;

/**
 * SearchResult represents a single result from an embedding similarity search.
 * Contains the similarity score, matched text, and associated metadata.
 */
@ServoyDocumented
public class SearchResult implements IJavaScriptType{

	/**
	 * The similarity score for this result.
	 */
	private final Double score;
	/**
	 * The matched text for this result.
	 */
	private final String text;
	/**
	 * The metadata associated with this result.
	 */
	private final Metadata metadata;

	/**
	 * Constructs a SearchResult with the given score, text, and metadata.
	 * @param score The similarity score.
	 * @param text The matched text.
	 * @param metadata The associated metadata.
	 */
	public SearchResult(Double score, String text, Metadata metadata) {
		this.score = score;
		this.text = text;
		this.metadata = metadata;
	}
	
	/**
	 * Returns the similarity score for this result.
	 * @return The similarity score.
	 */
	public Double getScore() {
		return score;
	}
	
	/**
	 * Returns the matched text for this result.
	 * @return The matched text.
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Returns the metadata associated with this result as a map.
	 * @return The metadata map.
	 */
	public Map<String,Object> getMetadata() {
		return metadata.toMap();
	}

}