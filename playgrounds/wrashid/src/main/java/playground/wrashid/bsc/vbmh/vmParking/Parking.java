package playground.wrashid.bsc.vbmh.vmParking;

import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;


/**
 * This represents a public / private parking lot. Every parking space is an own parking_spot object.
 * create_spots() creates the specified number of ev / nev parking spots with the specified attributes.
 * check_for_free_spot() checks if one of those spots is available.
 * clear_spots() should be called after each iteration to make sure there are no occupied spots at the begion of 
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
	public double chargingRate;
	public int parkingPriceM, chargingPriceM;
	public String facilityId;
	public String facilityActType;
	public String type;
	public boolean evExklusive;
	public double coordinateX, coordinateY;
	public LinkedList <ParkingSpot> spots;
	public LinkedList <ParkingSpot> evSpots;
	public LinkedList <ParkingSpot> nevSpots;
///*
	@XmlTransient
	public Coord getCoordinate(){
		Coord coordinate = new CoordImpl(coordinateX, coordinateY);
		return coordinate;
	}//*/
	
	public void setCoordinate(Coord coordinate){
		this.coordinateX=coordinate.getX();
		this.coordinateY=coordinate.getY();
		coordinate = null;
	}
	
	public void createSpots(){
		spots= new LinkedList<ParkingSpot>();
		evSpots = new LinkedList<ParkingSpot>();
		nevSpots = new LinkedList<ParkingSpot>();
		
		for (int i=0; i<capacityNEV; i++){
			ParkingSpot parkingSpot = new ParkingSpot();
			spots.add(parkingSpot);
			spots.getLast().evExclusive=false;
			spots.getLast().charge=false;
			spots.getLast().parkingPriceM=this.parkingPriceM;
			spots.getLast().setOccupied(false);
			spots.getLast().parking=this;
			nevSpots.add(spots.getLast());
		}
	
		for (int i=0; i<capacityEV; i++){
			ParkingSpot parkingSpot = new ParkingSpot();
			spots.add(parkingSpot);
			spots.getLast().charge=true;
			spots.getLast().evExclusive=this.evExklusive;
			spots.getLast().chargingPriceM=this.chargingPriceM;
			spots.getLast().chargingRate=this.chargingRate;
			spots.getLast().parkingPriceM=this.parkingPriceM;
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
	
	public ParkingSpot checkForFreeSpot(){ //Durchsucht zwar alle Spots, NEV Spots sind jedoch oben in der Liste, kommen daher morgens zuerst drann
		for(ParkingSpot spot : spots){
			//System.out.println("checke spot");
			if (spot.isOccupied() == false && spot.evExclusive == false){  //EV exclusive Spots werden nicht ausgegeben
				return spot;
			}
		}
		return null;
	}
	
	
	
	public ParkingSpot checkForFreeSpotEVPriority(){  //Durchsucht erst EV Spots dann NEV Spots
		for(ParkingSpot spot : evSpots){
			//System.out.println("checke spot");
			if (spot.isOccupied() == false){
				return spot;
			}
		}
		
		return checkForFreeNEVSpot();
	}
	
	
	public ParkingSpot checkForFreeNEVSpot(){
		for(ParkingSpot spot : nevSpots){
			//System.out.println("checke spot");
			if (spot.isOccupied() == false){
				return spot;
			}
		}
		return null;
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

	
	
	
}
