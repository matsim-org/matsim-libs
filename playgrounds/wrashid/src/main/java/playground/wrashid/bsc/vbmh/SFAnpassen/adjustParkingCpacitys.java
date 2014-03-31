package playground.wrashid.bsc.vbmh.SFAnpassen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.bind.JAXB;

import playground.wrashid.bsc.vbmh.vmParking.Parking;
import playground.wrashid.bsc.vbmh.vmParking.ParkingMap;
import playground.wrashid.bsc.vbmh.vmParking.ParkingWriter;


public class adjustParkingCpacitys {

	
	static HashMap <String,Double> pAnteile = new HashMap<String,Double>();	//Anzahl Parkplaetze / what ever nach P typ
	static HashMap <String,Double> evAnteile = new HashMap<String,Double>(); //Anteil EV Nach P Typ
	
	static HashMap <String,Integer> preiseEVSpots = new HashMap<String,Integer>();	//Preismodell EV nach P typ
	static HashMap <String,Integer> preiseNEVSpots = new HashMap<String,Integer>(); //Preismodell NEV Nach P Typ
	
	static HashMap <String,Double> chargingRates = new HashMap<String,Double>(); //Preismodell NEV Nach P Typ

	static HashMap <Integer,Integer> peakLoads = new HashMap<Integer,Integer>();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String parkFileName="";
		String peakLoadFileName="";
		String outputFile = "";
		//----
		pAnteile.put("home", 50.0);
		evAnteile.put("home", 0.5);
//		preiseEVSpots.put("home", 0);
//		preiseNEVSpots.put("home", 0);
//		chargingRates.put("home", 2.3);
		
		pAnteile.put("work", 100.0);
		evAnteile.put("work", 0.50);
		preiseEVSpots.put("work", 3);
//		preiseNEVSpots.put("work", 4);
//		chargingRates.put("work", 3.6);
		
		pAnteile.put("secondary",100.0);
		evAnteile.put("secondary", 0.5);
//		preiseEVSpots.put("secondary", 5);
//		preiseNEVSpots.put("secondary", 6);
//		chargingRates.put("secondary", 8.04);
		
		pAnteile.put("Street", 0.0);
		evAnteile.put("Street", 0.30);
//		preiseEVSpots.put("Street", 1);
//		preiseNEVSpots.put("Street", 2);
//		chargingRates.put("Street", 3.6);
		
		pAnteile.put("edu", 100.00);
		evAnteile.put("edu", 0.5);
//		preiseEVSpots.put("edu", 7);
//		preiseNEVSpots.put("edu", 7);
//		chargingRates.put("edu", 0.0);
		//----
		File file = new File( parkFileName );
		ParkingWriter writer = new ParkingWriter();
		ParkingMap karte = JAXB.unmarshal( file, ParkingMap.class );
		
		readCSV(peakLoadFileName);
		
		for (Parking parking : karte.getParkings()){
			int peakLoad = peakLoads.get(parking.id);
			double newCapacity = pAnteile.get(parking.facilityActType)*peakLoad;
			int newEVCapacity = (int) Math.round(newCapacity*evAnteile.get(parking.facilityActType));
			int newNEVCapacity = (int) Math.round(newCapacity*(1-evAnteile.get(parking.facilityActType)));
			parking.capacityEV=newEVCapacity;
			parking.capacityNEV=newNEVCapacity;
		}
		
		
		
		
		writer.write(karte, outputFile);
	}

	public static void readCSV(String peakLoadFileName){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(peakLoadFileName));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String zeile = "";
		
		try {
			zeile = reader.readLine();
			while ((zeile = reader.readLine()) != null) {
				String[] felder = zeile.split(","); //0 11
				
				peakLoads.put(Integer.parseInt(felder[0]), Integer.parseInt(felder[11]));
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //header
		
	}
	
	
	
}
