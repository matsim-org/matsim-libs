package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import java.util.*;

/**
 * This class creates the connection between the landuse categories of the OSM landuse data and the employee data.
 *
 * @author Ricardo Ewert
 */
public class LanduseDataConnectionCreatorForOSM_Data implements LanduseDataConnectionCreator{

	@Override
	public Map<String, List<String>> createLanduseDataConnection() {
		Map<String, List<String>> landuseCategoriesAndDataConnection = new HashMap<>();
		landuseCategoriesAndDataConnection.put("Inhabitants",
			new ArrayList<>(Arrays.asList("residential", "apartments", "dormitory", "dwelling_house", "house",
				"retirement_home", "semidetached_house", "detached")));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector", new ArrayList<>(
			Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse", "agricultural")));
		landuseCategoriesAndDataConnection.put("Employee Construction",
			new ArrayList<>(List.of("construction")));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
			new ArrayList<>(Arrays.asList("industrial", "factory", "manufacture", "bakehouse")));
		landuseCategoriesAndDataConnection.put("Employee Retail",
			new ArrayList<>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", new ArrayList<>(
			Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put("Employee Tertiary Sector Rest", new ArrayList<>(
			Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		return landuseCategoriesAndDataConnection;
	}
}
