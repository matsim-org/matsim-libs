package playground.tnicolai.matsim4opus.gis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.misc.NetworkUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.io.FeatureKMLWriter;
import playground.tnicolai.matsim4opus.gis.io.FeatureSHP;
import playground.tnicolai.matsim4opus.matsim4urbansim.controlerinterface.AccessibilityControlerInterface;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;
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
	 * @return
	 */
	public static Geometry getBoundary(String shpFile){
		
		try{
			// get boundaries of study area
			Set<Feature> featureSet = FeatureSHP.readFeatures(shpFile);
			log.info("Extracting boundary of the shape file ...");
			Geometry boundary = featureSet.iterator().next().getDefaultGeometry();
			// boundary.setSRID( Constants.SRID_WASHINGTON_NORTH );
			boundary.setSRID( Constants.SRID_SWITZERLAND );
			log.warn("Using SRID of Switzerland: " + Constants.SRID_SWITZERLAND);
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
	public static ZoneLayer<ZoneAccessibilityObject> createGridLayerByGridSizeByShapeFile(double gridSize, Geometry boundary) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<ZoneAccessibilityObject>> zones = new HashSet<Zone<ZoneAccessibilityObject>>();
		Envelope env = boundary.getEnvelopeInternal();
		
		ProgressBar bar = new ProgressBar( (env.getMaxX()-env.getMinX())/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
			
			bar.update();
						
			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				Point point = factory.createPoint(new Coordinate(x, y));
				if(boundary.contains(point)) {
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);
					coords[2] = new Coordinate(x + gridSize, y + gridSize);
					coords[3] = new Coordinate(x + gridSize, y);
					coords[4] = point.getCoordinate();
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					polygon.setSRID( Constants.SRID_SWITZERLAND ); 
					
					Zone<ZoneAccessibilityObject> zone = new Zone<ZoneAccessibilityObject>(polygon);
					zone.setAttribute( new ZoneAccessibilityObject( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<ZoneAccessibilityObject> layer = new ZoneLayer<ZoneAccessibilityObject>(zones);
		return layer;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundary
	 * @return
	 */
	public static ZoneLayer<ZoneAccessibilityObject> createGridLayerByGridSizeByNetwork(double gridSize, Network network) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<ZoneAccessibilityObject>> zones = new HashSet<Zone<ZoneAccessibilityObject>>();
		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());
		double xmin = networkBoundingBox[0];
		double ymin = networkBoundingBox[1];
		double xmax = networkBoundingBox[2];
		double ymax = networkBoundingBox[3];
		
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
					polygon.setSRID( Constants.SRID_SWITZERLAND ); 
					
					Zone<ZoneAccessibilityObject> zone = new Zone<ZoneAccessibilityObject>(polygon);
					zone.setAttribute( new ZoneAccessibilityObject( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<ZoneAccessibilityObject> layer = new ZoneLayer<ZoneAccessibilityObject>(zones);
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
	
	/**
	 * @param boundary
	 */
	public static SpatialGrid<Double> createSpatialGridByNetworkBoundary(double gridSize, Network network) {

		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());
		
		// Saptial Grid takes : xmin, ymin, xmax, ymax, grid size
		return new SpatialGrid<Double>(networkBoundingBox[0], // x min
									   networkBoundingBox[1], // y min
									   networkBoundingBox[2], // x max
									   networkBoundingBox[3], // y max
									   gridSize);
	}
	
	/**
	 * @param myListener
	 */
	public static void writeSpatialGridTables(AccessibilityControlerInterface myListener, String fileNameExtension) {
		
		SpatialGrid<Double> congestedTravelTimeAccessibilityGrid = myListener.getCongestedTravelTimeAccessibilityGrid();
		SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid = myListener.getFreespeedTravelTimeAccessibilityGrid();
		SpatialGrid<Double> walkTravelTimeAccessibilityGrid = myListener.getWalkTravelTimeAccessibilityGrid();
		
		log.info("Writing spatial grid tables ...");
		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
		try {
			sgTableWriter.write(congestedTravelTimeAccessibilityGrid, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_CONGESTED_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + congestedTravelTimeAccessibilityGrid.getResolution() + fileNameExtension + Constants.FILE_TYPE_TXT);
			sgTableWriter.write(freespeedTravelTimeAccessibilityGrid, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_FREESPEED_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + freespeedTravelTimeAccessibilityGrid.getResolution() + fileNameExtension + Constants.FILE_TYPE_TXT);
			sgTableWriter.write(walkTravelTimeAccessibilityGrid, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_WALK_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + walkTravelTimeAccessibilityGrid.getResolution() + fileNameExtension + Constants.FILE_TYPE_TXT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done with writing spatial grid tables ...");
	}

	/**
	 * @param myListener
	 */
	public static void writeKMZFiles(AccessibilityControlerInterface myListener, String fileNameExtension) {
		log.info("Writing Google Erath files ...");
		
		ZoneLayer<ZoneAccessibilityObject> startZones = myListener.getStartZones();
		
		SpatialGrid<Double> congestedTravelTimeAccessibilityGrid = myListener.getCongestedTravelTimeAccessibilityGrid();
		SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid = myListener.getFreespeedTravelTimeAccessibilityGrid();
		SpatialGrid<Double> walkTravelTimeAccessibilityGrid = myListener.getWalkTravelTimeAccessibilityGrid();
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		Set<Geometry> geometries = new HashSet<Geometry>();
		
		TObjectDoubleHashMap<Geometry> travelTimeValues = new TObjectDoubleHashMap<Geometry>();
		TObjectDoubleHashMap<Geometry> travelCostValues = new TObjectDoubleHashMap<Geometry>();
		TObjectDoubleHashMap<Geometry> travelDistanceValues = new TObjectDoubleHashMap<Geometry>();
		
		for(Zone<ZoneAccessibilityObject> zone : startZones.getZones()) {
			geometries.add(zone.getGeometry());
			travelTimeValues.put(zone.getGeometry(), zone.getAttribute().getCongestedTravelTimeAccessibility());
			travelCostValues.put(zone.getGeometry(), zone.getAttribute().getFreespeedTravelTimeAccessibility());
			travelDistanceValues.put(zone.getGeometry(), zone.getAttribute().getWalkTravelTimeAccessibility());
		}
		
		// writing travel time accessibility kmz file
		writer.setColorizable(new MyColorizer(travelTimeValues));
		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_CONGESTED_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + congestedTravelTimeAccessibilityGrid.getResolution() + fileNameExtension + Constants.FILE_TYPE_KMZ);
		
		// writing travel cost accessibility kmz file
		writer.setColorizable(new MyColorizer(travelCostValues));
		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_FREESPEED_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + freespeedTravelTimeAccessibilityGrid.getResolution() + fileNameExtension + Constants.FILE_TYPE_KMZ);
		
		// writing travel distance accessibility kmz file
		writer.setColorizable(new MyColorizer(travelDistanceValues));
		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_WALK_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + walkTravelTimeAccessibilityGrid.getResolution() + fileNameExtension + Constants.FILE_TYPE_KMZ);
		
		log.info("Done with writing Google Erath files ...");
	}
}
