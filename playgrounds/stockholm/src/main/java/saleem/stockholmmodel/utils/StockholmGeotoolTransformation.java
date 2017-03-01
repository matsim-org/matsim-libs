package saleem.stockholmmodel.utils;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Point;
/**
 * A class to convert coordinates from one coordinate system to
 * another one.
 * 
 * @author Mohammad Saleem
 */
public class StockholmGeotoolTransformation implements CoordinateTransformation{
	private MathTransform transform;

	/**
	 * Creates a new coordinate transformation that makes use of GeoTools.
	 * The coordinate systems to translate from and to can either be specified as
	 * shortened names, as defined in {@link TransformationFactory}, or as
	 * Well-Known-Text (WKT) as supported by the GeoTools.
	 *
	 * @param from Specifies the origin coordinate reference system
	 * @param to Specifies the destination coordinate reference system
	 *
	 * @see <a href="http://geoapi.sourceforge.net/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html">WKT specifications</a>
	 */
	public StockholmGeotoolTransformation(final String from, final String to) {
		CoordinateReferenceSystem sourceCRS = StockholmMGC.getCRS(from);
		CoordinateReferenceSystem targetCRS = StockholmMGC.getCRS(to);

		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}

	public Coord transform(final Coord coord) {
		Point p = null;
		try {
			p = (Point) JTS.transform(StockholmMGC.coord2Point(coord), this.transform);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		}
		return StockholmMGC.point2Coord(p);
	}

}
