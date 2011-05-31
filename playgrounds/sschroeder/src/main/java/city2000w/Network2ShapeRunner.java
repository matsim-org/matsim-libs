package city2000w;

import gis.arcgis.NetworkShapeFileWriter;

import java.io.IOException;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import utils.NetworkThinningForVis;




public class Network2ShapeRunner {
	
	public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		Config config = new Config();
		config.addCoreModules();
		Scenario scen = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scen).readFile("../../Diplomarbeit_Matthias/output/germany_bigroads_fused.xml");
		Network network = scen.getNetwork();
		NetworkThinningForVis networkThinner = new NetworkThinningForVis(network);
		networkThinner.thinOut();
		new NetworkWriter(network).write("../../Diplomarbeit_Matthias/output/germany_bigroads_fusedAndThinnedOut_test.xml");
		NetworkShapeFileWriter writer = new NetworkShapeFileWriter(network,CRS.decode("EPSG:32633"));
		writer.write("../../Diplomarbeit_Matthias/output/germany_links.shp", "../../Diplomarbeit_Matthias/output/germany_nodes.shp");
		
	}

}
