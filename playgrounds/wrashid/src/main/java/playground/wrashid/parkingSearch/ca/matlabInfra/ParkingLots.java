package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class ParkingLots {

	public static void main(String[] args) {
		ArrayList<String> outputList = new ArrayList<String>();

		outputList.add("<parkingLots>");

		collectStreetParkings(outputList);

		collectGarageParkings(outputList);

		outputList.add("</parkingLots>");

		GeneralLib.writeList(outputList, Config.getOutputFolder() + "parkingLots.xml");

	}

	private static void collectGarageParkings(ArrayList<String> outputList) {
		String sourcePath = "ETH/static data/parking/zürich city/Parkhaeuser/parkhäuser.txt";
		StringMatrix garageParkingData = GeneralLib.readStringMatrix("c:/data/My Dropbox/" + sourcePath);

		for (int i = 1; i < garageParkingData.getNumberOfRows(); i++) {
			outputList.add(getParkingString("gp-" + i, garageParkingData.getDouble(i, 0), garageParkingData.getDouble(i, 1),
					Integer.parseInt(garageParkingData.getString(i, 2))));
		}
	}

	private static void collectStreetParkings(ArrayList<String> outputList) {
		String streetParkings = "C:/data/My Dropbox/ETH/static data/parking/zürich city/Strassenparkplaetze/street parkings old - 2007 - aufbereitet/streetpark_facilities.xml";
		ActivityFacilitiesImpl streetParkingFacilities = GeneralLib.readActivityFacilities(streetParkings);

		for (Id facilityId : streetParkingFacilities.getFacilities().keySet()) {
			ActivityFacilityImpl facilityImpl = (ActivityFacilityImpl) streetParkingFacilities.getFacilities().get(facilityId);

			Map<String, ActivityOption> activityOptions = facilityImpl.getActivityOptions();

			outputList.add(getParkingString("sp-" + facilityId.toString(), facilityImpl.getCoord().getX(), facilityImpl
					.getCoord().getY(), Math.round(activityOptions.get("parking").getCapacity())));
		}
	}

	private static String getParkingString(String id, double x, double y, double capacity) {
		StringBuffer stringBuffer = new StringBuffer();

		stringBuffer.append("\t<parkingLot>\n");

		stringBuffer.append("\t\t<id>" + id + "</id>\n");
		stringBuffer.append("\t\t<x>" + x + "</x>\n");
		stringBuffer.append("\t\t<y>" + y + "</y>\n");
		stringBuffer.append("\t\t<size>" + capacity + "</size>\n");

		stringBuffer.append("\t<parkingLot>\n");

		return stringBuffer.toString();
	}

}
