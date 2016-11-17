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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class PlaceStationsVehicles extends MatsimXmlWriter {
	
	private Scenario scenario;
	private static int counter = 0;
	private static int counterTW = 1;
	public PlaceStationsVehicles(Scenario scenario) {
		
		this.scenario = scenario;
	}

	public void write(String file) {
		
		Network network = this.scenario.getNetwork();
		
		Object[] array = network.getLinks().values().toArray();
		
		int numberLinks = array.length;
		Random r = new Random(456);
		
			
		
		
		openFile(file);
		
		writeXmlHead();
		List<Tuple<String, String>> attsC = new ArrayList<Tuple<String, String>>();
		
		attsC.add(new Tuple<>("name", "Catchacar"));
		writeStartTag("companies", null);
		writeStartTag("company", attsC);
		for (int i = 1; i <= 400; i++) {
			Link link = (Link) array[r.nextInt(numberLinks)];

			writeStation("twoway", link, i);
		}
		writeVehicles();

		
		writeEndTag("company");
		/*List<Tuple<String, String>> attsC2 = new ArrayList<Tuple<String, String>>();
		attsC2.add(new Tuple<>("name", "Mobility"));

		writeStartTag("company", attsC2);

		writeVehicles();
		writeEndTag("company");*/

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
			writeVehicle(link, counter, r);
			counter++;
		}		
	}

	private void writeVehicle(Link link, int id, Random random) {

		
		List<Tuple<String, String>> attsV = new ArrayList<Tuple<String, String>>();
		
		attsV.add(new Tuple<>("id", "FF_" + Integer.toString(id)));
		attsV.add(new Tuple<>("x", Double.toString(link.getCoord().getX())));
		attsV.add(new Tuple<>("y", Double.toString(link.getCoord().getY())));
		
		if (random.nextDouble() < 0.9)
		
			attsV.add(new Tuple<>("type", "car"));
		else
			attsV.add(new Tuple<>("type", "transporter"));

		writeStartTag("freefloating", attsV, true);		
	}
	
	private void writeStation(String type, Link link, int id) {
		
		Random random = MatsimRandom.getRandom();
		
		List<Tuple<String, String>> attsV = new ArrayList<Tuple<String, String>>();
		
		attsV.add(new Tuple<>("id", Integer.toString(id)));
		attsV.add(new Tuple<>("x", Double.toString(link.getCoord().getX())));
		attsV.add(new Tuple<>("y", Double.toString(link.getCoord().getY())));

		writeStartTag("twoway", attsV);
		
		int numberOfVehicles = random.nextInt(4) + 1;
		
		for (int i = 1; i <= numberOfVehicles; i++) {
			
			List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
			
			atts.add(new Tuple<>("id", "TW_" + Integer.toString(counterTW++)));
			atts.add(new Tuple<>("type", "car"));
			writeStartTag("vehicle", atts, true);
		}
		writeEndTag("twoway");

	}

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(args[0]);
        Scenario scenario = ScenarioUtils.loadScenario(config);

		PlaceStationsVehicles place = new PlaceStationsVehicles(scenario);
		place.write(args[1]);		
	}
}
