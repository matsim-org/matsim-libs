package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import org.matsim.application.options.ShpOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

class SmallScaleCommercialTrafficPrepareUtils{
	private SmallScaleCommercialTrafficPrepareUtils(){}


	/**
	 * Creates and return the Index of the landuse shape.
	 *
	 * @param shapeFileLandusePath       	Path to the shape file of the landuse
     * @param shapeCRS 				 		CRS of the shape file
     * @param shapeFileLanduseTypeColumn 	Column name of the landuse in the shape file
     * @return indexLanduse
	 */
	 public static ShpOptions.Index getIndexLanduse( Path shapeFileLandusePath, String shapeCRS, String shapeFileLanduseTypeColumn ) {
		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, shapeCRS, StandardCharsets.UTF_8);
		if (shpLanduse.readFeatures().getFirst().getAttribute(shapeFileLanduseTypeColumn) == null)
			throw new NullPointerException("The column '" + shapeFileLanduseTypeColumn + "' does not exist in the landuse shape file. Please check the input.");
		return shpLanduse.createIndex(shapeCRS, shapeFileLanduseTypeColumn);
	}
	/**
	 * Creates and return the Index of the building shape.
	 *
	 * @param shapeFileBuildingsPath      	Path to the shape file of the buildings
     * @param shapeCRS 				 		CRS of the shape file
     * @param shapeFileBuildingTypeColumn 	Column name of the building in the shape file
     * @return indexBuildings
	 */
	public static ShpOptions.Index getIndexBuildings( Path shapeFileBuildingsPath, String shapeCRS, String shapeFileBuildingTypeColumn ) {
		ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, shapeCRS, StandardCharsets.UTF_8);
		if (shpBuildings.readFeatures().getFirst().getAttribute(shapeFileBuildingTypeColumn) == null)
			throw new NullPointerException("The column '" + shapeFileBuildingTypeColumn + "' does not exist in the building shape file. Please check the input.");

		return shpBuildings.createIndex(shapeCRS, shapeFileBuildingTypeColumn);
	}
	/**
	 * Creates and return the Index of the regions shapes.
	 *
	 * @param shapeFileRegionsPath     Path to the shape file of the regions
	 * @param shapeCRS                 CRS of the shape file
	 * @param regionsShapeRegionColumn Column name of the region in the shape file
	 * @return indexRegions
	 */
	public static ShpOptions.Index getIndexRegions( Path shapeFileRegionsPath, String shapeCRS, String regionsShapeRegionColumn ) {
		ShpOptions shpRegions = new ShpOptions(shapeFileRegionsPath, shapeCRS, StandardCharsets.UTF_8);
		if (shpRegions.readFeatures().getFirst().getAttribute(regionsShapeRegionColumn) == null)
			throw new NullPointerException("The column '" + regionsShapeRegionColumn + "' does not exist in the region shape file. Please check the input.");
		return shpRegions.createIndex(shapeCRS, regionsShapeRegionColumn);
	}
}
