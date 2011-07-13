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
		
		LinkedList<Parking> parkingCollection = getParkingCollection();
		
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

	private static LinkedList<Parking> getParkingCollection() {
		double streetParkingCalibrationFactor=1.0;
		double garageParkingCalibrationFactor=1.0;
		
		FlatParkingFormatReaderV1 flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		flatParkingFormatReaderV1.parse("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/streetParkings.xml");
		
		LinkedList<Parking> streetParkings= flatParkingFormatReaderV1.getParkings();
		calibarteParkings(streetParkings,streetParkingCalibrationFactor);
		
		
		flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		flatParkingFormatReaderV1.parse("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/garageParkings.xml");
		
		LinkedList<Parking> garageParkings= flatParkingFormatReaderV1.getParkings();
		calibarteParkings(garageParkings,garageParkingCalibrationFactor);
		
		LinkedList<Parking> parkingCollection = streetParkings;
		parkingCollection.addAll(garageParkings);
		return parkingCollection;
	}
	
	private static void calibarteParkings(LinkedList<Parking> parkingCollection, double calibrationFactor){
		for (Parking parking:parkingCollection){
			int capacity = parking.getCapacity();
			parking.setCapacity((int) Math.round(capacity*calibrationFactor));
		}
		
	}
	
}
