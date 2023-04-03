package org.matsim.simwrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.simwrapper.viz.Viz;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Helper class to define the layout of a {@link Dashboard}.
 */
@SuppressWarnings("unchecked")
public final class Layout {

	private static final Logger log = LogManager.getLogger(Layout.class);

	private final List<Holder> layouts = new ArrayList<>();


	/**
	 * Add a single element to row in the layout, using default context.
	 *
	 * @see #row(String, String, Class, RowElement)
	 */
	public <T extends Viz> void row(String name, Class<T> type, RowElement<T> el) {
		row(name, "", type, el);
	}

	/**
	 * Add a single element to row in the layout. Note that this only stores the specification. The actual code is executed later.
	 * <p>
	 * If a row with the same already exists, this element will be added to the same row.
	 * The order how this function is called will determine the order in the layout.
	 *
	 * @param name    name of the row
	 * @param context content which can hold different parameters for the same dashboard.
	 * @param type    class of the viz element
	 * @param el      setup of the viz element
	 * @param <T>     type of the element
	 */
	public <T extends Viz> void row(String name, String context, Class<T> type, RowElement<T> el) {
		layouts.add(new Holder(name, context, type, (RowElement<Viz>) el));
	}

	/**
	 * Create the actual data representation.
	 */
	Map<String, List<Viz>> create(Data data) {

		Map<String, List<Viz>> rows = new LinkedHashMap<>();

		for (Holder layout : layouts) {

			List<Viz> row = rows.computeIfAbsent(layout.name, (k) -> new ArrayList<>());

			data.setCurrentContext(layout.context);

			try {
				Constructor<?> constructor = layout.type.getConstructor();
				Viz o = (Viz) constructor.newInstance();
				layout.el.configure(o, data);
				row.add(o);

			} catch (NoSuchMethodException e) {
				log.error("Could not construct the specified type. Probably public default constructor is missing.", e);
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
				log.error("Error occurred when constructing the viz object", e);
			}

			data.setCurrentContext("");

		}

		return rows;
	}


	private record Holder(String name, String context, Class<?> type, RowElement<Viz> el) {
	}

}
