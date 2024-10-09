package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.plotly.components.Config;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.AbstractTrace;
import tech.tablesaw.plotly.traces.Trace;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Viz for arbitrary plotly graphs.
 */
public final class Plotly extends Viz {

	/**
	 * Empty placeholder array, which may be used as input for constructing traces.
	 * Actual data is loaded later from files and not used directly in the specification file.
	 */
	public static final double[] INPUT = new double[0];

	/**
	 * See {@link #INPUT}.
	 */
	public static final Object[] OBJ_INPUT = new Object[0];

	/**
	 * See {@link #INPUT}.
	 */
	public static final String[] TEXT_INPUT = new String[0];

	private static final Logger log = LogManager.getLogger(Plotly.class);

	@Nullable
	@JsonIgnore
	public Layout layout;

	@JsonProperty(index = 26)
	@Nullable
	public Config config;
	/**
	 * Set the color ramp that is applied if multiple traces are present.
	 */
	public String colorRamp;
	/**
	 * Merge all given datasets into one.
	 */
	public Boolean mergeDatasets;

	/**
	 * Define a fixed ratio for x and y axes domain.
	 */
	public Boolean fixedRatio;

	/**
	 * Add UI element to make individual traces interactive.
	 */
	public Interactive interactive;

	/**
	 * Merge two column as index. Column name as key will be merged with the column name value.
	 * This allows to build level multi indices for certain plot types.
	 */
	public Map<String, String> multiIndex;

	@JsonIgnore
	private final List<Trace> traces = new ArrayList<>();
	@JsonIgnore
	private final List<DataSet> data = new ArrayList<>();
	@JsonIgnore
	private final List<DataMapping> mappings = new ArrayList<>();

	public Plotly() {
		super("plotly");
	}

	/**
	 * Constructor for given layout and traces, which will be inlined.
	 */
	public Plotly(@Nullable Layout layout, Trace... trace) {
		super("plotly");
		this.layout = layout;
		for (Trace t : trace) {
			addTrace(t);
		}
	}

	/**
	 * Constructor for a given {@link Figure}.
	 */
	public Plotly(Figure figure) {
		super("plotly");
		fromFigure(figure);
	}

	private static List<Field> getAllFields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			fields.addAll(Arrays.asList(c.getDeclaredFields()));
		}
		return fields;
	}

	/**
	 * Add a trace to the figure.
	 *
	 * @param trace trace object, specifying one data set.
	 * @param d     data mapping for the given trace.
	 */
	public Plotly addTrace(Trace trace, @Nullable DataMapping d) {
		this.mappings.add(d);
		traces.add(trace);
		return this;
	}

	/**
	 * Add a trace to the figure. The trace object itself should already contain the data.
	 *
	 * @param trace trace object, specifying one data set.
	 */
	public Plotly addTrace(Trace trace) {
		return addTrace(trace, null);
	}

	/**
	 * Adds a dataset definition to the plot that can be referenced in the traces.
	 */
	public DataSet addDataset(String path) {

		Objects.requireNonNull(path, "Path argument can not be null");

		String name = data.isEmpty() ? "dataset" : FilenameUtils.removeExtension(FilenameUtils.getName(path)).replace("_", "");
		DataSet ds = new DataSet(path, name);
		data.add(ds);
		return ds;
	}

	/**
	 * Create a figure object from this plot.
	 */
	public Figure asFigure() {
		return new Figure(layout, config, traces.toArray(new Trace[0]));
	}

	/**
	 * Fill content from a given {@link Figure}.
	 */
	public Plotly fromFigure(Figure figure) {
		for (Trace t : figure.getTraces()) {
			addTrace(t, null);
		}

		layout = figure.getLayout();
		config = figure.getConfig();

		return this;
	}

	@JsonProperty(value = "datasets", index = 20)
	Map<String, Object> getDataSets() {
		if (data.isEmpty())
			return null;

		Map<String, Object> result = new LinkedHashMap<>();
		for (DataSet d : data) {
			result.put(d.name, d.toJSON());
		}
		return result;
	}

	@JsonProperty(value = "traces", index = 25)
	List<Map<String, Object>> getTraces() {

		List<Map<String, Object>> result = new ArrayList<>();

		for (int i = 0; i < traces.size(); i++) {
			Trace t = traces.get(i);
			DataMapping d = mappings.get(i);

			Map<String, Object> object = convertTrace(t, d, i);

			result.add(object);
		}

		return result;
	}

	private Map<String, Object> convertTrace(Trace t, DataMapping d, int i) {

		Map<String, Object> object = new LinkedHashMap<>();

		List<Field> fields = getAllFields(t.getClass());

		String type = "";
		for (Field field : fields) {

			String name = field.getName();

			if (name.equals("engine"))
				continue;

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

				// Some special cases that don't need to be written
				if (name.equals("opacity") && Objects.equals(o, 1.0))
					continue;
				if (name.equals("visible") && Objects.equals(o, AbstractTrace.Visibility.TRUE))
					continue;
				if (name.equals("xAxis") && Objects.equals(o, "x"))
					continue;
				if (name.equals("yAxis") && Objects.equals(o, "y"))
					continue;

				if (name.equals("type"))
					type = (String) o;

				object.put(name.toLowerCase(), o);
			} catch (IllegalAccessException e) {
				log.warn("Could not retrieve field {}", name, e);
			}

		}

		// Pie charts have an API that is quite confusing because the column names are different
		// This codes fixes some errors
		if ("pie".equals(type) && d != null) {
			// Never sort pie charts, attribute is also not settable via API
			object.put("sort", false);
			d.normalizePieTrace();
		}

		// Remove data entries that should be loaded from resources
		if (d != null)
			d.insert(object);

		return object;
	}

	@JsonProperty(value = "layout", index = 30)
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

		// Attributes that cause problems and should not be set
		copy.remove("width");
		copy.remove("height");

		object.putAll(copy);
		return object;
	}

	/**
	 * The available column types in plotly.
	 */
	enum ColumnType {
		NAME,
		X,
		X2,
		Y,
		LABELS,
		VALUES,
		TEXT,
		SIZE,
		COLOR,
		OPACITY,
		FACET_COL,
		FACET_ROW,
	}

	/**
	 * Available aggregate function for {@link DataSet#aggregate(List, String, AggrFunc)}.
	 */
	public enum AggrFunc {
		SUM
	}

	/**
	 * Specifies if plot should contain an interactive element.
	 */
	public enum Interactive {
		none,
		dropdown,
		slider
	}

	/**
	 * Class to specify a single dataset.
	 */
	public static final class DataSet {

		@JsonIgnore
		private final String name;
		private final String file;

		private Map<String, Object> pivot;
		private Map<String, Object> constant;
		private Map<String, Object> aggregate;
		private Map<String, Object> normalize;
		private Map<String, Object> rename;

		private DataSet(String file, String name) {
			this.file = file;
			this.name = name;
		}

		private Object toJSON() {
			if (pivot == null &&
				constant == null &&
				aggregate == null &&
				normalize == null &&
				rename == null)
				return file;

			return this;
		}

		/**
		 * Return dataset mapping which can be modified and passed as argument to traces.
		 */
		public DataMapping mapping() {
			return new DataMapping(name);
		}

		/**
		 * Indicates that each column beside the already mapped ones contain one data group. This is also known as 'wide-format'.
		 * Defining this will map each new column to the specified target array.
		 *
		 * @param exclude columns not to pivot, typical x-axis.
		 */
		public DataSet pivot(List<String> exclude, String namesTo, String valuesTo) {

			if (exclude.isEmpty())
				throw new IllegalArgumentException("Exclude list can not be empty");

			pivot = Map.of(
				"exclude", exclude,
				"namesTo", namesTo,
				"valuesTo", valuesTo
			);
			return this;
		}

		/**
		 * Adds a column with constant values. May be useful for multi index and stacking bar charts.
		 */
		public DataSet constant(String columnName, Object constant) {
			if (this.constant == null)
				this.constant = new LinkedHashMap<>();

			this.constant.put(columnName, constant);
			return this;
		}

		/**
		 * Aggregate data within each trace on the {@code targetColumn}. Uses all unmapped columns or only the given columns if not empty.
		 */
		public DataSet aggregate(List<String> groupBy, String target, AggrFunc func) {
			aggregate = Map.of(
				"groupBy", groupBy,
				"target", target,
				"func", func.toString()
			);
			return this;
		}

		/**
		 * Normalize data to 100% for each group.
		 * @param groupBy group columns to group by.
		 * @param target target column.
		 */
		public DataSet normalize(List<String> groupBy, String target) {
			normalize = Map.of(
				"groupBy", groupBy,
				"target", target
			);
			return this;
		}

		/**
		 * Rename values in the dataset. Useful after pivot or to rename certain entries.
		 */
		public DataSet rename(String oldName, String newName) {
			if (rename == null)
				rename = new LinkedHashMap<>();

			rename.put(oldName, newName);
			return this;
		}
	}

	/**
	 * Defines how columns are mapped to data sources.
	 */
	public static final class DataMapping {

		private final String ref;
		private final Map<ColumnType, String> columns = new EnumMap<>(ColumnType.class);

		private String colorRamp;

		public DataMapping(String name) {
			this.ref = name;
		}

		/**
		 * Group this data by categorical variable in column.
		 */
		public DataMapping name(String columnName) {
			columns.put(ColumnType.NAME, columnName);
			return this;
		}

		/**
		 * Group this data by categorical variable in column.
		 *
		 * @param colorRamp color ramp to use
		 */
		public DataMapping name(String columnName, String colorRamp) {
			columns.put(ColumnType.NAME, columnName);
			this.colorRamp = colorRamp;
			return this;
		}

		/**
		 * Mapping for the x column.
		 */
		public DataMapping x(String columnName) {
			columns.put(ColumnType.X, columnName);
			return this;
		}

		/**
		 * Mapping for the y column.
		 */
		public DataMapping y(String columnName) {
			columns.put(ColumnType.Y, columnName);
			return this;
		}

		/**
		 * Mapping for text (label) column.
		 */
		public DataMapping text(String columnName) {
			columns.put(ColumnType.TEXT, columnName);
			return this;
		}

		/**
		 * Labels are used for piecharts.
		 */
		public DataMapping labels(String columnName) {
			columns.put(ColumnType.LABELS, columnName);
			return this;
		}

		/**
		 * Mapping for size column.
		 */
		public DataMapping size(String columnName) {
			columns.put(ColumnType.SIZE, columnName);
			return this;
		}

		/**
		 * Mapping for color column.
		 */
		public DataMapping color(String columnName) {
			columns.put(ColumnType.COLOR, columnName);
			return this;
		}

		/**
		 * Mapping for opacity column.
		 */
		public DataMapping opacity(String columnName) {
			columns.put(ColumnType.OPACITY, columnName);
			return this;
		}

		/**
		 * Mapping for facet_col column.
		 */
		public DataMapping facetCol(String columnName) {
			columns.put(ColumnType.FACET_COL, columnName);
			return this;
		}

		/**
		 * Mapping for facet_row column.
		 */
		public DataMapping facetRow(String columnName) {
			columns.put(ColumnType.FACET_ROW, columnName);
			return this;
		}

		private void insert(Map<String, Object> obj) {

			// None standard attribute name to preserve its meaning
			if (obj.containsKey("name"))
				obj.put("original_name", obj.get("name"));

			// TODO: some attributes are at marker level
			for (ColumnType value : ColumnType.values()) {
				if (columns.containsKey(value))
					obj.put(value.name().toLowerCase(), "$" + ref + "." + columns.get(value));
			}

			obj.put("colorRamp", colorRamp);
		}

		private void normalizePieTrace() {

			// Swap text and label column
			if (columns.containsKey(ColumnType.TEXT) && !columns.containsKey(ColumnType.LABELS)) {
				columns.put(ColumnType.LABELS, columns.get(ColumnType.TEXT));
				columns.remove(ColumnType.TEXT);
			}

			if (columns.containsKey(ColumnType.X))
				columns.put(ColumnType.VALUES, columns.get(ColumnType.X));
			else if (columns.containsKey(ColumnType.Y))
				columns.put(ColumnType.VALUES, columns.get(ColumnType.Y));

			// x and y can not be used in this context
			columns.remove(ColumnType.X);
			columns.remove(ColumnType.Y);
		}
	}

}
