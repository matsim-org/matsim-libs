package playground.vbmh.vmParking;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.matsim.api.core.v01.Coord;


/**
 * This represents a public / private parking lot; Every parking space is an own parkingSpot object;
 * create_spots() creates the specified number of EVPS/CPS with the specified attributes;
 * checkForFreeSpot() checks if one of those spots is available (functions for EVPS/CPS are available);
 * clearSpots() should be called after each iteration to make sure there are no occupied spots at the beginning of 
 * the next iteration.
 * 
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class Parking {
	public @XmlAttribute int id;
	public long capacityEV;
	public long capacityNEV;
	private int occupancyEVSpots; //Anzahl belegte Plaetze
	private int occupancyNEVSpots;
	public double chargingRate;
	public int parkingPriceMEVSpot, parkingPriceMNEVSpot, chargingPriceM;
	public int peekLoadEV, peekLoadNEV;
	public String facilityId;
	public String facilityActType;
	public String type;
	public boolean evExklusive; //Is now defined in the Pricing Model
	public double coordinateX, coordinateY;
	public LinkedList <ParkingSpot> spots;
	public LinkedList <ParkingSpot> evSpots;
	public LinkedList <ParkingSpot> nevSpots;
	public boolean ocupancyStats = false;
	public LinkedList<Double[]> occupancyList;
///*
	@XmlTransient
	public Coord getCoordinate(){
		Coord coordinate = new Coord(coordinateX, coordinateY);
		return coordinate;
	}//*/
	
	public void setCoordinate(Coord coordinate){
		this.coordinateX=coordinate.getX();
		this.coordinateY=coordinate.getY();
		coordinate = null;
	}
	
	public void createSpots(PricingModels pricingModels){
		this.occupancyEVSpots=0;
		this.occupancyNEVSpots=0;
		spots= new LinkedList<ParkingSpot>();
		evSpots = new LinkedList<ParkingSpot>();
		nevSpots = new LinkedList<ParkingSpot>();
		
		for (int i=0; i<capacityNEV; i++){
			ParkingSpot parkingSpot = new ParkingSpot();
			spots.add(parkingSpot);
			spots.getLast().evExclusive=pricingModels.get_model(parkingPriceMNEVSpot).checkEvExc(); //Falls Preismodell fuer EV und NEV Platz das gleiche
			spots.getLast().charge=false;
			spots.getLast().parkingPriceM=this.parkingPriceMNEVSpot;
			spots.getLast().setOccupied(false);
			spots.getLast().parking=this;
			nevSpots.add(spots.getLast());
		}
	
		for (int i=0; i<capacityEV; i++){
			ParkingSpot parkingSpot = new ParkingSpot();
			spots.add(parkingSpot);
			spots.getLast().charge=true;
			spots.getLast().evExclusive=pricingModels.get_model(parkingPriceMEVSpot).checkEvExc();
			spots.getLast().chargingPriceM=this.chargingPriceM;
			spots.getLast().chargingRate=this.chargingRate;
			spots.getLast().parkingPriceM=this.parkingPriceMEVSpot;
			spots.getLast().setOccupied(false);
			spots.getLast().parking=this;
			evSpots.add(spots.getLast());
		}
	
	}
	
	public void clearSpots(){
		if(spots == null){ return; }
		for(ParkingSpot spot : spots){
			spot.setTimeVehicleParked(1); // !! 

			/*!!
			 * Die Spots wurden teilweise nicht richtig zurueck gesetzt 
			 * dardurch war die Parkzeit noch von der vorherigern Iteration drinnen
			 * dardurch negative Dauer >> Postive Util fuer Parkzeit.
			 * 
			 * Eigentlich sollte Scorekeeper Entfernen und spot = null reichen ...
			 * 
			 * Es muss aber ueberprueft werden ob der Fehler jetzt wirklich ausgeschlossen ist.
			 * 
			 */
			spot = null;
		}
		spots=null;
		evSpots=null;
		nevSpots=null;
	}
	
	//!! Vllt beschleunigen >> Liste mit freien EV / NEV spots fuehren und einfach ersten 
	//zurueck geben; dafuer dann neue funktion zum parken statt parkin in parkcontrol
	//auf occupied zu setzen 
	
	public ParkingSpot checkForFreeSpot(){ //Durchsucht zwar alle Spots, NEV Spots sind jedoch oben in der Liste, kommen daher morgens zuerst drann
		for(ParkingSpot spot : spots){
			//System.out.println("checke spot");
			//System.out.println(spot.evExclusive);
			if (spot.isOccupied() == false && spot.evExclusive == false){  //EV exclusive Spots werden nicht ausgegeben
				return spot;
			}
		}
		return null;
	}
	
	
	
	public ParkingSpot checkForFreeSpotEVPriority(){  //Durchsucht erst EV Spots dann NEV Spots
		if(this.occupancyEVSpots<this.capacityEV){
			for(ParkingSpot spot : evSpots){
				//System.out.println("checke spot");
				if (spot.isOccupied() == false){
					return spot;
				}
			}
		}
		return checkForFreeNEVSpot();
	}
	
	
	public ParkingSpot checkForFreeNEVSpot(){
		if(this.occupancyNEVSpots==this.capacityNEV){
			return null;
		}
		for(ParkingSpot spot : nevSpots){
			//System.out.println("checke spot");
			if (spot.isOccupied() == false){
				return spot;
			}
		}
		return null;
	}
	
	public void parkOnSpot(ParkingSpot parkingSpot, double time){
		parkingSpot.setOccupied(true);
		parkingSpot.setTimeVehicleParked(time);
		if(parkingSpot.charge){
			this.occupancyEVSpots++;
		} else{
			this.occupancyNEVSpots++;
		}
		this.addStatValue(time);
	}
	
	public void leaveSpot(ParkingSpot parkingSpot, double time){
		parkingSpot.setOccupied(false);
		if(parkingSpot.charge){
			this.occupancyEVSpots--;
		} else{
			this.occupancyNEVSpots--;
		}
		this.addStatValue(time);
	}
	
	
	
	public int[] diagnose(){
		int[] counts = new int[2];
		counts[0]=0; //EVs
		counts[1]=0; //NEVs
		for (ParkingSpot spot : spots){
			if(spot.charge){counts[0]++;}
			else{counts[1]++;}
		}
		return counts;
	}

	
	public LinkedList<String> getLinkedList(){
		String facilityId = this.facilityId;
		String facilityActType = this.facilityActType;
		if(this.type.equals("public")){
			if(this.facilityActType==null /* !this.facilityActType.equals("parkingLot") */){
				facilityActType="Street";
			}
			facilityId="Street";
		}
		
		LinkedList<String> list = new LinkedList<String>();
		list.add(Integer.toString(this.id));
		list.add(facilityId);
		list.add(facilityActType);
		list.add(type);
		list.add(Double.toString(coordinateX));
		list.add(Double.toString(coordinateY));
		list.add(Boolean.toString(this.evExklusive));
		list.add(Long.toString(this.capacityEV));
		list.add(Long.toString(this.capacityNEV));
		list.add(Long.toString(this.capacityEV+this.capacityNEV));
		list.add(Double.toString(this.chargingRate));
		return list;
	}

	public void setOcupancyStats(boolean ocupancyStats) {
		this.ocupancyStats = ocupancyStats;
		this.occupancyList = new LinkedList<Double[]>();
	}
	
	private void addStatValue(double time){
		if(this.ocupancyStats){
			this.occupancyList.add(new Double[]{time, this.occupancyEVSpots*1.0/this.capacityEV, this.occupancyNEVSpots*1.0/this.capacityNEV});
		}
	}

	public int getOccupancyEVSpots() {
		return occupancyEVSpots;
	}

	public int getOccupancyNEVSpots() {
		return occupancyNEVSpots;
	}
	
}
