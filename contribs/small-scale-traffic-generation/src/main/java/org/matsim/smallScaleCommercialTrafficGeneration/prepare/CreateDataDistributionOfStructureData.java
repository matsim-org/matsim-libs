package org.matsim.smallScaleCommercialTrafficGeneration.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.facilities.*;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;
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

	private final LanduseDataConnectionCreator landuseDataConnectionCreator;

	private enum LanduseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse
	}

	@CommandLine.Option(names = "--outputFacilityFile", description = "Path for the outputFacilityFile", defaultValue = "output/TestDistributionClass/commercialFacilities.xml.gz")
	private Path outputFacilityFile;

	@CommandLine.Option(names = "--outputDataDistributionFile", description = "Path for the outputDataDistributionFile", defaultValue = "output/TestDistributionClass/dataDistributionPerZone.csv")
	private Path outputDataDistributionFile;

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

	private Map<StructuralAttribute, List<String>> landuseCategoriesAndDataConnection;
	private final Map<String, Map<StructuralAttribute, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();

	public CreateDataDistributionOfStructureData(LanduseDataConnectionCreator landuseDataConnectionCreator) {
		this.landuseDataConnectionCreator = landuseDataConnectionCreator;
		log.info(
			"Using LanduseDataConnectionCreator {} to connect the types of the landuse data to the categories of the small scale commercial traffic generation",
			landuseDataConnectionCreator.getClass().getSimpleName());
	}

	public CreateDataDistributionOfStructureData() {
		this.landuseDataConnectionCreator = new LanduseDataConnectionCreatorForOSM_Data();
		log.info(
			"Using default LanduseDataConnectionCreatorForOSM_Data to connect the types of the landuse data to the categories of the small scale commercial traffic generation");
	}

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

		ShpOptions.Index indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS, shapeFileZoneNameColumn);
		ShpOptions.Index indexBuildings = SmallScaleCommercialTrafficUtils.getIndexBuildings(shapeFileBuildingsPath, shapeCRS,
			shapeFileBuildingTypeColumn);
		ShpOptions.Index indexLanduse = SmallScaleCommercialTrafficUtils.getIndexLanduse(shapeFileLandusePath, shapeCRS, shapeFileLanduseTypeColumn);
		ShpOptions.Index indexInvestigationAreaRegions = SmallScaleCommercialTrafficUtils.getIndexRegions(shapeFileRegionsPath, shapeCRS,
			regionsShapeRegionColumn);

		if (Files.notExists(outputFacilityFile.getParent()))
			new File(outputFacilityFile.toString()).mkdir();

		landuseCategoriesAndDataConnection = landuseDataConnectionCreator.createLanduseDataConnection();

		LanduseBuildingAnalysis.createInputDataDistribution(outputDataDistributionFile, landuseCategoriesAndDataConnection,
			usedLanduseConfiguration.toString(), indexLanduse, indexZones, indexBuildings, indexInvestigationAreaRegions, shapeFileZoneNameColumn,
			buildingsPerZone, pathToInvestigationAreaData, shapeFileBuildingTypeColumn);

		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

		ActivityFacilitiesFactory facilitiesFactory = facilities.getFactory();

		calculateAreaSharesOfTheFacilities(facilities, facilitiesFactory);
		log.info("Created {} facilities, writing to {}", facilities.getFacilities().size(), outputFacilityFile);

		FacilitiesWriter writer = new FacilitiesWriter(facilities);
		writer.write(outputFacilityFile.toString());

		return 0;
	}

	/**
	 * Adds the share of the area of each facility compared to the area of the facilities of this category in this zone
	 *
	 * @param facilities        The facilities
	 * @param facilitiesFactory The facilities factory
	 */
	private void calculateAreaSharesOfTheFacilities(ActivityFacilities facilities, ActivityFacilitiesFactory facilitiesFactory) {
		for (String zone : buildingsPerZone.keySet()) {
			for (StructuralAttribute assignedDataType : buildingsPerZone.get(zone).keySet()) {
				buildingsPerZone.get(zone).get(assignedDataType).forEach(singleBuilding -> {
					ActivityFacility facility;
					Id<ActivityFacility> id = Id.create(singleBuilding.getID(), ActivityFacility.class);
					if (facilities.getFacilities().containsKey(id)) {
						facility = facilities.getFacilities().get(id);
						if (!assignedDataType.equals(StructuralAttribute.EMPLOYEE))
							facility.addActivityOption(facilitiesFactory.createActivityOption(assignedDataType.getLabel()));
					} else {
						Coord coord = MGC.point2Coord(((Geometry) singleBuilding.getDefaultGeometry()).getCentroid());
						facility = facilitiesFactory.createActivityFacility(id, coord);
						if (!assignedDataType.equals(StructuralAttribute.EMPLOYEE))
							facility.addActivityOption(facilitiesFactory.createActivityOption(assignedDataType.getLabel()));
						addAttributes(zone, singleBuilding, facility);
						facilities.addActivityFacility(facility);
					}
				});
			}
		}
		facilities.getFacilities().values().forEach(facility -> {
			double calculatedAreaPerOSMBuildingCategory = (double) facility.getAttributes().getAttribute("areaOfBuilding");
			int numberOfActivityOptions = facility.getActivityOptions().size();
			facility.getAttributes().putAttribute("areaPerBuildingCategory", calculatedAreaPerOSMBuildingCategory/numberOfActivityOptions);
		});
	}

	/**
	 * Add the area and the zone as attributes to the facility.
	 *
	 * @param zone             The zone of the building
	 * @param singleBuilding   The building
	 * @param facility         The new facility
	 */
	private void addAttributes(String zone, SimpleFeature singleBuilding, ActivityFacility facility) {
		String[] buildingTypes = ((String) singleBuilding.getAttribute(shapeFileBuildingTypeColumn)).split(";");
		// calculates the area of one raw building type
		double calculatedAreaPerOSMBuildingCategory = calculateAreaPerBuildingCategory(singleBuilding, buildingTypes)  * buildingTypes.length;
		facility.getAttributes().putAttribute("areaOfBuilding", calculatedAreaPerOSMBuildingCategory);
		facility.getAttributes().putAttribute("zone", zone);
	}
}
