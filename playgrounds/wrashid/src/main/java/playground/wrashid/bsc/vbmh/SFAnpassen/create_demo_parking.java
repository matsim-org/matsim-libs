package playground.wrashid.bsc.vbmh.SFAnpassen;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.bsc.vbmh.vmParking.Parking;
import playground.wrashid.bsc.vbmh.vmParking.ParkingMap;
import playground.wrashid.bsc.vbmh.vmParking.ParkingWriter;
public class create_demo_parking {
	static HashMap <String,Double> Panteile = new HashMap<String,Double>();	//Anzahl Parkplaetze / what ever nach P typ
	static HashMap <String,Double> EVanteile = new HashMap<String,Double>(); //Anteil EV Nach P Typ
	static Scenario scenario;
	static HashMap <String,Integer> anzahlArbeiter = new HashMap<String, Integer>();
	static int diagZaehlEV=0;
	static int diagZaehlNEV=0;
	public static void main(String[] args) {
	
		Panteile.put("home", 1.0);
		EVanteile.put("home", 0.0);
		
		
		// Alle folgenden relativ zu Agents die in der Facility arbeiten
		Panteile.put("work", 0.5);
		EVanteile.put("work", 0.20);
		
		Panteile.put("secondary",0.5);
		EVanteile.put("secondary", 0.2);
		
		Panteile.put("Street", 0.05);
		EVanteile.put("Street", 0.30);
		
		Panteile.put("edu", 0.05);
		EVanteile.put("edu", 0.0);
		
		
		
		
		
		int i = 0;
		double zufallsz;
		ParkingMap parking_map = new ParkingMap();
		ParkingWriter writer = new ParkingWriter();
		Random zufall = new Random();
	
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF/config_SF_3.xml"));
		
		countEmployees();
		
		// P R I V A T E --------------------------------------------
		
		Map<Id, ? extends ActivityFacility> facility_map = scenario.getActivityFacilities().getFacilities();
	
		for (Facility facility : facility_map.values()){
			ActivityFacilityImpl actfacility = (ActivityFacilityImpl) facility;
			for(String key : actfacility.getActivityOptions().keySet()){
				ActivityOption activity = actfacility.getActivityOptions().get(key);
				
				double location_capacity= activity.getCapacity();
				String location_type = activity.getType();
				IdImpl location_id=(IdImpl)facility.getId();
				CoordImpl location_coord = (CoordImpl) actfacility.getCoord();
				
				System.out.println(location_type);
				
				
				
				
				
				if (location_capacity>1000){ 	// Facilitys ohne capacity Angabe haben intern unendlich >> Bessere loesung suchen!
					location_capacity=getAnzahlArbeiter(location_id.toString());
					System.out.println(location_capacity);
				}
				
				
				
				Parking parking = new Parking();
				setCapacity(parking, location_type, location_capacity);
				
				// Facilitys ohne capacity Angabe haben intern unendlich >> Bessere loesung suchen!

				
				
				parking.setCoordinate(location_coord);
				parking.facilityId=location_id.toString();
				parking.id=i;
				parking.type="private";
				parking.facilityActType=location_type;
				parking.parkingPriceM=3;
				
				parking_map.addParking(parking);
				i++;
			}
		}
		
		
		
		
		// P U B L I C -------------------------------------
		
		for (Link link : scenario.getNetwork().getLinks().values()){
			double link_length = link.getLength();
			Coord link_coord=link.getFromNode().getCoord();
			double location_capacity = link_length/10;
			Parking parking = new Parking();
			setCapacity(parking, "Street", location_capacity);

			
			parking.setCoordinate(link_coord);
			parking.id=i;
			parking.type="public";
			zufallsz=zufall.nextDouble();
			if (zufallsz<0.2){
				parking.parkingPriceM=0;
			} else{
				parking.parkingPriceM=3;
			}
				
			parking_map.addParking(parking);
			i++;
			
		}
		
		
		
		
		
		
		
		
		writer.write(parking_map, "input/parkings_demo.xml");
		System.out.println("feddisch");
		System.out.println("EV: "+create_demo_parking.diagZaehlEV);
		System.out.println("NEV: "+create_demo_parking.diagZaehlNEV);
	}
	
	static void setCapacity(Parking parking, String location_type, double location_capacity){
		parking.capacityEV=Math.round(location_capacity * Panteile.get(location_type) * EVanteile.get(location_type));
		parking.capacityNEV=Math.round(location_capacity * Panteile.get(location_type) * (1-EVanteile.get(location_type)));
		create_demo_parking.diagZaehlEV+=Math.round(location_capacity * Panteile.get(location_type) * EVanteile.get(location_type));
		create_demo_parking.diagZaehlNEV+=Math.round(location_capacity * Panteile.get(location_type) * (1-EVanteile.get(location_type)));
		
	}
	
	
	static int getAnzahlArbeiter(String facId){
		if(anzahlArbeiter.containsKey(facId)){
			return anzahlArbeiter.get(facId);
		}else{
			return 0;
		}
		
	}
	
	
	
	static void countEmployees(){
		
		Collection<? extends Person> personen = scenario.getPopulation().getPersons().values();
		
		for (Person person : personen){
			for(Plan plan : person.getPlans()){ 
				PlanImpl planImpl = (PlanImpl) plan;
				for(PlanElement element : planImpl.getPlanElements()){
					if(element.getClass()==ActivityImpl.class){
						ActivityImpl actImpl = (ActivityImpl) element;
						if(actImpl.getType()=="work"){
							String facId = actImpl.getFacilityId().toString();
							if(anzahlArbeiter.containsKey(facId)){
								int Anzahl = anzahlArbeiter.get(facId);
								Anzahl++;
								anzahlArbeiter.put(facId, Anzahl);
								//System.out.print("Arbeiter plus eins");
							}else{
								anzahlArbeiter.put(facId, 1);
							}
						}
					}
				}
			}
			
			
		}
		
		
	}
	

}
