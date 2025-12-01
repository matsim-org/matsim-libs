package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogisticViewer extends VizMap {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String carriers;

	@JsonProperty(required = true)
	public String lsps;

	public LogisticViewer() {
		super("lsp");
	}

	@Override
	public LogisticViewer addBackgroundLayer(String name, BackgroundLayer layer) {
		super.addBackgroundLayer(name, layer);
		return this;
	}

}
