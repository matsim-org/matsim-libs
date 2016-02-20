package playground.vbmh.vmParking;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import playground.vbmh.controler.VMConfig;
import playground.vbmh.util.CSVWriter;
import playground.vbmh.util.RemoveDuplicate;
import playground.vbmh.util.VMCharts;
import playground.vbmh.vmEV.EVControl;
import playground.vbmh.vmParking.AdvancedParkingChoice.Option;

import javax.xml.bind.JAXB;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;


/**
 * Manages the whole parking process of one Agent at a time; One instance of this class is kept by the ParkHandler 
 * which starts the park() / leave();
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */

public class ParkControl {
	
	
	//Zur berechnung des besten oeffentlichen Parkplatzes: (Negative Werte, hoechste Score gewinnt)
	//werden jetzt beim startup() aus der Config geladen
	double betaMoney; //= -10; 
	double betaWalk; //= -1; // !! Zweiphasige Kurve einbauen?
	
	
	int countPrivate = 0;
	int countPublic = 0;
	int countNotParked = 0;
	int countEVParkedOnEVSpot = 0;
	
	LinkedList<double[]> availableParkingStat = new LinkedList<double[]>();  //zaehlt fuer jeden Parkvorgang die Parkplaetze, die zur verfuegung stehen
	VMCharts vmCharts = new VMCharts(); 
	LinkedList<LinkedList<String>> notParked;
	HashMap<Integer, Integer> peakLoad;
	HashMap<Integer, Integer> load;
	HashMap<Id, Double> personalBetaSOC = new HashMap<Id, Double>();
	
	
	MatsimServices controller;
	public ParkingMap parkingMap = new ParkingMap(); //Beinhaltet alle Parkplaetze
	PricingModels pricing = new PricingModels(); //Behinhaltet die Preismodelle
	ParkHistoryWriter phwriter = new ParkHistoryWriter(); //Schreibt XML Datei mit Park events
	EVControl evControl;
	IterEndStats iterEndStats;
	boolean evUsage=false;
	
	double time; //Wird auf aktuelle Zeit gesetzt (Vom event)
	Coord cordinate; //Koordinaten an denen die Zie Facility ist. Von hier aus wird gesucht.
	boolean ev;
	
	
	//--------------------------- S T A R T  U P---------------------------------------------
	public int startup(String parkingFilename, String pricingFilename, MatsimServices controller){
		this.controller=controller;
		
		System.out.println("Start up park control");
		
		//Get Betas from Config
		Map<String, String> planCalcParams = this.controller.getConfig().getModule("planCalcScore").getParams();
		betaMoney=-Double.parseDouble(planCalcParams.get("marginalUtilityOfMoney")); //!! in Config positiver Wert >> stimmt das dann so?
		betaWalk=Double.parseDouble(planCalcParams.get("traveling_walk"));
		VMConfig.betaPayMoney=betaMoney;
		VMConfig.betaWalkPMetre=betaWalk/VMConfig.walkingSpeed;
		if(VMConfig.betaPayMoney>0){
			System.out.println("E R R O R  positive betaPayMoney");
		}
		
		
		//System.out.println(betaMoney);
		
		//Charts starten:
		
		
		//Parkplaetze Laden
		File parkingfile = new File( parkingFilename );
		ParkingMap karte = JAXB.unmarshal( parkingfile, ParkingMap.class ); //Laedt Parkplaetze aus XML
		this.parkingMap=karte;
		
		
		//Preise Laden
		File pricingfile = new File( pricingFilename ); 
		this.pricing = JAXB.unmarshal( pricingfile, PricingModels.class ); //Laedt Preise aus XML
		
		System.out.println("Number of initialised pricing models: "+this.pricing.getParking_Pricing_Models().size());
		
		return 0;
	
	}
	
	public void iterStart(){
		this.parkingMap.initHashMap(); //Im startup reicht eigentlich aber es muss sicher sein, dass parkings nicht neu erzeugt werden
		vmCharts.addChart("Available parkings");
		vmCharts.setAxis("Available parkings", "time", "available parkings in area");
		vmCharts.addSeries("Available parkings", "for ev");
		vmCharts.addSeries("Available parkings", "for nev");
		vmCharts.addChart("Available EVparkings");
		vmCharts.addSeries("Available EVparkings", "slow charge");
		vmCharts.addSeries("Available EVparkings", "fast charge");
		vmCharts.addSeries("Available EVparkings", "turbo charge");
		vmCharts.addChart("Walking Distance");
		vmCharts.setBox("Walking Distance", true);
		vmCharts.setInterval("Walking Distance", 900);
		vmCharts.setAxis("Walking Distance", "time", "distance");
		vmCharts.addSeries("Walking Distance", "Walking distance charge");
		vmCharts.addSeries("Walking Distance", "Walking distance no charge");
		vmCharts.setAx("Walking Distance", false);
		peakLoad = new HashMap<Integer, Integer>();
		load = new HashMap<Integer, Integer>();
		for(Parking parking : parkingMap.getParkings()){
			peakLoad.put(parking.id, 0);
			load.put(parking.id, 0);
			if(parking.facilityActType!=null){
				if(parking.facilityActType.equals("parkingLot")){
					parking.setOcupancyStats(true); //!! gehoert nicht hier rein
				}
			}
		}
		notParked = new LinkedList<LinkedList<String>>();
		
	}
	
	
	
	
	//--------------------------- P A R K ---------------------------------------------
	public int park(ActivityStartEvent event) {
		Id<Person> personId = event.getPersonId();
		this.time=event.getTime();

		
		// FACILITY UND KOORDINATEN LADEN
		Id<ActivityFacility> facilityid = Id.create(event.getAttributes().get("facility"), ActivityFacility.class);
        Map<Id<ActivityFacility>, ? extends ActivityFacility> facilitymap = controller.getScenario().getActivityFacilities().getFacilities();
		ActivityFacility facility = facilitymap.get(facilityid);
		this.cordinate = facility.getCoord();
		
		/*
		Parkplatz finden: Es werden zur Facility gehoerende Privatparkplaetze und oeffentliche in der Umgebung
		in einer Liste gesammelt. Anschliessend wird jeder bewertet und der beste ausgewaehlt. Bewertet wird fuer 
		NEVs nach Distanz und Kosten. Bei EVs wird das moegliche Laden beruecksichtigt
		*/
		
		//Geschaetzte Dauer laden
		//sSystem.out.println(getEstimatedDuration(event)/3600);
		
		//EV Checken:
		ev=false;
		if(evUsage){ //Ueberpruefen ob EV Control verwendet wird (Damit Parking weiterhin als standalone funktioniert)
			if(evControl.hasEV(personId)){
				ev=true;
				//System.out.println("Suche Parking fuer EV");
			}
		}
		
		// Geschatzte Dauer und noch Zurueckzulegende Strecke berechnen
		double [] futureInfo = getFutureInfo(event);
		double estimatedDuration = 0;
		double restOfDayDistance = 0;
		if(futureInfo != null ){	
			estimatedDuration = futureInfo[0];
			restOfDayDistance = futureInfo[1];
			//System.out.println("rest of day distance: "+restOfDayDistance);
		} else {
			System.out.println("F E H L E R in der Future Info");
		}
		
		
		// NICHT EV Plaetze
		ParkingSpot privateParking = checkPrivateParking(facilityid.toString(), event.getActType(), false);
		LinkedList<ParkingSpot> spotsInArea = getPublicParkings(cordinate, false);
		if (privateParking != null) {
			spotsInArea.add(privateParking); // Privates Parking anfuegen
		} 
		// ------------
		
		//EV Plaetze dazu	//!! Spots koennten doppelt sein !
		if (ev){
			ParkingSpot privateParkingEV = checkPrivateParking(facilityid.toString(), event.getActType(), true);
			LinkedList<ParkingSpot> spotsInAreaEV = getPublicParkings(cordinate, true);
			if (privateParkingEV != null) {
				spotsInAreaEV.add(privateParkingEV); // Privates Parking anfuegen
			} 
			
			spotsInArea.addAll(spotsInAreaEV);
		}
		//-----------
		RemoveDuplicate.RemoveDuplicate(spotsInArea);
		
		//Statistik (Bezieht sich auf Spots im Unkreis vom Suchradius aus der VMConfig)
		if(ev){
			VMCharts.addValues("Available parkings", "for ev", time, spotsInArea.size());
		}else{
			VMCharts.addValues("Available parkings", "for nev", time, spotsInArea.size());
		}	
		availableParkingStat.add(new double[]{time, spotsInArea.size()});
		//--
		
		//Diagnose //!! kann raus
		if(spotsInArea.size()==8){
		//System.out.println("8, "+facilityid.toString()+", "+facility.getCoord().toString()+", "+personId.toString());
		}
		
		//-----
		
		//-- If there is no Spot within the given distance: -----------
		if(spotsInArea.size()==0){ //mit 5km Radius
			//System.out.println("Agent is looking for parking in maximum range of 5 km");
			phwriter.addAgentNotParkedWithinDefaultDistance(Double.toString(time), personId.toString());
			spotsInArea = getPublicParkings(cordinate, false, 5000); //!! 5000 in die config
			if (ev){
				LinkedList<ParkingSpot> spotsInAreaEV = getPublicParkings(cordinate, true, 5000);
				spotsInArea.addAll(spotsInAreaEV);
			}
			RemoveDuplicate.RemoveDuplicate(spotsInArea);	
		}
		//--------------				--------------------------------
		

		
		//Select 
		if(spotsInArea.size()>0){ 
			selectParkingAdvanced(spotsInArea, personId, estimatedDuration, restOfDayDistance, ev);
			return 1;
		}
		/*else { //!! Groesserer Suchradius				//weiter oben ersetzt
			int maxDist = VMConfig.maxDistance;
			while (spotsInArea.size()==0){
				maxDist = maxDist * 2;
				//!!get publics
				break;
			}
		}*/
		//-----
		
		//System.err.println("Nicht geparkt");
		
		// Wer nicht umkreis von 5km parken kann bekommt massiv disutil
        Map<String, Object> personAttributes = controller.getScenario().getPopulation().getPersons().get(personId).getCustomAttributes();
		VMScoreKeeper scorekeeper;
		if (personAttributes.get("VMScoreKeeper")!= null){
			scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
		} else{
			scorekeeper = new VMScoreKeeper();
			personAttributes.put("VMScoreKeeper", scorekeeper);
		}
		scorekeeper.add(-30);
		
		phwriter.addAgentNotParked(Double.toString(this.time), personId.toString());
		
		LinkedList<String> notParkedLine = new LinkedList<String>();
		notParkedLine.add(personId.toString());
		notParkedLine.add(Double.toString(cordinate.getX()));
		notParkedLine.add(Double.toString(cordinate.getY()));
		notParkedLine.add(Double.toString(time));
		notParkedLine.add(Boolean.toString(ev));
		notParkedLine.add(facilityid.toString());
		notParkedLine.add(event.getActType());
		
		notParked.add(notParkedLine);
		
		
		this.countNotParked++;
		return -1;
	}

	
	//--------------------------- SELECT PARKING --------Using Advanced Parking Choice Model-------------------------------------	
	private void selectParkingAdvanced(LinkedList<ParkingSpot> spotsInArea, Id<Person> personId, double duration, double restOfDayDistance, boolean ev){
		double stateOfCharge=0;
		double neededBatteryPercentage=-1.0; //Stays -1 for NEVs which tells the Advanced Parking Choice that it is an NEV
		boolean hasToCharge = false;
		int countSlowCharge=0;
		int countFastCharge=0;
		int countTurboCharge=0;
		AdvancedParkingChoice choice = new AdvancedParkingChoice(); //!! direkt im start up initienren, dafuer eine clear methode in der adv parking choice einbauen, damit ein overwrite mit anderem model moeglich ist
		choice.startUp();
		
		//calculate needed Battery Percenatge and check if ev has to charge
		if(ev){
			stateOfCharge = evControl.stateOfChargePercentage(personId);
			neededBatteryPercentage = evControl.calcEnergyConsumptionForDistancePerc(personId, restOfDayDistance);
			//System.out.println("Needed battery perc :"+neededBatteryPercentage);
			//System.out.println("State of charge: "+stateOfCharge);
			if(neededBatteryPercentage>stateOfCharge){
				phwriter.addAgentHasToCharge(Double.toString(time), personId.toString());
				hasToCharge=true;
			}
			
			//Get Beta SOC
			Double betaSOC = personalBetaSOC.get(personId);
			if(betaSOC != null){
				choice.setBetaSOC(betaSOC);
			}else{
				personalBetaSOC.put(personId, choice.getBetaSOC());
			}
			
			
		}
		//----------------
		choice.setRequiredRestOfDayBatPerc(neededBatteryPercentage);
		
		for(ParkingSpot spot : spotsInArea){
			double distance = 2*CoordUtils.calcEuclideanDistance(this.cordinate, spot.parking.getCoordinate()); //2 times >> return
			double cost = pricing.calculateParkingPrice(duration, ev, spot);
			double newStateOfChargePerc = 0.0;
			if(cost==-1){ //this vehicle seems to be not allowed to park here
				//continue; //War fuer ev exc, sollte nicht mehr gebraucht werden
			}
			if(cost<0){
				cost=0;
			}
			
			if (ev && spot.charge) {
				newStateOfChargePerc = evControl.calcNewStateOfChargePercentage(personId,spot.chargingRate, duration);
			}else if(ev){
				newStateOfChargePerc=stateOfCharge;
			}
			choice.addOption(choice.new Option(spot, cost, distance, newStateOfChargePerc/100.0));
			
			//STATS
			if(spot.chargingRate<3){
				countSlowCharge++;
			}else if(spot.chargingRate<5){
				countFastCharge++;
			}else{
				countTurboCharge++;
			}
			//----------
		}
		
		Option bestOption = choice.selectBestOption();
		
		//Stat
		if(hasToCharge){
			//vmCharts.addValues("Available EVparkings", "slow charge", time, countSlowCharge);
			vmCharts.addValues("Available EVparkings", "fast charge", time, countFastCharge);
			vmCharts.addValues("Available EVparkings", "turbo charge", time, countTurboCharge);
		}
		//-----
		
		
		try {
			parkOnSpot(bestOption.spot, bestOption.score, personId); //!!!!
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if(bestOption != null){
				System.out.println(bestOption.toString());
				if(bestOption.spot!=null){
					System.out.println(bestOption.spot.toString());
				}
			}else{
				System.out.println("Best option ist null");
				System.out.println("Zeit "+this.time);
				System.out.println("Person "+personId.toString());
				System.out.println("Spots in area Size "+spotsInArea.size());
				System.out.println(spotsInArea.toString());
			}
			System.out.println();
			e.printStackTrace();
			System.out.println("Zeit "+this.time);
			System.out.println("Person "+personId.toString());
			System.out.println("Spots in area Size "+spotsInArea.size());
			System.out.println("Duration "+duration);
			System.out.println(spotsInArea.toString());

			int a = 1/0;
		}
		
		
		
		
		
		
		
	}
	
	
	//--------------------------- SELECT PARKING ---------------------------------------------	
	private void selectParking(LinkedList<ParkingSpot> spotsInArea, Id personId, double duration, double restOfDayDistance, boolean ev) {
		// TODO Auto-generated method stub
		boolean sufficientEVSpotFound = false; //Marks if there is a spot with anough possible charging to get the agent back home
		boolean hasToCharge=false;
		double score = 0;
		double bestScore=-10000; //Nicht elegant, aber Startwert muss kleiner sein als alle moeglichen Scores
		double stateOfCharge=0;
		double neededBatteryPercentage=0;
		int countSlowCharge=0;
		int countFastCharge=0;
		int countTurboCharge=0;
		ParkingSpot bestSpot;
		bestSpot=null;
		
		
		
		if(ev){
			stateOfCharge = evControl.stateOfChargePercentage(personId);
			neededBatteryPercentage = evControl.calcEnergyConsumptionForDistancePerc(personId, restOfDayDistance);
			//System.out.println("Needed battery perc :"+neededBatteryPercentage);
			//System.out.println("State of charge: "+stateOfCharge);
			if(neededBatteryPercentage>stateOfCharge){
				phwriter.addAgentHasToCharge(Double.toString(time), personId.toString());
				hasToCharge=true;
			}
		}
		
		
		
		for (ParkingSpot spot : spotsInArea){
			
			//Diagnose
			if(pricing.get_model(spot.parkingPriceM).checkEvExc() && !ev){
				System.out.println("NEV hat evExclusive Platz in Auswahl - should not happen!");
			}
			
			//----
			
			double evRelatedScore = 0;
			double distance = 2 * CoordUtils.calcEuclideanDistance(this.cordinate, spot.parking.getCoordinate());
			double pricem = spot.parkingPriceM;
			double cost = pricing.calculateParkingPrice(duration, ev, spot);
			//System.out.println("Cost :"+ Double.toString(cost));
			
			//EV Score:
			if(ev && spot.charge){
				
				double newStateOfChargePerc = evControl.calcNewStateOfChargePercentage(personId, spot.chargingRate, duration);
				double stateOfChargeGainPerc = newStateOfChargePerc-stateOfCharge;
				double chargableAmountOfEnergy =evControl.clalcChargedAmountOfEnergy(personId, spot.chargingRate, duration);
//				if(hasToCharge){
//					System.out.println("new state of charge % "+newStateOfChargePerc);
//				}
				//System.out.println("needed Battery Percentage "+neededBatteryPercentage);
				if(stateOfCharge<neededBatteryPercentage && newStateOfChargePerc>neededBatteryPercentage){
					//Rest des Tages kann ohne Laden nicht gefahren werden mit jedoch schon.
					evRelatedScore+=30; //!! Wert anpassen
					sufficientEVSpotFound = true;
//					System.out.println(stateOfCharge);
//					System.out.println("30 Punkte ev related");
				}
				
				evRelatedScore += VMConfig.pricePerKWH*chargableAmountOfEnergy*betaMoney*-1; //Ersparnis gegenueber zu hause Laden
				//!! Vorzeichen?
				
				double betaBatteryPerc = 0.1; //!! Gerhoert nicht hier her und sollte nicht als konstant angenommen werden
				//evRelatedScore += betaBatteryPerc  * stateOfChargeGainPerc; //!! Nur provisorisch !
				
				//System.out.println("Ev related Score :" + Double.toString(evRelatedScore));
			
				//Stats
				if(spot.chargingRate<3){
					countSlowCharge++;
				}else if(spot.chargingRate<5){
					countFastCharge++;
				}else{
					countTurboCharge++;
				}
				//---------
			
			
			
			
			}
			
			double walkingTime = distance/VMConfig.walkingSpeed; //in h 
			//System.out.println("Walking Time: "+Double.toString(walkingTime));
			score =  this.betaMoney*1*cost+this.betaWalk*walkingTime+evRelatedScore; //!! Vorzeichen
			//___

			if(score > bestScore && cost != -1){ //cost = -1 >> vehicle is not allowed to park here
				bestScore=score;
				bestSpot=spot;
			}
			
		}
		
		if(sufficientEVSpotFound && !bestSpot.charge){
			phwriter.addEVChoseWrongSpot(Double.toString(time), personId.toString(), bestScore);
		}
		//Stat
		if(hasToCharge){
			//vmCharts.addValues("Available EVparkings", "slow charge", time, countSlowCharge);
			vmCharts.addValues("Available EVparkings", "fast charge", time, countFastCharge);
			vmCharts.addValues("Available EVparkings", "turbo charge", time, countTurboCharge);
		}
		//-----
		
		parkOnSpot(bestSpot, bestScore, personId);
		
	}

	//--------------------------- C H E C K   P R I V A T ---------------------------------------------
	ParkingSpot checkPrivateParking(String facilityId, String facilityActType, boolean ev) {
		//Gibt falls verfuegbar Spot auf Privatparkplatz passend zur Aktivitaet in der Facility zurueck
		//Bei EVS werden priorisiert EV Plaetze zurueck gegeben
		// !! Zur Beschleunigung Map erstellen ? <facility ID, Private Parking> ?
		ParkingSpot selectedSpot = null;
		
		Parking parking = parkingMap.getPrivateParking(facilityId, facilityActType);
		if(parking==null){
			return null;
		}
		
		selectedSpot = parking.checkForFreeSpot(); //Gibt null oder einen freien Platz zurueck
		if(ev){
			selectedSpot = parking.checkForFreeSpotEVPriority(); // !!Wenn ev Spot vorhanden wird er genommen.
		}
		if (selectedSpot != null) {
			return selectedSpot;
		}
		
		return null;
	}

	//--------------------------- G E T  P U B L I C ---------------------------------------------
	public LinkedList<ParkingSpot> getPublicParkings(Coord coord, boolean ev) {
		return getPublicParkings(coord, ev, VMConfig.maxDistance);
	}
	
	
	LinkedList<ParkingSpot> getPublicParkings(Coord coord, boolean ev, int maxDistance) {
		// !! Mit quadtree oder aehnlichem Beschleunigen??
		LinkedList<ParkingSpot> list = new LinkedList<ParkingSpot>();
		
		for(Parking parking :this.parkingMap.getPublicParkings(coord.getX(), coord.getY(), maxDistance)){
			ParkingSpot spot = null;
			spot = parking.checkForFreeSpot();
			if(ev){
				spot = parking.checkForFreeSpotEVPriority();
			}
			
			if (spot != null) {
				list.add(spot);
			}
		}
		
		/*
		for (Parking parking : parkingMap.getParkings()) {
			if (parking.type.equals("public")) {
				ParkingSpot spot = null;
				double distance = CoordUtils.calcDistance(coord,
						parking.getCoordinate());
				if (distance < VMConfig.maxDistance) {
					spot = parking.checkForFreeSpot();
					if(ev){
						spot = parking.checkForFreeSpotEVPriority();
					}
					
					if (spot != null) {
						list.add(spot);
					}
				}
			}
		}*/
		
		
		if (list.isEmpty()) {
			//list = null; // !! Oder Radius vergroessern?
		}

		return list;
	}
	
	
	//--------------------------- leave Parking  ---------------------------------------------
	public void leave(ActivityEndEvent event) {
		double time = event.getTime();
		Id personId = event.getPersonId();
		ParkingSpot selectedSpot = null;
		VMScoreKeeper scorekeeper = null;
        Person person = controller.getScenario().getPopulation().getPersons().get(personId);
		
		ev=false;
		if(evUsage){
			if(evControl.hasEV(event.getPersonId())){
				ev=true;
			}
		}
		
		Map<String, Object> personAttributes = person.getCustomAttributes();
		if(personAttributes.get("selectedParkingspot")!=null){
			selectedSpot = (ParkingSpot) personAttributes.get("selectedParkingspot");
			personAttributes.remove("selectedParkingspot");
			
			boolean wasOccupied = false;
			if(selectedSpot.parking.checkForFreeSpot()==null){ //Sinde alle anderen Plaetze belegt? Dann von Besetzt >> Frei
				wasOccupied = true;
			}
			//selectedSpot.setOccupied(false); //Platz freigeben
			selectedSpot.parking.leaveSpot(selectedSpot, time);
			load.put(selectedSpot.parking.id, load.get(selectedSpot.parking.id)-1);
			
			
			//kosten auf matsim util funktion
			double duration=this.time-selectedSpot.getTimeVehicleParked(); //Parkzeit berechnen
			//System.out.println(duration);
			
			double payedParking = pricing.calculateParkingPrice(duration, ev, selectedSpot); // !! EV Boolean anpassen
			// System.out.println(payed_parking);
			
			//System.out.println("bezahltes Parken (Score): "+payedParking*this.betaMoney);

			
			if (personAttributes.get("VMScoreKeeper")!= null){
				scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
			} else{
				scorekeeper = new VMScoreKeeper();
				personAttributes.put("VMScoreKeeper", scorekeeper);
			}
			scorekeeper.add(payedParking*this.betaMoney);
			//System.out.println("payedParking util :"+payedParking*this.betaMoney);
			//EVs:
			if(!evUsage){return;}
			if(evControl.hasEV(event.getPersonId())){
				if(selectedSpot.charge){
					double chargedAmountOfEnergy;
					chargedAmountOfEnergy=evControl.charge(personId, selectedSpot.chargingRate, duration);
					scorekeeper.add(VMConfig.pricePerKWH*VMConfig.betaPayMoney*(-1)*chargedAmountOfEnergy);
					//System.out.println("EV charged person: "+personId.toString()+" parking: "+selectedSpot.parking.id+" new state of charge [%]: "+evControl.stateOfChargePercentage(personId));
				}
			}
		
			//Events
			String spotType;
			if(selectedSpot.charge){
				spotType="ev";
			}else{
				spotType="nev";
			}

			if(evControl.hasEV(personId)){
				phwriter.addEVLeft(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), selectedSpot.parking.type, spotType, Double.toString(evControl.stateOfChargePercentage(personId)));
			} else {
				phwriter.addNEVLeft(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), selectedSpot.parking.type, spotType);
			}
			
			if(selectedSpot.parking.checkForFreeSpot()==null){
				phwriter.addParkingOccupied(selectedSpot.parking, Double.toString(this.time), personId.toString());
			}
			
			if(wasOccupied){
				phwriter.addParkingAvailible(selectedSpot.parking, Double.toString(event.getTime()));
			}	
		
		
		}
		
		
	}

	
	//--------------------------- P A R K   O N   S P O T ---------------------------------------------
	int parkOnSpot(ParkingSpot selectedSpot, double score, Id personId) {
        Person person = controller.getScenario().getPopulation().getPersons().get(personId);
		Map<String, Object> personAttributes = person.getCustomAttributes();
		personAttributes.put("selectedParkingspot", selectedSpot);
		ParkingSpot selectedSpotToSet = (ParkingSpot) personAttributes.get("selectedParkingspot");
		if(selectedSpotToSet==null){
			System.out.println("Selecting a spot went wrong!");
		}
		selectedSpotToSet.parking.parkOnSpot(selectedSpotToSet, time);
		//selectedSpotToSet.setOccupied(true);
		//selectedSpotToSet.setTimeVehicleParked(this.time);
		int currentLoad = load.get(selectedSpotToSet.parking.id);
		load.put(selectedSpotToSet.parking.id, currentLoad+1);
		if(peakLoad.get(selectedSpotToSet.parking.id)<currentLoad+1){
			peakLoad.put(selectedSpotToSet.parking.id, currentLoad+1);
		}
		
		
		
		VMScoreKeeper scorekeeper;
		if (personAttributes.get("VMScoreKeeper")!= null){
			scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
		} else{
			scorekeeper = new VMScoreKeeper();
			personAttributes.put("VMScoreKeeper", scorekeeper);
		}
		double distance = 2 * CoordUtils.calcEuclideanDistance(this.cordinate, selectedSpot.parking.getCoordinate());
		double walkingTime = distance/VMConfig.walkingSpeed; 
		//System.out.println("Walking Score :"+betaWalk*walkingTime);
		scorekeeper.add(betaWalk*walkingTime);
		
		
		

		//Events
		
		String spotType;
		if(selectedSpot.charge){
			spotType="ev";
		}else{
			spotType="nev";
		}

		if(evUsage && evControl.hasEV(personId)){
			phwriter.addEVParked(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), score, selectedSpot.parking.type, spotType, Double.toString(evControl.stateOfChargePercentage(personId)));
		} else {
			phwriter.addNEVParked(Double.toString(time), person.getId().toString(), Integer.toString(selectedSpot.parking.id), score, selectedSpot.parking.type, spotType);
		}
		
		if(selectedSpot.parking.checkForFreeSpot()==null){
			phwriter.addParkingOccupied(selectedSpot.parking, Double.toString(this.time), personId.toString());
		}
		
		
		if(evUsage&&spotType.equals("ev")&&evControl.hasEV(personId)){
			vmCharts.addValues("Walking Distance", "Walking distance charge", time, distance/2);
		}else{
			vmCharts.addValues("Walking Distance", "Walking distance no charge", time, distance/2);
		}
		
		iterEndStats.parkEventHandler(selectedSpotToSet, personId, time);
		
		
		//--
		
		
		//statistik
		
		
		//---
		
		return 1;
	}

	//--------------------------- ---------------------------------------------
	public void printStatistics(){
		System.out.println("Privat geparkt:" + Double.toString(this.countPrivate));
		System.out.println("Oeffentlich geparkt:" + Double.toString(this.countPublic));
		System.out.println("Nicht geparkt:" + Double.toString(this.countNotParked));
		System.out.println("EVs auf EV Spots geparkt:" + this.countEVParkedOnEVSpot);
		
		/*
		String filename = controller.getConfig().getModule("controler").getValue("outputDirectory")+"/Charts/Parkplatzauswahl_"+controller.getIterationNumber()+".png";
		XYScatterChart chart = new XYScatterChart("Parkplatzauswahl", "Time", "available Spots");
		double[] time = new double[availableParkingStat.size()];
		double[] availableParkings = new double[availableParkingStat.size()];
		int i=0;
		for(double[] element : availableParkingStat){
			time[i]=element[0];
			availableParkings[i]=element[1];
			i++;
		}
		chart.addSeries("anzahl", time, availableParkings);
		chart.saveAsPng(filename, 800, 600);
		*/
		
		CSVWriter csvWriter = new CSVWriter(controller.getConfig().getModule("controler").getValue("outputDirectory")+"/parkhistory/peakload_"+controller.getIterationNumber());
		LinkedList<LinkedList<String>> peakLoadOutput = new LinkedList<LinkedList<String>>();
		LinkedList<String> headLine = new LinkedList<String>();
		headLine.add("ID");
		headLine.add("FacID");
		headLine.add("FacActType");
		headLine.add("type");
		headLine.add("X");
		headLine.add("Y");
		headLine.add("EV Exclusive");
		headLine.add("EV Capacity");
		headLine.add("NEV Capacitiy");
		headLine.add("Sum");
		headLine.add("Rate of charge");
		headLine.add("PeakLoad");
		peakLoadOutput.add(headLine);
		for(Parking parking : parkingMap.getParkings()){
			peakLoadOutput.add(parking.getLinkedList());
			peakLoadOutput.getLast().add(Integer.toString(peakLoad.get(parking.id)));
			//System.out.println(peekLoadOutput.getLast().toString());
		}
		csvWriter.writeAll(peakLoadOutput);
		csvWriter.close();
		
		csvWriter = new CSVWriter(controller.getConfig().getModule("controler").getValue("outputDirectory")+"/parkhistory/notParked_"+controller.getIterationNumber());
		headLine = new LinkedList<String>();
		headLine.add("PersonID");
		headLine.add("X");
		headLine.add("Y");
		headLine.add("time");
		headLine.add("EV");
		headLine.add("FacID");
		headLine.add("FacActType");
		
		
		notParked.add(0, headLine);
		csvWriter.writeAll(notParked);
		csvWriter.close();
		
		csvWriter = new CSVWriter(controller.getConfig().getModule("controler").getValue("outputDirectory")+"/parkhistory/personalBETASOC_"+controller.getIterationNumber());
		LinkedList<LinkedList<String>> personalBetaSOCOutput = new LinkedList<LinkedList<String>>();
		headLine = new LinkedList<String>();
		headLine.add("PersonID");
		headLine.add("betaSOC");
		personalBetaSOCOutput.add(0, headLine);
		Iterator<Id> iterartor = personalBetaSOC.keySet().iterator();
		for(Double item : personalBetaSOC.values()){
			personalBetaSOCOutput.add(new LinkedList<String>());
			personalBetaSOCOutput.getLast().add(iterartor.next().toString());
			personalBetaSOCOutput.getLast().add(Double.toString(item));
		}
		csvWriter.writeAll(personalBetaSOCOutput);
		csvWriter.close();
		
		
	}
	
	//---------------------------  ---------------------------------------------
	public void resetStatistics(){
		this.countNotParked=0;
		this.countPrivate=0;
		this.countPublic=0;
		this.countEVParkedOnEVSpot=0;
		availableParkingStat.clear();
	}
	
	//--------------------------- G E T     F U T U R E     I N F O  -----
	
	public double[] getFutureInfo(ActivityStartEvent event){
		//System.out.println("Get estimated duration:");
		//[0]: Estimated duration of parking
		//[1]: Estimated distance to travel during rest of day


        Person person = controller.getScenario().getPopulation().getPersons().get(event.getPersonId());
		PlanImpl plan = (PlanImpl) person.getSelectedPlan();
		double endTime=0;
		int actCount = (Integer) person.getCustomAttributes().get("ActCounter");
		ActivityImpl actFromCounter = (ActivityImpl) person.getSelectedPlan().getPlanElements().get((Integer) person.getCustomAttributes().get("ActCounter"));
		ActivityImpl activity = actFromCounter;
		
		//Aktuelle activity finden:
		/*
		boolean getnext = true;
		ActivityImpl activity = (ActivityImpl) plan.getFirstActivity();
		while(getnext){
			if(activity.equals(plan.getLastActivity())){
				endTime = plan.getFirstActivity().getEndTime();
				double [] returnValue = {24*3600-event.getTime()+endTime, 0}; //Letzte activity >> Parkdauer laenger als Rest der Iteration
				
				if(actFromCounter.equals(activity)){
					System.out.println("Aktivitaeten stimmen ueberein");
				}else{
					System.out.println("F E H L E R: Aktivitaeten stimmen N I C H T ueberein");
				}
				
				return returnValue;
			}
			
			if(activity.getFacilityId().equals(event.getFacilityId()) && Math.abs(activity.getStartTime()-event.getTime())<3600){ //!! Nicht zwei activitys am gleichen Ort innerhalb einer Stunde?
				//gefunden
				getnext=false;
			} else{
				Leg leg = plan.getNextLeg(activity);
				if(leg==null){return null;}
				activity=(ActivityImpl) plan.getNextActivity(leg); // Naechste laden
				if(activity==null){ return null;} //Aktuelle activity nicht gefunden >> sollte nicht passieren
				
			}
		}

		
		if(actFromCounter.equals(activity)){
			System.out.println("Aktivitaeten stimmen ueberein");
		}else{
			System.out.println("F E H L E R: Aktivitaeten stimmen N I C H T ueberein");
		}
		*/
		
		
		//Pruefen ob letzte am Tag:
		if(activity.equals(plan.getLastActivity())){
			endTime = plan.getFirstActivity().getEndTime();
			double [] returnValue = {24*3600-event.getTime()+endTime, 0}; //Letzte activity >> Parkdauer laenger als Rest der Iteration
			return returnValue;
		}
		

		
		// Naechste Car leg nach aktueller activity finden:
		boolean foundNextCarLeg = false;
		Leg nextCarLeg=null;
		while (foundNextCarLeg == false){
			Leg leg = plan.getNextLeg(activity);
			if(leg.getMode().equalsIgnoreCase("car")){
				//endTime = leg.getDepartureTime(); //!!!!!!! MatSim provides a wrong time !!!
				endTime=activity.getEndTime();
				nextCarLeg=leg;
				foundNextCarLeg=true;
			}else{
				Activity act = plan.getNextActivity(leg);
				if(act==null){return null;}
				leg=plan.getNextLeg(act);
				if(leg==null){
					System.out.println("F E H L E R letzte activity nicht identifiziert");
					System.out.println("Person: "+person.getId().toString()+" count: "+actCount);
					return null; //Scheint letzte Activity zu sein >> Parkdauer laenger als Rest der Iteration
				}
			}
			
		}
		
		
		//Calculate the distance to drive during the rest of the day
		double restOfDayDistance = 0;
		restOfDayDistance+=nextCarLeg.getRoute().getDistance();
		boolean goOn = true;
		while(goOn){
			Activity act = plan.getNextActivity(nextCarLeg);
			if(act==null){
				goOn=false;
				break;
			}
			nextCarLeg=plan.getNextLeg(act);
			if(nextCarLeg==null){
				break;
			}
			if(nextCarLeg.getMode().equalsIgnoreCase("car")){
				restOfDayDistance+=nextCarLeg.getRoute().getDistance();
			}
			
		}
		
		//System.out.println("Rest of day distance: "+restOfDayDistance);
		
		if(endTime==0){return null;}
		double parkDuration = endTime-event.getTime();
		double [] returnValue = {parkDuration, restOfDayDistance};
		return returnValue;
		
	}




	public void setEvControl(EVControl evControl) {
		this.evControl = evControl;
		this.evUsage=true;
	}

	
	
	public void clearAgents(){
        for (Person person : controller.getScenario().getPopulation().getPersons().values()){
			person.getCustomAttributes().remove("selectedParkingspot");
		}
	}

	public PricingModels getPricing() {
		return pricing;
	}
	
	
	
}


/*//			//				EVENT??
IdImpl person_park_id = new IdImpl(person_id.toString()+"P");
ActivityStartEvent write_event= new ActivityStartEvent(event.getTime(), person_park_id, event.getLinkId(), facilityid, "ParkO");
controller.getEvents().processEvent(write_event);
//-----------
*/

//Das Programm ist jetzt zu ende!!
