package playground.wrashid.bsc.vbmh.vm_parking;

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
	public int chargingRate, parkingPriceM, chargingPriceM;
	public String facilityId;
	public String type;
	public boolean evExklusive;
	public double coordinateX, coordinateY;
	public LinkedList <ParkingSpot> spots;
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
		for (int i=0; i<capacityEV; i++){
			ParkingSpot parkingSpot = new ParkingSpot();
			spots.add(parkingSpot);
			spots.getLast().charge=true;
			spots.getLast().chargingPriceM=this.chargingPriceM;
			spots.getLast().chargingRate=this.chargingRate;
			spots.getLast().parkingPriceM=this.parkingPriceM;
			spots.getLast().setOccupied(false);
			spots.getLast().parking=this;
		}
		for (int i=0; i<capacityNEV; i++){
			ParkingSpot parkingSpot = new ParkingSpot();
			spots.add(parkingSpot);
			spots.getLast().charge=false;
			spots.getLast().parkingPriceM=this.parkingPriceM;
			spots.getLast().setOccupied(false);
			spots.getLast().parking=this;
		}
	}
	
	public void clearSpots(){
		spots=null;
	}
	
	public ParkingSpot checkForFreeSpot(){
		for(ParkingSpot spot : spots){
			//System.out.println("checke spot");
			if (spot.isOccupied() == false){
				return spot;
			}
		}
		return null;
	}

	
	
	
}
