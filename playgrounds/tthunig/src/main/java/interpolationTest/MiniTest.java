package interpolationTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interpolation.Interpolation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

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
	
	private static final Logger logger = Logger.getLogger(MiniTest.class);	
	
	private SpatialGrid sg= null;
	private SpatialGrid interpolatedSG= null;
	private Interpolation interpolation= null;
	private boolean interpolationUseful= true;
	private double interpolationTime= Double.MAX_VALUE;
	
	private static String path = "Z:/WinHome/Docs/Interpolation/MiniTest/";
	private static File outputFile= new File(path + "Evaluation.txt");  //TODO in resources speichern
	private static FileWriter out;
	
	/**
	 * Creates a SpatialGrid with the given size for testing.
	 * The SpatialGrid has 100 as one element at (Xmin, Ymin) and 0 otherwise.
	 * 
	 * @param interpolationMethod
	 * @param numberOfRows
	 * @param numberOfColumns
	 * @param allNeighbors sets, whether the inverse distance weighting with all or four neighbors should be used. only necessary if interpolation method is inverse distance weighting.
	 * @param exponent the exponent for weights. only necessary if interpolation method is inverse distance weighting.
	 */
	public MiniTest(int interpolationMethod, int numberOfRows, int numberOfColumns, double exponent){
		
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
		long startTime= System.currentTimeMillis();
		this.interpolation = new Interpolation(this.sg, interpolationMethod, exponent);
		this.interpolateSG();
		this.interpolationTime= System.currentTimeMillis()-startTime;
		
		//Writing interpolated data
		System.out.println("interpolated SpatialGrid:");
		printSG(this.interpolatedSG);
		if (interpolationMethod == 2)
			this.interpolatedSG.writeToFile(path + interpolationMethod + "_exp" + exponent + "_" + numberOfRows + "x" + numberOfColumns + ".txt"); //TODO in resources speichern
		else
			this.interpolatedSG.writeToFile(path + interpolationMethod + "_" + numberOfRows + "x" + numberOfColumns + ".txt"); //TODO in resources speichern
			
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
	 * Method for preparing the interpolation test.
	 * Interpolates the SpatialGrid to one resolution higher.
	 */
	private void interpolateSG(){
		this.interpolatedSG = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2, Double.NaN);
		for (double x = this.sg.getXmin(); x <= this.sg.getXmax(); x += this.sg.getResolution()/2) {
			for (double y = this.sg.getYmin(); y <= this.sg.getYmax(); y += this.sg.getResolution()/2) {
				this.interpolatedSG.setValue(this.interpolation.interpolate(x, y), x, y);
			}
		}
	}
	
	/**
	 * Returns the time needed for interpolation of the current scenario with the used interpolation method.
	 * 
	 * @return the interpolation time
	 */
	public double getInterpolationTime(){
		return this.interpolationTime;
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
	 * This method checks the range of all interpolated values of the interpolated SpatialGrid at one resolution higher.
	 */
	private void testRangeOfValues() {
		logger.info("Test range of values...");
		
		boolean rangeOfInterpValuesUseful= true;
		double min= Double.MAX_VALUE;
		double max= Double.MIN_VALUE;
		for (double x = this.sg.getXmin(); x <= this.sg.getXmax(); x += this.sg.getResolution()/2) {
			for (double y = this.sg.getYmin(); y <= this.sg.getYmax(); y += this.sg.getResolution()/2) {
				double interpValue= this.interpolatedSG.getValue(x, y);
				
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
		logger.info("The minimum of the tested interpolation values is " + min + "; the maximum is " + max + ".");
		if(!rangeOfInterpValuesUseful){
			logger.warn("The range of interpolated values is not useful!");
			this.interpolationUseful= false;
		}
	}
	
	
	/**
	 * Tests the three implemented interpolation methods bilinear interpolation, bicubic spline interpolation and inverse distance weighting
	 * at different test scenarios and compares the needed calculation times.
	 * 
	 * @param args, not used
	 */
	public static void main(String[] args) {		
		try {
			out= new FileWriter(outputFile);
			out.write("Interpolation time comparison of the different methods:\n\n");
			out.write("interpolation method \t\t\t\t" + "1*2 grid \t" + "3*3 grid \t" + "10*10 grid\n");
			out.write("-----------------------------------------------------------------------------------------------------\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		testBiLinear();
		testBiCubic();
		testIDW();
		
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Starts different tests for bilinear interpolation.
	 */
	public static void testBiLinear(){
		System.out.println("");
		logger.info("-----Test of bilinear interpolation-----");
		
		try{
			out.write("bilinear interpolation \t\t\t\t");
		} catch (IOException e) {
			e.printStackTrace();
		}	
		interpolationTest(Interpolation.BILINEAR,1,2, Double.NaN);
		interpolationTest(Interpolation.BILINEAR,3,3, Double.NaN);
		interpolationTest(Interpolation.BILINEAR,10,10, Double.NaN);
	}
	
	/**
	 * Starts different tests for bicubic spline interpolation.
	 */
	public static void testBiCubic(){
		System.out.println("");
		logger.info("-----Test of bicubic spline interpolation-----");
		
		try{
			out.write("\nbicubic spline interpolation \t\t  - \t\t");
		} catch (IOException e) {
			e.printStackTrace();
		}		
		interpolationTest(Interpolation.BICUBIC,3,3, Double.NaN);
		interpolationTest(Interpolation.BICUBIC,10,10, Double.NaN);
	}
	
	/**
	 * Starts different tests for interpolation with the inverse distance weighting.
	 */
	public static void testIDW(){
		System.out.println("");
		logger.info("-----Test of the inverse distance weighting method for interpolation-----");
		
		for (int exp=1; exp<=10; exp++){
			try{
				out.write("\nidw with four neighbors and exp " + exp + "\t");
			} catch (IOException e) {
				e.printStackTrace();
			}
			interpolationTest(Interpolation.INVERSE_DISTANCE_WEIGHTING,1,2, exp);
			interpolationTest(Interpolation.INVERSE_DISTANCE_WEIGHTING,3,3, exp);
			interpolationTest(Interpolation.INVERSE_DISTANCE_WEIGHTING,10,10, exp);
		}
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
	static void interpolationTest(int interpolationMethod, int numberOfRows, int numberOfColumns, double exp){
				
		System.out.println("");
		switch (interpolationMethod){
		case 0: logger.info("Interpolation test for bilinear interpolation on a "+ numberOfRows +"*"+ numberOfColumns +" grid..."); break;
		case 1: logger.info("Interpolation test for bicubic spline interpolation on a "+ numberOfRows +"*"+ numberOfColumns +" grid..."); break;
		case 2: logger.info("Interpolation test for inverse distance weigthing on a " + numberOfRows +"*"+ numberOfColumns +" grid with an exponent of " + exp + "...");
		}
		
		MiniTest testScenario= new MiniTest(interpolationMethod, numberOfRows, numberOfColumns, exp);
		
		testScenario.testKnownValues(3);
		testScenario.testSpecificValues();
		testScenario.testRangeOfValues();
		
		logger.info("Interpolation time: " + testScenario.getInterpolationTime() + " ms.");
		try{
			out.write(testScenario.getInterpolationTime() + " ms \t\t");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!testScenario.isInterpolationUseful()){
			switch (interpolationMethod){
			case 0: logger.warn("The bilinear interpolation test on a "+ numberOfRows +"*"+ numberOfColumns +" grid was not successful!"); break;
			case 1: logger.warn("The bicubic spline interpolation test on a "+ numberOfRows +"*"+ numberOfColumns +" grid was not successful!"); break;
			case 2: logger.warn("The interpolation test for the inverse distance weighting method on a "+ numberOfRows +"*"+ numberOfColumns +" grid with an exponent of " + exp + ", was not successful!");
			}
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
