package playground.wrashid.parkingSearch.planLevel.analysis;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingCapacity;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

public class ParkingOccupancyAnalysisWriter {

	private final HashMap<Id, ParkingOccupancyBins> parkingOccupancyBins;
	private final ParkingCapacity parkingCapacity;

	public ParkingOccupancyAnalysisWriter(HashMap<Id, ParkingOccupancyBins> parkingOccupancyBins, ParkingCapacity parkingCapacity) {
		super();
		this.parkingOccupancyBins = parkingOccupancyBins;
		this.parkingCapacity = parkingCapacity;
	}
	
	public void write(String filePath){
		int numberOfParkings=parkingCapacity.getNumberOfParkings();
		double[][] matrix=new double[numberOfParkings][98];
		String headerLine = "parkingFacilityId" + "\t" + "parkingCapacity" + "\t";

		for (int i = 0; i < 96; i++) {
			headerLine +=  i*15/60.0 + "h-slot\t";
		}
		
		ArrayList<ActivityFacilityImpl> parkingFacilities = parkingCapacity.getParkingFacilities();
		
		// fill matrix data
		for (int i=0;i<parkingFacilities.size();i++){
			Id parkingFacilityId=parkingFacilities.get(i).getId();
			matrix[i][0]=Integer.parseInt(parkingFacilityId.toString());
			matrix[i][1]=parkingCapacity.getParkingCapacity(parkingFacilities.get(i).getId());
			
			for (int j = 0; j < 96; j++) {
				if (parkingOccupancyBins.get(parkingFacilityId)!=null){
					
					
					// instead of just writing the occupancy, the unused capacity is written out
					// therefore a minus number means a capacity violation.
					matrix[i][j+2]=matrix[i][1]-parkingOccupancyBins.get(parkingFacilityId).getOccupancy()[j];
				} else {
					// null means, that parking facility was not used during whole simulation
					matrix[i][j+2]=matrix[i][1];
				}
			}
		}
		
		GeneralLib.writeMatrix(matrix, filePath, headerLine);
	}

}
