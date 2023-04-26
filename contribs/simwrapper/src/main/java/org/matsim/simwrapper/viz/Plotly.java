package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.plotly.components.Config;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.Trace;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

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

	@JsonIgnore
	public List<Data> data = new ArrayList<>();

	@JsonIgnore
	public List<Trace> traces = new ArrayList<>();

	@Nullable
	@JsonIgnore
	public Layout layout;

	@Nullable
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
	List<Map<String, Object>> getTraces() {

		List<Map<String, Object>> result = new ArrayList<>();

		for (int i = 0; i < traces.size(); i++) {
			Trace t = traces.get(i);
			Data d = data.get(i);

			Map<String, Object> object = convertTrace(t, d, i);

			result.add(object);
		}

		return result;
	}

	private Map<String, Object> convertTrace(Trace t, Data d, int i) {

		Map<String, Object> object = new LinkedHashMap<>();

		if (d.file != null) {
			object.put("data", d);
		}

		Field[] fields = t.getClass().getDeclaredFields();

		for (Field field : fields) {

			if (!field.trySetAccessible())
				continue;

			if (Modifier.isStatic(field.getModifiers()))
				continue;

			try {
				Object o = field.get(t);

				if (o == null)
					continue;
				if (o.equals(""))
					continue;

				object.put(field.getName().toLowerCase(), o);
			} catch (IllegalAccessException e) {
				log.warn("Could not retrieve field {}", field.getName(), e);
				continue;
			}

		}

		// Remove data entries that should be loaded from resources
		if (d.x != null)
			object.remove("x");

		if (d.y != null)
			object.remove("y");

		if (d.text != null)
			object.remove("text");

		return object;
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

			return clean((Map<String, Object>) m.invoke(layout));

		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Recursively converts object attributes to be consistent with plotly.
	 *
	 * @return same instance as input.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> clean(Map<String, Object> object) {

		Iterator<Map.Entry<String, Object>> it = object.entrySet().iterator();

		Map<String, Object> copy = new LinkedHashMap<>();
		while (it.hasNext()) {
			Map.Entry<String, Object> e = it.next();

			if (e.getValue() == null || "".equals(e.getValue())) {
				it.remove();
				continue;
			}

			if (e.getValue() instanceof Map)
				clean((Map<String, Object>) e.getValue());

			copy.put(e.getKey().toLowerCase(), e.getValue());
			it.remove();
		}

		object.putAll(copy);
		return object;
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
	public static Data inline() {
		return new Data(null);
	}

	/**
	 * Class to specify input path and map column names to their meaning.
	 */
	public static final class Data {
		private final String file;
		private String x;
		private String y;
		private String text;
		private String size;
		private String color;
		private String opacity;

		private Data(String file) {
			this.file = file;
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
