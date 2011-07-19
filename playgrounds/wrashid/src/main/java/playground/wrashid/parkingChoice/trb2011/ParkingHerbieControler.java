package playground.wrashid.parkingChoice.trb2011;

import herbie.running.controler.HerbieControler;

import java.util.LinkedList;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.parkingChoice.ParkingModule;
import playground.wrashid.parkingChoice.infrastructure.FlatParkingFormatReaderV1;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ParkingHerbieControler {

	static String parkingDataBase=null;
	static ParkingModule parkingModule;
	
	public static void main(String[] args) {
		
		HerbieControler hControler=new HerbieControler(args);
		
		
		
		parkingModule=new ParkingModule(hControler, null);
		
		prepareParkingsForScenario(hControler);
		
//		hControler.setOverwriteFiles(true);

		hControler.run();
		
	}
	
	
	
	private static void prepareParkingsForScenario(HerbieControler hControler) {
		hControler.addControlerListener(new StartupListener() {
			
			@Override
			public void notifyStartup(StartupEvent event) {
				String isRunningOnServer = event.getControler().getConfig().findParam("parking", "isRunningOnServer");
				if (Boolean.parseBoolean(isRunningOnServer)){
					parkingDataBase="/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/flat/";
				} else {
					parkingDataBase="H:/data/experiments/TRBAug2011/parkings/flat/";
				}
				
				LinkedList<Parking> parkingCollection = getParkingsForScenario(event.getControler());
				parkingModule.getParkingManager().setParkingCollection(parkingCollection);
			}
		});
		
	}



	public static LinkedList<Parking> getParkingsForScenario(Controler controler) {
		double parkingsOutsideZHCityScaling=Double.parseDouble(controler.getConfig().findParam("parking", "publicParkingsCalibrationFactorOutsideZHCity"));
		
		LinkedList<Parking> parkingCollection=getParkingCollectionZHCity(controler);
		
		String streetParkingsFile=parkingDataBase + "publicParkingsOutsideZHCity.xml";
		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile,parkingCollection);
		
		return parkingCollection;
	}

	public static LinkedList<Parking> getParkingCollectionZHCity(Controler controler) {
		double streetParkingCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "streetParkingCalibrationFactorZHCity"));
		double garageParkingCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "garageParkingCalibrationFactorZHCity"));
		double privateParkingsIndoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "privateParkingsIndoorCalibrationFactorZHCity"));
		double privateParkingsOutdoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking", "privateParkingsOutdoorCalibrationFactorZHCity"));
		
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
	
	public static LinkedList<Parking> getParkingCollectionZHCity(){
		LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		
		String streetParkingsFile=parkingDataBase + "streetParkings.xml";
		readParkings(1.0, streetParkingsFile,parkingCollection);
		
		String garageParkingsFile=parkingDataBase + "garageParkings.xml";
		readParkings(1.0, garageParkingsFile,parkingCollection);
		
		String privateIndoorParkingsFile=parkingDataBase + "privateParkingsIndoor.xml";
		readParkings(1.0, privateIndoorParkingsFile,parkingCollection);
		
		String privateOutdoorParkingsFile=parkingDataBase + "privateParkingsOutdoor.xml";
		readParkings(1.0, privateOutdoorParkingsFile,parkingCollection);
		
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
			double capacity = parking.getCapacity();
			parking.setCapacity(capacity*calibrationFactor);
		}
	}
	
}
