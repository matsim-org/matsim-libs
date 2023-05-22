package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MapPlot extends Viz{

	public MapPlot() {
		super("map");
	}

	@JsonProperty(required = true)
	public String shapes;

	public double[] center;

	public Datasets datasets = new Datasets();

	public Display display = new Display();

	public class Datasets {

		@JsonProperty(required = true)
		public String counts;
	}

	public class Display {

		public LineWidth lineWidth = new LineWidth();

		public Fill fill = new Fill();

		public class LineWidth {

			@JsonProperty(required = true)
			public String dataset;

			@JsonProperty(required = true)
			public String columnName;

			@JsonProperty(required = true)
			public String join;

			public int scaleFactor;
		}

		public class Fill{

			@JsonProperty(required = true)
			public String dataset;

			@JsonProperty(required = true)
			public String columnName;

			public String filters;

			public ColorRamp colorRamp = new ColorRamp();

			public class ColorRamp{

				public String ramp;

				public boolean reversed;

				public int steps;
			}
		}
	}
}
