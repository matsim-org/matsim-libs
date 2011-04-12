/**
 * 
 */
package playground.yu.utils.googleMap;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author Chen
 * 
 */
public abstract class X2GoogleMap {
	/** converts coordinate system to WGS84 */
	protected CoordinateTransformation coordTransform;

	protected static String URL_HEADER = "http://maps.google.com/maps/api/staticmap?",
			MARKERS = "&markers=",
			COLOR = "color:",
			LABEL = "label:",
			SEPERATOR_IN_PARAMETER = "|",
			PATH = "&path=",
			SENSOR = "&sensor=",
			DEFAULT_LABEL_COLOR = "blue",
			COORDINATE_SEPERATOR = ",",
			DEFAULT_PATH_COLOR = "0x0000ff",
			WEIGHT = "weight:",
			SIZE = "&size=",
			DEFAULT_SIZE = "1024x768",
			DEFAULT_SENSOR = "false";
	protected static int DEFAULT_WEIGHT = 5;

	public X2GoogleMap(String fromSystem) {
		coordTransform = TransformationFactory.getCoordinateTransformation(
				fromSystem, TransformationFactory.WGS84);
	}

	/**
	 * @param coord
	 *            original coordinate in MATSim files
	 * @return
	 */
	protected String createCoordinate(Coord coord) {
		coord = coordTransform.transform(coord);
		StringBuffer strBuf = new StringBuffer(Double.toString(coord.getY()));
		strBuf.append(COORDINATE_SEPERATOR);
		strBuf.append(Double.toString(coord.getX()));

		return strBuf.toString();
	}

	protected String createMarker(Coord coord, String label, String color) {
		StringBuffer strBuf = new StringBuffer(MARKERS);
		strBuf.append(COLOR);
		strBuf.append(color);
		strBuf.append(SEPERATOR_IN_PARAMETER);
		strBuf.append(LABEL);
		strBuf.append(label);
		strBuf.append(SEPERATOR_IN_PARAMETER);
		strBuf.append(this.createCoordinate(coord));

		return strBuf.toString();
	}

	/**
	 * @param linkIds
	 * @param color
	 *            color of path incl. transparency
	 * @return
	 */
	protected String createPath(List<Coord> coords, String color, int weight) {
		StringBuffer strBuf = new StringBuffer(PATH);
		strBuf.append(COLOR);
		strBuf.append(color);
		strBuf.append(SEPERATOR_IN_PARAMETER);
		strBuf.append(WEIGHT);
		strBuf.append(weight);
		strBuf.append(SEPERATOR_IN_PARAMETER);

		strBuf.append(this.createCoordinate(coords.remove(0)/* first point */));
		for (Coord coord : coords) {
			strBuf.append(SEPERATOR_IN_PARAMETER);
			strBuf.append(this.createCoordinate(coord));
		}
		return strBuf.toString();
	}
}
