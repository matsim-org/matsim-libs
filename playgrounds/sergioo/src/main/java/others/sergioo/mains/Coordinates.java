package others.sergioo.mains;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class Coordinates {

	public static void main(String[] args) {
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_SVY21);
		System.out.println(coordinateTransformation.transform(new CoordImpl(103.84790986776352, 1.295153902189859)));
	}

}
