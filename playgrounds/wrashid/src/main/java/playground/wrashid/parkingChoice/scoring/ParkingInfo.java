package playground.wrashid.parkingChoice.scoring;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class ParkingInfo {

	private Id parkingId;
	private double arrivalTime;
	private double departureTime;

	public Id getParkingId() {
		return parkingId;
	}
	public double getArrivalTime() {
		return arrivalTime;
	}
	public double getDepartureTime() {
		return departureTime;
	}
	public ParkingInfo( Id parkingId, double arrivalTime, double departureTime) {
		super();
		this.parkingId = parkingId;
		this.arrivalTime = GeneralLib.projectTimeWithin24Hours(arrivalTime);
		this.departureTime = GeneralLib.projectTimeWithin24Hours(departureTime);
	}
	
	public static LinkedListValueHashMap<Id, ParkingInfo> readParkingInfo(String fileName){
		LinkedListValueHashMap<Id, ParkingInfo> container=new LinkedListValueHashMap<Id, ParkingInfo>();
		
		Matrix matrix = GeneralLib.readStringMatrix(fileName,"\t");
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			Id personId=new IdImpl(matrix.getString(i, 0));
			Id parkingId=new IdImpl(matrix.getString(i, 1));
			double arrivalTime=matrix.getDouble(i, 2);
			double departureTime=matrix.getDouble(i, 3);
			container.put(personId,new ParkingInfo(parkingId, arrivalTime, departureTime));
		}
		
		return container;
	}

	
}
