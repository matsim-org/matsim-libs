package playground.wrashid.PHEV.parking.data;

import java.util.HashMap;

public class Facility {

	int id=0;
	double x,y;
	int capacity;
	String activityType="parking garage";
	static HashMap<Integer,Facility> hm=new HashMap<Integer,Facility>();
	
	public Facility(int id){
		this.id=id;
		x=0;
		y=0;
		capacity=0;
	}
	
	public static void addStreetParkingData(StreetParkingData parkingData){
		if (!hm.containsKey(parkingData.id)){
			hm.put(parkingData.id, new Facility(parkingData.id));
		}
		Facility facility=hm.get(parkingData.id);
		facility.capacity++;
		facility.x+=parkingData.x;
		facility.y+=parkingData.y;
	}
	
	// compute x and y coordinates through averaging
	public static void finalizeFacilities(){
		for (Facility facility:hm.values()){
			facility.x=facility.x/facility.capacity;
			facility.y=facility.y/facility.capacity;
		}
	}
	
	public static void printFacilities(){
		for (Facility facility:hm.values()){
			System.out.println(facility.id +  " - " + facility.capacity);
		}
		
	}

	public static void writeToXml(String string) {
		// TODO Auto-generated method stub
		
	}
	
}
