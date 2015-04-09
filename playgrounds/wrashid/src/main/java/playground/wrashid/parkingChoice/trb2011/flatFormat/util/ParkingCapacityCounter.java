package playground.wrashid.parkingChoice.trb2011.flatFormat.util;

import java.util.LinkedList;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class ParkingCapacityCounter {

	public static void main(String[] args) {
		LinkedList<PParking> parkingCollection=new LinkedList<PParking>();
		String baseFolder="C:/data/My Dropbox/ETH/static data/parking/zürich city/flat/";
		ParkingHerbieControler.readParkings(1.0, baseFolder + "privateParkings_v1_kti.xml", parkingCollection);
	
		double totalCapacity=0;
		for (PParking parking:parkingCollection){
			totalCapacity+=parking.getCapacity();
		}
		
		System.out.println("totalCapacity:" + totalCapacity);
		
	}
	
}
