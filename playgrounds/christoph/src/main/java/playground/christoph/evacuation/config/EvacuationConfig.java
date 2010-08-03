package playground.christoph.evacuation.config;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class EvacuationConfig {

	public static final double evacuationTime = 3600 * 8.0;
	
//	public static final double innerRadius = 10000.0;
//	public static final double outerRadius = 14000.0;
	public static final double innerRadius = 5000.0;
	public static final double outerRadius = 7000.0;
	
//	public static final Id centerNodeId = new IdImpl("3073"); // Central in the ivt-ch-cut Network
	public static final Id centerNodeId = new IdImpl("2531"); // Bellevue in the ivt-ch-cut Network
	
	public static final Coord centerCoord = new CoordImpl("683518","246836");	// Bellevue Coord
}
