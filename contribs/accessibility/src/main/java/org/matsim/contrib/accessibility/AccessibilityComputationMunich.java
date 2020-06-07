package org.matsim.contrib.accessibility;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.*;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AccessibilityComputationMunich {
    public static final Logger LOG = Logger.getLogger(AccessibilityComputationMunich.class);

    private static final String ZONE = "zone";

    public static void main(String[] args) {
//        String zonesShapeFile = "/Users/dominik/Workspace/git/muc/input/zonesShapefile/zones.shp";
//        String zoneIdInShpFile = "id";
//        String networkFile = "/Users/dominik/Workspace/git/muc/input/mito/trafficAssignment/pt_2020/network_pt_road.xml.gz";
//        String populationFile = "/Users/dominik/Workspace/runs-svn/fabilut/muc/29052020_baseCase/resultFileSpatial_2050.csv";
//        String outputDirectory = "/Users/dominik/Workspace/runs-svn/fabilut/muc/29052020_baseCase/matsim/2050/accessibilities-logSum";
//        String transitScheduleFile = "/Users/dominik/Workspace/git/muc/input/mito/trafficAssignment/pt_2020/schedule.xml";
//        String transitVehiclesFile = "/Users/dominik/Workspace/git/muc/input/mito/trafficAssignment/pt_2020/vehicles.xml";
//        char popFileDelimiter = ',';
//        String zoneIdInPopFile = "Year2050";
//        String popIdInPopFile = "population";

        String zonesShapeFile = "/Users/dominik/Workspace/git/muc/input/zonesShapefile/zones.shp";
        String zoneIdInShpFile = "id";
        String networkFile = "/Users/dominik/Workspace/git/muc/input/mito/trafficAssignment/pt_2020/network_pt_road_maglev.xml.gz";
        String populationFile = "/Users/dominik/Workspace/runs-svn/fabilut/muc/29052020_baseCaseMagLev/resultFileSpatial_2050.csv";
        String outputDirectory = "/Users/dominik/Workspace/runs-svn/fabilut/muc/29052020_baseCaseMagLev/matsim/2050/accessibilities-rawSum";
        String transitScheduleFile = "/Users/dominik/Workspace/git/muc/input/mito/trafficAssignment/pt_2020/schedule_magLev.xml.gz";
        String transitVehiclesFile = "/Users/dominik/Workspace/git/muc/input/mito/trafficAssignment/pt_2020/vehicles.xml";
        char popFileDelimiter = ',';
        String zoneIdInPopFile = "Year2050";
        String popIdInPopFile = "population";

//        String zonesShapeFile = "/Users/dominik/Workspace/git/silo/useCases/fabiland/scenario/input/zonesShapefile/fabiland.shp";
//        String zoneIdInShpFile = "Id";
//        String networkFile = "/Users/dominik/Workspace/git/silo/useCases/fabiland/scenario/matsimInput/nw_cap30_2-l_x.xml";
//        String populationFile = "/Users/dominik/Workspace/runs-svn/fabilut/fabiland/25r_ae_cap30_2-l_x_smc/resultFileSpatial_year10.csv";
//        String outputDirectory = "/Users/dominik/Workspace/runs-svn/fabilut/fabiland/25r_ae_cap30_2-l_x_smc/matsim/10/accessibilities";
//        String transitScheduleFile = "/Users/dominik/Workspace/git/silo/useCases/fabiland/scenario/matsimInput/ts_2-l_x.xml";
//        String transitVehiclesFile = "/Users/dominik/Workspace/git/silo/useCases/fabiland/scenario/matsimInput/tv_2-l_x.xml";
//        char popFileDelimiter = ',';
//        String zoneIdInPopFile = "Year10";
//        String popIdInPopFile = "population";

        Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);

        Map<Id<ActivityFacility>, Geometry> zoneGeometryMap = createZoneGeometryMap(zonesShapeFile, zoneIdInShpFile);
        ActivityFacilities networkConnectedZones = createNetworkConnectedZones(zoneGeometryMap, network);
        Map<Id<ActivityFacility>, Double> zonePopulationMap = createZonePopulationMap(populationFile, popFileDelimiter, zoneIdInPopFile, popIdInPopFile);
        addPopulationAsWeight(networkConnectedZones, zonePopulationMap);

        // Prepare config
        Config config = ConfigUtils.createConfig();

        ConfigUtils.setVspDefaults(config);

        boolean push2Geoserver = false; // Set true for run on server
        boolean createQGisOutput = false; // Set false for run on server


        config.global().setCoordinateSystem("EPSG:31468");
        config.network().setInputCRS(config.global().getCoordinateSystem());
        config.network().setInputFile(networkFile);

        String runId = "de_munich_zones";
        // config.controler().setOutputDirectory();
        config.controler().setOutputDirectory(outputDirectory);

        config.controler().setRunId(runId);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(0);

        config.transit().setUseTransit(true);
        config.transit().setTransitScheduleFile(transitScheduleFile);
        config.transit().setVehiclesFile(transitVehiclesFile);
        config.transit().setUsingTransitInMobsim(false);

        config.plansCalcRoute().setRoutingRandomness(0.);

        AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
        acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile);
        acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
        acg.setComputingAccessibilityForMode(Modes4Accessibility.car, false);
        acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
        acg.setOutputCrs(config.global().getCoordinateSystem());

        acg.setUseParallelization(false);

        // measure points
        acg.setMeasurePointGeometryProvision(AccessibilityConfigGroup.MeasurePointGeometryProvision.fromShapeFile);
        acg.setMeasuringPointsFacilities(networkConnectedZones);
        acg.setMeasurePointGeometryMap(zoneGeometryMap);


        config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);

        acg.setTileSize_m(1000); // TODO This is only a dummy value here
        acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromFacilitiesObject);
        acg.setUseOpportunityWeights(true);
        acg.setWeightExponent(1.2); // TODO Need differentiation for different modes
        acg.setAccessibilityMeasureType(AccessibilityConfigGroup.AccessibilityMeasureType.rawSum);
        //acg.setAccessibilityMeasureType(AccessibilityConfigGroup.AccessibilityMeasureType.logSum);

        //final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork());

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // facilities
        for (ActivityFacility zone : networkConnectedZones.getFacilities().values()) {
            scenario.getActivityFacilities().addActivityFacility(zone);
        }

        final Controler controler = new Controler(scenario);

        // Use sbb pt raptor router
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});

        AccessibilityModule module = new AccessibilityModule();
        module.setConsideredActivityType("zone");
        // module.addAdditionalFacilityData(densityFacilities);
        module.setPushing2Geoserver(push2Geoserver);
        module.setCreateQGisOutput(createQGisOutput);
        controler.addOverridingModule(module);

        controler.run();
    }

    public static Map<Id<ActivityFacility>, Geometry> createZoneGeometryMap(String zonesShapeFile, String zoneIdInShpFile) {
        LOG.info("Reading geometries.");
        Map<Id<ActivityFacility>, Geometry> zoneGeometryMap = new HashMap<>();

        Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(zonesShapeFile);
        LOG.info(features.size() + " features found.");

        for (SimpleFeature feature : features) {
            Id<ActivityFacility> zoneId = Id.create(String.valueOf(feature.getAttribute(zoneIdInShpFile)), ActivityFacility.class);
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            zoneGeometryMap.put(zoneId, geometry);
        }
        return zoneGeometryMap;
    }

    public static ActivityFacilities createNetworkConnectedZones(Map<Id<ActivityFacility>, Geometry> zoneGeometryMap, Network network) {
        LOG.info("Creating network-connected zones.");
        ActivityFacilities networkConnectedZones = FacilitiesUtils.createActivityFacilities();
        ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();

        for (Id<ActivityFacility> zoneId : zoneGeometryMap.keySet()) {
            Geometry geometry = zoneGeometryMap.get(zoneId);
            Coord centroid = CoordUtils.createCoord(geometry.getCentroid().getX(), geometry.getCentroid().getY());
            Node nearestNode = NetworkUtils.getNearestNode(network, centroid); // TODO choose road of certain category
            Coord coord = CoordUtils.createCoord(nearestNode.getCoord().getX(), nearestNode.getCoord().getY());
            double distance = CoordUtils.calcEuclideanDistance(centroid, coord);
            LOG.info("Zone " + zoneId + " has centroid " + centroid + " and nearest network node " + coord + ". Euclidean distance between them: " + distance);
            if (distance > 500.) {
                LOG.warn("For zone " + zoneId + ", the distance between centroid and nearest network node is " + distance);
            }
            ActivityFacility activityFacility = aff.createActivityFacility(Id.create(zoneId, ActivityFacility.class), coord);
            activityFacility.addActivityOption(aff.createActivityOption(ZONE));
            networkConnectedZones.addActivityFacility(activityFacility);
        }
        return networkConnectedZones;
    }

    public static Map<Id<ActivityFacility>, Double> createZonePopulationMap(String populationFile, char popFileDelimiter, String zoneIdInPopFile, String popIdInPopFile) {
        LOG.info("Starting to read population information.");
        Path inputFile = Paths.get(populationFile);

        Map<Id<ActivityFacility>, Double> zonePopulationMap = new HashMap<>();

        try {
            CSVParser parser = CSVParser.parse(inputFile, StandardCharsets.UTF_8, CSVFormat.newFormat(popFileDelimiter).withFirstRecordAsHeader());
            for (CSVRecord record : parser) {
                Id<ActivityFacility> zoneId = Id.create(record.get(zoneIdInPopFile), ActivityFacility.class);
                double pop = Double.valueOf(record.get(popIdInPopFile));
                zonePopulationMap.put(zoneId, pop);
            }
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
        return zonePopulationMap;
    }

    private static void addPopulationAsWeight(ActivityFacilities zones, Map<Id<ActivityFacility>, Double> zonePopulationMap) {
        for (ActivityFacility zone : zones.getFacilities().values()) {
            double pop = 0.;
            if (zonePopulationMap.get(zone.getId()) != null) {
                pop = zonePopulationMap.get(zone.getId());
            } else {
                LOG.warn("No population information for zone " + zone.getId());
            }
            zone.getAttributes().putAttribute(Labels.WEIGHT, pop);
        }
    }
}