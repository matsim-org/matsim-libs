package playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class VisualizeFacilitiesWithNoPrivateParkingAssigned {

	public static void main(String[] args) {
		QuadTree<ActivityFacilityImpl> facilitiesQuadTree = PrivateParkingsIndoorWriter_v0.getFacilitiesQuadTree();
		HashMap<String, ActivityFacilityImpl> facilities = new HashMap<String, ActivityFacilityImpl>();

		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();
		ParkingHerbieControler.readParkings(1.0, "H:/data/experiments/TRBAug2011/parkings/flat/privateParkingsIndoor.xml",
				parkingCollection);
		ParkingHerbieControler.readParkings(1.0, "H:/data/experiments/TRBAug2011/parkings/flat/privateParkingsOutdoor.xml",
				parkingCollection);

		String outputKmlFile = "H:/data/experiments/TRBAug2011/parkings/kmls/facilitiesWithNoPrivateParkingAssigned.kml";

		BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();

		for (ActivityFacilityImpl actFacility : facilitiesQuadTree.values()) {
			if (isInZHCityRectangle(actFacility.getCoord())) {
				facilities.put(actFacility.getId().toString(), actFacility);
			}
		}

		for (Parking parking : parkingCollection) {
			if (isInZHCityRectangle(parking.getCoord())) {
				PrivateParking privateParking = (PrivateParking) parking;
				Id facilityId = privateParking.getActInfo().getFacilityId();
				if (facilities.containsKey(facilityId.toString())) {
					facilities.remove(facilityId.toString());
				} else {
					// DebugLib.stopSystemAndReportInconsistency(facilityId.toString());
				}
			}
		}

		for (ActivityFacilityImpl actFacility : facilities.values()) {
			basicPointVisualizer.addPointCoordinate(actFacility.getCoord(), actFacility.toString(), Color.GREEN);
		}

		System.out.println("writing kml file...");
		basicPointVisualizer.write(outputKmlFile);

	}

	private static boolean isInZHCityRectangle(Coord coord) {
		if (coord.getX() > 676227.0 && coord.getX() < 689671.0) {
			if (coord.getY() > 241585.0 && coord.getY() < 254320.0) {
				return true;
			}
		}

		return false;
	}

}
