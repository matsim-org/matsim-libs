package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.testcases.MatsimTestUtils;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class RunCreateDataDistributionOfStructureDataTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDataDistributionOfStructureData() throws MalformedURLException {
		String useOSMBuildingsAndLanduse = "useOSMBuildingsAndLanduse";
		String regionsShapeFileName = Path.of(utils.getPackageInputDirectory()).getParent().resolve("shp/testRegions.shp").toString();
		String regionsShapeRegionColumn = "region";
		String zoneShapeFileName = Path.of(utils.getPackageInputDirectory()).getParent().resolve("shp/testZones.shp").toString();
		String zoneShapeFileNameColumn = "name";
		String buildingsShapeFileName = Path.of(utils.getPackageInputDirectory()).getParent().resolve("shp/testBuildings.shp").toString();
		String shapeFileBuildingTypeColumn = "type";
		String landuseShapeFileName = Path.of(utils.getPackageInputDirectory()).getParent().resolve("shp/testLanduse.shp").toString();
		String shapeFileLanduseTypeColumn = "fclass";
		String shapeCRS = "EPSG:4326";
		String investigationAreaData = Path.of(utils.getPackageInputDirectory()).getParent().resolve("investigationAreaData.csv").toString();

		new CreateDataDistributionOfStructureData().execute(
			"--outputFacilityFile", utils.getOutputDirectory() + "/commercialFacilities.xml.gz",
			"--outputDataDistributionFile", utils.getOutputDirectory() + "/dataDistributionPerZone.csv",
			"--landuseConfiguration", useOSMBuildingsAndLanduse,
			"--regionsShapeFileName", regionsShapeFileName,
			"--regionsShapeRegionColumn", regionsShapeRegionColumn,
			"--zoneShapeFileName", zoneShapeFileName,
			"--zoneShapeFileNameColumn", zoneShapeFileNameColumn,
			"--buildingsShapeFileName", buildingsShapeFileName,
			"--shapeFileBuildingTypeColumn", shapeFileBuildingTypeColumn,
			"--landuseShapeFileName", landuseShapeFileName,
			"--shapeFileLanduseTypeColumn", shapeFileLanduseTypeColumn,
			"--shapeCRS", shapeCRS,
			"--pathToInvestigationAreaData", investigationAreaData);

		Assertions.assertTrue(Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv").toFile().exists());
		Assertions.assertTrue(Path.of(utils.getOutputDirectory()).resolve("dataDistributionPerZone.csv").toFile().length() > 0);

		Assertions.assertTrue(Path.of(utils.getOutputDirectory()).resolve("commercialFacilities.xml.gz").toFile().exists());
		Assertions.assertTrue(Path.of(utils.getOutputDirectory()).resolve("commercialFacilities.xml.gz").toFile().length() > 0);

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.parse(Path.of(utils.getOutputDirectory()).resolve("commercialFacilities.xml.gz").toUri().toURL());	;
		ActivityFacilities createdFacilities = scenario.getActivityFacilities();

		Scenario scenarioInput = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		reader = new MatsimFacilitiesReader(scenarioInput);
		reader.parse(Path.of(utils.getPackageInputDirectory()).getParent().resolve("commercialFacilities.xml.gz").toUri().toURL());	;
		ActivityFacilities existingFacilities = scenarioInput.getActivityFacilities();

		Assertions.assertEquals(existingFacilities.getFacilities().size(), createdFacilities.getFacilities().size());

		for (ActivityFacility createdFacility : createdFacilities.getFacilities().values()) {
			Assertions.assertEquals(existingFacilities.getFacilities().get(createdFacility.getId()).getLinkId(), createdFacility.getLinkId());
			Assertions.assertEquals(existingFacilities.getFacilities().get(createdFacility.getId()).getCoord(), createdFacility.getCoord());
			Assertions.assertEquals(existingFacilities.getFacilities().get(createdFacility.getId()).getActivityOptions().size(), createdFacility.getActivityOptions().size());
			Assertions.assertEquals(existingFacilities.getFacilities().get(createdFacility.getId()).getActivityOptions().keySet(), createdFacility.getActivityOptions().keySet());
			Assertions.assertEquals(existingFacilities.getFacilities().get(createdFacility.getId()).getAttributes().getAsMap().keySet(), createdFacility.getAttributes().getAsMap().keySet());
			for (String key : createdFacility.getAttributes().getAsMap().keySet()) {
				Assertions.assertEquals(existingFacilities.getFacilities().get(createdFacility.getId()).getAttributes().getAsMap().get(key), createdFacility.getAttributes().getAsMap().get(key));
			}
		}
		Map<String, Object2DoubleMap<String>> sumsOfSharesPerZoneAndCategory = new HashMap<>();
		createdFacilities.getFacilities().values().forEach(facility -> {
			String zone = facility.getAttributes().getAttribute("zone").toString();
			sumsOfSharesPerZoneAndCategory.computeIfAbsent(zone, k -> new Object2DoubleOpenHashMap<>());
			for (String assignedDataType : facility.getActivityOptions().keySet()){
				double share = (double) facility.getAttributes().getAttribute("shareOfZone_" + assignedDataType);
				sumsOfSharesPerZoneAndCategory.get(zone).mergeDouble(assignedDataType, share, Double::sum);
			}
		});
		Assertions.assertEquals(3, sumsOfSharesPerZoneAndCategory.keySet().size());
		sumsOfSharesPerZoneAndCategory.values().forEach(sumsOfShares -> {
			sumsOfShares.values().forEach(share -> {
				Assertions.assertEquals(1.0, share, 0.0001);
			});
		});
	}
}
