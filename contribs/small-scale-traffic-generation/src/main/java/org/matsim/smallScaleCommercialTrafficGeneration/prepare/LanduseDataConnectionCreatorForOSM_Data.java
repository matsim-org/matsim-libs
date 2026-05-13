package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;

import java.util.*;

/**
 * This class creates the connection between the landuse categories of the OSM landuse data and the employee data.
 *
 * @author Ricardo Ewert
 */
public class LanduseDataConnectionCreatorForOSM_Data implements LanduseDataConnectionCreator {

	@Override
	public Map<StructuralAttribute, List<String>> createLanduseDataConnection() {
		Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection = new EnumMap<>(StructuralAttribute.class);

		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.INHABITANTS,
			new ArrayList<>(Arrays.asList("residential", "apartments", "dormitory", "dwelling_house", "house",
				"retirement_home", "semidetached_house", "detached")));
		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.EMPLOYEE_PRIMARY,
			new ArrayList<>(Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse", "agricultural")));
		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.EMPLOYEE_CONSTRUCTION,
			new ArrayList<>(List.of("construction")));
		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.EMPLOYEE_SECONDARY,
			new ArrayList<>(Arrays.asList("industrial", "factory", "manufacture", "bakehouse")));
		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.EMPLOYEE_RETAIL,
			new ArrayList<>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.EMPLOYEE_TRAFFIC,
			new ArrayList<>(Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put(
			StructuralAttribute.EMPLOYEE_TERTIARY,
			new ArrayList<>(Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		return landuseCategoriesAndDataConnection;
	}
}
