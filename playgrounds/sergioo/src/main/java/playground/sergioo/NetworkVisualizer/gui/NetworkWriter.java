package playground.sergioo.NetworkVisualizer.gui;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class NetworkWriter {
	
	private static final String SEPARATOR_CSV = ",";
	
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(args[0]);
		PrintWriter writer = new PrintWriter(args[1]);
		writer.println("link"+SEPARATOR_CSV+"length");
		for(Link link:scenario.getNetwork().getLinks().values())
			writer.println(link.getId()+SEPARATOR_CSV+link.getLength());
		writer.close();
	}
	
}
