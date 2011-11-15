package playground.wrashid.parkingSearch.ca.matlabInfra;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class Config {

	private static double radiusInMetersOfStudyArea = 1000;
	private static Coord studyAreaCenter = ParkingHerbieControler.getCoordinatesQuaiBridgeZH();
	private static String baseFolder = "H:/data/experiments/TRBAug2011/runs/ktiRun22/output/";
	private static String outputFolder= "H:/data/experiments/matlabExprVoresung/Nov2011/";
	
	private static NetworkImpl network=null;
	
	public static String getNetworkFile() {
		return baseFolder + "output_network.xml.gz";
	}

	public static String getOutputFolder(){
		return outputFolder;
	}
	
	public static String getEventsFile() {
		return baseFolder + "ITERS/it.50/50.events.xml.gz";
	}

	public static boolean isInsideStudyArea(Coord coord) {
		return GeneralLib.getDistance(coord, studyAreaCenter) < radiusInMetersOfStudyArea;
	}
	
	public static boolean isInsideStudyArea(Id linkId) {
		Coord coord=getNetwork().getLinks().get(linkId).getCoord();
		return GeneralLib.getDistance(coord, studyAreaCenter) < radiusInMetersOfStudyArea;
	}
	
	public static NetworkImpl getNetwork(){
		if (network==null){
			network=GeneralLib.readNetwork(getNetworkFile());
		}
		return network;
	}

}
