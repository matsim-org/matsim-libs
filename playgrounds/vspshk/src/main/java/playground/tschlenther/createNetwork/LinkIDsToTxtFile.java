package playground.tschlenther.createNetwork;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class LinkIDsToTxtFile {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Network net = scenario.getNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(net);
		reader.readFile("C:/Users/Work/Bachelor Arbeit/input/GridNet/grid_network_length200.xml");
		
		String output = "C:/Users/Work/Bachelor Arbeit/input/GridNet/GridNet_ALL_LINK_IDs.txt";
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try {
			
			for (Id<Link> link : net.getLinks().keySet()){				
				bw.write(""+link);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.getLogger(LinkIDsToTxtFile.class).info("FINISHED WRITING LINK ID FILE TO: " + output);
	
	}

}
