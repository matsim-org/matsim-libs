package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

class CreateDataDistributionOfStructureDataTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDataDistributionOfStructureData() {
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
			"--pathOutput", utils.getOutputDirectory(),
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

		//TODO add tests for the created facilities
	}
}
