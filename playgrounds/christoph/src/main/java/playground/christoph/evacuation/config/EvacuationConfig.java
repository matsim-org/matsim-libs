package playground.christoph.evacuation.config;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class EvacuationConfig {

	public static final double evacuationTime = 3600 * 8.0;
	
	public static final double innerRadius = 15000.0;
	public static final double outerRadius = 15500.0;
	
//	public static final Id centerNodeId = new IdImpl("3073"); // Central in the ivt-ch-cut Network
//	public static final Id centerNodeId = new IdImpl("2531"); // Bellevue in the ivt-ch-cut Network
	
//	public static final Coord centerCoord = new CoordImpl("683518.0","246836.0");	// Bellevue Coord
	public static final Coord centerCoord = new CoordImpl("640050.0", "246256.0");	// Coordinates of KKW Goesgen
}
