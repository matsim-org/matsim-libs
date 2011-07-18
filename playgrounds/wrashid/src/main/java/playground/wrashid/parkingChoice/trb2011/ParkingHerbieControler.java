package playground.wrashid.parkingChoice.trb2011;

import herbie.running.controler.HerbieControler;

import java.util.LinkedList;

import playground.wrashid.parkingChoice.ParkingModule;
import playground.wrashid.parkingChoice.infrastructure.FlatParkingFormatReaderV1;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ParkingHerbieControler {

	static String parkingDataBase=null;
	
	public static void main(String[] args) {
		
		HerbieControler hControler=new HerbieControler(args);
		
		String isRunningOnServer = args[1];
		if (Boolean.parseBoolean(isRunningOnServer)){
			parkingDataBase="/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/flat/";
		} else {
			parkingDataBase="H:/data/experiments/TRBAug2011/parkings/flat/";
		}
		
		
		LinkedList<Parking> parkingCollection = getParkingsForScenario();
		
		
		
		
		
		ParkingModule parkingModule=new ParkingModule(hControler, parkingCollection);
		
//		hControler.setOverwriteFiles(true);

		hControler.run();
		
	}
	
	public static LinkedList<Parking> getParkingsForScenario() {
		double parkingsOutsideZHCityScaling=1.0;
		
		LinkedList<Parking> parkingCollection=getParkingCollectionZHCity();
		
		String streetParkingsFile=parkingDataBase + "publicParkingsOutsideZHCity.xml";
		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile,parkingCollection);
		
		return parkingCollection;
	}

	public static LinkedList<Parking> getParkingCollectionZHCity() {
		double streetParkingCalibrationFactor=1.0;
		double garageParkingCalibrationFactor=1.0;
		double privateParkingsIndoorCalibrationFactor=1.0;
		double privateParkingsOutdoorCalibrationFactor=1.0;
		
		LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		
		String streetParkingsFile=parkingDataBase + "streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile,parkingCollection);
		
		String garageParkingsFile=parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile,parkingCollection);
		
		String privateIndoorParkingsFile=parkingDataBase + "privateParkingsIndoor.xml";
		readParkings(privateParkingsIndoorCalibrationFactor, privateIndoorParkingsFile,parkingCollection);
		
		String privateOutdoorParkingsFile=parkingDataBase + "privateParkingsOutdoor.xml";
		readParkings(privateParkingsOutdoorCalibrationFactor, privateOutdoorParkingsFile,parkingCollection);
		
		return parkingCollection;
	}

	private static void readParkings(double streetParkingCalibrationFactor, String streetParkingsFile, LinkedList<Parking> parkingCollection) {
		FlatParkingFormatReaderV1 flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		flatParkingFormatReaderV1.parse(streetParkingsFile);
		
		LinkedList<Parking> parkings= flatParkingFormatReaderV1.getParkings();
		calibarteParkings(parkings,streetParkingCalibrationFactor);
		
		parkingCollection.addAll(parkings);
	}
	
	private static void calibarteParkings(LinkedList<Parking> parkingCollection, double calibrationFactor){
		for (Parking parking:parkingCollection){
			int capacity = parking.getCapacity();
			parking.setCapacity((int) Math.round(capacity*calibrationFactor));
		}
	}
	
}
