package playground.gregor.grips;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.grips.config.GripsConfigModule;

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

	public ScenarioGenerator(String config) {
		this.configFile = config;
	}

	private void run() {
		Config c = ConfigUtils.loadConfig(this.configFile);
		Scenario sc = ScenarioUtils.createScenario(c);

		generateAndSaveNetwork(sc);

		generateAndSavePopulation(sc);

	}




	private void generateAndSaveNetworkChangeEvents(Scenario sc) {
		throw new RuntimeException("This has to be done during network generation. The reason is that at this stage the mapping between original link ids (e.g. from osm) to generated matsim link ids is forgotten!");

	}

	private void generateAndSavePopulation(Scenario sc) {
		// TODO Auto-generated method stub

	}

	private void generateAndSaveNetwork(Scenario sc) {

		GripsConfigModule gcm = getGripsConfig(sc.getConfig());
		String gripsNetworkFile = gcm.getNetworkFileName();

		// Step 1 raw network input
		// for now grips network meta format is osm

		String wgs84 = MGC.getCRS("EPSG: 4326").toWKT();
		String wgs84utm32n = MGC.getCRS("EPSG: 32632").toWKT();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(wgs84, wgs84utm32n);
		OsmNetworkReader reader = new OsmNetworkReader(sc.getNetwork(), ct, true);
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
		new EvacuationNetworkGenerator(sc, p).run();

		new NetworkWriter(sc.getNetwork()).write(gcm.getOutputDir()+"/network.xml.gz");


	}

	private GripsConfigModule getGripsConfig(Config c) {

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
