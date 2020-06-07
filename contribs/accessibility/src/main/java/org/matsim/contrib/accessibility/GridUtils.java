package org.matsim.contrib.accessibility;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.opengis.feature.simple.SimpleFeature;

public final class GridUtils {
	// used from outside, e.g. in vsp playgrounds

	private static final Logger LOG = Logger.getLogger(GridUtils.class);

	/**
	 * @param shapeFileName
	 * @return Geometry determines the scenario boundary for the accessibility measure
	 */
	public static Geometry getBoundary( String shapeFileName ){

		try{
			// get boundaries of study area
			Set<SimpleFeature> featureSet = FeatureSHP.readFeatures(shapeFileName );
			// yyyy I find this quite terrible to have the reader hidden in here.  Now I cannot pass a shape file which
			// I may have gotten in some way, but need to first write it to file. kai, mar'14

			LOG.info("Extracting boundary of the shape file ...");
			Geometry boundary = (Geometry) featureSet.iterator().next().getDefaultGeometry();
			LOG.info("Done extracting boundary ...");

			if(featureSet.size() > 1){
				LOG.warn("The given shape file is not suitable for accessibility calculations.");
				LOG.warn("This means you have to provide a shape file that only contains the border of the study area without any further features, i.e. zones or fazes, are allowed!");
				LOG.warn("Replace the shape file in your UrbanSim configuration provided at \"travel_model_configuration/matsim4urbansim/controler_parameter/shape_file\"");
				LOG.warn("If the shape file contains features accessibilities will be computes for only feature, i.e. for one zone or faz.");
			}
			return boundary;
		} catch (NullPointerException npe){
			npe.printStackTrace();
		} catch (IOException io){
			io.printStackTrace();
			LOG.error("Geometry object containing the study area boundary shape is null !");
			System.exit(-1);
		}
		return null;
	}

	/**
	 * creates measuring points for accessibility computation
	 * @param boundary
	 * @param gridSize
	 * 
	 * @return ActivityFacilitiesImpl containing the coordinates for the measuring points 
	 */
	static ActivityFacilitiesImpl createGridLayerByGridSizeByShapeFileV2( Geometry boundary, double gridSize ) {
		LOG.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;

		GeometryFactory factory = new GeometryFactory();

		ActivityFacilitiesImpl measuringPoints = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("Accessibility measuring points");
		Envelope env = boundary.getEnvelopeInternal();

		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {

			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {

				// check first if cell centroid is within study area
				double centerX = x + (gridSize/2);
				double centerY = y + (gridSize/2);
				Point centroid = factory.createPoint(new Coordinate(centerX, centerY));

				if(boundary.contains(centroid)) {
					Coord center = new Coord(centerX, centerY);
					measuringPoints.createAndAddFacility(Id.create( setPoints , ActivityFacility.class), center);
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		LOG.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		LOG.info("Done with setting starting points!");
		return measuringPoints;
	}

	static ActivityFacilitiesImpl createGridLayerByGridSizeByBoundingBoxV2( BoundingBox box, int gridSize ) {
		return createGridLayerByGridSizeByBoundingBoxV2(box.getXMin(), box.getYMin(), box.getXMax(), box.getYMax(), gridSize);
	}

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
	public static ActivityFacilitiesImpl createGridLayerByGridSizeByBoundingBoxV2( double minX, double minY, double maxX, double maxY, int gridSize ) {
		LOG.info("Start creating measure points on a grid.");
		int skippedPoints = 0;
		int setPoints = 0;
		ActivityFacilitiesImpl measuringPoints = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("accessibility measuring points");

		for (double x = minX; x <maxX; x += gridSize) {
			for (double y = minY; y < maxY; y += gridSize) {
				double centerX = x + (gridSize/2);
				double centerY = y + (gridSize/2);

				if (centerX <= maxX && centerX >= minX && centerY <= maxY && centerY >= minY) {
					Coord center = new Coord(centerX, centerY);
					measuringPoints.createAndAddFacility(Id.create(setPoints, ActivityFacility.class), center);
					setPoints++;
				}
				else skippedPoints++;
			}
		}
		LOG.info("Created " + setPoints + " inside the boundary. " + skippedPoints + " lie outside.");
		LOG.info("Finished creating measure points on a grid.");
		return measuringPoints;
	}
	
	static ActivityFacilities createHexagonLayer( BoundingBox box, int maxDiameter_m ) {
		return createHexagonLayer(box.getXMin(), box.getYMin(), box.getXMax(), box.getYMax(), maxDiameter_m);
	}
	
	private static ActivityFacilities createHexagonLayer( double minX, double minY, double maxX, double maxY, int maxDiameter_m ) {
		LOG.info("Start creating measure points on a hexagon pattern.");
		double inRadius_m = Math.sqrt(3) / 4 * maxDiameter_m;
		int skippedPoints = 0;
		int setPoints = 0;
		ActivityFacilitiesImpl measuringPoints = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("accessibility measuring points");

		int columnNumber = 1;
		for (double x = minX; x < (maxX - 1./2 * maxDiameter_m); x += (3./4 * maxDiameter_m)) {
			for (double y = minY; y <= maxY; y += (2 * inRadius_m)) {

				double centerX = x;
				double centerY;
				if (columnNumber % 2 != 0) {
					centerY = y;
				} else {
					centerY = y + inRadius_m;
				}

				if (centerX <= maxX && centerX >= minX && centerY <= maxY && centerY >= minY) {
					Coord center = new Coord(centerX, centerY);
					measuringPoints.createAndAddFacility(Id.create(setPoints, ActivityFacility.class), center);
					setPoints++;
				}
				else skippedPoints++; 
			}
			columnNumber++;
		}
		LOG.info("Created " + setPoints + " inside the boundary. " + skippedPoints + " lie outside.");
		LOG.info("Finished creating measure points on a hexagon pattern.");
		return measuringPoints;
	}

	/**
	 * stores measured accessibilities in a file
	 * 
	 * @param grid SpatialGrid containing measured accessibilities
	 * @param fileName output file
	 */
	static void writeSpatialGridTable( SpatialGrid grid, String fileName ){

		LOG.info("Writing spatial grid table " + fileName + " ...");
		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
		try{
			sgTableWriter.write(grid, fileName);
			LOG.info("... done!");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}