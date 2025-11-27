package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogisticViewer extends Viz {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String carriers;

	@JsonProperty(required = true)
	public String lsps;

	public LogisticViewer() {
		super("lsp");
	}

}
