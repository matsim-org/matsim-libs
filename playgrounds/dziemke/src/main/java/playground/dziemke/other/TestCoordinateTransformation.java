package playground.dziemke.other;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TestCoordinateTransformation {

	public static void main(String[] args) {
		// WGS84 = EPSG:4326
		// Arc 1960 / UTM zone 37S = "EPSG:21037"
		// WGS 84 / UTM zone 37S = "EPSG:31468"
//		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
//				TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation
//				("EPSG:4326", "EPSG:31468");
//				("EPSG:4326", "EPSG:21037");
				("EPSG:3006", "EPSG:4326");

//		Coord originalCoord1 = new Coord(36.82829619497265, -1.291087691581653); // near Nairobi, Kenya
		Coord originalCoord1 = new Coord(372300, 5802900); // Berlin lower left
//		Coord originalCoord1 = new Coord(413300, 5833900); // Berlin upper right
//		Coord originalCoord2 = new Coord(171583.944, y);
		
		Coord convertedCoord1 = transformation.transform(originalCoord1);
//		Coord convertedCoord2 = transformation.transform(originalCoord2);
		
		System.out.println("###########################################################################");
		System.out.println("originalCoord1: " + originalCoord1);
//		System.out.println("originalCoord2: " + originalCoord2);
		System.out.println("convertedCoord1: " + convertedCoord1);
//		System.out.println("convertedCoord2: " + convertedCoord2);
		System.out.println("###########################################################################");
	}

}
