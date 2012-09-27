package playground.toronto.gtfsutils;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;



public class ConvertNetworkToESRIFile{
	
	
	public static void main(String args[]){
		
		String LinksFile = args[0];
		String NodesFile = args[1];
		String NetworkFile = args[2];
		String System = "epsg:26917";
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
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