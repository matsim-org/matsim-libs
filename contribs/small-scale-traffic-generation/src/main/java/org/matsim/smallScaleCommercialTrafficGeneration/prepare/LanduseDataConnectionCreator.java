package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interface for creating the connection between landuse or building categories and the required employee data categories for the simulation.
 *
 * @author Ricardo Ewert
 */
public interface LanduseDataConnectionCreator {
	Map<String, List<String>> createLanduseDataConnection();

	/**
	 * Counts the number of employee categories in which a type is represented.
	 *
	 * @return
	 */
	static int getNumberOfEmployeeCategoriesOfThisTyp(Map<String, List<String>> landuseCategoriesAndDataConnection, String type) {
		AtomicInteger count = new AtomicInteger();
		landuseCategoriesAndDataConnection.values().forEach(list -> {
			if (list.contains(type)) {
				count.getAndIncrement();
			}
		});
		return count.get();
	}

	;
}
