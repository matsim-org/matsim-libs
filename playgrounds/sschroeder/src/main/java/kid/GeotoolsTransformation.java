package kid;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class GeotoolsTransformation {
	
	public static class TransformationFactory {
		
		public static final String WGS84_32N = "EPSG:32632";
		
		public static final String WGS84_33N = "EPSG:32633";
		
		public static final String WGS84 = "WGS84";
		
		public static GeotoolsTransformation create(String oldRefName, String newRefName){
			GeotoolsTransformation geotoolsTransformation;
			try {
				geotoolsTransformation = new GeotoolsTransformation(CRS.decode(oldRefName),CRS.decode(newRefName));
			} catch (NoSuchAuthorityCodeException e) {
				e.printStackTrace();
				throw new IllegalStateException(e.toString());
				
			} catch (FactoryException e) {
				e.printStackTrace();
				throw new IllegalStateException(e.toString());
			}
			return geotoolsTransformation;
		}
		
		public static GeotoolsTransformation createFromWGS84To(String refName){
			GeotoolsTransformation geotoolsTransformation;
			try {
				geotoolsTransformation = new GeotoolsTransformation(DefaultGeographicCRS.WGS84,CRS.decode(refName));
			} catch (NoSuchAuthorityCodeException e) {
				e.printStackTrace();
				throw new IllegalStateException(e.toString());
			} catch (FactoryException e) {
				e.printStackTrace();
				throw new IllegalStateException(e.toString());
			}
			return geotoolsTransformation;
		}
		
		
	}
	
	private CoordinateReferenceSystem oldReferenceSystem;
	
	private CoordinateReferenceSystem newReferenceSystem;

	public GeotoolsTransformation(CoordinateReferenceSystem oldReferenceSystem,
			CoordinateReferenceSystem newReferenceSystem) {
		super();
		this.oldReferenceSystem = oldReferenceSystem;
		this.newReferenceSystem = newReferenceSystem;
	}
	
	public Coordinate transform(Coordinate coord){
		DirectPosition currentPosition = new DirectPosition2D(coord.x,coord.y);
		DirectPosition transformedPosition = null;
		
		try {
			MathTransform transform = CRS.findMathTransform(oldReferenceSystem, newReferenceSystem);
			transformedPosition = transform.transform(currentPosition, transformedPosition);
			
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Coordinate newCoord = makeCoordinate(transformedPosition.getCoordinate());
		return newCoord;
	}
	
	public CoordinateReferenceSystem getOldReferenceSystem() {
		return oldReferenceSystem;
	}

	public CoordinateReferenceSystem getNewReferenceSystem() {
		return newReferenceSystem;
	}

	public SimpleFeature transformFeature(SimpleFeature feature){
		SimpleFeature f = feature;
		SimpleFeature copy = SimpleFeatureBuilder.copy(f);
		Geometry geometry = (Geometry)f.getDefaultGeometry();
		try {
			MathTransform transform = CRS.findMathTransform(oldReferenceSystem, newReferenceSystem);
			Geometry newGeometry = JTS.transform(geometry, transform);
			copy.setDefaultGeometry(newGeometry);
			
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MismatchedDimensionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return copy;	
	}

	private Coordinate makeCoordinate(double[] coordinate) {
		return new Coordinate(coordinate[0],coordinate[1]);
	}
}
