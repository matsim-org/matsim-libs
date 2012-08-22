package playground.acmarmol.Avignon;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class LinksFreeSpeedWriter {

	public static void main(String[] args) throws IOException{
		
		final  Logger log = Logger.getLogger(MainAvignon.class);
		Config config = ConfigUtils.loadConfig(args[0]);
		BufferedWriter out; 
		
		out = IOUtils.getBufferedWriter("./output/Avignon/freeSpeed_norm.txt");
		
		config.setParam("network", "inputNetworkFile", "./input/Avignon/zurich_1pc/network.xml");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		
		Iterator<?> it = scenario.getNetwork().getLinks().entrySet().iterator();
		
		while (it.hasNext()) {
			Entry entry = (Entry) it.next();
			Link link = (Link) entry.getValue();
			out.write(String.valueOf(link.getFreespeed()));
			out.newLine();
			
		}
	
		out.close();
		log.info("...done");
		
	}
	
	
	
}
