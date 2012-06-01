package interpolation_old;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class SpatialGrid4Interpolation {
	
	// logger 
	private static final Logger logger = Logger.getLogger(SpatialGrid4Interpolation.class);

	private SpatialGrid spatialGrid = null;
	private double gridSizeInMeter = 1.;
	private double[] boundingBox = null;
	
	public SpatialGrid4Interpolation() {
		this.boundingBox = initBoundingBox();
		this.spatialGrid = new SpatialGrid(this.boundingBox, gridSizeInMeter);
		initTestSpatialGrid(this.spatialGrid);
	}
	
	private double[] initBoundingBox(){
		double[] box = new double[4];
		box[0] = 0; // xmin
		box[1] = 0; // ymin
		box[2] = 6; // xmax
		box[3] = 8; // ymax
		
		logger.info("Using bounding box with xmin=" + box[0] + ", ymin=" + box[1] + ", xmax=" + box[2] + ", ymax=" + box[3]);
		return box;
	}
	
	private void initTestSpatialGrid(final SpatialGrid spatialGrid){
		
		int rows = spatialGrid.getNumRows();
		int columns = spatialGrid.getNumCols(0);
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < columns; col++){
				
				if(col == (columns / 2) && row == (rows / 2 - 1))
					spatialGrid.setValue(row, col, 100.);
				else
					spatialGrid.setValue(row, col, 1.);
			}
		}
	}
	
	public SpatialGrid getSpatialGrid(){
		return this.spatialGrid;
	}

	
	/**
	 * just for testing
	 * @param args
	 */
	public static void main(String args[]){
		
		SpatialGrid4Interpolation sg4i = new SpatialGrid4Interpolation();
		SpatialGrid sg = sg4i.getSpatialGrid();
		
		logger.info("The SpatialGrid looks like :");
			
		for(int row = 0; row < sg.getNumRows(); row++){
			for(int col = 0; col < sg.getNumCols(0); col++){
				System.out.print( sg.getValue(row, col) + " " );
			}
			System.out.println();
		}
		
		logger.info("These are the values for the correspondent coordinates ...");
		// coordinates
		GeometryFactory factory = new GeometryFactory();
		
		Point center = factory.createPoint(new Coordinate(4., 4.));
		Point corner = factory.createPoint(new Coordinate(0., 0.));
		
		logger.info("At coordinate x="+ center.getX() + " y="+ center.getY() + " the stored value is ="+ sg.getValue(center));
		logger.info("At coordinate x="+ corner.getX() + " y="+ corner.getY() + " the stored value is ="+ sg.getValue(corner));

//		double[][] test = sg.getMatrix();
		
//		SpatialGrid interp_sg= BiLinearInterpolator.biCubicGridInterpolation(sg);
//		
//		logger.info("The interpolated SpatialGrid looks like :");
//		
//		for(int row = 0; row < interp_sg.getNumRows(); row++){
//			for(int col = 0; col < interp_sg.getNumCols(0); col++){
//				System.out.print( Math.round(interp_sg.getValue(row, col)*100)/100. + "\t " );
//			}
//			System.out.println();
//		}
//		
//		Point nearcenter = factory.createPoint(new Coordinate(3.5,4.));
//		Point nearcenter2 = factory.createPoint(new Coordinate(4.,3.5));
//		Point nearcenter3 = factory.createPoint(new Coordinate(4.5,3.5));
//		Point farcenter = factory.createPoint(new Coordinate(2.3, 1.7));
//		logger.info("At coordinate x="+ nearcenter.getX() + " y="+ nearcenter.getY() + " the stored value is ="+ BiCubicInterpolator.biCubicInterpolation(sg, flip(sg.getMatrix()), nearcenter.getX(), nearcenter.getY()));
//		logger.info("At coordinate x="+ nearcenter2.getX() + " y="+ nearcenter2.getY() + " the stored value is ="+ BiCubicInterpolator.biCubicInterpolation(sg, flip(sg.getMatrix()), nearcenter2.getX(), nearcenter2.getY()));
//		logger.info("At coordinate x="+ nearcenter3.getX() + " y="+ nearcenter3.getY() + " the stored value is ="+ BiCubicInterpolator.biCubicInterpolation(sg, flip(sg.getMatrix()), nearcenter3.getX(), nearcenter3.getY()));
//		logger.info("At coordinate x="+ farcenter.getX() + " y="+ farcenter.getY() + " the stored value is ="+ BiCubicInterpolator.biCubicInterpolation(sg, flip(sg.getMatrix()), farcenter.getX(), farcenter.getY()));
		
		logger.info("...done");
	}
	
	/**
	 * just for testing 
	 * flips the given matrix horizontal
	 * 
	 * @param matrix
	 * @return the horizontal mirrored matrix
	 */
	@Deprecated
	private static double[][] flip(double[][] matrix) {
		double[][] flip= new double[matrix.length][matrix[0].length];
		for (int i=0; i<flip.length; i++){
			for (int j=0; j<flip[0].length; j++){
				flip[i][j]= matrix[matrix.length-1-i][j];
			}
		}
		return flip;
	}
}
