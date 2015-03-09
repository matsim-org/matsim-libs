package playground.vbmh.SFAnpassen;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;

import playground.vbmh.vmParking.Parking;
import playground.vbmh.vmParking.ParkingMap;
import playground.vbmh.vmParking.ParkingWriter;
public class CreateDemoParking {
	
	static HashMap <String,Double> pAnteile = new HashMap<String,Double>();	//Anzahl Parkplaetze / what ever nach P typ
	static HashMap <String,Double> evAnteile = new HashMap<String,Double>(); //Anteil EV Nach P Typ
	
	static HashMap <String,Integer> preiseEVSpots = new HashMap<String,Integer>();	//Preismodell EV nach P typ
	static HashMap <String,Integer> preiseNEVSpots = new HashMap<String,Integer>(); //Preismodell NEV Nach P Typ
	
	static HashMap <String,Double> chargingRates = new HashMap<String,Double>(); //Preismodell NEV Nach P Typ

	
	static Scenario scenario;
	static HashMap <String,Integer> anzahlArbeiter = new HashMap<String, Integer>();
	static int diagZaehlEV=0;
	static int diagZaehlNEV=0;
	public static void main(String[] args) {
		
		
		String inputFile; // = "input/SF_PLUS/config_SF_PLUS_3.xml";
		String outputFile; // = "input/SF_PLUS/VM/parkings_demo.xml";
		
		inputFile=args[0];
		outputFile=args[1];
		
		
		pAnteile.put("home", 1.0);
		evAnteile.put("home", 0.5);
		preiseEVSpots.put("home", 0);
		preiseNEVSpots.put("home", 0);
		chargingRates.put("home", 2.3);
		
		// Alle folgenden relativ zu Agents die in der Facility arbeiten
		pAnteile.put("work", 1.0);
		evAnteile.put("work", 0.50);
		preiseEVSpots.put("work", 3);
		preiseNEVSpots.put("work", 4);
		chargingRates.put("work", 3.6);
		
		pAnteile.put("secondary",1.0);
		evAnteile.put("secondary", 0.5);
		preiseEVSpots.put("secondary", 5);
		preiseNEVSpots.put("secondary", 6);
		chargingRates.put("secondary", 8.04);
		
		pAnteile.put("Street", 1.0);
		evAnteile.put("Street", 0.0);
		preiseEVSpots.put("Street", 1);
		preiseNEVSpots.put("Street", 2);
		chargingRates.put("Street", 3.6);
		
		pAnteile.put("edu", 1.00);
		evAnteile.put("edu", 0.5);
		preiseEVSpots.put("edu", 7);
		preiseNEVSpots.put("edu", 7);
		chargingRates.put("edu", 0.0);
		
		
		
		int i = 0;
		double zufallsz;
		ParkingMap parking_map = new ParkingMap();
		ParkingWriter writer = new ParkingWriter();
		Random zufall = new Random();
	
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(inputFile));
		
		countEmployees();
		
		// P R I V A T E --------------------------------------------
		
		Map<Id<ActivityFacility>, ? extends ActivityFacility> facility_map = scenario.getActivityFacilities().getFacilities();
	
		for (ActivityFacility facility : facility_map.values()){
			ActivityFacilityImpl actfacility = (ActivityFacilityImpl) facility;
			for(String key : actfacility.getActivityOptions().keySet()){
				ActivityOption activity = actfacility.getActivityOptions().get(key);
				
				double location_capacity= activity.getCapacity();
				String location_type = activity.getType();
				Id<ActivityFacility> location_id= facility.getId();
				CoordImpl location_coord = (CoordImpl) actfacility.getCoord();
				
				//System.out.println(location_type);
				
				
				
				
				
				if (location_capacity>1000){ 	// Facilitys ohne capacity Angabe haben intern unendlich >> Bessere loesung suchen!
					location_capacity=getAnzahlArbeiter(location_id.toString());
					
					if(location_capacity==0){ //!! In testlaeufen reduzierte pop > oft kein Arbeiter
						location_capacity=10;
					}
					
					//System.out.println(location_capacity);
				}
				
				
				
				Parking parking = new Parking();
				setCapacity(parking, location_type, location_capacity);
				
				// Facilitys ohne capacity Angabe haben intern unendlich >> Bessere loesung suchen!

				
				
				parking.setCoordinate(location_coord);
				parking.facilityId=location_id.toString();
				parking.id=i;
				parking.type="private";
				parking.facilityActType=location_type;
				
				
				
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
				parking.parkingPriceMEVSpot=0;
				parking.parkingPriceMNEVSpot=0;
			} else{
				parking.parkingPriceMEVSpot=4;
				parking.parkingPriceMNEVSpot=3;
			}
				
			parking_map.addParking(parking);
			i++;
			
		}
		
		
		
		
		
		
		
		
		writer.write(parking_map, outputFile);
		System.out.println("feddisch");
		System.out.println("EV: "+CreateDemoParking.diagZaehlEV);
		System.out.println("NEV: "+CreateDemoParking.diagZaehlNEV);
	}
	
	static void setCapacity(Parking parking, String location_type, double location_capacity){
		parking.capacityEV=Math.round(location_capacity * pAnteile.get(location_type) * evAnteile.get(location_type));
		parking.capacityNEV=Math.round(location_capacity * pAnteile.get(location_type) * (1-evAnteile.get(location_type)));
		CreateDemoParking.diagZaehlEV+=Math.round(location_capacity * pAnteile.get(location_type) * evAnteile.get(location_type));
		CreateDemoParking.diagZaehlNEV+=Math.round(location_capacity * pAnteile.get(location_type) * (1-evAnteile.get(location_type)));
		
		parking.chargingRate=chargingRates.get(location_type);//!! Gehoert nicht hier her und muss dynamisch werden
		parking.parkingPriceMEVSpot=preiseEVSpots.get(location_type); //!! Nach typ + Zone unterscheiden; Zu hause aufs Laden den Stromtarif verechnen >> vorausgesetzt der Anschluss gehoert dem Agent..
		parking.parkingPriceMNEVSpot=preiseNEVSpots.get(location_type);

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
