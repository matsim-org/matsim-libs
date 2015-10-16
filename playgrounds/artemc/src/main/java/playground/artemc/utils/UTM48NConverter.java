package playground.artemc.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class UTM48NConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Double Lon = 103.86804;
		Double Lat = 1.32567;
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		Coord coordWGS84 = new Coord(Lon, Lat);
		Coord coordUTM = ct.transform(coordWGS84);
		System.out.println(coordUTM.getX()+","+coordUTM.getY());
	}

}
