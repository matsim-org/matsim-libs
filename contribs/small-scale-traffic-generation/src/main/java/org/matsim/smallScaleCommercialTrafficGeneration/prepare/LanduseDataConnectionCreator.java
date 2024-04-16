package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import java.util.List;
import java.util.Map;

/**
 * Interface for creating the connection between landuse categories and the employee data.
 *
 * @author Ricardo Ewert
 */
public interface LanduseDataConnectionCreator {
	void createLanduseDataConnection(Map<String, List<String>> landuseCategoriesAndDataConnection);
}
