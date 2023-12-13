package org.matsim.smallScaleCommercialTrafficGeneration;

import org.matsim.application.options.ShpOptions;

import java.nio.file.Path;

/**
 * Helper tests methods.
 */
public class SCTUtils {

	static ShpOptions.Index getZoneIndex(Path inputDataDirectory) {
		Path shapeFileZonePath = inputDataDirectory.resolve("shp/testZones.shp");
		return new ShpOptions(shapeFileZonePath, null, null).createIndex("areaID");
	}

}
