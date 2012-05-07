package interpolation;

import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.BivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math.analysis.interpolation.BivariateRealGridInterpolator;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

/**
 * interpolates data on a grid. uses SpatialGrid
 * 
 * interpolation methods:
 * 	bicubic spline interpolation from apache 
 * 	bilinear interpolation (own implementation)
 * 
 * @author tthunig
 *
 */
public class InterpolateSpatialGrid {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String resolution = "6400.0"; //bicubic schafft bis 400.0, bilinear bis 100.0 (100.0 kann R nicht mehr zeichnen)
		String directory= "java-versuch3-SpatialGrid";

		System.out.println("interpolate file " + resolution + ":");

		System.out.println("\nread data...");
		SpatialGrid sg = SpatialGrid
				.readFromFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/results/"
						+ resolution + "travel_time_accessibility.txt");

		// generate coordinate vector
		double[] x = coord(sg.getXmin(), sg.getXmax(), sg.getResolution());
		double[] y = coord(sg.getYmin(), sg.getYmax(), sg.getResolution());
		
		// generate new coordinates for higher resolution
		double[] x_new = coord(sg.getXmin(), sg.getXmax(), sg.getResolution() / 2);
		double[] y_new = coord(sg.getYmin(), sg.getYmax(), sg.getResolution() / 2);
		
//		SpatialGrid sg_bicubic= bicubicSplineInterpolation(sg, x, y, x_new, y_new);
//		SpatialGrid sg_bilinear= myBiLinearSplineInterpolation(sg, x, y, x_new, y_new);
//		SpatialGrid sg_allValuesIDW= myAllValuesIDW(sg, x, y, x_new, y_new);
		SpatialGrid sg_4NeighborsIDW= my4NeighborsIDW(sg, x, y, x_new, y_new);

//		sg_bicubic.writeToFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_bicubic.txt");
//		sg_bilinear.writeToFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_bilinear.txt");
//		sg_allValuesIDW.writeToFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_allValuesIDW.txt");
		sg_4NeighborsIDW.writeToFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_4NeighborsIDW.txt");
		
		System.out.println("\ndone");
	}

	/**
	 * interpolates the given data with bilinear interpolation (own implementation)
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param x the original x-coordinate vector
	 * @param y the original y-coordinate vector
	 * @param x_new the x-coordinate vector for higher resolution
	 * @param y_new the y-coordinate vector for higher resolution
	 * @return new SpatialGrid with higher resolution
	 */
	private static SpatialGrid myBiLinearSplineInterpolation(SpatialGrid sg, double[] x, double[] y, double[] x_new, double[] y_new) {
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		
		System.out.println("\ninterpolate...");
		// calculate new values for higher resolution
		for (int k = 0; k < y_new.length; k++) {
			for (int l = 0; l < x_new.length; l++) {
				sg_new.setValue(sg_new.getRow(y_new[k]), sg_new.getColumn(x_new[l]), MyBiLinearInterpolator.myBiLinearValueInterpolation(sg.getMatrix(), sg_new.getColumn(x_new[l])/2., sg_new.getRow(y_new[k])/2.));
			}
		}
		return sg_new;
		
//		//alternative 1
//		System.out.println("\ninterpolate...");
//		// calculate new values for higher resolution
//		for (int k = 0; k < y_new.length; k++) {
//			for (int l = 0; l < x_new.length; l++) {
//				sg_new.setValue(sg_new.getRow(y_new[k]), sg_new.getColumn(x_new[l]), MyBiLinearInterpolator.myBiLinearValueInterpolation(sg, x_new[l], y_new[k]));
//			}
//		}
//		return sg_new;		
		
//		//alternative 2
//		return MyBiLinearInterpolator.myBiLinearGridInterpolation(sg);
	}


	/**
	 * interpolates the given data with bicubic spline interpolation from apache (http://commons.apache.org)
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param x the original x-coordinate vector
	 * @param y the original y-coordinate vector
	 * @param x_new the x-coordinate vector for higher resolution
	 * @param y_new the y-coordinate vector for higher resolution
	 * @return new SpatialGrid with higher resolution
	 */
	private static SpatialGrid bicubicSplineInterpolation(SpatialGrid sg, double[] x, double[] y, double[] x_new, double[] y_new) {
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		
		double[] x_default= coord(0,x.length-1,1);
		double[] y_default= coord(0,y.length-1,1);
		
		System.out.println("\ninterpolate...");
		try {
			BivariateRealGridInterpolator interpolator = new BicubicSplineInterpolator();
			BivariateRealFunction func= interpolator.interpolate(y_default, x_default, sg.getMatrix()); //benoetigt default Koordinaten (0,1,2,...)

			// calculate new values for higher resolution
			for (int k = 0; k < y_new.length; k++) {
				for (int l = 0; l < x_new.length; l++) {
					sg_new.setValue(sg_new.getRow(y_new[k]), sg_new.getColumn(x_new[l]), func.value(k/2., l/2.));
				}
			}
		} catch (MathException e) {
			e.printStackTrace();
		}
		return sg_new;
	}
	
	/**
	 * interpolates the given data with inverse distance weighting (own implementation)
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param x the original x-coordinate vector
	 * @param y the original y-coordinate vector
	 * @param x_new the x-coordinate vector for higher resolution
	 * @param y_new the y-coordinate vector for higher resolution
	 * @return new SpatialGrid with higher resolution
	 */
	private static SpatialGrid myAllValuesIDW(SpatialGrid sg, double[] x, double[] y, double[] x_new, double[] y_new) {
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		
		System.out.println("\ninterpolate...");
		// calculate new values for higher resolution
		for (int k = 0; k < y_new.length; k++) {
			for (int l = 0; l < x_new.length; l++) {
				sg_new.setValue(sg_new.getRow(y_new[k]), sg_new.getColumn(x_new[l]), MyInverseDistanceWeighting.myAllValuesIDW(sg, x_new[l], y_new[k]));
			}
		}
		return sg_new;
	}
	
	/**
	 * interpolates the given data with inverse distance weighting (own implementation)
	 * considers only 4 neighboring values
	 * 
	 * @param sg the SpatialGrid to interpolate
	 * @param x the original x-coordinate vector
	 * @param y the original y-coordinate vector
	 * @param x_new the x-coordinate vector for higher resolution
	 * @param y_new the y-coordinate vector for higher resolution
	 * @return new SpatialGrid with higher resolution
	 */
	private static SpatialGrid my4NeighborsIDW(SpatialGrid sg, double[] x, double[] y, double[] x_new, double[] y_new) {
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		
		System.out.println("\ninterpolate...");
		// calculate new values for higher resolution
		for (int k = 0; k < y_new.length; k++) {
			for (int l = 0; l < x_new.length; l++) {
				sg_new.setValue(sg_new.getRow(y_new[k]), sg_new.getColumn(x_new[l]), MyInverseDistanceWeighting.my4NeighborsIDW(sg, x_new[l], y_new[k]));
			}
		}
		return sg_new;
	}

	/**
	 * creates a coordinate vector
	 * 
	 * @param min the minimum coordinate
	 * @param max the maximum coordinate
	 * @param resolution
	 * @return coordinate vector from min to max with the given resolution
	 */
	static double[] coord(double min, double max, double resolution) {
		double[] coord = new double[(int) ((max - min) / resolution) + 1];
		coord[0] = min;
		for (int i = 1; i < coord.length; i++) {
			coord[i] = min + i * resolution;
		}
		return coord;
	}

}
