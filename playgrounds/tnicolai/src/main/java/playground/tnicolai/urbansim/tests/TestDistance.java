package playground.tnicolai.urbansim.tests;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculatorFactory;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.tnicolai.urbansim.constants.Constants;

public class TestDistance {
	
	// Logger
	private static final Logger logger = Logger.getLogger(TestDistance.class);
	
	private String shapeFile = null;
	private int gridSize = -1;
	
	public TestDistance(){
		shapeFile = "/Users/thomas/Development/opus_home/data/seattle_parcel/shapefiles/boundary.shp";
		gridSize = 15000;
	}
	
	
	public void runTest(){
		
		Geometry boundary;
		try {
			
			boundary = getBoundary(shapeFile);
			testDistance(gridSize, boundary);
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	

	/**
	 * @param psrcSHPFile
	 * @return
	 * @throws IOException
	 */
	private Geometry getBoundary(String psrcSHPFile) throws IOException {
		// get boundaries of study area
		Set<Feature> featureSet = FeatureSHP.readFeatures(psrcSHPFile);
		logger.info("Extracting boundary of the shape file ...");
		Geometry boundary = featureSet.iterator().next().getDefaultGeometry();
		boundary.setSRID( Constants.SRID_WASHINGTON_NORTH ); // tnicolai: check if this is the correct id
		logger.info("Done extracting boundary ...");
		
		return boundary;
	}
	
	
	private void testDistance(double gridSize, Geometry boundary){
		
		GeometryFactory factory = new GeometryFactory();
		Envelope env = boundary.getEnvelopeInternal();
		
		int factorySrid = factory.getSRID();
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
			
			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				Point point = factory.createPoint(new Coordinate(x, y));
				if(boundary.contains(point)) {
					
					Point testPoint1 = factory.createPoint(new Coordinate(x, y + gridSize));
					Point testPoint2 = factory.createPoint(new Coordinate(x + gridSize, y + gridSize));
					Point testPoint3 = factory.createPoint(new Coordinate(x + gridSize, y));
					
					int srid = boundary.getSRID();
					CoordinateReferenceSystem crs = CRSUtils.getCRS(srid);
					
					DistanceCalculator distanceCalculator = DistanceCalculatorFactory.createDistanceCalculator(crs);
					double d1 = distanceCalculator.distance(point, testPoint1);
					double d2 = distanceCalculator.distance(point, testPoint2);
					double d3 = distanceCalculator.distance(point, testPoint3);
				}
			}
			
		}
	}
	

	
	/**
	 * starting point
	 */
	public static void main(String args[]){
		
		TestDistance testDistance = new TestDistance();
		testDistance.runTest();
	}

}
