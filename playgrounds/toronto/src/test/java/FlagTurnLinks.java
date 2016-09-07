import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
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
		Network network = (Network) scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInfile);
		
		for (Link l : network.getLinks().values()){
			Link L = (Link) l;
			
			Node fn = L.getFromNode();
			Node tn = L.getToNode();
			
			if (fn.getId().toString().contains("-") || tn.getId().toString().contains("-")){
				//Link is a turn!
				NetworkUtils.setType( L, (String) "Turn");
			}
		}
		
		new NetworkWriter(network).write(networkOutfile);
		
	}

}
