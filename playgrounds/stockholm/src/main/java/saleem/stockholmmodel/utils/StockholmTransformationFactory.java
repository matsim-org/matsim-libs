package saleem.stockholmmodel.utils;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.geometry.transformations.WGS84toAtlantis;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
/**
 * A factory to instantiate a specific coordinate transformation.
 *
 * @author Mohammad Saleem
 *
 */
public class StockholmTransformationFactory extends TransformationFactory{
	public final static String WGS84_RT90 = "WGS84toRT90";
	public final static String WGS84_SWEREF99 = "WGS84toSWEREF99";
	public final static String WGS84_EPSG3857 = "WGS84_EPSG3857";
	/**
	 * Returns a coordinate transformation to transform coordinates from one
	 * coordinate system to another one.
	 *
	 * @param fromSystem The source coordinate system.
	 * @param toSystem The destination coordinate system.
	 * @return Coordinate Transformation
	 */
	public static CoordinateTransformation getCoordinateTransformation(final String fromSystem, final String toSystem) {
		if (fromSystem.equals(toSystem)) return new IdentityTransformation();
		if (WGS84.equals(fromSystem)) {
			if (CH1903_LV03.equals(toSystem)) return new WGS84toCH1903LV03();
			if (CH1903_LV03_Plus.equals(toSystem)) return new WGS84toCH1903LV03Plus();
			if (ATLANTIS.equals(toSystem)) return new WGS84toAtlantis();
		}
		if (WGS84.equals(toSystem)) {
			if (CH1903_LV03.equals(fromSystem)) return new CH1903LV03toWGS84();
			if (CH1903_LV03_Plus.equals(fromSystem)) return new CH1903LV03PlustoWGS84();
			if (GK4.equals(fromSystem)) return new GK4toWGS84();
			if (ATLANTIS.equals(fromSystem)) return new AtlantisToWGS84();
		}
		return new StockholmGeotoolTransformation(fromSystem, toSystem);
	}
}
