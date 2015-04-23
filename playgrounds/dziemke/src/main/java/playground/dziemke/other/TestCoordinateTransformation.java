package playground.dziemke.other;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TestCoordinateTransformation {

	/**
	 * Use our standard coordinate transformation procedure to convert coordinates from Nelson Mandela Bay from South African Albers to WGS84
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);
		
		Coord lowerLeftCoordinateSAALbers = new CoordImpl(100000,-3720000);
		Coord upperRightCoordinateSAALbers = new CoordImpl(180000,-3675000);
		Coord lowerLeftCoordinateWGS84 = transformation.transform(lowerLeftCoordinateSAALbers);
		Coord upperRightCoordinateWGS84 = transformation.transform(upperRightCoordinateSAALbers);
		
		System.out.println("###########################################################################");
		System.out.println("lowerLeftCoordinateWGS84: " + lowerLeftCoordinateWGS84);
		System.out.println("upperRightCoordinateWGS84: " + upperRightCoordinateWGS84);
		System.out.println("###########################################################################");
	}

}
