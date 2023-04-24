package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.el.MethodNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.plotly.components.Config;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.AbstractTrace;
import tech.tablesaw.plotly.traces.Trace;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Viz for arbitrary plotly graphs.
 */
public class Plotly extends Viz {

	private static final Logger log = LogManager.getLogger(Plotly.class);

	/**
	 * Empty placeholder array, which may be used as input for constructing traces.
	 * Actual data is loaded later from files and not used directly in the specification file.
	 */
	public static final double[] INPUT = new double[0];

	/**
	 * See {@link #INPUT}.
	 */
	public static final Object[] OBJ_INPUT = new Object[0];

	public List<Data> data = new ArrayList<>();

	@JsonIgnore
	public List<Trace> traces = new ArrayList<>();

	@Nullable
	@JsonIgnore
	public Layout layout;

	@Nullable
	@JsonIgnore
	public Config config;


	public Plotly() {
		super("plotly");
	}

	/**
	 * Add a trace to the figure.
	 *
	 * @param trace trace object, specifying one data set.
	 * @param data  data mapping for the given trace.
	 */
	public Plotly addTrace(Trace trace, Data data) {
		this.data.add(data);
		traces.add(trace);
		return this;
	}

	/**
	 * Create a figure object from this plot.
	 */
	public Figure asFigure() {
		return new Figure(layout, config, traces.toArray(new Trace[0]));
	}

	@JsonProperty("traces")
	@SuppressWarnings("unchecked")
	List<Map<String, Object>> getTraces() {

		List<Map<String, Object>> result = new ArrayList<>();

		// TODO: enable traces to be loaded from files


		// TODO: data mapping
		// TODO: check if the mapping is valid and present
		// TODO: remove if data is present

		int i = 0;
		for (Trace t : traces) {

			if (!(t instanceof AbstractTrace trace)) {
				log.warn("Generation is only supported for AbstractTrace");
				continue;
			}

			// Use one of the getContext methods
			try {
				Method m = t.getClass().getDeclaredMethod("getContext", int.class);
				m.setAccessible(true);
				result.add((Map<String, Object>) m.invoke(t, i++));
				continue;

			} catch (MethodNotFoundException e) {
				// Nothing done, go to next block
			} catch (ReflectiveOperationException e) {
				log.error("Could not create trace", e);
			}

			try {
				Method m = AbstractTrace.class.getDeclaredMethod("getContext");
				m.setAccessible(true);

				result.add((Map<String, Object>) m.invoke(t));
			} catch (ReflectiveOperationException e) {
				log.error("Could not create trace", e);
			}
		}

		return result;
	}

	@JsonProperty("config")
	@SuppressWarnings("unchecked")
	Map<String, Object> getConfig() {

		if (config == null)
			return null;

		try {
			// Use protected method
			Method m = Config.class.getDeclaredMethod("getJSONContext");
			m.setAccessible(true);

			return (Map<String, Object>) m.invoke(config);

		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@JsonProperty("layout")
	@SuppressWarnings("unchecked")
	Map<String, Object> getLayout() {

		if (layout == null)
			return null;

		try {
			// Use protected method
			Method m = Layout.class.getDeclaredMethod("getContext");
			m.setAccessible(true);

			return (Map<String, Object>) m.invoke(layout);

		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new data mapping.
	 *
	 * @param path path to input csv.
	 */
	public static Data data(String path) {
		return new Data(path);
	}

	/**
	 * Data mapping that is not using external data, but inlines it into the yaml files.
	 */
	public static Data inlineData() {
		return new Data(null);
	}

	/**
	 * Class to specify input path and map column names to their meaning.
	 */
	public static final class Data {
		private final String path;
		private String x;
		private String y;
		private String text;
		private String size;
		private String color;
		private String opacity;

		private Data(String path) {
			this.path = path;
		}

		/**
		 * Mapping for the x column.
		 */
		public Data x(String columnName) {
			x = columnName;
			return this;
		}

		/**
		 * Mapping for the y column.
		 */
		public Data y(String columnName) {
			y = columnName;
			return this;
		}

		/**
		 * Mapping for text (label) column.
		 */
		public Data text(String columnName) {
			text = columnName;
			return this;
		}

		/**
		 * Mapping for size column.
		 */
		public Data size(String columnName) {
			size = columnName;
			return this;
		}

		/**
		 * Mapping for color column.
		 */
		public Data color(String columnName) {
			color = columnName;
			return this;
		}

		/**
		 * Mapping for opacity column.
		 */
		public Data opacity(String columnName) {
			opacity = columnName;
			return this;
		}
	}

}
