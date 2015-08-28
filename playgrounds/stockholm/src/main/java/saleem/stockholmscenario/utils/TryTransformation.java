package saleem.stockholmscenario.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TryTransformation {
	public static void main(String[] args) {
		CoordinateTransformation ct = StockholmTransformationFactory.getCoordinateTransformation(StockholmTransformationFactory.WGS84, StockholmTransformationFactory.WGS84_RT90);
		Coord coord = new CoordImpl(18.0686, 59.3294);
		coord = ct.transform(coord);
		System.out.println("X: " + coord.getX() + " Y: " + coord.getY());
		
		CoordinateTransformation ct1 = StockholmTransformationFactory.getCoordinateTransformation(StockholmTransformationFactory.WGS84_RT90, StockholmTransformationFactory.WGS84_SWEREF99);
		Coord coord1 = new CoordImpl(coord.getX(), coord.getY());
		coord1 = ct1.transform(coord1);
		System.out.println("X: " + coord1.getX() + " Y: " + coord1.getY());
	}
}
