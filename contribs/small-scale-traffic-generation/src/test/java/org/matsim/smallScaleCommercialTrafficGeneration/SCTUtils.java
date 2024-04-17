package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.application.options.ShpOptions;

import java.nio.file.Path;

/**
 * Helper tests methods.
 */
public class SCTUtils {

	public static ShpOptions.Index getZoneIndex(Path inputDataDirectory) {
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		return new ShpOptions(shapeFileZonePath, null, null).createIndex("name");
	}

	public static ShpOptions.Index getIndexLanduse(Path inputDataDirectory) {
		Path shapeFileLandusePath = inputDataDirectory.resolve("shp/testLanduse.shp");
		return new ShpOptions(shapeFileLandusePath, null, null).createIndex("fclass");
	}

	public static ShpOptions.Index getIndexBuildings(Path inputDataDirectory) {
		Path shapeFileBuildingsPath = inputDataDirectory.resolve("shp/testBuildings.shp");
		return new ShpOptions(shapeFileBuildingsPath, null, null).createIndex("type");
	}

	public static ShpOptions.Index getIndexRegions(Path inputDataDirectory) {
		Path shapeFileRegionsPath = inputDataDirectory.resolve("shp/testRegions.shp");
		return new ShpOptions(shapeFileRegionsPath, null, null).createIndex("region");
	}

}
