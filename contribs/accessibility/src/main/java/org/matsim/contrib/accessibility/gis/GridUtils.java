package org.matsim.contrib.accessibility.gis;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GridUtils {
	
	// logger
	private static final Logger log = Logger.getLogger(GridUtils.class);
	
	/**
	 * 
	 * @param shapeFile
	 * @return Geometry determines the scenario boundary for the accessibility measure
	 */
	public static Geometry getBoundary(String shapeFile){
		
		try{
			// get boundaries of study area
			Set<SimpleFeature> featureSet = FeatureSHP.readFeatures(shapeFile);
			log.info("Extracting boundary of the shape file ...");
			Geometry boundary = (Geometry) featureSet.iterator().next().getDefaultGeometry();
			log.info("Done extracting boundary ...");
			
			if(featureSet.size() > 1){
				log.warn("The given shape file is not suitable for accessibility calculations.");
				log.warn("This means you have to provide a shape file that only contains the border of the study area without any further features, i.e. zones or fazes, are allowed!");
				log.warn("Replace the shape file in your UrbanSim configuration provided at \"travel_model_configuration/matsim4urbansim/controler_parameter/shape_file\"");
				log.warn("If the shape file contains features accessibilities will be computes for only feature, i.e. for one zone or faz.");
			}
			return boundary;
		} catch (NullPointerException npe){
			npe.printStackTrace();
		} catch (IOException io){
			io.printStackTrace();
			log.error("Geometry object containing the study area boundary shape is null !");
			System.exit(-1);
		}
		return null;
	}
	
//	/**
//	 * creates measuring points for accessibility computation
//	 * 
//	 * @param <T>
//	 * @param gridSize
//	 * @param boundary
//	 * @return
//	 */
//	@Deprecated
//	public static ZoneLayer<Id> createGridLayerByGridSizeByShapeFile(double gridSize, Geometry boundary) {
//		
//		log.info("Setting statring points for accessibility measure ...");
//
//		int skippedPoints = 0;
//		int setPoints = 0;
//		
//		GeometryFactory factory = new GeometryFactory();
//		
//		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
//		Envelope env = boundary.getEnvelopeInternal();
//		
//		ProgressBar bar = new ProgressBar( (env.getMaxX()-env.getMinX())/gridSize );
//		
//		// goes step by step from the min x and y coordinate to max x and y coordinate
//		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
//			
//			bar.update();
//						
//			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
//				
//				// check first if cell centroid is within study area
//				double center_X = x + (gridSize/2);
//				double center_Y = y + (gridSize/2);
//				Point centroid = factory.createPoint(new Coordinate(center_X, center_Y));
//				
//				if(boundary.contains(centroid)) {
//					Point point = factory.createPoint(new Coordinate(x, y));
//					
//					Coordinate[] coords = new Coordinate[5];
//					coords[0] = point.getCoordinate();
//					coords[1] = new Coordinate(x, y + gridSize);			// upper left
//					coords[2] = new Coordinate(x + gridSize, y + gridSize);	// upper right
//					coords[3] = new Coordinate(x + gridSize, y);			// lower right
//					coords[4] = point.getCoordinate();						// lower left
//					
//					// Linear Ring defines an artificial zone
//					LinearRing linearRing = factory.createLinearRing(coords);
//					Polygon polygon = factory.createPolygon(linearRing, null);
//					// polygon.setSRID( srid ); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
//					
//					Zone<Id> zone = new Zone<Id>(polygon);
//					zone.setAttribute( new IdImpl( setPoints ) );
//					zones.add(zone);
//					
//					setPoints++;
//				}
//				else skippedPoints++;
//			}
//		}
//
//		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
//		log.info("Done with setting starting points!");
//		
//		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
//		return layer;
//	}
	
	/**
	 * creates measuring points for accessibility computation
	 * @param boundary
	 * @param gridSize
	 * 
	 * @return ActivityFacilitiesImpl containing the coordinates for the measuring points 
	 */
	public static ActivityFacilitiesImpl createGridLayerByGridSizeByShapeFileV2(Geometry boundary, double gridSize) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		ActivityFacilitiesImpl measuringPoints = new ActivityFacilitiesImpl("accessibility measuring points");
		Envelope env = boundary.getEnvelopeInternal();

		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {

			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				
				// check first if cell centroid is within study area
				double centerX = x + (gridSize/2);
				double centerY = y + (gridSize/2);
				Point centroid = factory.createPoint(new Coordinate(centerX, centerY));
				
				if(boundary.contains(centroid)) {
					Coord center = new CoordImpl(centerX, centerY);
					measuringPoints.createAndAddFacility(new IdImpl( setPoints ), center);
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		return measuringPoints;
	}
	
//	/**
//	 * creates measuring points for accessibility computation
//	 * 
//	 * @param <T>
//	 * @param gridSize
//	 * @param boundingBox defines an area within the network file, this area will later be processed when calculating accessibilities.
//	 * @param boundary
//	 * @return
//	 */
//	@Deprecated
//	public static ZoneLayer<Id> createGridLayerByGridSizeByNetwork(double gridSize, double [] boundingBox) {
//		
//		log.info("Setting statring points for accessibility measure ...");
//
//		int skippedPoints = 0;
//		int setPoints = 0;
//		
//		GeometryFactory factory = new GeometryFactory();
//		
//		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
//
//		double xmin = boundingBox[0];
//		double ymin = boundingBox[1];
//		double xmax = boundingBox[2];
//		double ymax = boundingBox[3];
//		
//		
//		ProgressBar bar = new ProgressBar( (xmax-xmin)/gridSize );
//		
//		// goes step by step from the min x and y coordinate to max x and y coordinate
//		for(double x = xmin; x <xmax; x += gridSize) {
//			
//			bar.update();
//						
//			for(double y = ymin; y < ymax; y += gridSize) {
//				
//				// check first if cell centroid is within study area
//				double center_X = x + (gridSize/2);
//				double center_Y = y + (gridSize/2);
//				
//				// check if x, y is within network boundary
//				if (center_X <= xmax && center_X >= xmin && 
//					center_Y <= ymax && center_Y >= ymin) {
//				
//					Point point = factory.createPoint(new Coordinate(x, y));
//					
//					Coordinate[] coords = new Coordinate[5];
//					coords[0] = point.getCoordinate();
//					coords[1] = new Coordinate(x, y + gridSize);
//					coords[2] = new Coordinate(x + gridSize, y + gridSize);
//					coords[3] = new Coordinate(x + gridSize, y);
//					coords[4] = point.getCoordinate();
//					// Linear Ring defines an artificial zone
//					LinearRing linearRing = factory.createLinearRing(coords);
//					Polygon polygon = factory.createPolygon(linearRing, null);
//					// polygon.setSRID( srid ); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
//					
//					Zone<Id> zone = new Zone<Id>(polygon);
//					zone.setAttribute( new IdImpl( setPoints ) );
//					zones.add(zone);
//					
//					setPoints++;
//				}
//				else skippedPoints++;
//			}
//		}
//
//		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
//		log.info("Done with setting starting points!");
//		
//		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
//		return layer;
//	}
	
	/**
	 * creates measuring points for accessibility computation
	 * @param minX The smallest x coordinate (easting, longitude) expected
	 * @param minY The smallest y coordinate (northing, latitude) expected
	 * @param maxX The largest x coordinate (easting, longitude) expected
	 * @param maxY The largest y coordinate (northing, latitude) expected
	 * @param gridSize
	 * 
	 * @return ActivityFacilitiesImpl containing the coordinates for the measuring points 
	 */
	public static ActivityFacilitiesImpl createGridLayerByGridSizeByBoundingBoxV2(double minX, double minY, double maxX, double maxY, double gridSize) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;

		ActivityFacilitiesImpl measuringPoints = new ActivityFacilitiesImpl("accessibility measuring points");
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = minX; x <maxX; x += gridSize) {

			for(double y = minY; y < maxY; y += gridSize) {
				
				// check first if cell centroid is within study area
				double centerX = x + (gridSize/2);
				double centerY = y + (gridSize/2);
				
				// check if x, y is within network boundary
				if (centerX <= maxX && centerX >= minX && 
					centerY <= maxY && centerY >= minY) {
					
					Coord center = new CoordImpl(centerX, centerY);
					measuringPoints.createAndAddFacility(new IdImpl( setPoints ), center);
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		return measuringPoints;
	}
	
//	/**
//	 * Converting zones (or zone centroids) of type ActivityFacilitiesImpl into a ZoneLayer
//	 * 
//	 * @param facility ActivityFacilitiesImpl
//	 * @return ZoneLayer<Id>
//	 */
//	public static ZoneLayer<Id> convertActivityFacilities2ZoneLayer(ActivityFacilitiesImpl facility){
//		
//		int setPoints = 0;
//		
//		GeometryFactory factory = new GeometryFactory();
//		
//		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
//
//		Iterator<ActivityFacility> facilityIterator =  facility.getFacilities().values().iterator();
//		
//		while(facilityIterator.hasNext()){
//			
//			ActivityFacility af = facilityIterator.next();
//
//			Coord coord = af.getCoord();
//			// Point defines the artificial zone centroid
//			Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
//			// point.setSRID(srid); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
//			
//			Zone<Id> zone = new Zone<Id>(point);
//			zone.setAttribute( af.getId() );
//			zones.add(zone);
//			
//			setPoints++;
//		}
//		
//		log.info("Having " + setPoints + " 'ActivityFacilitiesImpl' items converted into 'ZoneLayer' format");
//		log.info("Done with conversion!");
//		
//		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
//
//		if(!checkConversion(facility, layer))
//			log.error("Conversion error: Either not all items are converted or coordinates are wrong!");
//		
//		return layer;
//	}
	
	/**
	 * returns a spatial grid for a given geometry (e.g. shape file) with a given grid size
	 * @param boundary a boundary, e.g. from a shape file
	 * @param gridSize side length of the grid
	 * 
	 * @return SpatialGrid storing accessibility values
	 */
	public static SpatialGrid createSpatialGridByShapeBoundary(Geometry boundary, double gridSize) {
		Envelope env = boundary.getEnvelopeInternal();
		double xMin = env.getMinX();
		double xMax = env.getMaxX();
		double yMin = env.getMinY();
		double yMax = env.getMaxY();
		
		return new SpatialGrid(xMin, yMin, xMax, yMax, gridSize, Double.NaN);
	}

	/**
	 * stores measured accessibilities in a file
	 * 
	 * @param grid SpatialGrid containing measured accessibilities
	 * @param fileName output file
	 */
	public static void writeSpatialGridTable(SpatialGrid grid, String fileName){
		
		log.info("Writing spatial grid table " + fileName + " ...");
		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
		try{
			sgTableWriter.write(grid, fileName);
			log.info("... done!");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
