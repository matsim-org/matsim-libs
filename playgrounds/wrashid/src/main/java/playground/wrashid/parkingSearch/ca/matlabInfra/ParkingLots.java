package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;


public class ParkingLots {

	public static void main(String[] args) {
		ArrayList<String> outputList = new ArrayList<String>();
		outputList.add("<parkinglots>");
		collectStreetParkings(outputList);
		collectGarageParkings(outputList);
		outputList.add("</parkinglots>");
		GeneralLib.writeList(outputList, Config.getOutputFolder() + "parkingLots.xml");
	}

	private static void collectGarageParkings(ArrayList<String> outputList) {
		Matrix garageParkingData = GeneralLib.readStringMatrix(Config.baseFolder + "../parkhäuser.txt");

		int totalCapacity = 0;
		for (int i = 1; i < garageParkingData.getNumberOfRows(); i++) {
			double x = garageParkingData.getDouble(i, 0);
			double y = garageParkingData.getDouble(i, 1);

			if (Config.isInsideStudyArea(x, y)) {
				int capacity = Integer.parseInt(garageParkingData.getString(i, 2));
				totalCapacity += capacity;
				outputList.add(getParkingString("gp-" + i, x, y, capacity));
			}
		}
		System.out.println("total number of garage parking in the scenario:" + totalCapacity);
	}

	private static void collectStreetParkings(ArrayList<String> outputList) {
		String streetParkings = Config.baseFolder + "../streetpark_facilities.xml";
		ActivityFacilities streetParkingFacilities = GeneralLib.readActivityFacilities(streetParkings);

		int totalCapacity = 0;
		for (Id facilityId : streetParkingFacilities.getFacilities().keySet()) {
			ActivityFacilityImpl facilityImpl = (ActivityFacilityImpl) streetParkingFacilities.getFacilities().get(facilityId);

			Map<String, ActivityOption> activityOptions = facilityImpl.getActivityOptions();

			if (Config.isInsideStudyArea(facilityImpl.getCoord())) {
				long capacity = Math.round(activityOptions.get("parking").getCapacity());
				totalCapacity += capacity;
				outputList.add(getParkingString("sp-" + facilityId.toString(), facilityImpl.getCoord().getX(), facilityImpl
						.getCoord().getY(), capacity));
			}
		}
		System.out.println("total number of street parking in the scenario:" + totalCapacity);
	}

	private static String getParkingString(String id, double x, double y, double capacity) {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("\t<parkinglot>\n");
		stringBuffer.append("\t\t<id>" + id + "</id>\n");
		stringBuffer.append("\t\t<x>" + x + "</x>\n");
		stringBuffer.append("\t\t<y>" + y + "</y>\n");
		stringBuffer.append("\t\t<size>" + capacity + "</size>\n");
		stringBuffer.append("\t</parkinglot>\n");
		return stringBuffer.toString();
	}
}
