package playground.wrashid.parkingChoice.trb2011;

import java.util.LinkedList;

import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.parkingChoice.ParkingModule;
import playground.wrashid.parkingChoice.infrastructure.FlatParkingFormatReaderV1;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import herbie.running.controler.HerbieControler;

public class ParkingHerbieControler {

	public static void main(String[] args) {
		
		HerbieControler hControler=new HerbieControler(args);
		
		LinkedList<Parking> parkingCollection = getParkingCollectionZHCity();
		
		/*
		LinkedList<Parking> parkingCollection= new LinkedList<Parking>();
		
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(30000);
				parkingCollection.add(parking);
			}
		}*/
		
		
		ParkingModule parkingModule=new ParkingModule(hControler, parkingCollection);
		
//		hControler.setOverwriteFiles(true);

		hControler.run();
		
	}
	
	public static LinkedList<Parking> getParkingsForScenario() {
		double parkingsOutsideZHCityScaling=1.0;
		
		LinkedList<Parking> parkingCollection=getParkingCollectionZHCity();
		
		String streetParkingsFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/garageParkingsOutsideZHCity.xml";
		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile,parkingCollection);
		
		return parkingCollection;
	}

	public static LinkedList<Parking> getParkingCollectionZHCity() {
		double streetParkingCalibrationFactor=1.0;
		double garageParkingCalibrationFactor=1.0;
		double privateParkingsIndoorCalibrationFactor=1.0;
		double privateParkingsOutdoorCalibrationFactor=1.0;
		
		LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		
		String streetParkingsFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile,parkingCollection);
		
		String garageParkingsFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile,parkingCollection);
		
		String privateIndoorParkingsFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/privateParkingsIndoor.xml";
		readParkings(privateParkingsIndoorCalibrationFactor, privateIndoorParkingsFile,parkingCollection);
		
		String privateOutdoorParkingsFile="C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/privateParkingsOutdoor.xml";
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
