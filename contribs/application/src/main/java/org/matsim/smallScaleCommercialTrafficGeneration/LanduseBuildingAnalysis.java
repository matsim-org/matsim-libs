/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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
package org.matsim.smallScaleCommercialTrafficGeneration;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ricardo Ewert
 *
 */
public class LanduseBuildingAnalysis {

	private static final Logger log = LogManager.getLogger(LanduseBuildingAnalysis.class);

	/**
	 * Creates a distribution of the given input data for each zone based on the
	 * used OSM data.
	 */
	static HashMap<String, Object2DoubleMap<String>> createInputDataDistribution(Path output,
			HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection, Path inputDataDirectory,
			String usedLanduseConfiguration, Path shapeFileLandusePath, Path shapeFileZonePath,
			Path shapeFileBuildingsPath, String shapeCRS, HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone)
			throws IOException {

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<>();
		HashMap<String, String> zoneIdNameConnection = new HashMap<>();
		Path outputFileInOutputFolder = output.resolve("calculatedData").resolve("dataDistributionPerZone.csv");

		landuseCategoriesAndDataConnection.put("Inhabitants",
				new ArrayList<String>(Arrays.asList("residential", "apartments", "dormitory", "dwelling_house", "house",
						"retirement_home", "semidetached_house", "detached")));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector", new ArrayList<String>(
				Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse", "agricultural")));
		landuseCategoriesAndDataConnection.put("Employee Construction",
				new ArrayList<String>(List.of("construction")));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
				new ArrayList<String>(Arrays.asList("industrial", "factory", "manufacture", "bakehouse")));
		landuseCategoriesAndDataConnection.put("Employee Retail",
				new ArrayList<String>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", new ArrayList<String>(
				Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put("Employee Tertiary Sector Rest", new ArrayList<String>(
				Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));

		if (usedLanduseConfiguration.equals("useExistingDataDistribution")) {
			Path existingDataDistribution = inputDataDirectory.resolve("dataDistributionPerZone.csv");

			if (!Files.exists(existingDataDistribution)) {
				log.error("Required data per zone file {} not found", existingDataDistribution);
			}

			try (BufferedReader reader = IOUtils.getBufferedReader(existingDataDistribution.toString())) {
				CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
						.setSkipHeaderRecord(true).build().parse(reader);

				for (CSVRecord record : parse) {
					String zoneID = record.get("areaID");
					resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
					for (int n = 2; n < parse.getHeaderMap().size(); n++) {
						resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n),
								Double.parseDouble(record.get(n)), Double::sum);
					}
				}
			}
			log.info("Data distribution for " + resultingDataPerZone.size() + " zones was imported from ",
					existingDataDistribution);
			Files.copy(existingDataDistribution, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
		}

		else {

			log.info("New analyze for data distribution is started. The used method is: " + usedLanduseConfiguration);

			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<>();
			createLanduseDistribution(landuseCategoriesPerZone, shapeFileLandusePath, shapeFileZonePath,
					usedLanduseConfiguration, shapeFileBuildingsPath, landuseCategoriesAndDataConnection,
					buildingsPerZone, zoneIdNameConnection, shapeCRS);

			HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<>();
			readAreaData(investigationAreaData, inputDataDirectory);

			createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone,
					landuseCategoriesAndDataConnection);

			SmallScaleCommercialTrafficUtils.writeResultOfDataDistribution(resultingDataPerZone, outputFileInOutputFolder,
					zoneIdNameConnection);
		}

		return resultingDataPerZone;
	}

	/**
	 * Creates the resulting data for each zone based on the landuse distribution
	 * and the original data.
	 */
	private static void createResultingDataForLanduseInZones(
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			HashMap<String, HashMap<String, Integer>> investigationAreaData,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection) {

		HashMap<String, Object2DoubleOpenHashMap<String>> totalSquareMetersPerCategory = new HashMap<String, Object2DoubleOpenHashMap<String>>();
		HashMap<String, Object2DoubleOpenHashMap<String>> totalEmployeesInCategoriesPerZone = new HashMap<String, Object2DoubleOpenHashMap<String>>();
		HashMap<String, Object2DoubleOpenHashMap<String>> totalEmployeesPerCategories = new HashMap<String, Object2DoubleOpenHashMap<String>>();

		investigationAreaData.keySet()
				.forEach(c -> totalSquareMetersPerCategory.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));
		investigationAreaData.keySet().forEach(
				c -> totalEmployeesInCategoriesPerZone.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));
		investigationAreaData.keySet()
				.forEach(c -> totalEmployeesPerCategories.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));

		// connects the collected landuse data with the needed categories
		for (String zoneId : landuseCategoriesPerZone.keySet()) {
			String investigationArea = zoneId.split("_")[0];
			resultingDataPerZone.put(zoneId, new Object2DoubleOpenHashMap<>());
			for (String categoryLanduse : landuseCategoriesPerZone.get(zoneId).keySet())
				for (String categoryData : landuseCategoriesAndDataConnection.keySet()) {
					resultingDataPerZone.get(zoneId).mergeDouble(categoryData, 0., Double::sum);
					if (landuseCategoriesAndDataConnection.get(categoryData).contains(categoryLanduse)) {
						double additionalArea = landuseCategoriesPerZone.get(zoneId).getDouble(categoryLanduse);
						// because the category commercial is in two categories (traffic/parcels and
						// Tertiary Sector Rest
						if (categoryLanduse.equals("commercial"))
							additionalArea = additionalArea * 0.5;
						resultingDataPerZone.get(zoneId).mergeDouble(categoryData, additionalArea, Double::sum);
						totalSquareMetersPerCategory.get(investigationArea).mergeDouble(categoryData, additionalArea,
								Double::sum);
					}
				}
		}

		/*
		 * creates the percentages of each category and zones based on the sum in this
		 * category
		 */
		HashMap<String, Object2DoubleOpenHashMap<String>> checkPercentages = new HashMap<String, Object2DoubleOpenHashMap<String>>();
		investigationAreaData.keySet()
				.forEach(c -> checkPercentages.computeIfAbsent(c, k -> new Object2DoubleOpenHashMap<>()));
		for (String zoneId : resultingDataPerZone.keySet())
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				String investigationArea = zoneId.split("_")[0];
				double newValue = resultingDataPerZone.get(zoneId).getDouble(categoryData)
						/ totalSquareMetersPerCategory.get(investigationArea).getDouble(categoryData);
				resultingDataPerZone.get(zoneId).replace(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), newValue);
				checkPercentages.get(investigationArea).mergeDouble(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
			}
		// tests the result
		for (String investigationArea : investigationAreaData.keySet()) {
			for (String category : checkPercentages.get(investigationArea).keySet())
				if (Math.abs(1 - checkPercentages.get(investigationArea).getDouble(category)) > 0.01)
					throw new RuntimeException("Sum of percentages is not 1. For " + category + " the sum is "
							+ checkPercentages.get(investigationArea).getDouble(category) + "%");
		}
		// calculates the data per zone and category data
		for (String zoneId : resultingDataPerZone.keySet()) {
			String investigationArea = zoneId.split("_")[0];
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double percentageValue = resultingDataPerZone.get(zoneId).getDouble(categoryData);
				int inputDataForCategory = investigationAreaData.get(investigationArea).get(categoryData);
				double resultingNumberPerCategory = percentageValue * inputDataForCategory;
				resultingDataPerZone.get(zoneId).replace(categoryData, percentageValue, resultingNumberPerCategory);
				totalEmployeesPerCategories.get(investigationArea).mergeDouble(categoryData, resultingNumberPerCategory,
						Double::sum);
				if (!categoryData.equals("Inhabitants"))
					totalEmployeesInCategoriesPerZone.get(investigationArea).mergeDouble(zoneId,
							resultingNumberPerCategory, Double::sum);
			}
			if (totalEmployeesInCategoriesPerZone.get(investigationArea).getDouble(zoneId) != 0)
			resultingDataPerZone.get(zoneId).mergeDouble("Employee",
					totalEmployeesInCategoriesPerZone.get(investigationArea).getDouble(zoneId), Double::sum);
		}
	}

	/**
	 * Method create the percentage for each land use category in each zone based on
	 * the sum of this category in all zones of the zone shape file
	 */
	private static void createLanduseDistribution(HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			Path shapeFileLandusePath, Path shapeFileZonePath, String usedLanduseConfiguration,
			Path shapeFileBuildingsPath, HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection,
			HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone,
			HashMap<String, String> zoneIdNameConnection, String shapeCRS) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland", "construction");

		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, shapeCRS, StandardCharsets.UTF_8);
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, shapeCRS, StandardCharsets.UTF_8);

		List<SimpleFeature> landuseFeatures = shpLanduse.readFeatures();
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();

		Index indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shpZones.getShapeCrs());

		for (SimpleFeature singleZone : zonesFeatures) {
			Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
			landuseCategoriesPerZone.put((String) singleZone.getAttribute("areaID"), landusePerCategory);
			zoneIdNameConnection.put((String) singleZone.getAttribute("areaID"),
					(String) singleZone.getAttribute("name"));
		}

		if (usedLanduseConfiguration.equals("useOSMBuildingsAndLanduse")) {

			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, shapeCRS, StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			analyzeBuildingType(buildingsFeatures, buildingsPerZone, landuseCategoriesAndDataConnection,
					shapeFileLandusePath, indexZones, shpLanduse.getShapeCrs());

			for (String zone : buildingsPerZone.keySet())
				for (String category : buildingsPerZone.get(zone).keySet())
					if (category.equals("Employee") || category.equals("Inhabitants"))
						for (SimpleFeature building : buildingsPerZone.get(zone).get(category)) {
							String[] buildingTypes = ((String) building.getAttribute("type")).split(";");
							for (String singleCategoryOfBuilding : buildingTypes) {
								double buildingLevels;
								if (building.getAttribute("levels") == null)
									buildingLevels = 1;
								else
									buildingLevels = (long) building.getAttribute("levels")
											/ (double) buildingTypes.length;
								double area = (int) (long) building.getAttribute("area") * buildingLevels;
								landuseCategoriesPerZone.get(zone).mergeDouble(singleCategoryOfBuilding, area,
										Double::sum);
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
								.get((String) singleZone.getAttribute("region") + "_"
										+ (String) singleZone.getAttribute("id"))
								.mergeDouble((String) singleLanduseFeature.getAttribute("fclass"),
										(double) singleLanduseFeature.getAttribute("area"), Double::sum);
					}
			}
	}

	/**
	 * Reads the input data for certain areas from the csv file.
	 */
	private static void readAreaData(HashMap<String, HashMap<String, Integer>> areaData, Path inputDataDirectory)
			throws IOException {

		Path areaDataPath = inputDataDirectory.resolve("investigationAreaData.csv");
		if (!Files.exists(areaDataPath)) {
			log.error("Required input data file {} not found", areaDataPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(areaDataPath),
				CSVFormat.Builder.create(CSVFormat.TDF).setHeader().setSkipHeaderRecord(true).build())) {

			for (CSVRecord record : parser) {
				HashMap<String, Integer> lookUpTable = new HashMap<>();
				for (String csvRecord : parser.getHeaderMap().keySet()) {
					if (parser.getHeaderMap().get(csvRecord) > 0)
						lookUpTable.put(csvRecord, Integer.valueOf(record.get(csvRecord)));
				}
				areaData.put(record.get("Area"), lookUpTable);
			}
		}
	}

	/**
	 * Analysis the building types so that you have the buildings per zone and type.
	 */
	static void analyzeBuildingType(List<SimpleFeature> buildingsFeatures,
			HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone,
			HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection, Path shapeFileLandusePath,
			Index indexZones, String crsLanduse) {

		Index indexLanduse = SmallScaleCommercialTrafficUtils.getIndexLanduse(shapeFileLandusePath, crsLanduse);

		int countOSMObjects = 0;
		log.info("Analyzing buildings types. This may take some time...");
		for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
			countOSMObjects++;
			if (countOSMObjects % 10000 == 0)
				log.info("Investigate Building " + countOSMObjects + " of " + buildingsFeatures.size() + " buildings: "
						+ Math.round((double) countOSMObjects / buildingsFeatures.size() * 100) + " %");

			List<String> categoriesOfBuilding = new ArrayList<String>();
			String[] buildingTypes;
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) singleBuildingFeature.getDefaultGeometry()).getCentroid());
			String singleZone = indexZones.query(centroidPointOfBuildingPolygon);
			String buildingType = String.valueOf(singleBuildingFeature.getAttribute("type"));
			if (buildingType.equals("") || buildingType.equals("null") || buildingType.equals("yes")) {
				buildingType = indexLanduse.query(centroidPointOfBuildingPolygon);
				buildingTypes = new String[] { buildingType };
			} else {
				buildingType.replace(" ", "");
				buildingTypes = buildingType.split(";");
			}
			singleBuildingFeature.setAttribute("type", String.join(";", buildingTypes));
			boolean isEmployeeCategory = false;
			for (String singleBuildingType : buildingTypes) {
				for (String category : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(category).contains(singleBuildingType)
							&& !categoriesOfBuilding.contains(category)) {
						categoriesOfBuilding.add(category);
						if (category.contains("Employee"))
							isEmployeeCategory = true;
					}
				}
			}
			if (isEmployeeCategory)
				categoriesOfBuilding.add("Employee");
			if (singleZone != null) {
				categoriesOfBuilding.forEach(c -> buildingsPerZone
						.computeIfAbsent(singleZone, k -> new HashMap<String, ArrayList<SimpleFeature>>())
						.computeIfAbsent(c, k -> new ArrayList<SimpleFeature>()).add(singleBuildingFeature));
			}
		}
		log.info("Finished analyzing buildings types.");
	}
}
