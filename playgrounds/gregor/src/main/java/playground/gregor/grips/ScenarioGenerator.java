package playground.gregor.grips;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84ToMercator;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.grips.config.GripsConfigModule;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Grips scenario generator
 * Workflow:
 * GIS Metaformat --> ScenarioGenertor --> MATSim Szenario
 * - Wo wird entschied ob 10% oder 100% Scenario erzeugt wird?
 * 
 * @author laemmel
 *
 */
public class ScenarioGenerator {


	private static final Logger log = Logger.getLogger(ScenarioGenerator.class);
	private final String configFile;
	private Id safeLinkId;

	public ScenarioGenerator(String config) {
		this.configFile = config;
	}

	private void run() {
		log.info("loading config file");
		Config c = ConfigUtils.loadConfig(this.configFile);
		c.addSimulationConfigGroup(new SimulationConfigGroup());
		Scenario sc = ScenarioUtils.createScenario(c);
		this.safeLinkId = sc.createId("el1");

		log.info("generating network file");
		generateAndSaveNetwork(sc);

		log.info("generating population file");
		generateAndSavePopulation(sc);

		log.info("saving simulation config file");

		c.global().setCoordinateSystem("EPSG:32632");

		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(getGripsConfig(c).getOutputDir()+"/output");
		new ConfigWriter(c).write(getGripsConfig(c).getOutputDir() + "/config.xml");

	}


	private void generateAndSaveNetworkChangeEvents(Scenario sc) {
		throw new RuntimeException("This has to be done during network generation. The reason is that at this stage the mapping between original link ids (e.g. from osm) to generated matsim link ids is forgotten!");

	}

	private void generateAndSavePopulation(Scenario sc) {
		// for now a simple ESRI shape file format is used to emulated the a more sophisticated not yet defined population meta format
		GripsConfigModule gcm = getGripsConfig(sc.getConfig());
		String gripsPopulationFile = gcm.getPopulationFileName();
		new PopulationFromESRIShapeFileGenerator(sc, gripsPopulationFile, this.safeLinkId).run();

		String outputPopulationFile = gcm.getOutputDir() + "/population.xml.gz";
		new PopulationWriter(sc.getPopulation(), sc.getNetwork(), gcm.getSampleSize()).write(outputPopulationFile);
		sc.getConfig().plans().setInputFile(outputPopulationFile);

		sc.getConfig().simulation().setStorageCapFactor(gcm.getSampleSize());
		sc.getConfig().simulation().setFlowCapFactor(gcm.getSampleSize());

		ActivityParams pre = new ActivityParams("pre-evac");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		ActivityParams post = new ActivityParams("post-evac");
		post.setTypicalDuration(49); // dito
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);


	}

	private void generateAndSaveNetwork(Scenario sc) {

		GripsConfigModule gcm = getGripsConfig(sc.getConfig());
		String gripsNetworkFile = gcm.getNetworkFileName();

		// Step 1 raw network input
		// for now grips network meta format is osm
		// Hamburg example UTM32N. In future coordinate transformation should be performed beforehand
		CoordinateTransformation ct =  new GeotoolsTransformation("WGS84", "EPSG: 32632");
		OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct, false);
		//		reader.setHighwayDefaults(1, "motorway",4, 5.0/3.6, 1.0, 10000,true);
		//		reader.setHighwayDefaults(1, "motorway_link", 4,  5.0/3.6, 1.0, 10000,true);
		reader.setHighwayDefaults(2, "trunk",         2,  5.0/3.6, 1., 10000);
		reader.setHighwayDefaults(2, "trunk_link",    2,  5.0/3.6, 1.0, 10000);
		reader.setHighwayDefaults(3, "primary",       2,  5.0/3.6, 1.0, 10000);
		reader.setHighwayDefaults(3, "primary_link",  2,  5.0/3.6, 1.0, 10000);
		reader.setHighwayDefaults(4, "secondary",     1,  5.0/3.6, 1.0, 5000);
		reader.setHighwayDefaults(5, "tertiary",      1,  5.0/3.6, 1.0,  5000);
		reader.setHighwayDefaults(6, "minor",         1,  5.0/3.6, 1.0,  5000);
		reader.setHighwayDefaults(6, "unclassified",  1,  5.0/3.6, 1.0,  5000);
		reader.setHighwayDefaults(6, "residential",   1,  5.0/3.6, 1.0,  5000);
		reader.setHighwayDefaults(6, "living_street", 1,  5.0/3.6, 1.0,  5000);
		reader.setHighwayDefaults(6,"path",           1,  5.0/3.6, 1.0,  2500);
		reader.setHighwayDefaults(6,"cycleay",        1,  5.0/3.6, 1.0,  2500);
		reader.setHighwayDefaults(6,"footway",        1,  5.0/3.6, 1.0,  1000);
		reader.setKeepPaths(true);
		reader.parse(gripsNetworkFile);



		// Step 2 evacuation network generator
		// 2a) read the evacuation area
		// for now grips evacuation area meta format is ESRI Shape with no validation etc.
		// TODO switch to gml by writing a  xsd + corresponding parser. may be geotools is our friend her? The xsd has to make sure that the evacuation area consists of one and only one
		// polygon
		FeatureSource fs = ShapeFileReader.readDataFile(gcm.getEvacuationAreaFileName());
		Feature ft = null;
		try {
			ft = (Feature) fs.getFeatures().iterator().next();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-2);
		}
		MultiPolygon mp = (MultiPolygon) ft.getDefaultGeometry();
		Polygon p = (Polygon) mp.getGeometryN(0);
		// 2b) generate network
		new EvacuationNetworkGenerator(sc, p,this.safeLinkId).run();

		String networkOutputFile = gcm.getOutputDir()+"/network.xml.gz";
		((NetworkImpl)sc.getNetwork()).setEffectiveCellSize(0.26);
		((NetworkImpl)sc.getNetwork()).setEffectiveLaneWidth(0.71);
		new NetworkWriter(sc.getNetwork()).write(networkOutputFile);
		sc.getConfig().network().setInputFile(networkOutputFile);
	}

	public GripsConfigModule getGripsConfig(Config c) {

		Module m = c.getModule("grips");
		if (m instanceof GripsConfigModule) {
			return (GripsConfigModule) m;
		}
		GripsConfigModule gcm = new GripsConfigModule(m);
		c.getModules().put("grips", gcm);
		return gcm;
	}

	public static void main(String [] args) {
		if (args.length != 1) {
			printUsage();
			System.exit(-1);
		}
		new ScenarioGenerator(args[0]).run();

	}

	private static void printUsage() {
		System.out.println();
		System.out.println("ScenarioGenerator");
		System.out.println("Generates a MATSim scenario from meta format input files.");
		System.out.println();
		System.out.println("usage : ScenarioGenerator config-file");
		System.out.println();
		System.out.println("config-file:   A MATSim config file that defines the input file in meta format and the corresponding MATSim outputfiles as well.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2011, matsim.org");
		System.out.println();
	}

}
