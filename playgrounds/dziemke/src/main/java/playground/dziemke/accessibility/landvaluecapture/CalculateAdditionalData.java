package playground.dziemke.accessibility.landvaluecapture;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CalculateAdditionalData {
	// private final Logger log = Logger.getLogger(CalculateAdditionalData.class);
	
	public static void main(String[] args) {
		
		String fromCRS = "EPSG:31468"; //GK4
//		String crs2 = "EPSG:4326"; //WGS84
		String toCRS = "EPSG:3068"; //DHDN / Soldner Berlin
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
		
		GeometryFactory geometryFactory = new GeometryFactory();
		
		//String inputFile = "../../shared-svn/projects/accessibility_berlin/output/01/s/accessibilities.csv";
		String parkInputFile = "../../shared-svn/projects/accessibility_berlin/landvaluecapture/QGIs/Gewaesser.shp";
		
		double minX = 4574000;
		double minY = 5802000;
		double maxX = 4620000;
		double maxY = 5839000;
		double gridSize = 1000;
		
		ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, gridSize);
		
		ShapeFileReader shapefileReader = new ShapeFileReader();
		shapefileReader.readFileAndInitialize(parkInputFile);
		Collection<SimpleFeature> waterFeatures = shapefileReader.getFeatureSet();
		
		int numberOfMeasurePoints = 0;
		
//		System.out.println("measuringPoints.getFacilities().size() = " + measuringPoints.getFacilities().size());
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			
			Coord measuringPointCoord = ct.transform(measuringPoint.getCoord());
			Coordinate measuringPointCoordinate = new Coordinate(measuringPointCoord.getX(), measuringPointCoord.getY());
			Geometry measuringPointAsGeometry = geometryFactory.createPoint(measuringPointCoordinate);
			
			Double minDistanceToWater = Double.POSITIVE_INFINITY;
			
			for (SimpleFeature waterFeature : waterFeatures) {
				Geometry waterGeometry = (Geometry) waterFeature.getDefaultGeometry();
				
				Double distanceToWater = measuringPointAsGeometry.distance(waterGeometry);
				
				if (distanceToWater < minDistanceToWater) {
					minDistanceToWater = distanceToWater;
				}				
			}
			
			numberOfMeasurePoints++;
			System.out.println("distance to water from measuringPoint = " + numberOfMeasurePoints + " with coordinate"
					+ measuringPoint.getCoord() + " is = " + minDistanceToWater + "m.");
		}
	}
}