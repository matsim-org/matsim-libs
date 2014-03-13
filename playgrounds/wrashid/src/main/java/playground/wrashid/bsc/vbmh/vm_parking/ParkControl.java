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

public class ParkControl {
	int maxDistance = 2000; //Maximaler Umkreis in dem Parkplaetze gesucht werden
	
	//Zur berechnung des besten oeffentlichen Parkplatzes: (Negative Werte, hoechste Score gewinnt)
	double betaMoney = -10; 
	double betaDistance = -1; // !! Zweiphasige Kurve einbauen?
	
	int countPrivate = 0;
	int countPublic = 0;
	int countNotParked = 0;
	
	Controler controller;
	ParkingMap parkingMap = new ParkingMap(); //Beinhaltet alle Parkplaetze
	PricingModels pricing = new PricingModels(); //Behinhaltet dei Preismodelle
	ParkHistoryWriter phwriter = new ParkHistoryWriter(); //Schreibt XML Datei mit Park events
	double time; //Wird auf aktuelle Zeit gesetzt (Vom event)
	Coord cordinate; //Koordinaten an denen die Zie Facility ist. Von hier aus wird gesucht.
	
	
	
	//--------------------------- S T A R T  U P---------------------------------------------
	public int startup(String parkingFilename, String pricingFilename, Controler controller){
		this.controller=controller;
		
		//Parkplaetze Laden
		File parkingfile = new File( parkingFilename );
		ParkingMap karte = JAXB.unmarshal( parkingfile, ParkingMap.class ); //Laedt Parkplaetze aus XML
		this.parkingMap=karte;
		
		//Preise Laden
		File pricingfile = new File( pricingFilename ); 
		this.pricing = JAXB.unmarshal( pricingfile, PricingModels.class ); //Laedt Preise aus XML
		return 0;
		
	
	}
	
	
	
	
	//--------------------------- P A R K ---------------------------------------------
	public int park(ActivityStartEvent event) {
		Id personId = event.getPersonId();
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
		ParkingSpot privateParking = checkPrivateParking(facilityid.toString());
		if (privateParking != null) {
			//System.out.println("Privaten Parkplatz gefunden");
			parkOnSpot(privateParking, personId);
			this.countPrivate ++;
			return 1;
		} 
		
		
		// OEFFENTLICHES PARKEN
		LinkedList<ParkingSpot> spotsInArea = getPublicParkings(cordinate);
		if (spotsInArea != null) {
			parkPublic(spotsInArea, personId);
			//System.out.println("Oeffentlich geparkt");
			this.countPublic ++;
			return 1;
		}
		
		//System.err.println("Nicht geparkt"); // !! Was passiert wenn Kein Parkplatz im Umkreis gefunden?
		
		// !! Provisorisch: Agents bestrafen die nicht Parken:
		Map<String, Object> personAttributes = controller.getPopulation().getPersons().get(personId).getCustomAttributes();
		VMScoreKeeper scorekeeper;
		if (personAttributes.get("VMScoreKeeper")!= null){
			scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
		} else{
			scorekeeper = new VMScoreKeeper();
			personAttributes.put("VMScoreKeeper", scorekeeper);
		}
		scorekeeper.add(30);
		
		phwriter.addAgentNotParked(Double.toString(this.time), personId.toString());
		
		this.countNotParked++;
		return -1;
	}

	//--------------------------- P A R K   P U B L I C ---------------------------------------------	
	private void parkPublic(LinkedList<ParkingSpot> spotsInArea, Id personId) {
		// TODO Auto-generated method stub
		double score = 0;
		double bestScore=-10000; //Nicht elegant, aber Startwert muss kleiner sein als alle moeglichen Scores
		ParkingSpot bestSpot;
		bestSpot=null;
		for (ParkingSpot spot : spotsInArea){
			// SCORE
			double distance = CoordUtils.calcDistance(this.cordinate, spot.parking.getCoordinate());
			double pricem = spot.parkingPriceM;
			double cost = pricing.calculateParkingPrice(1, false, (int) pricem);
			score =  this.betaMoney*cost+this.betaDistance*distance;
			//___

			if(score > bestScore){
				bestScore=score;
				bestSpot=spot;
			}
			
		}
		parkOnSpot(bestSpot,personId);
		
	}

	//--------------------------- C H E C K   P R I V A T ---------------------------------------------
	ParkingSpot checkPrivateParking(String facilityId) {
		// !! Zur Beschleunigung Map erstellen ? <facility ID, Private Parking> ?
		for (Parking parking : parkingMap.getParkings()) {
			// System.out.println("Suche Parking mit passender facility ID");
			if(parking.facilityId!=null){ //Es gibt datensaetze ohne Facility ID >> Sonst Nullpointer
				if (parking.facilityId.equals(facilityId)) {
					//System.out.println("checke Parking");
					ParkingSpot selectedSpot = parking.checkForFreeSpot(); //Gibt null oder einen freien Platz zurueck
					if (selectedSpot != null) {
						return selectedSpot;
					}
				}
			}
		}
		return null;
	}

	//--------------------------- G E T  P U B L I C ---------------------------------------------
	LinkedList<ParkingSpot> getPublicParkings(Coord coord) {
		// !! Mit quadtree oder aehnlichem Beschleunigen??
		LinkedList<ParkingSpot> list = new LinkedList<ParkingSpot>();
		for (Parking parking : parkingMap.getParkings()) {
			if (parking.type.equals("public")) {
				double distance = CoordUtils.calcDistance(coord,
						parking.getCoordinate());
				if (distance < maxDistance) {
					ParkingSpot spot = parking.checkForFreeSpot();
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
		Id personId = event.getPersonId();
		ParkingSpot selectedSpot = null;
		VMScoreKeeper scorekeeper = null;
		Person person = controller.getPopulation().getPersons().get(personId);
		Map<String, Object> personAttributes = person.getCustomAttributes();
		if(personAttributes.get("selectedParkingspot")!=null){
			selectedSpot = (ParkingSpot) personAttributes.get("selectedParkingspot");
			personAttributes.remove("selectedParkingspot");
			if(selectedSpot.parking.checkForFreeSpot()==null){ //Sinde alle anderen Plaetze belegt? Dann von Besetzt >> Frei
				phwriter.addParkingAvailible(selectedSpot.parking, Double.toString(event.getTime()));
			}
			selectedSpot.setOccupied(false); //Platz freigeben
			
			//kosten auf matsim util funktion
			double duration=this.time-selectedSpot.getTimeVehicleParked(); //Parkzeit berechnen
			double payedParking = pricing.calculateParkingPrice(duration/60, false, selectedSpot.parkingPriceM); // !! EV Boolean anpassen
			// System.out.println(payed_parking);
			
			if (personAttributes.get("VMScoreKeeper")!= null){
				scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
			} else{
				scorekeeper = new VMScoreKeeper();
				personAttributes.put("VMScoreKeeper", scorekeeper);
			}
			scorekeeper.add(payedParking);
		}
		
		
	}

	
	//--------------------------- P A R K   O N   S P O T ---------------------------------------------
	int parkOnSpot(ParkingSpot selectedSpot, Id personId) {
		Person person = controller.getPopulation().getPersons().get(personId);
		Map<String, Object> personAttributes = person.getCustomAttributes();
		personAttributes.put("selectedParkingspot", selectedSpot);
		ParkingSpot selectedSpotToSet = (ParkingSpot) personAttributes.get("selectedParkingspot");
		selectedSpotToSet.setOccupied(true);
		selectedSpotToSet.setTimeVehicleParked(this.time);
		
		if(selectedSpot.parking.checkForFreeSpot()==null){
			phwriter.addParkingOccupied(selectedSpot.parking, Double.toString(this.time), personId.toString());
		}
		
		
		return 1;
	}

	public void printStatistics(){
		System.out.println("Privat geparkt:" + Double.toString(this.countPrivate));
		System.out.println("Oeffentlich geparkt:" + Double.toString(this.countPublic));
		System.out.println("Nicht geparkt:" + Double.toString(this.countNotParked));
		
	}
	
	public void resetStatistics(){
		this.countNotParked=0;
		this.countPrivate=0;
		this.countPublic=0;
	}

	
	
}


/*//			//				EVENT??
IdImpl person_park_id = new IdImpl(person_id.toString()+"P");
ActivityStartEvent write_event= new ActivityStartEvent(event.getTime(), person_park_id, event.getLinkId(), facilityid, "ParkO");
controller.getEvents().processEvent(write_event);
//-----------
*/

//Das Programm ist jetzt zu ende!!
