import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class FlagTurnLinks {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkInfile = args[0];
		String networkOutfile = args[1];
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkInfile);
		
		for (Link l : network.getLinks().values()){
			LinkImpl L = (LinkImpl) l;
			
			Node fn = L.getFromNode();
			Node tn = L.getToNode();
			
			if (fn.getId().toString().contains("-") || tn.getId().toString().contains("-")){
				//Link is a turn!
				L.setType("Turn");
			}
		}
		
		new NetworkWriter(network).write(networkOutfile);
		
	}

}
