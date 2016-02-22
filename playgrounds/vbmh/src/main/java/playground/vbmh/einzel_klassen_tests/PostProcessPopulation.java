package playground.vbmh.einzel_klassen_tests;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.vbmh.util.ReadParkhistory;
import playground.vbmh.vmParking.Parking;
import playground.vbmh.vmParking.ParkingMap;

public class PostProcessPopulation {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String lauf="null_ev_exc_sa";
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("output/finalEXP/"+lauf+"/output_config.xml"));
		Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
		File parkingfile = new File("input/SF_PLUS/generalinput/parking_08pmsl.xml");
//		CSVWriter evWriter = new CSVWriter("output/distances/evDistances_"+lauf);
//		CSVWriter nevWriter = new CSVWriter("output/distances/nevDistances_"+lauf);
		ParkingMap karte = JAXB.unmarshal( parkingfile, ParkingMap.class ); //Laedt Parkplaetze aus XML
		karte.initHashMap();
		ReadParkhistory parkhistory = new ReadParkhistory();
		parkhistory.readXML("output/finalEXP/"+lauf+"/parkhistory/parkhistory_200.xml");
		LinkedList<HashMap<String, String>> evParked = parkhistory.getAllEventByAttribute("eventtype", "EV_left");
		LinkedList<LinkedList<String>> evList = getPersonDistance(evParked, karte, persons);
		printDepartureTimeDifferent(evParked, persons);
//		evWriter.writeAll(evList);
//		evWriter.close();
		LinkedList<HashMap<String, String>> nevParked = parkhistory.getAllEventByAttribute("eventtype", "NEV_left");
		LinkedList<LinkedList<String>> nevList = getPersonDistance(nevParked, karte, persons);
//		nevWriter.writeAll(nevList);
//		nevWriter.close();
		System.out.println("Fertig!");
		
//		Person dieter = persons.get(new IdImpl("35287_1"));
//		System.out.println(dieter.getSelectedPlan().getPlanElements().toString());
		
	}
	
	
	
	public static LinkedList<LinkedList<String>> getPersonDistance(LinkedList<HashMap<String, String>> events, ParkingMap karte, Map<Id<Person>, ? extends Person> persons){
		LinkedList<LinkedList<String>> list = new LinkedList<LinkedList<String>>();
		for(HashMap<String, String> event:events){
			LinkedList<String> line = new LinkedList<String>();
			String personId = event.get("person");
			int parkingId = Integer.parseInt(event.get("parking"));
//			System.out.println(event.toString());
			Parking parking = karte.getParkingById(parkingId);
//			System.out.println(parking.toString());
			Coord parkingCoord = parking.getCoordinate();
			Person person = persons.get(Id.create(personId, Person.class));
			ActivityImpl activity = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(2);
//			System.out.println(activity.toString());
			Coord facilityCoord = activity.getCoord();
			double distance = CoordUtils.calcEuclideanDistance(parkingCoord, facilityCoord);
			line.add(personId);
			line.add(Double.toString((distance)));
			list.add(line);
		}
		return list;
	}
	
	public static void printDepartureTimeDifferent(LinkedList<HashMap<String, String>> parkhisEvents, Map<Id<Person>, ? extends Person> persons){
		
		for(HashMap<String, String> event:parkhisEvents){
			String personId = event.get("person");
			double histTime = Double.parseDouble(event.get("time"));
			Person person = persons.get(Id.create(personId, Person.class));
			PlanElement element = person.getSelectedPlan().getPlanElements().get(3);
			System.out.println(person.getSelectedPlan().getPlanElements().get(2));
			System.out.println(person.getSelectedPlan().getPlanElements().get(3));
//			LegImpl legImpl = (LegImpl)person.getSelectedPlan().getPlanElements().get(3);
//			System.out.println(legImpl.toString());
//			double planTime = legImpl.getDepartureTime();
//			double differenz = histTime-planTime;
//			System.out.println(differenz);
//			System.out.println(histTime);
		}
	}

}
