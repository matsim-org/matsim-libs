package playground.acmarmol.Avignon;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class LinksFreeSpeedModifier {

	public static void main(String args[]){
		
		String inputBase = "C:/local/marmolea/input/Avignon/zurich_10pc/";
		double factor = 0.7;
		
		Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", inputBase + "network.xml");
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.loadScenario(config);
		
		ConfigGroup a = config.getModule("planCalcScore");
		System.out.println(a.getParams().toString());
	
		Network network = scenario.getNetwork();
		
		for(Link link: network.getLinks().values()){
			
			link.setFreespeed(link.getFreespeed()*factor);
			
		}
		
		new NetworkWriter(network).write(inputBase + "netbais.xml");
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
