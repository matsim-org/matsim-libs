package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.ZoneAttribute;

import java.util.*;

/**
 * This class creates the connection between the landuse categories of the OSM landuse data and the employee data.
 *
 * @author Ricardo Ewert
 */
public class LanduseDataConnectionCreatorForOSM_Data implements LanduseDataConnectionCreator {

	@Override
	public Map<ZoneAttribute, List<String>> createLanduseDataConnection() {
		Map<ZoneAttribute, List<String>> landuseCategoriesAndDataConnection = new EnumMap<>( ZoneAttribute.class);

		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.INHABITANTS,
			new ArrayList<>(Arrays.asList("residential", "apartments", "dormitory", "dwelling_house", "house",
				"retirement_home", "semidetached_house", "detached")));
		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.EMPLOYEE_PRIMARY,
			new ArrayList<>(Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse", "agricultural")));
		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.EMPLOYEE_CONSTRUCTION,
			new ArrayList<>(List.of("construction")));
		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.EMPLOYEE_SECONDARY,
			new ArrayList<>(Arrays.asList("industrial", "factory", "manufacture", "bakehouse")));
		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.EMPLOYEE_RETAIL,
			new ArrayList<>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.EMPLOYEE_TRAFFIC,
			new ArrayList<>(Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put(
			ZoneAttribute.EMPLOYEE_TERTIARY,
			new ArrayList<>(Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		return landuseCategoriesAndDataConnection;
	}
}
