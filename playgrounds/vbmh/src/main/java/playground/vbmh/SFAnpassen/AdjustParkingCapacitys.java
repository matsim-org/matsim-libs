package playground.vbmh.SFAnpassen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.bind.JAXB;

import playground.vbmh.vmParking.Parking;
import playground.vbmh.vmParking.ParkingMap;
import playground.vbmh.vmParking.ParkingWriter;


public class AdjustParkingCapacitys {

	
	static HashMap <String,Double> pAnteile = new HashMap<String,Double>();	//Anzahl Parkplaetze / what ever nach P typ
	static HashMap <String,Double> evAnteile = new HashMap<String,Double>(); //Anteil EV Nach P Typ
	
	static HashMap <String,Integer> preiseEVSpots = new HashMap<String,Integer>();	//Preismodell EV nach P typ
	static HashMap <String,Integer> preiseNEVSpots = new HashMap<String,Integer>(); //Preismodell NEV Nach P Typ
	
	static HashMap <String,Double> chargingRates = new HashMap<String,Double>(); //Preismodell NEV Nach P Typ

	static HashMap <Integer,Integer> peakLoads = new HashMap<Integer,Integer>();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String parkFileName="input/SF_PLUS/base/parkingLotsIncl.xml";
		String peakLoadFileName="input/SF_PLUS/base/peakLoad.csv";
		String streetFileName="input/SF_PLUS/base/street100p.csv";
		String outputFile = "input/SF_PLUS/base/parking_08pmsl_korrigiert.xml";
		//----
		pAnteile.put("home", 1.0);
		evAnteile.put("home", 1.0);
		preiseEVSpots.put("home", 0);
		preiseNEVSpots.put("home", 0);
		chargingRates.put("home", 2.3);
		
		pAnteile.put("work", 0.8);
		evAnteile.put("work", 0.2);
		preiseEVSpots.put("work", 3);
		preiseNEVSpots.put("work", 4);
		chargingRates.put("work", 3.6);
		
		pAnteile.put("secondary",0.8);
		evAnteile.put("secondary", 0.2);
		preiseEVSpots.put("secondary", 5);
		preiseNEVSpots.put("secondary", 6);
		chargingRates.put("secondary", 8.04);
		
		pAnteile.put("Street", 0.10);
		evAnteile.put("Street", 0.2);
		preiseEVSpots.put("Street", 1);
		preiseNEVSpots.put("Street", 2);
		chargingRates.put("Street", 3.6);
		
		pAnteile.put("edu", 0.8);
		evAnteile.put("edu", 0.2);
		preiseEVSpots.put("edu", 7);
		preiseNEVSpots.put("edu", 7);
		chargingRates.put("edu", 0.0);
		//----
		File file = new File( parkFileName );
		ParkingWriter writer = new ParkingWriter();
		ParkingMap karte = JAXB.unmarshal( file, ParkingMap.class );
		
		karte.initHashMap();
		System.out.println(karte.getPrivateParking("9261_11", "work").toString());
		
		readCSV(peakLoadFileName);
		readStreetCSV(streetFileName);
		
		for (Parking parking : karte.getParkings()){
			String actType;
			if(parking.facilityActType==null){
				actType="Street";
			}else{
				actType = parking.facilityActType;
			}
			if(!actType.equalsIgnoreCase("parkingLot")){
				int peakLoad = peakLoads.get(parking.id);
				double newCapacity = pAnteile.get(actType)*peakLoad;
				int newEVCapacity = (int) Math.round(newCapacity*evAnteile.get(actType));
				int newNEVCapacity = (int) Math.round(newCapacity*(1-evAnteile.get(actType)));
				parking.capacityEV=newEVCapacity;
				parking.capacityNEV=newNEVCapacity;
				parking.parkingPriceMEVSpot=preiseEVSpots.get(actType);
				parking.parkingPriceMNEVSpot=preiseNEVSpots.get(actType);
				parking.chargingRate=chargingRates.get(actType);
			}
		}


		
		
		writer.write(karte, outputFile);
		System.out.println("Fertig");
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

	public static void readStreetCSV(String streetFileName){
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(streetFileName));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String zeile = "";
		
		try {
			zeile = reader.readLine();
			while ((zeile = reader.readLine()) != null) {
				String[] felder = zeile.split(","); //0 11
				
				peakLoads.put(Integer.parseInt(felder[0]), Integer.parseInt(felder[6]));
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //header
		
	}
	
	
}
