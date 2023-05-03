package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Links extends Viz {

	public Links() {
		super("links");
	}

	public Datasets datasets = new Datasets();

	@JsonProperty(required = true)
	public String network;

	public String projection;

	public String center;

	public Display display = new Display();

	public static final class Datasets{

		@JsonProperty(required = true)
		public String csvFile;

		public String csvBase;
	}

	public static final class Display{

		public Color color = new Color();

		public static final class Color{

			public String dataset;

			public String columnName;
		}
	}
}
