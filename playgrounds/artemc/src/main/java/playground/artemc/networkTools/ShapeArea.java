package playground.artemc.networkTools;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class ShapeArea {

	/**
	 * @param args
	 */
	private static final Logger log = Logger.getLogger(ShapeArea.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("EvacuationAreaGenerator usage:");
			System.out
					.println("java -cp <MATSim release file> org.matsim.evacuation.tutorial.EvacuationAreaGenerator <path to network.xml> <path to evacuationArea.shp> <path to output evacuationarea.xml>");
			System.exit(-1);
		}
		String network = args[0];
		String areaShape = args[1];

		Scenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();

		new MatsimNetworkReader(sc).readFile(network);
		log.info("Generating evacuation area links file.");

		Collection<SimpleFeature> fts = new ShapeFileReader().readFileAndInitialize(areaShape);

		log.info("Shape file contains " + fts.size() + " zones!");

		for (SimpleFeature ft : fts) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coordinates = geo.getCoordinates();
			System.out.println("Feature:");
			for (int i = 0; i < coordinates.length; i++) {
				System.out.print(coordinates[i].x + "," + coordinates[i].y + "   ");
			}
			System.out.println();
		}

		log.info("done.");

	}

}
