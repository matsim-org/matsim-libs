package playground.wrashid.artemis.parkingPostprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.geotools.math.Statistics;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.MathLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.scoring.ParkingInfo;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class AssignParkingLinkIds {

	private static String parkingLogInfoFileName;
	// key: personId
	private static LinkedListValueHashMap<Id, ParkingInfo> parkingInfo;
	private static NetworkImpl network;
	private static HashMap<Id, Parking> parkings;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		parkingLogInfoFileName = "H:/data/experiments/TRBAug2011/runs/ktiRun45/output/ITERS/it.50/50.parkingLogInfo.txt";
		parkingInfo = ParkingInfo.readParkingInfo(parkingLogInfoFileName);

		network = GeneralLib.readNetwork("H:/data/experiments/TRBAug2011/runs/ktiRun45/output/output_network.xml.gz");

		parkings = readParkings();

		String parkingTimesInputFileName = "H:/data/experiments/ARTEMIS/nov2011/szenario28/output/parkingTimesAndLegEnergyConsumption.txt";
		String parkingTimesOutputFileName = "H:/data/experiments/ARTEMIS/nov2011/szenario28/output/parkingTimesAndLegEnergyConsumption_o.txt";
		replaceLinkIdsInParkingTimesLog(parkingTimesInputFileName, parkingTimesOutputFileName);

		String chargingLogInputFileName = "H:/data/experiments/ARTEMIS/nov2011/szenario28/output/chargingLog.txt";
		String chargingLogOutputFileName = "H:/data/experiments/ARTEMIS/nov2011/szenario28/output/chargingLog_o.txt";
		replaceLinkIdsInLogChargigLong(chargingLogInputFileName, chargingLogOutputFileName);

	}

	private static void replaceLinkIdsInLogChargigLong(String chargingLogInputFileName, String chargingLogOutputFileName) {
		StringMatrix chargingLog = GeneralLib.readStringMatrix(chargingLogInputFileName, "\t");

		for (int i = 1; i < chargingLog.getNumberOfRows(); i++) {
			Id personId = new IdImpl(chargingLog.getString(i, 1));
			double chargingStartTime = chargingLog.getDouble(i, 2);

			LinkedList<ParkingInfo> parkingInf = parkingInfo.get(personId);

			double minDifference = Double.MAX_VALUE;
			ParkingInfo selectedParking = null;
			for (ParkingInfo pInfo : parkingInf) {
				double currentDistance = Math.abs(pInfo.getArrivalTime()- chargingStartTime);
				if (currentDistance < minDifference) {
					minDifference = currentDistance;
					selectedParking = pInfo;
				}
			}

			chargingLog.setString(i, 0, getClosestLinkFromParking(parkings.get(selectedParking.getParkingId())).toString());

			if (minDifference > 5000) {
				//DebugLib.stopSystemAndReportInconsistency();
			}

		}

		chargingLog.writeMatrix(chargingLogOutputFileName);
	}

	private static void replaceLinkIdsInParkingTimesLog(String parkingTimesInputFileName, String parkingTimesOutputFileName) {
		StringMatrix parkingTimes = GeneralLib.readStringMatrix(parkingTimesInputFileName, "\t");

		Statistics statisticsOnDistanceBetweenParkingAndActivity=new Statistics();
		
		for (int i = 1; i < parkingTimes.getNumberOfRows(); i++) {
			Id personId = new IdImpl(parkingTimes.getString(i, 0));
			double arrivalTime = parkingTimes.getDouble(i, 1);
			Id activityLinkId =new IdImpl(parkingTimes.getString(i, 3));
			
			LinkedList<ParkingInfo> parkingInf = parkingInfo.get(personId);

			double minDifference = Double.MAX_VALUE;
			ParkingInfo selectedParking = null;
			for (ParkingInfo pInfo : parkingInf) {
				double currentDistance = Math.abs(pInfo.getArrivalTime()- arrivalTime);
				if (currentDistance < minDifference) {
					minDifference = currentDistance;
					selectedParking = pInfo;
				}
			}

			Id closestLinkFromParking = getClosestLinkFromParking(parkings.get(selectedParking.getParkingId()));
			parkingTimes.setString(i, 3, closestLinkFromParking.toString());

			statisticsOnDistanceBetweenParkingAndActivity.add(GeneralLib.getDistance(network.getLinks().get(closestLinkFromParking).getCoord(), network.getLinks().get(activityLinkId).getCoord()));
			
			if (minDifference > 5000) {
				//DebugLib.stopSystemAndReportInconsistency();
			}

		}

		parkingTimes.writeMatrix(parkingTimesOutputFileName);
		
		System.out.println(statisticsOnDistanceBetweenParkingAndActivity.toString());
		
	}

	private static Id getClosestLinkFromParking(Parking parking) {
		return network.getNearestLink(parking.getCoord()).getId();
	}

	private static HashMap<Id, Parking> readParkings() {
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();
		String parkingDataBase = "H:/data/experiments/TRBAug2011/parkings/flat/";
		ParkingHerbieControler.readParkings(1.0, parkingDataBase + "privateParkings_v1_kti.xml", parkingCollection);
		ParkingHerbieControler.readParkings(1.0, parkingDataBase + "streetParkings.xml", parkingCollection);
		ParkingHerbieControler.readParkings(1.0, parkingDataBase + "garageParkings.xml", parkingCollection);
		ParkingHerbieControler.readParkings(1.0, parkingDataBase + "publicParkingsOutsideZHCity_v0_dilZh30km_10pct.xml",
				parkingCollection);

		HashMap<Id, Parking> parkingHashmap = new HashMap<Id, Parking>();

		for (Parking parking : parkingCollection) {
			parkingHashmap.put(parking.getId(), parking);
		}

		return parkingHashmap;
	}

	private static void makeChecks() {
		DebugLib.stopSystemAndReportInconsistency();

	}

	private static void writeChargigLongWithNewLinks() {
		// TODO Auto-generated method stub

	}

	private static void writeParkingTimesWithNewLinks() {
		// TODO Auto-generated method stub

	}

	private static void readParkingTimes() {
		// TODO Auto-generated method stub

	}

	private static void readChargingIds() {
		// TODO Auto-generated method stub

	}

	private static void assignLinkIdsToParkingIds() {
		// TODO Auto-generated method stub

	}

}
