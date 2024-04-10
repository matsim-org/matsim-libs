package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.smallScaleCommercialTrafficGeneration.prepare.LanduseBuildingAnalysis.*;

@CommandLine.Command(name = "create-data-distribution-of-structure-data", description = "Create data distribution as preparation for the generation of small scale commercial traffic", showDefaultValues = true)
public class CreateDataDistributionOfStructureData implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateDataDistributionOfStructureData.class);

	private enum LanduseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse
	}

	@CommandLine.Option(names = "--pathOutput", description = "Path for the output", defaultValue = "output/TestDistributionClass")
	private Path output;

	@CommandLine.Option(names = "--landuseConfiguration", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution", defaultValue = "useOSMBuildingsAndLanduse")
	private LanduseConfiguration usedLanduseConfiguration;

	@CommandLine.Option(names = "--regionsShapeFileName", description = "Path of the region shape file.", defaultValue = "contribs/small-scale-traffic-generation/test/input/org/matsim/smallScaleCommercialTrafficGeneration/shp/testRegions.shp")
	private Path shapeFileRegionsPath;

	@CommandLine.Option(names = "--regionsShapeRegionColumn", description = "Name of the region column in the region shape file.", defaultValue = "region")
	private String regionsShapeRegionColumn;

	@CommandLine.Option(names = "--zoneShapeFileName", description = "Path of the zone shape file.", defaultValue = "contribs/small-scale-traffic-generation/test/input/org/matsim/smallScaleCommercialTrafficGeneration/shp/testZones.shp")
	private Path shapeFileZonePath;

	@CommandLine.Option(names = "--zoneShapeFileNameColumn", description = "Name of the unique column of the name/Id of each zone in the zones shape file.", defaultValue = "name")
	private String shapeFileZoneNameColumn;

	@CommandLine.Option(names = "--buildingsShapeFileName", description = "Path of the buildings shape file", defaultValue = "contribs/small-scale-traffic-generation/test/input/org/matsim/smallScaleCommercialTrafficGeneration/shp/testBuildings.shp")
	private Path shapeFileBuildingsPath;

	@CommandLine.Option(names = "--shapeFileBuildingTypeColumn", description = "Name of the unique column of the building type in the buildings shape file.", defaultValue = "type")
	private String shapeFileBuildingTypeColumn;

	@CommandLine.Option(names = "--landuseShapeFileName", description = "Path of the landuse shape file", defaultValue = "contribs/small-scale-traffic-generation/test/input/org/matsim/smallScaleCommercialTrafficGeneration/shp/testLanduse.shp")
	private Path shapeFileLandusePath;

	@CommandLine.Option(names = "--shapeFileLanduseTypeColumn", description = "Name of the unique column of the landuse type in the landuse shape file.", defaultValue = "fclass")
	private String shapeFileLanduseTypeColumn;

	@CommandLine.Option(names = "--shapeCRS", description = "CRS of the three input shape files (zones, landuse, buildings", defaultValue = "EPSG:4326")
	private String shapeCRS;

	@CommandLine.Option(names = "--pathToInvestigationAreaData", description = "Path to the investigation area data", defaultValue = "contribs/small-scale-traffic-generation/test/input/org/matsim/smallScaleCommercialTrafficGeneration/investigationAreaData.csv")
	private Path pathToInvestigationAreaData;

	private final Map<String, List<String>> landuseCategoriesAndDataConnection = new HashMap<>();
	private final Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

	private ShpOptions.Index indexZones;
	private ShpOptions.Index indexBuildings;
	private ShpOptions.Index indexLanduse;
	private ShpOptions.Index indexInvestigationAreaRegions;

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateDataDistributionOfStructureData()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		log.info("Create data distribution of structure data");

		if (!Files.exists(shapeFileLandusePath)) {
			throw new Exception("Required landuse shape file not found:" + shapeFileLandusePath.toString());
		}
		if (!Files.exists(shapeFileBuildingsPath)) {
			throw new Exception(
				"Required OSM buildings shape file {} not found" + shapeFileBuildingsPath.toString());
		}
		if (!Files.exists(shapeFileZonePath)) {
			throw new Exception("Required districts shape file {} not found" + shapeFileZonePath.toString());
		}
		if (!Files.exists(shapeFileRegionsPath)) {
			throw new Exception("Required regions shape file {} not found" + shapeFileRegionsPath.toString());
		}

		indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS, shapeFileZoneNameColumn);
		indexBuildings = SmallScaleCommercialTrafficUtils.getIndexBuildings(shapeFileBuildingsPath, shapeCRS, shapeFileBuildingTypeColumn);
		indexLanduse = SmallScaleCommercialTrafficUtils.getIndexLanduse(shapeFileLandusePath, shapeCRS, shapeFileLanduseTypeColumn);
		indexInvestigationAreaRegions = SmallScaleCommercialTrafficUtils.getIndexRegions(shapeFileRegionsPath, shapeCRS, regionsShapeRegionColumn);

		if(Files.notExists(output))
			new File(output.toString()).mkdir();

		createDefaultDataConnectionForOSM(landuseCategoriesAndDataConnection); //TODO: find way to import this connection

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
			.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
				usedLanduseConfiguration.toString(), indexLanduse, indexZones,
				indexBuildings, indexInvestigationAreaRegions, shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData);

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		ActivityFacilitiesFactory f = facilities.getFactory();

		for (String zone : buildingsPerZone.keySet()) {
			for (String assignedDataType : buildingsPerZone.get(zone).keySet()) {
				buildingsPerZone.get(zone).get(assignedDataType).forEach(singleBuilding -> {
					Id<ActivityFacility> id = Id.create(singleBuilding.getID(), ActivityFacility.class);
					if (facilities.getFacilities().containsKey(id)) {
						facilities.getFacilities().get(id).addActivityOption(f.createActivityOption(assignedDataType));
					} else {
						Coord coord = MGC.point2Coord(((Geometry) singleBuilding.getDefaultGeometry()).getCentroid());
						ActivityFacility facility = f.createActivityFacility(id, coord);
						facility.addActivityOption(f.createActivityOption(assignedDataType));
						String[] buildingTypes = ((String) singleBuilding.getAttribute("type")).split(";");
						int calculatedAreaPerBuildingCategory = calculateAreaPerBuildingCategory(singleBuilding, buildingTypes);
						facility.getAttributes().putAttribute("shareOfZone_" + assignedDataType,
							getShareOfTheBuildingAreaOfTheRelatedAreaOfTheZone(zone, calculatedAreaPerBuildingCategory,
								assignedDataType)); //TODO: check if this is the right
						//TODO Employee hat momentan noch infinity als Wert
						facility.getAttributes().putAttribute("zone", zone);
						facilities.addActivityFacility(facility);
					}
				});

			}
		}
		Path facilityOutput = output.resolve("commercialFacilities.xml.gz");
		log.info("Created {} facilities, writing to {}", facilities.getFacilities().size(), facilityOutput);

		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(facilityOutput.toString());

		return 0;
	}
}
