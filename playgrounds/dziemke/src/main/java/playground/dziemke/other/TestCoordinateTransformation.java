package playground.dziemke.other;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TestCoordinateTransformation {

	/**
	 * Use our standard coordinate transformation procedure to convert coordinates from Nelson Mandela Bay from South African Albers to WGS84
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);

		Coord coordinateMoabitDHDN_GK4 = new Coord(4590918.313160132, 5822867.249212103);

		final double y1 = -3714912.098;
		Coord lowerLeftCoordinateSAALbers = new Coord(111583.944, y1);
		final double y = -3667912.098;
		Coord upperRightCoordinateSAALbers = new Coord(171583.944, y);
		
		Coord coordinateMoabitWGS84 = transformation.transform(coordinateMoabitDHDN_GK4);
		
		Coord lowerLeftCoordinateWGS84 = transformation.transform(lowerLeftCoordinateSAALbers);
		Coord upperRightCoordinateWGS84 = transformation.transform(upperRightCoordinateSAALbers);
		
		System.out.println("###########################################################################");
		System.out.println("coordinateMoabitWGS84: " + coordinateMoabitWGS84);
		System.out.println("lowerLeftCoordinateWGS84: " + lowerLeftCoordinateWGS84);
		System.out.println("upperRightCoordinateWGS84: " + upperRightCoordinateWGS84);
		System.out.println("###########################################################################");
	}

}
