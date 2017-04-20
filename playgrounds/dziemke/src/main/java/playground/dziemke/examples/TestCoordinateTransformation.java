package playground.dziemke.examples;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TestCoordinateTransformation {

	public static void main(String[] args) {
		// WGS84 = EPSG:4326
		// Arc 1960 / UTM zone 37S = "EPSG:21037"
		// DHDN / 3-degree Gauss-Kruger zone 4 = "EPSG:31468"
//		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
//				TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation
//				("EPSG:4326", "EPSG:31468");
//				("EPSG:4326", "EPSG:21037");
				("EPSG:4326", "EPSG:26911");
//				("EPSG:3006", "EPSG:4326");
//				(TransformationFactory.WGS84_SA_Albers, "EPSG:4326");

//		Coord originalCoord1 = new Coord(36.82829619497265, -1.291087691581653); // near Nairobi, Kenya
//		Coord originalCoord1 = new Coord(13.124627, 52.361485); // Berlin lower left
//		Coord originalCoord2 = new Coord(13.718465, 52.648131); // Berlin upper right
//		Coord originalCoord1 = new Coord(150583.9441831379,-3699678.99131796); // somewhere in NMB in SA_Albers
//		Coord originalCoord2 = new Coord(171583.944, y);
//		Coord coordNE = new Coord(41.88, 5.000); // Kenya northeast corner
//		Coord coordSW = new Coord(33.88, -4.75); // Kenya southwest corner 
//		Coord coordNE = new Coord(36.8014, -1.3055); // Kibera northeast corner 
//		Coord coordSW = new Coord(36.7715, -1.3198); // Kibera southwest corner 
		Coord coordNE = new Coord(-119.55, 34.45); // Santa Barbara northeast corner
		Coord coordSW = new Coord(-119.90, 34.38); // Santa Barbara southwest corner
		
		Coord convertedCoordNE = transformation.transform(coordNE);
		Coord convertedCoordSW = transformation.transform(coordSW);
		
		System.out.println("###########################################################################");
		System.out.println("coordNE: " + coordNE);
		System.out.println("coordSW: " + coordSW);
		System.out.println("convertedCoordNE: " + convertedCoordNE);
		System.out.println("convertedCoordSW: " + convertedCoordSW);
		System.out.println("###########################################################################");
	}
}