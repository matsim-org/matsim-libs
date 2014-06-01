package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.wrashid.bsc.vbmh.util.CSVWriter;
import playground.wrashid.bsc.vbmh.util.ReadParkhistory;
import playground.wrashid.bsc.vbmh.vmParking.ParkControl;
import playground.wrashid.bsc.vbmh.vmParking.Parking;
import playground.wrashid.bsc.vbmh.vmParking.ParkingMap;
import playground.wrashid.bsc.vbmh.vmParking.ParkingSpot;

public class PostProcessPopulation {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String lauf="normal_di";
		int zaehl=0;
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("output/finalEXP/"+lauf+"/output_config.xml"));
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("output/finalEXP/normal_sa/output_config.xml"));
//		Controler controler = new Controler(ConfigUtils.loadConfig("output/finalEXP/"+lauf+"/output_config.xml"));
		Map<Id, ? extends Person> persons = scenario.getPopulation().getPersons();
		File parkingfile = new File("input/SF_PLUS/generalinput/parking_08pmsl.xml");
		CSVWriter evWriter = new CSVWriter("output/distances_di/evDistances_"+lauf);
		CSVWriter nevWriter = new CSVWriter("output/distances_di/nevDistances_"+lauf);
		ParkingMap karte = JAXB.unmarshal( parkingfile, ParkingMap.class ); //Laedt Parkplaetze aus XML
		karte.initHashMap();
		ReadParkhistory parkhistory = new ReadParkhistory();
		parkhistory.readXML("output/finalEXP/"+lauf+"/parkhistory/parkhistory_200.xml");
		LinkedList<HashMap<String, String>> evParked = parkhistory.getAllEventByAttribute("parking", "9000006");
//		LinkedList<LinkedList<String>> evList = getPersonDistance(evParked, karte, persons);
//		ReadParkhistory park = parkhistory.getSubHist(evParked);
//		evParked=park.getAllEventByAttribute("eventtype", "EV_left");
//		for(HashMap<String, String> events:evParked){
//			if(Double.parseDouble(events.get("time"))>6500){
//				String personId = events.get("person");
//				Person person = persons.get(new IdImpl(personId));
//				ActivityImpl act = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(2);
//				if(act.getType().equals("work")){
//					System.out.println("work");
//					zaehl++;
//				}
//			}
//		}
		System.out.println(zaehl);
//		printDepartureTimeDifferent(evParked, persons);
//		evWriter.writeAll(evList);
//		evWriter.close();
//		LinkedList<HashMap<String, String>> nevParked = parkhistory.getAllEventByAttribute("eventtype", "NEV_left");
//		LinkedList<LinkedList<String>> nevList = getPersonDistance(nevParked, karte, persons);
//		nevWriter.writeAll(nevList);
//		nevWriter.close();
		System.out.println("Fertig!");
		
//		Person dieter = persons.get(new IdImpl("35287_1"));
//		System.out.println(dieter.getSelectedPlan().getPlanElements().toString());
		
	}
	
	
	
	public static LinkedList<LinkedList<String>> getPersonDistance(LinkedList<HashMap<String, String>> events, ParkingMap karte, Map<Id, ? extends Person> persons){
		LinkedList<LinkedList<String>> list = new LinkedList<LinkedList<String>>();
		for(HashMap<String, String> event:events){
			LinkedList<String> line = new LinkedList<String>();
			String personId = event.get("person");
			int parkingId = Integer.parseInt(event.get("parking"));
//			System.out.println(event.toString());
			Parking parking = karte.getParkingById(parkingId);
//			System.out.println(parking.toString());
			Coord parkingCoord = parking.getCoordinate();
			Person person = persons.get(new IdImpl(personId));
			ActivityImpl activity = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(2);
//			System.out.println(activity.toString());
			Coord facilityCoord = activity.getCoord();
			double distance = CoordUtils.calcDistance(parkingCoord, facilityCoord);
			line.add(personId);
			line.add(Double.toString((distance)));
			list.add(line);
		}
		return list;
	}
	
	public static void printDepartureTimeDifferent(LinkedList<HashMap<String, String>> parkhisEvents, Map<Id, ? extends Person> persons){
		int count=0;
		int fehler=0;
		int stunde=0;
		for(HashMap<String, String> event:parkhisEvents){
			
			
			String personId = event.get("person");
			double histTime = Double.parseDouble(event.get("time"));
			Person person = persons.get(new IdImpl(personId));
			PlanElement element = person.getSelectedPlan().getPlanElements().get(3);
			ActivityImpl actImpl = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(2);
//			System.out.println((LegImpl)person.getSelectedPlan().getPlanElements().get(3));
			LegImpl legImpl = (LegImpl)person.getSelectedPlan().getPlanElements().get(3);
//			System.out.println(legImpl.toString());
			double planStartTime = actImpl.getStartTime();
			double planEndTime = actImpl.getEndTime();
			double planDepTime = legImpl.getDepartureTime();
			double differenz = histTime-planDepTime;
//			count++;
			if(differenz!=0.0){
				fehler++;
			}
//			System.out.println(personId+"\t"+differenz);
//			if(differenz>1800){
//				System.out.println(differenz);
//				stunde++;
////				System.out.println(person.getSelectedPlan().toString());
//			}
			if(planEndTime-planStartTime<3601){
				System.out.println(personId);
				count++;
			}
			System.out.println(count);
		}
		System.out.println(fehler+" fehler von "+count);
	}

}
