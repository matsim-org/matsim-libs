package org.matsim.simwrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.simwrapper.viz.Viz;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to define the layout of a {@link Dashboard}.
 */
@SuppressWarnings("unchecked")
public final class Layout {

	private static final Logger log = LogManager.getLogger(Layout.class);

	/**
	 * Different context allows different set of arguments and output folders for the same type of output.
	 */
	private final String defaultContext;
	private final Map<String, Row> rows = new LinkedHashMap<>();

	/**
	 * Create new layout.
	 */
	Layout(String defaultContext) {
		this.defaultContext = defaultContext;
	}


	/**
	 * Returns a row, which can be used to add elements to it.
	 * If a row with the same name already exists it will be returned instead of creating a new one.
	 *
	 * @param name internal name of the row.
	 */
	public Row row(String name) {
		return rows.computeIfAbsent(name, Row::new);
	}

	/**
	 * Create the actual data representation.
	 */
	Map<String, List<Viz>> create(Data data) {

		Map<String, List<Viz>> rows = new LinkedHashMap<>();

		for (Row def : this.rows.values()) {

			List<Viz> row = rows.computeIfAbsent(def.name, (k) -> new ArrayList<>());
			for (Holder el : def.elements) {
				data.setCurrentContext(el.context != null ? el.context : defaultContext);

				try {
					Constructor<?> constructor = el.type.getConstructor();
					Viz o = (Viz) constructor.newInstance();
					el.el.configure(o, data);
					row.add(o);
				} catch (IllegalArgumentException e) {
					log.error("Illegal argument in dashboards", e);
					throw e;
				} catch (NoSuchMethodException e) {
					log.error("Could not construct the specified type. Probably public default constructor is missing.", e);
				} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
					log.error("Error occurred when constructing the viz object", e);
				}

				data.setCurrentContext(defaultContext);
			}
		}

		return rows;
	}


	/**
	 * One row holding multiple {@link Viz} elements.
	 */
	public static final class Row {

		private final String name;

		private final List<Holder> elements = new ArrayList<>();

		private Row(String name) {
			this.name = name;
		}

		/**
		 * Adds a viz element to the row with default context.
		 *
		 * @see #el(String, Class, VizElement)
		 */
		public <T extends Viz> Row el(Class<T> type, VizElement<T> el) {
			return el(null, type, el);
		}

		/**
		 * Adds a viz element to the row.
		 *
		 * @param context content which can hold different parameters for the same dashboard.
		 * @param type    class of the viz element
		 * @param el      setup of the viz element
		 * @param <T>     type of the element
		 */
		public <T extends Viz> Row el(String context, Class<T> type, VizElement<T> el) {
			elements.add(new Holder(context, type, (VizElement<Viz>) el));
			return this;
		}

	}

	private record Holder(String context, Class<?> type, VizElement<Viz> el) {
	}

}
