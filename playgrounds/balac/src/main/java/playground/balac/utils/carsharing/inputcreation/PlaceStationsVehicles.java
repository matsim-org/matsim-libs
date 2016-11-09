package playground.balac.utils.carsharing.inputcreation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class PlaceStationsVehicles extends MatsimXmlWriter {
	
	private Scenario scenario;
	private static int counter = 0;
	public PlaceStationsVehicles(Scenario scenario) {
		
		this.scenario = scenario;
	}

	public void write(String file) {
		openFile(file);
		
		writeXmlHead();
		List<Tuple<String, String>> attsC = new ArrayList<Tuple<String, String>>();
		
		attsC.add(new Tuple<>("name", "Catchacar"));
		writeStartTag("companies", null);
		writeStartTag("company", attsC);

		writeVehicles();
		writeEndTag("company");
		List<Tuple<String, String>> attsC2 = new ArrayList<Tuple<String, String>>();
		attsC2.add(new Tuple<>("name", "Mobility"));

		writeStartTag("company", attsC2);

		writeVehicles();
		writeEndTag("company");

		writeEndTag("companies");

		
		close();
	}
	
	
	private void writeVehicles() {

		Network network = this.scenario.getNetwork();
		
		int cars = 1000;
		Object[] array = network.getLinks().values().toArray();
		
		int numberLinks = array.length;
		Random r = new Random(456);
		
		for (int i = 0; i < cars; i++) {
			
			Link link = (Link) array[r.nextInt(numberLinks)];
			writeVehicle(link, counter);
			counter++;
		}		
	}

	private void writeVehicle(Link link, int id) {

		List<Tuple<String, String>> attsV = new ArrayList<Tuple<String, String>>();
		
		attsV.add(new Tuple<>("id", "FF_" + Integer.toString(id)));
		attsV.add(new Tuple<>("x", Double.toString(link.getCoord().getX())));
		attsV.add(new Tuple<>("y", Double.toString(link.getCoord().getY())));
		attsV.add(new Tuple<>("type", "car"));
		writeStartTag("freefloating", attsV, true);		
	}

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(args[0]);
        Scenario scenario = ScenarioUtils.loadScenario(config);

		PlaceStationsVehicles place = new PlaceStationsVehicles(scenario);
		place.write(args[1]);		
	}
}
