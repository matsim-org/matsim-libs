package playground.toronto.gtfsutils;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;

/** converts a MATSIM network to 2 ESRI shape files: links and nodes
 * 
 * @author Yi
 */

public class ConvertNetworkToESRIFile{
	
	
	public static void main(String args[]){
		
		/* input:
		 * [1] empty links.shp file
		 * [2] empty nodes.shp file
		 * [3] matsim network file you want to convert
		 */
		String LinksFile = args[0];
		String NodesFile = args[1];
		String NetworkFile = args[2];
		//String System = "epsg:26917";
		String System = "epsg:4326";
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(NetworkFile);
		
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, System);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		Links2ESRIShape l = new Links2ESRIShape(network, LinksFile, builder);
		l.write();
		
		Nodes2ESRIShape n = new Nodes2ESRIShape(network, NodesFile, System);
		n.write();
		
	}
	
	
	
}