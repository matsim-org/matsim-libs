package playground.tnicolai.matsim4opus.interpolation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.BivariateRealFunction;
import org.apache.commons.math.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math.analysis.interpolation.BivariateRealGridInterpolator;

/**
 * just for testing, please use Interpolation.java
 * 
 * interpolates data on a grid without SpatialGrid
 * 
 * interpolation methods:
 * 	bicubic spline interpolation from apache 
 * 	bilinear interpolation (own implementation)
 * 
 * @author tthunig
 *
 */
@Deprecated
public class Interpolate {
	
	static double[] x_coord;
	static double[] y_coord;
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String resolution= "6400.0";
		boolean printData= false; //should be false, with increasing resolution; otherwise the data will be printed to the console
		boolean plotData= false; //should be false, with increasing resolution; otherwise the data will be plotted
		int numberOfInterp= 1;
		
		bicubicSplineInterpolation(resolution, "java-versuch1-Bicubic", numberOfInterp, printData, plotData);
//		myBiLinearInterpolation(resolution, "java-versuch2-Bilinear_selbst", printData, plotData);
		
		System.out.println("\ndone");
	}
	
	/**
	 * reads the given data and interpolates it with bilinear interpolation (own implementation)
	 * writes the interpolated data with the original coordinates in a txt file
	 * 
	 * @param resolution the resolution of the data to interpolate
	 * @param directory the directory to write in the results
	 * @param printData should be false, with increasing resolution; otherwise the data will be printed to the console
	 * @param plotData should be false, with increasing resolution; otherwise the data will be plotted
	 */
	public static void myBiLinearInterpolation(String resolution, String directory, boolean printData, boolean plotData){
		System.out.println("interpolate file " + resolution);
		
		System.out.println("\nread data...");
		//read data and save matrix without coordinates
		double[][] values= read("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/results/" + resolution + "travel_time_accessibility.txt");
		 
		if (plotData == true){
			//draw original image
			DrawMatrix draw = new DrawMatrix(values[0].length,values.length,0,0,"original data "+resolution,-10,10);
			draw.draw(values);
		}
		if (printData == true){
			System.out.println("original data:");
			print(values);
		}
		
		System.out.println("\ninterpolate...");
		double[][] interp_values= MyBiLinearInterpolator.myBiLinearGridInterpolation(values);
		
		//write matrix with original coordinates
		double[][] coord= originalCoord(1);
		double[][] matrix= merge(interp_values, coord[0], coord[1]);
		write(matrix, "Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_interp" + ".txt");
				
		if (plotData == true){
			//draw interpolated image
			DrawMatrix draw2 = new DrawMatrix(matrix[1].length,matrix.length,0,0,"interpolated data "+resolution,-10,10);
			draw2.draw(matrix);
		}
		if (printData == true){
			System.out.println("interpolated data:");
			print(matrix);
		}
	}
	
	/**
	 * reads the given data and interpolates it with bicubic spline interpolation from apache (http://commons.apache.org)
	 * writes the interpolated data with the original coordinates in a txt file
	 * 
	 * @param resolution the resolution of the data to interpolate
	 * @param directory the directory to write in the results
	 * @param numberOfInterp the number of interpolations
	 * @param printData should be false, with increasing resolution; otherwise the data will be printed to the console
	 * @param plotData should be false, with increasing resolution; otherwise the data will be plotted
	 */
	public static void bicubicSplineInterpolation(String resolution, String directory, int numberOfInterp, boolean printData, boolean plotData){
		System.out.println("interpolate file " + resolution + " " + numberOfInterp + " times:");
		
		System.out.println("\nread data...");
		//read data and save matrix without coordinates
		double[][] values= read("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/results/" + resolution + "travel_time_accessibility.txt");
			
		//generate default coordinates
		double[] x= coord(values[0].length,1);
		double[] y= coord(values.length,1);
		
		if (plotData == true){
			//draw original image
			DrawMatrix draw = new DrawMatrix(values[0].length,values.length,0,0,"original data "+resolution,-10,10);
			draw.draw(values);
		}
		if (printData == true){
			System.out.println("original data:");
			print(values);
		}
		
		System.out.println("\ninterpolate...");
		try {
			BivariateRealGridInterpolator interpolator = new BicubicSplineInterpolator();
			BivariateRealFunction function = interpolator.interpolate(y, x, values);
				
			for (int i=1; i<=numberOfInterp; i++){
				System.out.println("analyse interpolation " + i);
				
				//generate new default coordinates for higher resolution
				double[] xnew= coord(coordLength(i,x.length), Math.pow(0.5, i));
				double[] ynew= coord(coordLength(i,y.length), Math.pow(0.5, i));
			
				//calculate new values for higher resolution
				double[][] valuesnew= new double[ynew.length][xnew.length];
				for (int k=0; k<valuesnew.length; k++){
					for (int l=0; l<valuesnew[0].length; l++){
						valuesnew[k][l]= function.value(ynew[k],xnew[l]);
					}
				}
				
//				//write matrix with own coordinates
//				double[][] matrix= merge(valuesnew, xnew, ynew);
//				write(matrix, "Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_interp_" + i + ".txt");
				
				//write matrix with original coordinates
				double[][] coord= originalCoord(i);
				double[][] matrix= merge(valuesnew, coord[0], coord[1]);
				write(matrix, "Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_interp_" + i + ".txt");
				
				if (plotData == true){
					//draw interpolated image
					DrawMatrix draw2 = new DrawMatrix(matrix[1].length,matrix.length,0,0,"interpolated data "+resolution,-10,10);
					draw2.draw(matrix);
				}
				if (printData == true){
					System.out.println("interpolated data:");
					print(matrix);
				}
			}
		} catch (MathException e) {
			e.printStackTrace();
		}
	}
	
//	/**
//	 * reads the given data and interpolates it with bilinear interpolation from oracle
//	 * writes the interpolated data with the original coordinates in a txt file
//	 * 
//	 * @param resolution the resolution of the data to interpolate
//	 * @param directory the directory to write in the results
//	 * @param numberOfInterp the number of interpolations
//	 * @param printData should be false, with increasing resolution; otherwise the data will be printed to the console
//	 * @param plotData should be false, with increasing resolution; otherwise the data will be plotted
//	 */
//	public static void bilinearInterpolation(String resolution, String directory, boolean printData, boolean plotData){
//		System.out.println("interpolate file " + resolution);
//		
//		System.out.println("\nread data...");
//		//read data and save matrix without coordinates
//		double[][] values= read("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/results/" + resolution + "travel_time_accessibility.txt");
//			
//		//generate default coordinates
//		double[] x= coord(values[0].length,1);
//		double[] y= coord(values.length,1);
//		
//		if (plotData == true){
//			//draw original image
//			DrawMatrix draw = new DrawMatrix(values[0].length,values.length,0,0,"original data "+resolution,-10,10);
//			draw.draw(values);
//		}
//		if (printData == true){
//			System.out.println("original data:");
//			print(values);
//		}
//		
//		System.out.println("\ninterpolate...");
//		InterpolationBilinear interpolator = new InterpolationBilinear(); //TODO import
//				
//		//generate new default coordinates for higher resolution
//		double[] xnew= coord(coordLength(1,x.length), 0.5);
//		double[] ynew= coord(coordLength(1,y.length), 0.5);
//			
//		//calculate new values for higher resolution
//		double[][] valuesnew= new double[ynew.length][xnew.length];
//		for (int k=0; k<valuesnew.length; k++){
//			for (int l=0; l<valuesnew[0].length; l++){
//				valuesnew[k][l]= interpolator.interpolate(values,ynew[k],xnew[l]); //TODO x und y tauschen?
//			}
//		}
//		
//		//write matrix with original coordinates
//		double[][] coord= originalCoord(1);
//		double[][] matrix= merge(valuesnew, coord[0], coord[1]);
//		write(matrix, "Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_interp" + ".txt");
//						
//		if (plotData == true){
//			//draw interpolated image
//			DrawMatrix draw2 = new DrawMatrix(matrix[1].length,matrix.length,0,0,"interpolated data "+resolution,-10,10);
//			draw2.draw(matrix);
//		}
//		if (printData == true){
//			System.out.println("interpolated data:");
//			print(matrix);
//		}
//	}
	
	/**
	 * reads data from the given file without coordinates (first row, first column)
	 * saves data in an array
	 * 
	 * @param file the filename
	 * @return array with the data without the coordinates
	 */
	static double[][] read(String file){
		ArrayList<String[]> lines= new ArrayList<String[]>();
		try {
			BufferedReader in= new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = in.readLine()) != null){
				lines.add(line.split("\t"));
			}
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//writes data in a double[][]
		double[][] values= parse(lines);
		
		//skip coordinates
		return skip(values);
	}
	
	/**
	 * writes a given data array in a file, separated by tabs
	 * 
	 * @param matrix the data to write out
	 * @param file the filename
	 */
	static void write(double[][] matrix, String file){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			
			for (int i=0; i<matrix.length; i++){
				for (int j=0; j<matrix[0].length; j++){
					if (i!=0 || j!=0) {
						out.write(String.valueOf(matrix[i][j]) + "\t");
					}
					else
						out.write("\t");
				}
				out.newLine();
			}
			
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * returns array with coordinates
	 * 
	 * @param a the data array
	 * @param x the x-coordinate vector
	 * @param y the y-coordinate vector
	 * @return merged array
	 */
	static double[][] merge(double[][] a, double[] x, double[] y){
		double[][] b= new double[a.length+1][a[0].length+1];
		for (int i=1; i<b.length; i++){
			b[i][0]= y[i-1];
			for (int j=1; j<b[0].length; j++){
				b[0][j]= x[j-1];
				b[i][j]= a[i-1][j-1];
			}
		}
		return b;
	}
	
	/**
	 * converts data from ArrayList<String[]> to double[][]
	 * 
	 * @param lines the ArrayList to convert
	 * @return converted ArrayList
	 */
	static double[][] parse(ArrayList<String[]> lines){
		String[][] linearray= lines.toArray(new String[1][]);
		double[][] values= new double[linearray.length][linearray[0].length];
		for (int i=0; i<values.length; i++){
			for (int j=0; j<values[0].length; j++){
				if(i!=0 || j!=0) //omit empty corner
					values[i][j]= Double.parseDouble(linearray[i][j]);
			}
		}
		return values;
	}
	
	/**
	 * calculates the length of the coordinate vector in interpolation depth i and initial length l
	 * 
	 * @param i the interpolation depth
	 * @param l the initial length of the coordinate vector
	 * @return length of the coordinate vector in interpolation depth i
	 */
	static int coordLength(int i, int l){
		int sum= 0;
		for (int j=0; j<i; j++){
			sum+= Math.pow(2, j);
		}
		return l * (int)Math.pow(2,i) - sum;
	}
	
	/**
	 * creates a coordinate vector with given length and step size
	 * 
	 * @param length
	 * @param step
	 * @return a coordinate vector (0, step, 2*step, ..., (length-1)*step)
	 */
	static double[] coord(int length, double step){
		double[] x= new double[length];
		for (int i=0; i<length; i++){
			x[i]= i*step; 
		}
		return x;
	}
	
	/**
	 * creates new coordinate vector for the interpolation depth from old coordinates
	 * 
	 * @param i the interpolation depth
	 * @return array with x-coordinates as first row and y-coordinates as second row. attention: x- and y-coordinates have usually not the same size
	 */
	static double[][] originalCoord(int i){
		double[][] new_coord= new double[2][];
		new_coord[0]= new double[coordLength(i,x_coord.length)];
		new_coord[1]= new double[coordLength(i,y_coord.length)];
		
		double x_step= x_coord[1]-x_coord[0];
		for (int k=0; k<new_coord[0].length; k++){
			new_coord[0][k]= x_coord[0] + Math.pow(0.5,i) * k * x_step;
		}
		double y_step= y_coord[1]-y_coord[0];
		for (int l=0; l<new_coord[1].length; l++){
			new_coord[1][l]= y_coord[0] + Math.pow(0.5,i) * l * y_step;
		}
		
		return new_coord;
	}
	
	/**
	 * skips coordinates (first row and first column) from the given array
	 * 
	 * @param a the array to skip
	 * @return array a without first row and column
	 */
	static double[][] skip(double[][] a){
		double[][] values= new double[a.length-1][a[1].length-1];
		x_coord= new double[values[0].length];
		y_coord= new double[values.length];
		for(int i=0; i<values.length; i++){
			y_coord[i]= a[i+1][0];
			for(int j=0; j<values[0].length; j++){
				x_coord[j]= a[0][j+1];
				values[i][j]= a[i+1][j+1];
			}
		}
		return values;
	}
	
	/**
	 * prints the given array to the console
	 * 
	 * @param a the array to print
	 */
	static void print(double[][] a){
		for (int i=0; i<a.length; i++){
			for (int j=0; j<a[0].length; j++){
				System.out.print(a[i][j] + ", "); 
			}
			System.out.println("");
		}
	}

}
