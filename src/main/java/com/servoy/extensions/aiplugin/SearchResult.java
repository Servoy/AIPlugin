package com.servoy.extensions.aiplugin;

import java.util.Map;

import com.servoy.j2db.documentation.ServoyDocumented;

import dev.langchain4j.data.document.Metadata;

@ServoyDocumented(scriptingName = "SearchResult")
public class SearchResult {

	private final Double score;
	private final String text;
	private final Metadata metadata;

	public SearchResult(Double score, String text, Metadata metadata) {
		this.score = score;
		this.text = text;
		this.metadata = metadata;
	}
	
	public Double getScore() {
		return score;
	}
	
	public String getText() {
		return text;
	}
	
	public Map<String,Object> getMetadata() {
		return metadata.toMap();
	}

}
