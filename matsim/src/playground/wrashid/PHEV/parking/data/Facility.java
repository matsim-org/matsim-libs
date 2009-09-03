package playground.wrashid.PHEV.parking.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Facility {

	int id=0;
	double x,y;
	int capacity;
	static HashMap<Integer,Facility> hm=new HashMap<Integer,Facility>();
	String typeLabel=null;
	String openingTimes=null;
	
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
		facility.typeLabel="parking";
	}
	
	public static void addGarageParkingData(GarageParkingData parkingData){
		if (!hm.containsKey(parkingData.id)){
			hm.put(parkingData.id, new Facility(parkingData.id));
		}
		Facility facility=hm.get(parkingData.id);
		facility.capacity=parkingData.capacity;
		facility.x+=parkingData.x;
		facility.y+=parkingData.y;
		facility.typeLabel="parking";
	}
	
	public static void addOpeningTimes(int id, String openingTimes){
		Facility facility=hm.get(id);
		openingTimes="\t\t\t" + openingTimes + "\n";
		openingTimes = openingTimes.replaceAll("><", ">\n\t\t\t<");
		facility.openingTimes=openingTimes;
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

	public static void writeToXml(String path) {
		FileWriter fw;
		try {
			fw = new FileWriter(path);
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			bw.write("<!DOCTYPE facilities SYSTEM \"http://www.matsim.org/files/dtd/facilities_v1.dtd\">\n");
			bw.write("\n");
			bw.write("<facilities name=\"parking facilities\">\n");
			bw.write("\n");
			
			for (Facility facility:hm.values()){
				bw.write("<!-- ====================================================================== -->\n\n");
				bw.write("\t<facility id=\""+ facility.id +"\" x=\""+ facility.x +"\" y=\""+ facility.y +"\">\n");
				bw.write("\t\t<activity type=\""+ facility.typeLabel +"\">\n");
				bw.write("\t\t\t<capacity value=\""+ facility.capacity +"\" />\n");
				if (facility.openingTimes!=null){bw.write(facility.openingTimes);}
				bw.write("\t\t</activity>\n");
				bw.write("\t</facility>\n\n");
			}
			
			bw.write("<!-- ====================================================================== -->\n\n");
			bw.write("</facilities>");
			
			
			bw.close();
			
			System.out.println(hm.size() + " street parkings facilities written out to xml");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		
		
		
	}
	
}
