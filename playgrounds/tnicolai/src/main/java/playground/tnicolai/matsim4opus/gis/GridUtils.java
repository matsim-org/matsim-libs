package playground.tnicolai.matsim4opus.gis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;

import playground.tnicolai.matsim4opus.gis.io.FeatureKMLWriter;
import playground.tnicolai.matsim4opus.gis.io.FeatureSHP;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;
import playground.tnicolai.matsim4opus.utils.io.writer.SpatialGridTableWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GridUtils {
	
	// logger
	private static final Logger log = Logger.getLogger(GridUtils.class);
	
	/**
	 * @param shpFile
	 * @param srid gives the spatial reference id for the shapefile
	 * @return
	 */
	public static Geometry getBoundary(String shpFile, int srid){
		
		try{
			// get boundaries of study area
			Set<Feature> featureSet = FeatureSHP.readFeatures(shpFile);
			log.info("Extracting boundary of the shape file ...");
			Geometry boundary = featureSet.iterator().next().getDefaultGeometry();
			boundary.setSRID( srid );
			log.warn("Using SRID: " + srid);
			log.info("Done extracting boundary ...");
			
			return boundary;
		} catch (IOException io){
			io.printStackTrace();
			log.error("Geometry object containing the study area boundary shape is null !");
			System.exit(-1);
		}
		return null;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundary
	 * @return
	 */
	public static ZoneLayer<CounterObject> createGridLayerByGridSizeByShapeFile(double gridSize, Geometry boundary, int srid) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<CounterObject>> zones = new HashSet<Zone<CounterObject>>();
		Envelope env = boundary.getEnvelopeInternal();
		
		ProgressBar bar = new ProgressBar( (env.getMaxX()-env.getMinX())/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
			
			bar.update();
						
			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				
				// check first if cell centroid is within study area
				double center_X = x + (gridSize/2);
				double center_Y = y + (gridSize/2);
				Point centroid = factory.createPoint(new Coordinate(center_X, center_Y));
				
				if(boundary.contains(centroid)) {
					Point point = factory.createPoint(new Coordinate(x, y));
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);			// upper left
					coords[2] = new Coordinate(x + gridSize, y + gridSize);	// upper right
					coords[3] = new Coordinate(x + gridSize, y);			// lower right
					coords[4] = point.getCoordinate();						// lower left
					
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					polygon.setSRID( srid ); 
					
					Zone<CounterObject> zone = new Zone<CounterObject>(polygon);
					zone.setAttribute( new CounterObject( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<CounterObject> layer = new ZoneLayer<CounterObject>(zones);
		return layer;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundingBox defines an area within the network file, this area will later be processed when calculating accessibilities.
	 * @param boundary
	 * @return
	 */
	public static ZoneLayer<CounterObject> createGridLayerByGridSizeByNetwork(double gridSize, double [] boundingBox, int srid) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<CounterObject>> zones = new HashSet<Zone<CounterObject>>();

		double xmin = boundingBox[0];
		double ymin = boundingBox[1];
		double xmax = boundingBox[2];
		double ymax = boundingBox[3];
		
		
		ProgressBar bar = new ProgressBar( (xmax-xmin)/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = xmin; x <xmax; x += gridSize) {
			
			bar.update();
						
			for(double y = ymin; y < ymax; y += gridSize) {
				
				// check if x, y is within network boundary
				if(x <= xmax && x >= xmin && y <= ymax && y >= ymin){
				
					Point point = factory.createPoint(new Coordinate(x, y));
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);
					coords[2] = new Coordinate(x + gridSize, y + gridSize);
					coords[3] = new Coordinate(x + gridSize, y);
					coords[4] = point.getCoordinate();
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					polygon.setSRID( srid ); 
					
					Zone<CounterObject> zone = new Zone<CounterObject>(polygon);
					zone.setAttribute( new CounterObject( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<CounterObject> layer = new ZoneLayer<CounterObject>(zones);
		return layer;
	}
	
	/**
	 * @param boundary
	 */
	public static SpatialGrid<Double> createSpatialGridByShapeBoundary(double gridSize, Geometry boundary) {
		Envelope env = boundary.getEnvelopeInternal();
		double xMin = env.getMinX();
		double xMax = env.getMaxX();
		double yMin = env.getMinY();
		double yMax = env.getMaxY();
		
		return new SpatialGrid<Double>(xMin, yMin, xMax, yMax, gridSize);
	}

	public static void writeSpatialGridTable(SpatialGrid<Double> grid, String fileName){
		
		log.info("Writing spatial grid tables ...");
		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
		try{
			sgTableWriter.write(grid, fileName);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * @param myListener
	 */
	public static void writeKMZFiles(ZoneLayer<CounterObject> measuringPoints, SpatialGrid<Double> grid, String fileName) {
		log.info("Writing Google Erath files ...");
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		Set<Geometry> geometries = new HashSet<Geometry>();
		
		TObjectDoubleHashMap<Geometry> kmzData = new TObjectDoubleHashMap<Geometry>();
		
		for(Zone<CounterObject> zone : measuringPoints.getZones()) {
			Geometry geometry = zone.getGeometry();
			geometries.add(geometry);
			kmzData.put( geometry, grid.getValue(geometry.getCentroid()) );
		}
		
		// writing travel time accessibility kmz file
		writer.setColorizable(new MyColorizer(kmzData));
		writer.write(geometries, fileName);
		log.info("Done with writing Google Erath files ...");
	}
}
