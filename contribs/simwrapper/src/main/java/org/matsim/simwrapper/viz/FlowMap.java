package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;

public class FlowMap extends Viz {

	@JsonProperty(required = true)
	public ArrayList<Metrics> metrics = new ArrayList<>();

	@JsonProperty
	public String description;

	@JsonProperty
	public String title;

	public FlowMap() {
		super("flowmap");
	}
	public static class Metrics {

		@JsonProperty(required = true)
		private String label;

		@JsonProperty(required = true)
		private String dataset;

		@JsonProperty(required = true)
		private String origin;

		@JsonProperty
		private double zoom;

		@JsonProperty(required = true)
		private String destination;

		@JsonProperty(required = true)
		private String flow;

		@JsonProperty(required = true)
		private String colorScheme;

		@JsonProperty(required = true)
		private ValueTransform valueTransform;

		public void setColorScheme(String colorScheme) {
			this.colorScheme = colorScheme;
		}

		public void setDataset(String dataset) {
			this.dataset = dataset;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public void setFlow(String flow) {
			this.flow = flow;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public void setZoom(double zoom) {
			this.zoom = zoom;
		}

		public void setOrigin(String origin) {
			this.origin = origin;
		}

		public void setValueTransform(ValueTransform valueTransform) {
			this.valueTransform = valueTransform;
		}

		public String getDataset() {
			return dataset;
		}

		public String getColorScheme() {
			return colorScheme;
		}

		public String getDestination() {
			return destination;
		}

		public String getOrigin() {
			return origin;
		}

		public ValueTransform getValueTransform() {
			return valueTransform;
		}

		public String getFlow() {
			return flow;
		}

		public double getZoom() {
			return zoom;
		}

		public String getLabel() {
			return label;
		}

		public static enum ValueTransform {
			@JsonProperty("normal") NORMAL,
			@JsonProperty("inverse") INVERSE
		}

	}


}



