package playground.wrashid.bsc.vbmh.vm_parking;

import java.io.File;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Manages the whole parking process of one Agent at a time. One instance of this class is kept by the Park_Handler 
 * which starts the Park() / leave().
 * Parking: First the availability of a private parking belonging to the destination facility is checked. If there
 * is no private parking available all public parking in a specific area around the destination of the agent are checked 
 * for free spots and then the best one is selected. 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */

public class Park_Control {
	int max_distance = 2000; //Maximaler Umkreis in dem Parkplaetze gesucht werden
	
	//Zur berechnung des besten oeffentlichen Parkplatzes: (Negative Werte, hoechste Score gewinnt)
	double beta_money = -10; 
	double beta_distance = -1; // !! Zweiphasige Kurve einbauen?
	
	int count_private = 0;
	int count_public = 0;
	int count_not_parked = 0;
	
	Controler controller;
	ParkingMap parking_map = new ParkingMap(); //Beinhaltet alle Parkplaetze
	Pricing_Models pricing = new Pricing_Models(); //Behinhaltet dei Preismodelle
	Park_History_Writer phwriter = new Park_History_Writer(); //Schreibt XML Datei mit Park events
	double time; //Wird auf aktuelle Zeit gesetzt (Vom event)
	Coord cordinate; //Koordinaten an denen die Zie Facility ist. Von hier aus wird gesucht.
	
	
	
	//--------------------------- S T A R T  U P---------------------------------------------
	public int startup(String parking_filename, String pricing_filename, Controler controller){
		this.controller=controller;
		
		//Parkplaetze Laden
		File parkingfile = new File( parking_filename );
		ParkingMap karte = JAXB.unmarshal( parkingfile, ParkingMap.class ); //Laedt Parkplaetze aus XML
		this.parking_map=karte;
		
		//Preise Laden
		File pricingfile = new File( pricing_filename ); 
		this.pricing = JAXB.unmarshal( pricingfile, Pricing_Models.class ); //Laedt Preise aus XML
		return 0;
		
	
	}
	
	
	
	
	//--------------------------- P A R K ---------------------------------------------
	public int park(ActivityStartEvent event) {
		Id person_id = event.getPersonId();
		this.time=event.getTime();

		
		// FACILITY UND KOORDINATEN LADEN
		IdImpl facilityid = new IdImpl(event.getAttributes().get("facility"));
		Map<Id, ? extends ActivityFacility> facilitymap = controller.getFacilities().getFacilities();
		ActivityFacility facility = facilitymap.get(facilityid);
		this.cordinate = facility.getCoord();
		
		/*
		Parkplatz finden: Est wird ueberprueft ob es an der Zielfacility einen Freien Privatparkplatz gibt.
		Falls nicht werden freie Oeffentliche Parkplaetze im Umkreis um die Facility gesammellt und daraus 
		der Beste ausgewaehlt.
		*/
		
		
		// PRIVATES PARKEN
		Parkingspot private_parking = check_private_parking(facilityid.toString());
		if (private_parking != null) {
			//System.out.println("Privaten Parkplatz gefunden");
			park_on_spot(private_parking, person_id);
			this.count_private ++;
			return 1;
		} 
		
		
		// OEFFENTLICHES PARKEN
		LinkedList<Parkingspot> spots_in_area = get_public_parkings(cordinate);
		if (spots_in_area != null) {
			park_public(spots_in_area, person_id);
			//System.out.println("Oeffentlich geparkt");
			this.count_public ++;
			return 1;
		}
		
		//System.err.println("Nicht geparkt"); // !! Was passiert wenn Kein Parkplatz im Umkreis gefunden?
		
		// !! Provisorisch: Agents bestrafen die nicht Parken:
		Map<String, Object> person_attributes = controller.getPopulation().getPersons().get(person_id).getCustomAttributes();
		VM_Score_Keeper scorekeeper;
		if (person_attributes.get("VM_Score_Keeper")!= null){
			scorekeeper = (VM_Score_Keeper) person_attributes.get("VM_Score_Keeper");
		} else{
			scorekeeper = new VM_Score_Keeper();
			person_attributes.put("VM_Score_Keeper", scorekeeper);
		}
		scorekeeper.add(30);
		
		phwriter.add_agent_not_parked(Double.toString(this.time), person_id.toString());
		
		this.count_not_parked++;
		return -1;
	}

	//--------------------------- P A R K   P U B L I C ---------------------------------------------	
	private void park_public(LinkedList<Parkingspot> spots_in_area, Id person_id) {
		// TODO Auto-generated method stub
		double score = 0;
		double best_score=-10000; //Nicht elegant, aber Startwert muss kleiner sein als alle moeglichen Scores
		Parkingspot best_spot;
		best_spot=null;
		for (Parkingspot spot : spots_in_area){
			// SCORE
			double distance = CoordUtils.calcDistance(this.cordinate, spot.parking.get_coordinate());
			double pricem = spot.parking_pricem;
			double cost = pricing.calculate_parking_price(1, false, (int) pricem);
			score =  this.beta_money*cost+this.beta_distance*distance;
			//___

			if(score > best_score){
				best_score=score;
				best_spot=spot;
			}
			
		}
		park_on_spot(best_spot,person_id);
		
	}

	//--------------------------- C H E C K   P R I V A T ---------------------------------------------
	Parkingspot check_private_parking(String facility_id) {
		// !! Zur Beschleunigung Map erstellen ? <facility ID, Private Parking> ?
		for (Parking parking : parking_map.getParkings()) {
			// System.out.println("Suche Parking mit passender facility ID");
			if(parking.facility_id!=null){ //Es gibt datensaetze ohne Facility ID >> Sonst Nullpointer
				if (parking.facility_id.equals(facility_id)) {
					//System.out.println("checke Parking");
					Parkingspot selected_spot = parking.check_for_free_spot(); //Gibt null oder einen freien Platz zurueck
					if (selected_spot != null) {
						return selected_spot;
					}
				}
			}
		}
		return null;
	}

	//--------------------------- G E T  P U B L I C ---------------------------------------------
	LinkedList<Parkingspot> get_public_parkings(Coord coord) {
		// !! Mit quadtree oder aehnlichem Beschleunigen??
		LinkedList<Parkingspot> list = new LinkedList<Parkingspot>();
		for (Parking parking : parking_map.getParkings()) {
			if (parking.type.equals("public")) {
				double distance = CoordUtils.calcDistance(coord,
						parking.get_coordinate());
				if (distance < max_distance) {
					Parkingspot spot = parking.check_for_free_spot();
					if (spot != null) {
						list.add(spot);
					}
				}
			}
		}
		if (list.isEmpty()) {
			list = null; // !! Oder Radius vergroessern?
		}

		return list;
	}
	
	
	//--------------------------- leave Parking  ---------------------------------------------
	public void leave(ActivityEndEvent event) {
		Id person_id = event.getPersonId();
		Parkingspot selected_spot = null;
		VM_Score_Keeper scorekeeper = null;
		Person person = controller.getPopulation().getPersons().get(person_id);
		Map<String, Object> person_attributes = person.getCustomAttributes();
		if(person_attributes.get("selected_parkingspot")!=null){
			selected_spot = (Parkingspot) person_attributes.get("selected_parkingspot");
			person_attributes.remove("selected_parkingspot");
			if(selected_spot.parking.check_for_free_spot()==null){ //Sinde alle anderen Plaetze belegt? Dann von Besetzt >> Frei
				phwriter.add_parking_availible(selected_spot.parking, Double.toString(event.getTime()));
			}
			selected_spot.setOccupied(false); //Platz freigeben
			
			//kosten auf matsim util funktion
			double duration=this.time-selected_spot.getTime_vehicle_parked(); //Parkzeit berechnen
			double payed_parking = pricing.calculate_parking_price(duration/60, false, selected_spot.parking_pricem); // !! EV Boolean anpassen
			// System.out.println(payed_parking);
			
			if (person_attributes.get("VM_Score_Keeper")!= null){
				scorekeeper = (VM_Score_Keeper) person_attributes.get("VM_Score_Keeper");
			} else{
				scorekeeper = new VM_Score_Keeper();
				person_attributes.put("VM_Score_Keeper", scorekeeper);
			}
			scorekeeper.add(payed_parking);
		}
		
		
	}

	
	//--------------------------- P A R K   O N   S P O T ---------------------------------------------
	int park_on_spot(Parkingspot selected_spot, Id person_id) {
		Person person = controller.getPopulation().getPersons().get(person_id);
		Map<String, Object> person_attributes = person.getCustomAttributes();
		person_attributes.put("selected_parkingspot", selected_spot);
		Parkingspot selected_spot_to_set = (Parkingspot) person_attributes.get("selected_parkingspot");
		selected_spot_to_set.setOccupied(true);
		selected_spot_to_set.setTime_vehicle_parked(this.time);
		
		if(selected_spot.parking.check_for_free_spot()==null){
			phwriter.add_parking_occupied(selected_spot.parking, Double.toString(this.time), person_id.toString());
		}
		
		
		return 1;
	}

	public void print_statistics(){
		System.out.println("Privat geparkt:" + Double.toString(this.count_private));
		System.out.println("Oeffentlich geparkt:" + Double.toString(this.count_public));
		System.out.println("Nicht geparkt:" + Double.toString(this.count_not_parked));
		
	}
	
	public void reset_statistics(){
		this.count_not_parked=0;
		this.count_private=0;
		this.count_public=0;
	}

	
	
}


/*//			//				EVENT??
IdImpl person_park_id = new IdImpl(person_id.toString()+"P");
ActivityStartEvent write_event= new ActivityStartEvent(event.getTime(), person_park_id, event.getLinkId(), facilityid, "ParkO");
controller.getEvents().processEvent(write_event);
//-----------
*/

//Das Programm ist jetzt zu ende!!
