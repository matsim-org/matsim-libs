package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CarrierViewer extends VizMap {

	@JsonProperty(required = true)
	public String network;

	@JsonProperty(required = true)
	public String carriers;

	public CarrierViewer() {
		super("carriers");
	}

	@Override
	public CarrierViewer addBackgroundLayer(String name, BackgroundLayer layer) {
		super.addBackgroundLayer(name, layer);
		return this;
	}

}
