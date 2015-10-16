package playground.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vbmh.util.ReadParkhistory;
import playground.vbmh.vmEV.EVControl;

public class histReaderTest {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ReadParkhistory readera = new ReadParkhistory();
		readera.readXML("input/postProcess/5c200.xml");
		ReadParkhistory readerb = new ReadParkhistory();
		readerb.readXML("input/postProcess/exc200.xml");
		System.out.println(readera.events.get(10).get("person"));
		
		System.out.println(readera.getEventByAttribute("person", "5859_1"));
		
		int ia = 0;
		for (HashMap<String, String> event : readerb.events) {
			if (event.get("eventtype").equals("ev_out_of_battery")) {
				ia++;
				LinkedList<HashMap<String, String>> listeb = readerb.getAllEventByAttribute("person",event.get("person"));
				int j = 0;
				for (HashMap<String, String> personenevents : listeb){
					if(personenevents.get("eventtype").equals("Agent_looking_for_parking_has_to_charge")){
						j++;
					}
				}
				
				if(j!=2){
					System.out.println("typ muss nur einmal laden "+ event.get("person"));
				}
				
			}
		}
		
		System.out.println(ia);
		
		
		
		
		if (false) {
			int i = 0;
			for (HashMap<String, String> event : readera.events) {
				if (event.get("eventtype").equals(
						"Agent_looking_for_parking_has_to_charge")) {
					i++;
				}
			}
			System.out.println("has to in a" + i);
			i = 0;
			for (HashMap<String, String> event : readerb.events) {
				if (event.get("eventtype").equals(
						"Agent_looking_for_parking_has_to_charge")) {
					i++;
				}
			}
			System.out.println("has to in b" + i);
			for (HashMap<String, String> event : readerb.events) {
				if (event.get("eventtype").equals(
						"Agent_looking_for_parking_has_to_charge")) {
					LinkedList<HashMap<String, String>> listea = readera
							.getAllEventByAttribute("person",
									event.get("person"));
					LinkedList<HashMap<String, String>> listeb = readerb
							.getAllEventByAttribute("person",
									event.get("person"));
					int counta = 0;
					for (HashMap<String, String> map : listea) {
						if (map.get("eventtype").equals(
								"Agent_looking_for_parking_has_to_charge")) {
							counta++;
						}
					}

					int countb = 0;
					for (HashMap<String, String> map : listeb) {
						if (map.get("eventtype").equals(
								"Agent_looking_for_parking_has_to_charge")) {
							countb++;
						}
					}

					if (counta != countb) {
						System.out
								.println("typ muss unterschiedlich oft laden "
										+ event.get("person"));
						event.toString();
					}

					HashMap<String, String> eventa = readera
							.getEventByAttribute("person", event.get("person"));
					if (eventa.equals(null)) {
						System.out.println("typ gibts nicht "
								+ event.get("person"));
					} else if (!eventa.get("eventtype").equals(
							"Agent_looking_for_parking_has_to_charge")) {
						System.out.println("typ muss nicht laden "
								+ event.get("person"));
						event.toString();
					} else if (eventa.get("eventtype").equals(
							"Agent_looking_for_parking_has_to_charge")) {
						//					System.out.println("typ muss immernoch laden "+ event.get("person"));
						//					event.toString();
					}
				}

			}
		}
		if (false) {
			Config config = ConfigUtils
					.loadConfig("input/SF_PLUS/exp/example/config.xml");
			config.getModule("plans").getParams().remove("inputPlansFile");
			config.getModule("plans").addParam("inputPlansFile",
					"input/postProcess/200.plans.xml.gz");
			Scenario scenario = ScenarioUtils.loadScenario(config);
			EVControl evControl = new EVControl();
			evControl.startUp("input/SF_PLUS/generalinput/evs.xml", null);
			Population population = scenario.getPopulation();
			for (Person person : population.getPersons().values()) {
				PersonImpl personImpl = (PersonImpl) person;
				ActivityImpl firstAct = (ActivityImpl) person.getSelectedPlan()
						.getPlanElements().get(0);
				LegImpl firstLeg = (LegImpl) person.getSelectedPlan()
						.getPlanElements().get(1);
				boolean brookings = firstAct.getFacilityId().toString()
						.contains("B");
				boolean ev = evControl.hasEV(person.getId());
				if (brookings && ev) {
					HashMap<String, String> event = readera.getEventByAttribute(
							"person", person.getId().toString());
					if (event != null) {
						if (event.get("eventtype").equals("Agent_looking_for_parking_has_to_charge")) {
							System.out.println("Brookings typ muss nicht laden "+ person.getId().toString());
							event.toString();
							System.out.println(person.getSelectedPlan().toString());
							System.out.println(firstLeg.toString());
						}
						//				System.out.println(""+person.getId().toString());
						//				System.out.println(person.getSelectedPlan().toString());
						//				System.out.println(firstLeg.toString());

					}
				}

			}
		}
		
		
	}

}
