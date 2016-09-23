package saleem.stockholmscenario.teleportation.gaming;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

import com.vividsolutions.jts.geom.Coordinate;

public class TempCoordinateTransformer {

	public static void main(String[] args) {
		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84_SWEREF99,
						StockholmTransformationFactory.WGS84);
		Coord coord = new Coord(676397.6826141255, 6571273.998452182);
		
		coord=coordinateTransform.transform(coord);
		System.out.println(coord.getY() + " , " + coord.getX());
	}

}
