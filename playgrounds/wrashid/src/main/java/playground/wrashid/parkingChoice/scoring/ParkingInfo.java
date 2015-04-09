package playground.wrashid.parkingChoice.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;


public class ParkingInfo {

	private Id<PParking> parkingId;
	private double arrivalTime;
	private double departureTime;

	public Id<PParking> getParkingId() {
		return parkingId;
	}
	public double getArrivalTime() {
		return arrivalTime;
	}
	public double getDepartureTime() {
		return departureTime;
	}
	public ParkingInfo( Id<PParking> parkingId, double arrivalTime, double departureTime) {
		super();
		this.parkingId = parkingId;
		this.arrivalTime = GeneralLib.projectTimeWithin24Hours(arrivalTime);
		this.departureTime = GeneralLib.projectTimeWithin24Hours(departureTime);
	}
	
	public static LinkedListValueHashMap<Id<Person>, ParkingInfo> readParkingInfo(String fileName){
		LinkedListValueHashMap<Id<Person>, ParkingInfo> container=new LinkedListValueHashMap<>();
		
		Matrix matrix = GeneralLib.readStringMatrix(fileName,"\t");
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			Id<Person> personId=Id.create(matrix.getString(i, 0), Person.class);
			Id<PParking> parkingId=Id.create(matrix.getString(i, 1), PParking.class);
			double arrivalTime=matrix.getDouble(i, 2);
			double departureTime=matrix.getDouble(i, 3);
			container.put(personId,new ParkingInfo(parkingId, arrivalTime, departureTime));
		}
		
		return container;
	}

	
}
