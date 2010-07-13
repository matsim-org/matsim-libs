package playground.wrashid.parkingSearch.planLevel.init;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingCapacity;
import playground.wrashid.parkingSearch.planLevel.ranking.ClosestParkingMatrix;

public class ParkingRoot {

	private static ClosestParkingMatrix cpm = null;
	private static LinkParkingFacilityAssociation lpfa = null;
	private static ParkingCapacity pc = null;

	public static void init(ActivityFacilitiesImpl facilities, NetworkLayer network) {
		cpm = new ClosestParkingMatrix(facilities, network);
		lpfa = new LinkParkingFacilityAssociation(facilities, network);
		pc= new ParkingCapacity(facilities);
	}
	
	public static ClosestParkingMatrix getClosestParkingMatrix(){
		checkIfNull(cpm);
		return cpm;
	}
	
	public static LinkParkingFacilityAssociation getLinkParkingFacilityAssociation(){
		checkIfNull(lpfa);
		return lpfa;
	}	

	public static ParkingCapacity getParkingCapacity(){
		checkIfNull(pc);
		return pc;
	}
	
	private static void checkIfNull(Object obj){
		if (obj==null){
			throw new Error("Please initialize the variables first.");
		}
		
	}
	
}
