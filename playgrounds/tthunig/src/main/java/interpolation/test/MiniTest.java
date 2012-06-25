package interpolation.test;

import interpolation.Interpolation;
import interpolation_old.SpatialGrid4Interpolation;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.opengis.coverage.InterpolationMethod;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * Class for testing the implemented interpolation methods bilinear interpolation, bicubic spline interpolation and inverse distance weighting.
 * Uses small test scenarios like a 10*10 grid with a 100 as one element and 0 otherwise
 * or a 1*2 grid [100,0].
 * Tests interpolation of known and specific values and the range of interpolated values.
 * 
 * @author tthunig
 *
 */
public class MiniTest {
	
	private static final Logger logger = Logger.getLogger(SpatialGrid4Interpolation.class);	
	
	private SpatialGrid sg= null;
	private SpatialGrid interpolatedSG= null;
	private Interpolation interpolation= null;
	private boolean interpolationUseful= true;
	
	/**
	 * Creates a SpatialGrid with the given size for testing.
	 * The SpatialGrid has 100 as one element at (Xmin, Ymin) and 0 otherwise.
	 * 
	 * @param interpolationMethod
	 * @param numberOfRows
	 * @param numberOfColumns
	 */
	public MiniTest(int interpolationMethod, int numberOfRows, int numberOfColumns){
		//initialize the SpatialGrid
		double[] boundingBox = initBoundingBox(numberOfRows,numberOfColumns);
		double gridSizeInMeter = 1.;
		this.sg = new SpatialGrid(boundingBox, gridSizeInMeter);
		
		//set values
		for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()){
			for (double y = sg.getYmin(); y <= sg.getYmax(); y += sg.getResolution()){
				sg.setValue(0, x, y);
			}
		}
		sg.setValue(100, sg.getXmin(), sg.getYmin());
		
		System.out.println("SpatialGrid for testing:");
		printSG(this.sg);
		this.interpolation = new Interpolation(this.sg, interpolationMethod);
	}
	
	/**
	 * creates a bounding box for initializing the SpatialGrid
	 * 
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @return the bounding box
	 */
	private static double[] initBoundingBox(double numberOfRows, double numberOfColumns){
		double[] box = new double[4];
		box[0] = 0; // xmin
		box[1] = 0; // ymin
		box[2] = numberOfColumns-1; // xmax
		box[3] = numberOfRows-1; // ymax
			
		logger.info("Using bounding box with xmin=" + box[0] + ", ymin=" + box[1] + ", xmax=" + box[2] + ", ymax=" + box[3]);
		return box;
	}
	
	/**
	 * @return true if the interpolation test was successful, false otherwise
	 */
	public boolean isInterpolationUseful() {
		return interpolationUseful;
	}
	
	
	/**
	 * tests the interpolation at randomly chosen points, where values are known
	 * 
	 * @param numberOfTests the number of points to test
	 */
	private void testKnownValues(int numberOfTests){
		logger.info("Test known values...");
		
		boolean knownValueInterpUseful= true;
		for (int i=0; i<numberOfTests; i++){
			//create a random grid point (xCoord, yCoord)
			double xCoord= (Math.random()*(this.sg.getXmax()-this.sg.getXmin()+this.sg.getResolution()) + this.sg.getXmin());
			xCoord-= xCoord % this.sg.getResolution();
			double yCoord= (Math.random()*(this.sg.getYmax()-this.sg.getYmin()) + this.sg.getYmin());
			yCoord-= yCoord % this.sg.getResolution();
			
			knownValueInterpUseful= testSpecificValue(xCoord, yCoord, this.sg.getValue(xCoord, yCoord), this.interpolation.interpolate(xCoord, yCoord));
		}
		
		if(!knownValueInterpUseful){
			logger.warn("The tested known values are not interpolated useful!");
			this.interpolationUseful= false;
		}
	}
	
	/**
	 * tests the interpolation of a specific expected or known value
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param expectedValue the expected value at (x,y)
	 * @param interpValue the interpolated value at (x,y)
	 * @return true if the interpolated value is similar to the expected value; false otherwise
	 */
	private static boolean testSpecificValue(double x, double y, double expectedValue, double interpValue){
		logger.info("At coordinate ("+ x +", "+ y +") the expected value is "+ expectedValue +"; the interpolated value is "+ interpValue +".");
		
		boolean specificValueInterpUseful= true;
		if (interpValue > expectedValue + 1 || interpValue < expectedValue - 1) specificValueInterpUseful=false;
		return specificValueInterpUseful;
	}
	
	/**
	 * This method tests the interpolation at the specific point (Xmin, Ymin).
	 * If the interpolation method is bilinear, it tests additional the exact interpolation of two neighbored points of (Xmin, Ymin).
	 */
	private void testSpecificValues() {
		logger.info("Test specific values...");
		
		GeometryFactory factory = new GeometryFactory();
		Point peak = factory.createPoint(new Coordinate(this.sg.getXmin(), this.sg.getYmin()));
		boolean specificValueInterpUseful= testSpecificValue(peak.getX(), peak.getY(), this.sg.getValue(peak), this.interpolation.interpolate(peak.getX(), peak.getY()));
		
		if ((this.interpolation.getInterpolationMethod() == 0)
				&& (this.sg.getXmax() > this.sg.getXmin())){
			Point nearpeak = factory.createPoint(new Coordinate(this.sg.getXmin() + 0.5*this.sg.getResolution(), this.sg.getYmin()));
			specificValueInterpUseful= testSpecificValue(nearpeak.getX(), nearpeak.getY(), 50, this.interpolation.interpolate(nearpeak.getX(), nearpeak.getY())); 
			if (this.sg.getYmax() > this.sg.getYmin()){
				Point nearvalley = factory.createPoint(new Coordinate(this.sg.getXmin() + 0.5*this.sg.getResolution(), this.sg.getYmin() + 0.5*this.sg.getResolution()));
				specificValueInterpUseful= testSpecificValue(nearvalley.getX(), nearvalley.getY(), 25, this.interpolation.interpolate(nearvalley.getX(), nearvalley.getY()));
			}
		}
		
		if(!specificValueInterpUseful){
			logger.warn("The tested specific values are not interpolated useful!");
			this.interpolationUseful= false;
		}
	}	
	
	/**
	 * This method interpolates the SpatialGrid to one resolution higher and checks the range of all interpolated values.
	 */
	private void testRangeOfValues() {
		logger.info("Test range of values...");
		
		boolean rangeOfInterpValuesUseful= true;
		double min= Double.MAX_VALUE;
		double max= Double.MIN_VALUE;
		this.interpolatedSG = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		for (double x = this.sg.getXmin(); x <= this.sg.getXmax(); x += this.sg.getResolution()/2) {
			for (double y = this.sg.getYmin(); y <= this.sg.getYmax(); y += this.sg.getResolution()/2) {
				double interpValue= this.interpolation.interpolate(x, y);
				this.interpolatedSG.setValue(interpValue, x, y);
				
				switch (this.interpolation.getInterpolationMethod()){
				case 0:
				case 2: if (interpValue < 0 || interpValue > 100) rangeOfInterpValuesUseful= false;
						break;
				case 1: if (interpValue < -35 || interpValue > 135) rangeOfInterpValuesUseful= false;
				}
				
				if (interpValue < min) min= interpValue;
				if (interpValue > max) max= interpValue;
			}
		}
//		System.out.println("The tested values are:");
//		printSG(this.interpolatedSG);
		logger.info("The minimum of the tested interpolation values is " + min + "; the maximum is " + max + ".");
		if(!rangeOfInterpValuesUseful){
			logger.warn("The range of interpolated values is not useful!");
			this.interpolationUseful= false;
		}
	}
	
	
	/**
	 * Tests the three implemented interpolation methods bilinear interpolation, bicubic spline interpolation and inverse distance weighting at different test scenarios.
	 * 
	 * @param args, not used
	 */
	public static void main(String[] args) {		
		testBiLinear();
		testBiCubic();
		testIDW();
	}
	
	/**
	 * Starts different tests for bilinear interpolation.
	 */
	public static void testBiLinear(){
		System.out.println("");
		logger.info("-----Test of bilinear interpolation-----");
		
		interpolationTest(Interpolation.BILINEAR,1,2);
		interpolationTest(Interpolation.BILINEAR,3,3);
		interpolationTest(Interpolation.BILINEAR,4,4); 
		interpolationTest(Interpolation.BILINEAR,10,10);
	}
	
	/**
	 * Starts different tests for bicubic spline interpolation.
	 */
	public static void testBiCubic(){
		System.out.println("");
		logger.info("-----Test of bicubic spline interpolation-----");
		
		interpolationTest(Interpolation.BICUBIC,5,5);
		interpolationTest(Interpolation.BICUBIC,10,10);
	}
	
	/**
	 * Starts different tests for interpolation with the inverse distance weighting.
	 */
	public static void testIDW(){
		System.out.println("");
		logger.info("-----Test of the inverse distance weighting method for interpolation-----");
		
		interpolationTest(Interpolation.INVERSE_DISTANCE_WEIGHTING,3,3);
		interpolationTest(Interpolation.INVERSE_DISTANCE_WEIGHTING,10,10);
		
		
	}

	
	/**
	 * Tests the given interpolation method on a grid with the given size.
	 * The grid contains a 100 as one element at (Xmin, Ymin) and 0 otherwise.
	 * The method tests first whether randomly chosen known values and some specific values are interpolated correctly,
	 * then the range of some interpolated points between known values is checked.
	 * 
	 * @param interpolationMethod the interpolation method which should be tested
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @return true if the interpolation test was successful; false otherwise
	 */
	static void interpolationTest(int interpolationMethod, int numberOfRows, int numberOfColumns){
		System.out.println("");
		switch (interpolationMethod){
		case 0: logger.info("Interpolation test for bilinear interpolation on a "+ numberOfRows +"*"+ numberOfColumns +" grid..."); break;
		case 1: logger.info("Interpolation test for bicubic spline interpolation on a "+ numberOfRows +"*"+ numberOfColumns +" grid..."); break;
		case 2: logger.info("Interpolation test for inverse distance weigthing on a " + numberOfRows +"*"+ numberOfColumns +" grid...");
		}
		
		MiniTest testScenario= new MiniTest(interpolationMethod, numberOfRows, numberOfColumns);
		
		testScenario.testKnownValues(3);
		testScenario.testSpecificValues();
		testScenario.testRangeOfValues();
		
		if(!testScenario.isInterpolationUseful()){
			switch (interpolationMethod){
			case 0: logger.warn("The bilinear interpolation test on a "+ numberOfRows +"*"+ numberOfColumns +" grid was not successful!"); break;
			case 1: logger.warn("The bicubic spline interpolation test on a "+ numberOfRows +"*"+ numberOfColumns +" grid was not successful!"); break;
			case 2: logger.warn("The interpolation test for the inverse distance weighting method on a "+ numberOfRows +"*"+ numberOfColumns +" grid was not successful!");
			}
		}else
		switch (interpolationMethod){
		case 0: logger.info("The bilinear interpolation test on a "+ numberOfRows +"*"+ numberOfColumns +" grid was successful!"); break;
		case 1: logger.info("The bicubic spline interpolation test on a "+ numberOfRows +"*"+ numberOfColumns +" grid was successful!"); break;
		case 2: logger.info("The interpolation test for the inverse distance weighting method on a "+ numberOfRows +"*"+ numberOfColumns +" grid was successful!");
		}
	}
	

	private static void printSG(SpatialGrid sg){
				
		for (double y = sg.getYmax(); y >= sg.getYmin(); y -= sg.getResolution()) {
			System.out.print(y + "\t | \t");
			for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()){
				System.out.print(Math.round(sg.getValue(x, y)*100)/100 + "\t");
			}
			System.out.println("");
		}
		
		for (int i=0; i<sg.getNumCols(0)+3; i++){
			System.out.print("------");
		}
		System.out.print("\n\t | \t");
		for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()){
			System.out.print(x + "\t");
		}
		System.out.println("");
	}

}
