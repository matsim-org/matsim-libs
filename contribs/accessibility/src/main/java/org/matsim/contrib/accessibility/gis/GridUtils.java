package org.matsim.contrib.accessibility.gis;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.CSVWriter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public final class GridUtils {

	// logger
	private static final Logger log = Logger.getLogger(GridUtils.class);

	/**
	 * 
	 * @param shapeFileName
	 * @return Geometry determines the scenario boundary for the accessibility measure
	 */
	public static Geometry getBoundary(String shapeFileName){

		try{
			// get boundaries of study area
			Set<SimpleFeature> featureSet = FeatureSHP.readFeatures(shapeFileName);
			// yyyy I find this quite terrible to have the reader hidden in here.  Now I cannot pass a shape file which
			// I may have gotten in some way, but need to first write it to file. kai, mar'14


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

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		return measuringPoints;
	}

	public static ActivityFacilitiesImpl createGridLayerByGridSizeByBoundingBoxV2( org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox box, double gridSize ) {
		return createGridLayerByGridSizeByBoundingBoxV2( box.getXMin(), box.getYMin(), box.getXMax(), box.getYMax(), gridSize) ;
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
	public static ActivityFacilitiesImpl createGridLayerByGridSizeByBoundingBoxV2(double minX, double minY, double maxX, double maxY, double gridSize) {

		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;

		//		ActivityFacilitiesImpl measuringPoints = new ActivityFacilitiesImpl("accessibility measuring points");
		ActivityFacilitiesImpl measuringPoints = (ActivityFacilitiesImpl) FacilitiesUtils.createActivityFacilities("accessibility measuring points");

		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = minX; x <maxX; x += gridSize) {

			for(double y = minY; y < maxY; y += gridSize) {

				// check first if cell centroid is within study area
				double centerX = x + (gridSize/2);
				double centerY = y + (gridSize/2);

				// check if x, y is within network boundary
				if (centerX <= maxX && centerX >= minX && 
						centerY <= maxY && centerY >= minY) {

					Coord center = new Coord(centerX, centerY);
					measuringPoints.createAndAddFacility(Id.create( setPoints , ActivityFacility.class), center);
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		return measuringPoints;
	}


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

	public static final String WEIGHT = "weight";

	public static final void writeSpatialGrids( List<SpatialGrid> spatialGrids, String path ) {
		// seems that we cannot re-use the accessibility write method since it plays games with the modes. kai, mar'14

		final CSVWriter writer = new CSVWriter( path ) ;

		writer.writeField("x");
		writer.writeField("y");
		for ( SpatialGrid spatialGrid : spatialGrids ) {
			writer.writeField( spatialGrid.getLabel() ) ; 
		}
		writer.writeNewLine();

		final SpatialGrid spatialGrid = spatialGrids.get(0) ;
		for ( double y = spatialGrid.getYmin() ; y <= spatialGrid.getYmax() ; y += spatialGrid.getResolution() ) {
			for ( double x = spatialGrid.getXmin() ; x <= spatialGrid.getXmax(); x+= spatialGrid.getResolution() ) {
				writer.writeField( x ) ;
				writer.writeField( y ) ;
				for ( SpatialGrid theSpatialGrid : spatialGrids ) {
					writer.writeField( theSpatialGrid.getValue(x,y) ) ;
				}
				writer.writeNewLine();
			}
			writer.writeNewLine() ; // gnuplot pm3d scanlines
		}
		writer.close();

	}


	public static void aggregateFacilitiesIntoSpatialGrid(ActivityFacilities facilities, SpatialGrid spatialGridSum, SpatialGrid spatialGridCnt,
			SpatialGrid spatialGridAv ) {
		for ( ActivityFacility fac : facilities.getFacilities().values() ) {
			Coord coord = fac.getCoord() ;

			final Object weight = fac.getCustomAttributes().get(WEIGHT);
			double value = 1 ;
			if ( weight != null ) {
				value = (Double) weight ;
			}
			//				double value = fac.getActivityOptions().get("h").getCapacity() ; // infinity if undefined!!!

			spatialGridSum.addToValue(value, coord) ;
			spatialGridCnt.addToValue(1., coord) ;
		}
		if ( spatialGridAv!=null ) {
			double[][] cntMatrix = spatialGridCnt.getMatrix();
			for ( int ii=0 ; ii<cntMatrix.length ; ii++ ) {
				//				log.warn("ii=" + ii );
				for ( int jj=0 ; jj<cntMatrix[ii].length ; jj++ ) {
					double cnt = cntMatrix[ii][jj];
					if ( cnt > 0. ) {
						//						log.warn("jj=" + jj );
						double sum = spatialGridSum.getMatrix()[ii][jj];
						spatialGridAv.getMatrix()[ii][jj] = sum/cnt ;
						log.warn("sum=" + sum + "; cnt=" + cnt + "; av=" + spatialGridAv.getMatrix()[ii][jj] ) ;
					}
				}
			}
		}
	}
}
