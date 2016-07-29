package playground.wrashid.parkingChoice.trb2011;

import java.util.LinkedList;
import java.util.Random;

import herbie.running.controler.HerbieModule;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.meisterk.kti.controler.KTIModule;
import playground.wrashid.parkingChoice.ParkingModule;
import playground.wrashid.parkingChoice.apiDefImpl.ParkingScoringFunctionZhScenario_v1;
import playground.wrashid.parkingChoice.apiDefImpl.PriceAndDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.apiDefImpl.ShortestWalkingDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.FlatParkingFormatReaderV1;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.scoring.ParkingScoreAccumulator;

public class ParkingHerbieControler {

	private final static Logger log = Logger.getLogger(ParkingHerbieControler.class);
	static String parkingDataBase = "H:/data/experiments/TRBAug2011/parkings/flat/";
	static ParkingModule parkingModule;
	public static boolean isRunningOnServer = false;
	public static boolean isKTIMode = true;

	public static void main(String[] args) {
		Controler controler = new Controler(args);
		if (isKTIMode) {
			controler.addOverridingModule(new KTIModule());
		} else {
			controler.addOverridingModule(new HerbieModule());
		}

		parkingModule = new ParkingModule(controler, null);

		prepareParkingsForScenario(controler);

		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		controler.run();

	}

	private static void prepareParkingsForScenario(MatsimServices controler) {
		controler.addControlerListener(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				String isRunningOnServer = event.getServices().getConfig().findParam("parking", "isRunningOnServer");
				if (Boolean.parseBoolean(isRunningOnServer)) {
					parkingDataBase = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/flat/";
					ParkingHerbieControler.isRunningOnServer = true;
				} else {
					parkingDataBase = "H:/data/experiments/TRBAug2011/parkings/flat/";
					ParkingHerbieControler.isRunningOnServer = false;
				}

				LinkedList<PParking> parkingCollection = getParkingsForScenario(event.getServices());
				modifyParkingCollectionIfMainExperimentForTRB2011(event.getServices(), parkingCollection);
				parkingModule.getParkingManager().setParkingCollection(parkingCollection);

				ParkingScoreAccumulator.initializeParkingCounts(event.getServices());

				initParkingSelectionManager(event.getServices());
			}

			private void modifyParkingCollectionIfMainExperimentForTRB2011(MatsimServices controler,
					LinkedList<PParking> parkingCollection) {
					
				Random rand=new Random();
				
				if (controler.getConfig().findParam("parking", "isMainTRB2011Experiment") != null) {
					double percentagePublicParkingsToKeep=Double.parseDouble(controler.getConfig().findParam("parking", "mainExperimentTRB2011.percentagePublicParkingsToKeep"));
					LinkedList<PParking> tmpList = new LinkedList<PParking>();
					log.info("mainExperimentTRB2011 - initNumberOfParkings:" + parkingCollection.size());
					double clusterRadius = 500.0;
					for (PParking parking : parkingCollection) {
						Coord clusterCenter1 = new Coord(684149.1, 246562.3);
						Coord clusterCenter2 = new Coord(682361.4, 248520.4);
						Coord clusterCenter3 = new Coord(681551.2, 247606.6);
						Coord clusterCenter4 = new Coord(683206.5, 247454.3);

						if (!(parking instanceof PrivateParking)) {
							if (rand.nextDouble()<percentagePublicParkingsToKeep/100.0){
								continue;
							}
							
							if (GeneralLib.getDistance(parking.getCoord(), clusterCenter1) < clusterRadius) {
								tmpList.add(parking);
							}
							if (GeneralLib.getDistance(parking.getCoord(), clusterCenter2) < clusterRadius) {
								tmpList.add(parking);
							}
							if (GeneralLib.getDistance(parking.getCoord(), clusterCenter3) < clusterRadius) {
								tmpList.add(parking);
							}
							if (GeneralLib.getDistance(parking.getCoord(), clusterCenter4) < clusterRadius) {
								tmpList.add(parking);
							}
						}

					}
					parkingCollection.removeAll(tmpList);
					log.info("mainExperimentTRB2011 - updatedNumberOfParkings:" + parkingCollection.size());
				}

			}

			private void initParkingSelectionManager(MatsimServices controler) {
				String parkingSelectionManager = controler.getConfig().findParam("parking", "parkingSelectionManager");
				if (parkingSelectionManager.equalsIgnoreCase("shortestWalkingDistance")) {
					parkingModule.setParkingSelectionManager(new ShortestWalkingDistanceParkingSelectionManager(parkingModule
							.getParkingManager()));
				} else if (parkingSelectionManager.equalsIgnoreCase("PriceAndDistance_v1")) {
					parkingModule.setParkingSelectionManager(new PriceAndDistanceParkingSelectionManager(parkingModule
							.getParkingManager(), new ParkingScoringFunctionZhScenario_v1(controler.getConfig())));
					ParkingScoringFunctionZhScenario_v1.disutilityOfWalkingPerMeterShorterThanhresholdDistance = Double
							.parseDouble(controler.getConfig().findParam("parking",
									"disutilityOfWalkingPerMeterShorterThanhresholdDistance"));
					ParkingScoringFunctionZhScenario_v1.disutilityOfWalkingPerMeterLongerThanThresholdDistance = Double
							.parseDouble(controler.getConfig().findParam("parking",
									"disutilityOfWalkingPerMeterLongerThanThresholdDistance"));
					ParkingScoringFunctionZhScenario_v1.thresholdWalkingDistance = Double.parseDouble(controler.getConfig()
							.findParam("parking", "thresholdWalkingDistance"));
					ParkingScoringFunctionZhScenario_v1.boardingDurationInSeconds = Double.parseDouble(controler.getConfig()
							.findParam("parking", "boardingDurationInSeconds"));
					ParkingScoringFunctionZhScenario_v1.streetParkingPricePerSecond = Double.parseDouble(controler.getConfig()
							.findParam("parking", "streetParkingPricePerSecond"));
					ParkingScoringFunctionZhScenario_v1.garageParkingPricePerSecond = Double.parseDouble(controler.getConfig()
							.findParam("parking", "garageParkingPricePerSecond"));

				} else {
					DebugLib.stopSystemAndReportInconsistency("unknown parkingSelectionManager:" + parkingSelectionManager);
				}

			}
		});

	}

	public static LinkedList<PParking> getParkingsForScenario(MatsimServices controler) {
		double parkingsOutsideZHCityScaling = Double.parseDouble(controler.getConfig().findParam("parking",
				"publicParkingsCalibrationFactorOutsideZHCity"));

		LinkedList<PParking> parkingCollection = getParkingCollectionZHCity(controler);
		String streetParkingsFile = null;
		if (isKTIMode) {
			streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0_dilZh30km_10pct.xml";
		} else {
			streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0.xml";
		}

		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile, parkingCollection);

		return parkingCollection;
	}

	public static LinkedList<PParking> getParkingCollectionZHCity(MatsimServices controler) {
		double streetParkingCalibrationFactor = Double.parseDouble(controler.getConfig().findParam("parking",
				"streetParkingCalibrationFactorZHCity"));
		double garageParkingCalibrationFactor = Double.parseDouble(controler.getConfig().findParam("parking",
				"garageParkingCalibrationFactorZHCity"));
		double privateParkingCalibrationFactorZHCity = Double.parseDouble(controler.getConfig().findParam("parking",
				"privateParkingCalibrationFactorZHCity"));
		// double
		// privateParkingsOutdoorCalibrationFactor=Double.parseDouble(services.getConfig().findParam("parking",
		// "privateParkingsOutdoorCalibrationFactorZHCity"));

		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		String streetParkingsFile = parkingDataBase + "streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile, parkingCollection);

		String garageParkingsFile = parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile, parkingCollection);

		String privateIndoorParkingsFile = null;
		if (isKTIMode) {
			privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1_kti.xml";
		} else {
			privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1.xml";
		}

		readParkings(privateParkingCalibrationFactorZHCity, privateIndoorParkingsFile, parkingCollection);

		// String privateOutdoorParkingsFile=parkingDataBase +
		// "privateParkingsOutdoor.xml";
		// readParkings(privateParkingsOutdoorCalibrationFactor,
		// privateOutdoorParkingsFile,parkingCollection);

		return parkingCollection;
	}

	public static LinkedList<PParking> getParkingCollectionZHCity() {
		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		String streetParkingsFile = parkingDataBase + "streetParkings.xml";
		readParkings(1.0, streetParkingsFile, parkingCollection);

		String garageParkingsFile = parkingDataBase + "garageParkings.xml";
		readParkings(1.0, garageParkingsFile, parkingCollection);

		String privateIndoorParkingsFile = parkingDataBase + "privateParkingsIndoor_v0.xml";
		readParkings(1.0, privateIndoorParkingsFile, parkingCollection);

		String privateOutdoorParkingsFile = parkingDataBase + "privateParkingsOutdoor_v0.xml";
		readParkings(1.0, privateOutdoorParkingsFile, parkingCollection);

		return parkingCollection;
	}

	public static void readParkings(double parkingCalibrationFactor, String parkingsFile, LinkedList<PParking> parkingCollection) {
		FlatParkingFormatReaderV1 flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		flatParkingFormatReaderV1.readFile(parkingsFile);

		LinkedList<PParking> parkings = flatParkingFormatReaderV1.getParkings();
		calibarteParkings(parkings, parkingCalibrationFactor);

		parkingCollection.addAll(parkings);
	}

	private static void calibarteParkings(LinkedList<PParking> parkingCollection, double calibrationFactor) {
		LinkedList<PParking> emptyParking=new LinkedList<PParking>();
		
		for (PParking parking : parkingCollection) {
			double capacity = parking.getCapacity();
			parking.setCapacity(capacity * calibrationFactor);
			
			if (parking.getCapacity() <0.5){
				emptyParking.add(parking);
			} else if (parking.getCapacity() <1){
				parking.setCapacity(1.0);
			}
		}
		
		parkingCollection.removeAll(emptyParking);
		
	}

	public static Coord getCoordinatesQuaiBridgeZH() {
		return new Coord(683423.0, 246819.0);
	}

	public static Coord getCoordinatesLindenhofZH() {
		return new Coord(683235.0, 247497.0);
	}

}
