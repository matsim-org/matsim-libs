package playground.christoph.evacuation.config;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class EvacuationConfig {
	
	public static double evacuationTime = 3600 * 8.0;
	
	public static double innerRadius = 30000.0;
	public static double outerRadius = 30500.0;
	
//	public static Coord centerCoord = new CoordImpl("683518.0","246836.0");	// Bellevue Coord
	public static Coord centerCoord = new CoordImpl("640050.0", "246256.0");	// Coordinates of KKW Goesgen
	
	public static String dhm25File = "../../matsim/mysimulations/networks/GIS/nodes_3d_dhm25.shp";
	public static String srtmFile = "../../matsim/mysimulations/networks/GIS/nodes_3d_srtm.shp";
	
	public static String householdObjectAttributesFile = "";
	
	public static List<String> evacuationArea = new ArrayList<String>();
	
	public static List<String> vehicleFleet = new ArrayList<String>();

	public static double affectedAreaTimePenaltyFactor = 1.20;
	public static double affectedAreaDistanceBuffer = 2500.0;
	
	/*
	 * Analysis modules
	 */
	public static boolean createEvacuationTimePicture = true;
	public static boolean countAgentsInEvacuationArea = true;
	
	public static Coord getRescueCoord() {
		return new CoordImpl(centerCoord.getX() + 50000.0, centerCoord.getY() + 50000.0);
	}
}
