/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import com.google.common.base.Joiner;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Ricardo Ewert
 *
 */
public class LanduseBuildingAnalysis {

	private static final Logger log = LogManager.getLogger(LanduseBuildingAnalysis.class);
	private static final Joiner JOIN = Joiner.on("\t");
	static Map<String, Object2DoubleMap<StructuralAttribute>> sumsOfAreasPerZoneAndCategory = new HashMap<>();

	/**
	 * Creates a distribution of the given input data for each zone based on the
	 * used OSM data.
	 */
	public static Map<String, Object2DoubleMap<StructuralAttribute>> createInputDataDistribution(Path outputDataDistributionFile,
																					Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection,
																					String usedLanduseConfiguration, Index indexLanduse,
																					Index indexZones,
																					Index indexBuildings, Index indexInvestigationAreaRegions,
																					String shapeFileZoneNameColumn,
																					Map<String, Map<StructuralAttribute, List<SimpleFeature>>> buildingsPerZone,
																					Path pathToInvestigationAreaData,
																					String shapeFileBuildingTypeColumn)
		throws IOException {

		Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone = new HashMap<>();
		Map<String, String> zoneIdRegionConnection = new HashMap<>();

		log.info("New analyze for data distribution is started. The used method is: {}", usedLanduseConfiguration);
		Map<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<>();
		createLanduseDistribution(landuseCategoriesPerZone, indexLanduse, indexZones, indexInvestigationAreaRegions,
			usedLanduseConfiguration, indexBuildings, landuseCategoriesAndDataConnection,
			buildingsPerZone, shapeFileZoneNameColumn, zoneIdRegionConnection, shapeFileBuildingTypeColumn);

		Map<String, Map<StructuralAttribute, Integer>> investigationAreaData = new HashMap<>();
		readAreaData(investigationAreaData, pathToInvestigationAreaData);

		createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone,
			landuseCategoriesAndDataConnection, zoneIdRegionConnection);

		writeResultOfDataDistribution(resultingDataPerZone, outputDataDistributionFile,
			zoneIdRegionConnection);


		return resultingDataPerZone;
	}

	/**
	 * Creates the resulting data for each zone based on the landuse distribution
	 * and the original data.
	 */
	private static void createResultingDataForLanduseInZones(
		Map<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
		Map<String, Map<StructuralAttribute, Integer>> investigationAreaData,
		Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone,
		Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection, Map<String, String> zoneIdRegionConnection) {

		Map<String, Object2DoubleOpenHashMap<StructuralAttribute>> totalSquareMetersPerCategory = new HashMap<>();
		Map<String, Object2DoubleOpenHashMap<String>> totalEmployeesInCategoriesPerZone = new HashMap<>();
		Map<String, Object2DoubleOpenHashMap<StructuralAttribute>> totalEmployeesPerCategories = new HashMap<>();

		investigationAreaData.keySet()
				.forEach(c -> totalSquareMetersPerCategory.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));
		investigationAreaData.keySet().forEach(
				c -> totalEmployeesInCategoriesPerZone.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));
		investigationAreaData.keySet()
				.forEach(c -> totalEmployeesPerCategories.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));

		// connects the collected landuse data with the needed categories
		for (String zoneId : landuseCategoriesPerZone.keySet()) {
			String regionOfZone = zoneIdRegionConnection.get(zoneId);
			resultingDataPerZone.put(zoneId, new Object2DoubleOpenHashMap<>());
			for (String categoryLanduse : landuseCategoriesPerZone.get(zoneId).keySet())
				for (StructuralAttribute categoryData : landuseCategoriesAndDataConnection.keySet()) {
					resultingDataPerZone.get(zoneId).mergeDouble(categoryData, 0., Double::sum);
					if (landuseCategoriesAndDataConnection.get(categoryData).contains(categoryLanduse)) {
						double additionalArea = landuseCategoriesPerZone.get(zoneId).getDouble(categoryLanduse);
//						// because the categoryLanduse can be in two categories (e.g., traffic/parcels and Tertiary Sector Rest
						additionalArea = additionalArea / LanduseDataConnectionCreator.getNumberOfEmployeeCategoriesOfThisTyp(landuseCategoriesAndDataConnection, categoryLanduse);
						resultingDataPerZone.get(zoneId).mergeDouble(categoryData, additionalArea, Double::sum);
						totalSquareMetersPerCategory.get(regionOfZone).mergeDouble(categoryData, additionalArea,
								Double::sum);
					}
				}
		}
		for (Map.Entry<String, Object2DoubleMap<StructuralAttribute>> entry : resultingDataPerZone.entrySet()) {
			sumsOfAreasPerZoneAndCategory.put(entry.getKey(), new Object2DoubleOpenHashMap<>(entry.getValue()));
		}
		/*
		 * creates the percentages of each category and zones based on the sum in this
		 * category
		 */
		Map<String, Object2DoubleOpenHashMap<StructuralAttribute>> checkPercentages = new HashMap<>();
		investigationAreaData.keySet()
				.forEach(c -> checkPercentages.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));
		for (String zoneId : resultingDataPerZone.keySet())
			for (StructuralAttribute categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				String regionOfZone = zoneIdRegionConnection.get(zoneId);
				double newValue = resultingDataPerZone.get(zoneId).getDouble(categoryData)
						/ totalSquareMetersPerCategory.get(regionOfZone).getDouble(categoryData);
				resultingDataPerZone.get(zoneId).replace(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), newValue);
				checkPercentages.get(regionOfZone).mergeDouble(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
			}
		// tests the result
		for (String investigationArea : investigationAreaData.keySet()) {
			for (StructuralAttribute category : checkPercentages.get(investigationArea).keySet())
				if (Math.abs(1 - checkPercentages.get(investigationArea).getDouble(category)) > 0.01)
					throw new RuntimeException("Sum of percentages is not 1. For " + category + " the sum is "
							+ checkPercentages.get(investigationArea).getDouble(category) + "%");
		}
		// calculates the data per zone and category data
		for (String zoneId : resultingDataPerZone.keySet()) {
			String regionOfZone = zoneIdRegionConnection.get(zoneId);
			for (StructuralAttribute categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double percentageValue = resultingDataPerZone.get(zoneId).getDouble(categoryData);
				int inputDataForCategory = investigationAreaData.get(regionOfZone).get(categoryData);
				double resultingNumberPerCategory = percentageValue * inputDataForCategory;
				resultingDataPerZone.get(zoneId).replace(categoryData, percentageValue, resultingNumberPerCategory);
				totalEmployeesPerCategories.get(regionOfZone).mergeDouble(categoryData, resultingNumberPerCategory,
						Double::sum);
				if (!categoryData.equals(StructuralAttribute.INHABITANTS))
					totalEmployeesInCategoriesPerZone.get(regionOfZone).mergeDouble(zoneId,
							resultingNumberPerCategory, Double::sum);
			}
			if (totalEmployeesInCategoriesPerZone.get(regionOfZone).getDouble(zoneId) != 0)
				resultingDataPerZone.get(zoneId).mergeDouble(StructuralAttribute.EMPLOYEE,
					totalEmployeesInCategoriesPerZone.get(regionOfZone).getDouble(zoneId), Double::sum);
		}
	}

	/**
	 * Method create the percentage for each land use category in each zone based on
	 * the sum of this category in all zones of the zone shape file
	 */
	private static void createLanduseDistribution(Map<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
												  Index indexLanduse, Index indexZones, Index indexInvestigationAreaRegions, String usedLanduseConfiguration,
												  Index indexBuildings, Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection,
												  Map<String, Map<StructuralAttribute, List<SimpleFeature>>> buildingsPerZone,
												  String shapeFileZoneNameColumn, Map<String, String> zoneIdRegionConnection,
												  String shapeFileBuildingTypeColumn) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland", "construction");

		List<SimpleFeature> landuseFeatures = indexLanduse.getAllFeatures();
		List<SimpleFeature> zonesFeatures = indexZones.getAllFeatures();
		for (SimpleFeature singleZone : zonesFeatures) {
			// get the region of the zone
			Coord middleCoordOfZone = MGC.point2Coord(((Geometry) singleZone.getDefaultGeometry()).getCentroid());
			String regionName = indexInvestigationAreaRegions.query(middleCoordOfZone);
			if (regionName != null) {
				Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
				String zoneID = (String) singleZone.getAttribute(shapeFileZoneNameColumn);
				var previousValue = landuseCategoriesPerZone.putIfAbsent(zoneID, landusePerCategory);
				if (previousValue != null) {
					throw new IllegalStateException(
						"Key " + zoneID + " already exists in the zone map. This should not happen. Please check if the data in the column " + shapeFileZoneNameColumn + " is unique.");
				}
				zoneIdRegionConnection.put(zoneID, regionName);
			} else
				log.warn("The zone {} has no region assigned. This may lead to problems in the analysis.", singleZone.getAttribute(shapeFileZoneNameColumn));
		}

		if (usedLanduseConfiguration.equals("useOSMBuildingsAndLanduse")) {

			List<SimpleFeature> buildingsFeatures = indexBuildings.getAllFeatures();
			analyzeBuildingType(buildingsFeatures, buildingsPerZone, landuseCategoriesAndDataConnection,
				indexLanduse, indexZones, shapeFileBuildingTypeColumn);

			for (String zone : buildingsPerZone.keySet())
				for (StructuralAttribute category : buildingsPerZone.get(zone).keySet())
					if (category.equals(StructuralAttribute.EMPLOYEE) || category.equals(StructuralAttribute.INHABITANTS))
						for (SimpleFeature building : buildingsPerZone.get(zone).get(category)) {
							String[] buildingTypes = ((String) building.getAttribute(shapeFileBuildingTypeColumn)).split(";");
							for (String singleCategoryOfBuilding : buildingTypes) {
								double area = calculateAreaPerBuildingCategory(building, buildingTypes);
								if (landuseCategoriesPerZone.get(zone) != null)
									landuseCategoriesPerZone.get(zone).mergeDouble(singleCategoryOfBuilding, area,
										Double::sum);
								else
									log.warn("The zone {} was not set as part of the regions. So this zone is skipped", zone);
							}
						}
		} else if (usedLanduseConfiguration.equals("useOnlyOSMLanduse"))
			for (SimpleFeature singleLanduseFeature : landuseFeatures) {
				if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
					continue;
				Point centroidPointOfLandusePolygon = ((Geometry) singleLanduseFeature.getDefaultGeometry())
						.getCentroid();

				for (SimpleFeature singleZone : zonesFeatures)
					if (((Geometry) singleZone.getDefaultGeometry()).contains(centroidPointOfLandusePolygon)) {
						landuseCategoriesPerZone
								.get(singleZone.getAttribute("region") + "_"
										+ singleZone.getAttribute("id"))
								.mergeDouble((String) singleLanduseFeature.getAttribute("fclass"),
										(double) singleLanduseFeature.getAttribute("area"), Double::sum);
					}
			}
		else {
			throw new RuntimeException("The used landuse configuration is not known.");

		}
	}

	/**
	 * Calculates the area for each usage category of a building based on the number of levels and the area.
	 *
	 * @param building      the building to be analyzed
	 * @param buildingTypes the types of the building
	 * @return the area of the building for each category
	 */
	public static double calculateAreaPerBuildingCategory(SimpleFeature building, String[] buildingTypes) {
		double buildingLevels;
		double buildingLevelsPerType;
		if (building.getAttribute("levels") == null || String.valueOf(building.getAttribute("levels")).isEmpty())
			buildingLevels = 1;
		else {
			Object levelsAttribute = building.getAttribute("levels");
			if (levelsAttribute instanceof String) {
				buildingLevels = Double.parseDouble((String) levelsAttribute);
			} else if (levelsAttribute instanceof Long) {
				buildingLevels = (long) levelsAttribute;
			} else {
				throw new RuntimeException("The attribute 'levels' of the building shape is not a string or a long.");
			}
		}
		buildingLevelsPerType = buildingLevels / (double) buildingTypes.length;

		double groundArea;
		if (building.getAttribute("area") != null)
			groundArea = (int) (long) building.getAttribute("area");
		else
			groundArea = ((Geometry) building.getDefaultGeometry()).getArea();
		return groundArea * buildingLevelsPerType;
	}

	/**
	 * Reads the input data for certain areas from the csv file.
	 */
	private static void readAreaData(Map<String, Map<StructuralAttribute, Integer>> areaData, Path pathToInvestigationAreaData)
			throws IOException {

		if (!Files.exists(pathToInvestigationAreaData)) {
			log.error("Required input data file {} not found", pathToInvestigationAreaData);
		}
		try (CSVParser parser = CSVParser.parse(Files.newBufferedReader(pathToInvestigationAreaData),
			CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).get())) {

			for (CSVRecord record : parser) {
				Map<StructuralAttribute, Integer> lookUpTable = new HashMap<>();
				for (String csvRecord : parser.getHeaderMap().keySet()) {
					if (parser.getHeaderMap().get(csvRecord) > 0) {
						Optional<StructuralAttribute> category = StructuralAttribute.fromLabel(csvRecord);
						if (category.isEmpty()) {
							throw new RuntimeException("The category " + csvRecord + " is not known. Please check the header of the input data file.");
						}
						lookUpTable.put(category.get(), Integer.valueOf(record.get(csvRecord)));
					}
				}
				areaData.put(record.get("Region"), lookUpTable);
			}
		}
	}

	/**
	 * Analysis of the building types so that you have the buildings per zone and type.
	 */
	static void analyzeBuildingType(List<SimpleFeature> buildingsFeatures,
									Map<String, Map<StructuralAttribute, List<SimpleFeature>>> buildingsPerZone,
									Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection, Index indexLanduse,
									Index indexZones, String shapeFileBuildingTypeColumn) {

		int countOSMObjects = 0;
		log.info("Analyzing buildings types. This may take some time...");
		for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
			countOSMObjects++;
			if ((countOSMObjects % (int) (buildingsFeatures.size() * 0.05)) == 0)
				log.info("Investigate Building {} of {} buildings: {} %", countOSMObjects, buildingsFeatures.size(),
					Math.round((double) countOSMObjects / buildingsFeatures.size() * 100));

			List<StructuralAttribute> categoriesOfBuilding = new ArrayList<>();
			String[] buildingTypes;
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) singleBuildingFeature.getDefaultGeometry()).getCentroid());
			String singleZone = indexZones.query(centroidPointOfBuildingPolygon);
			// if the building is not in a zone, it is not considered
			if (singleZone == null)
				continue;
			String buildingType = String.valueOf(singleBuildingFeature.getAttribute(shapeFileBuildingTypeColumn));
			if (buildingType.isEmpty() || buildingType.equals("null") || buildingType.equals("yes")) {
				buildingType = indexLanduse.query(centroidPointOfBuildingPolygon);
				buildingTypes = new String[] { buildingType };
			} else {
				buildingType.replace(" ", "");
				buildingTypes = buildingType.split(";");
			}
			singleBuildingFeature.setAttribute(shapeFileBuildingTypeColumn, String.join(";", buildingTypes));
			boolean isEmployeeCategory = false;
			for (String singleBuildingType : buildingTypes) {
				for (StructuralAttribute category : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(category).contains(singleBuildingType)
							&& !categoriesOfBuilding.contains(category)) {
						categoriesOfBuilding.add(category);
						if (category.getLabel().contains(StructuralAttribute.EMPLOYEE.getLabel()))
							isEmployeeCategory = true;
					}
				}
			}
			if (isEmployeeCategory)
				categoriesOfBuilding.add(StructuralAttribute.EMPLOYEE);

			categoriesOfBuilding.forEach(c -> buildingsPerZone
				.computeIfAbsent(singleZone, k -> new HashMap<>())
				.computeIfAbsent(c, k -> new ArrayList<>()).add(singleBuildingFeature));
		}
		log.info("Finished analyzing buildings types.");
	}


	/**
	 * Writes a csv file with the result of the distribution per zone of the input data.
	 */
	private static void writeResultOfDataDistribution(Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone,
											  Path outputFileInOutputFolder, Map<String, String> zoneIdRegionConnection)
		throws IOException {

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInOutputFolder, zoneIdRegionConnection);
		log.info("The data distribution is finished and written to: {}", outputFileInOutputFolder);
	}

	/**
	 * Writer of data distribution data.
	 */
	private static void writeCSVWithCategoryHeader(Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone,
												   Path outputFileInInputFolder,
												   Map<String, String> zoneIdRegionConnection) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
			StandardCharsets.UTF_8, true);
		try {
			String[] header = Stream.concat(Stream.of("zoneID", "region"),
				Stream.of(StructuralAttribute.INHABITANTS, StructuralAttribute.EMPLOYEE, StructuralAttribute.EMPLOYEE_PRIMARY,
					StructuralAttribute.EMPLOYEE_CONSTRUCTION, StructuralAttribute.EMPLOYEE_SECONDARY, StructuralAttribute.EMPLOYEE_RETAIL,
					StructuralAttribute.EMPLOYEE_TRAFFIC, StructuralAttribute.EMPLOYEE_TERTIARY).map(StructuralAttribute::getLabel)).toArray(
				String[]::new);

			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				row.add(zoneIdRegionConnection.get(zone));
				for (String category : header) {
					if (!category.equals("zoneID") && !category.equals("region"))
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(StructuralAttribute.fromLabel(category).get()))));
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			log.error("Could not write the csv file with the data distribution data.", e);
		}
	}

	/**
	 * This method calculates the share of the building area of the related area of the complete zone of this category.
	 *
	 * @param zone                               the zone of the building
	 * @param area                               the area of the building
	 * @param assignedDataType                   the data type of the building
	 * @return
	 */
	static double getShareOfTheBuildingAreaOfTheRelatedAreaOfTheZone(String zone, int area, StructuralAttribute assignedDataType){
		if (assignedDataType.equals(StructuralAttribute.EMPLOYEE)){
			double sumOfEmployees = sumsOfAreasPerZoneAndCategory.get(zone).keySet().stream().filter(s -> s.getLabel().contains(StructuralAttribute.EMPLOYEE.getLabel())).mapToInt(s -> (int) sumsOfAreasPerZoneAndCategory.get(zone).getDouble(s)).sum();
			return area / sumOfEmployees;
		}
			return area / sumsOfAreasPerZoneAndCategory.get(zone).getDouble(assignedDataType);
	}
}
