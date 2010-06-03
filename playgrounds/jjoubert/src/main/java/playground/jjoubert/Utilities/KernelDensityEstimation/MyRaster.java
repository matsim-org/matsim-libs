/* *********************************************************************** *
 * project: org.matsim.*
 * MyRaster.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.jjoubert.Utilities.KernelDensityEstimation;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class used to generate a raster image (with square pixels) that represents the 
 * Kernel Density Estimate (KDE) of 2-dimensional points.
 * 
 * @author johanwjoubert
 */
public class MyRaster{
	private final Logger log = Logger.getLogger(MyRaster.class);
	private Geometry envelope = null;
	private BufferedImage bufferedImage = null;
	private SparseDoubleMatrix2D imageMatrix = null;
	private Color color = null;
	private double resolution = 0.0; 
	private Double radius;
	/*==========================================================================
	 * The following Kernel Density Estimate function types are available:		|
	 * 	0 - Only the pixel in which the point occurs.							|
	 *  1 - Uniform function. A radius must be given.							|
	 *  2 - Triangular function.												|
	 *  																		|
	 *  For all but the `0' case a value for the radius is required. 			|
	 *========================================================================*/
	private int KdeType;
	
	private Double originX = null;
	private Double originY = null;
	private double maxValue = 0;
	
	/**
	 * Constructs an instance of the raster-generating class.
	 * @param polygon the <i>com.vividsolutions.jts.geom</i> polygon representing 
	 * 		  the study area for which the raster should be created. It is assumed
	 * 		  that the polygon's coordinate system is in WGS84-UTM so that the unit 
	 * 		  of measure is meters. 
	 * @param stride the number of units (meters, if WGS84-UTM reference system is
	 * 		  used) that each raster pixel will consider, i.e. the resolution of 
	 * 		  the raster image.
	 * @param radius expressed in meters (if WGS84-UTM reference system is used) 
	 * 		  within which the Kernel Density Estimate function will increase the 
	 * 		  activity density. 
	 * @param KdeType a limited selection of Kernel Density Estimate (KDE) functions
	 * 		  that can be selected. This will increase over time.
	 * 		  // TODO implement other Kernel Density Estimate functions. 
	 * @param color the <i>java.awt.Color</i> of the raster image. The final image
	 * 		  will have this colour in various levels of transparency.
	 */
	public MyRaster(Polygon polygon, double resolution, Double radius, int KdeType, Color color) {
		this.resolution = resolution;
		/*
		 * The polygon provided uses a xy-coordinate system in format shown in (a)
		 * 
		 *     y									0------> x
		 *     ^									|
		 *     |									|
		 *     |									v
		 *     0------> x							y
		 *     
		 *        (a)									(b)
		 *      
		 * while the raster works in the xy-coordinate system shown in (b). So 
		 * when creating the raster, we should ensure we convert the envelope
		 * of the polygon correctly into the raster's extent. The raster's 
		 * origin will always be (0,0).
		 */
		envelope = polygon.getEnvelope();
		originX = envelope.getCoordinates()[3].x;
		originY = envelope.getCoordinates()[3].y;
		this.radius = radius;
		this.KdeType = KdeType;
		this.color = color;
		
		Coordinate[] c = polygon.getEnvelope().getCoordinates();
		if((c[2].x - c[0].x)/this.resolution < 1 || (c[2].y - c[0].y)/this.resolution < 1){
			log.warn("Adjust stride! The envelope of the area fits within a single pixel.");
		} else{
			int numberOfPixelsX = (int) Math.ceil((c[2].x - c[0].x)/this.resolution);
			int numberOfPixelsY = (int) Math.ceil((c[2].y - c[0].y)/this.resolution);

			bufferedImage = new BufferedImage(numberOfPixelsX,numberOfPixelsY,BufferedImage.TYPE_INT_ARGB);
			setInitialColor(this.color);

			imageMatrix = new SparseDoubleMatrix2D(numberOfPixelsX, numberOfPixelsY);			
		}	
	}
	
	private void setInitialColor(Color color){
		for (int i = 0; i < bufferedImage.getWidth(); i++){
			for (int j = 0; j < bufferedImage.getHeight(); j++) {
				Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
				bufferedImage.setRGB(i, j, c.getRGB());
			}
		}
	}
	
	/**
	 * Writes the object's <i>BufferedImage</i> as an image. It is suggested that
	 * only image types that can handle transparency, such as *.png, be used since
	 * the raster image is essentially only calculated using various alpha-channel
	 * values, i.e. using transparency.
	 * @param filename the absolute path of the output file.
	 * @param filetype the file type, for example "png", or "jpg".
	 * @return
	 */
	public boolean writeMyRasterToFile(String filename, String filetype){
		log.info("Writing raster image to " + filename);
		boolean b = false;
		File f1 = new File(filename);
		try {
			b = ImageIO.write(this.bufferedImage, filetype, f1);
		} catch (IOException e1) {
			e1.printStackTrace();
		}		
		if(b){
			log.info("   ... file written sucessfully.");
		} else
			log.warn("   ... file was unsucessfully written.");
		return b;
	}

	
	/**
	 * Just a dummy method to test the image reading capability of the 
	 * <i>java.awt</i> package.
	 * @return
	 */
	private BufferedImage readTriumphPicture(){
		File f2 = new File("/Users/johanwjoubert/Documents/Personal/Financing/Triumph/Pictures/S1.jpg");
		BufferedImage bi = null;
		try {
			bi = ImageIO.read(f2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bi;		
	}
	
	/**
	 * Returns the object's <i>BufferedImage</i>.
	 * @return
	 */
	public BufferedImage getBufferedImage(){
		return this.bufferedImage;
	}
	
	/**
	 * A method that iterates through the given list, calling 
	 * <i>processPoint(Point point)</i> for each.
	 * @param list of points to be processed
	 */
	public void processPoints(List<com.vividsolutions.jts.geom.Point> list){
		int counter = 0;
		int multiplier = 1;
		log.info("Processing list of points (total " + list.size() + "):");
		for (com.vividsolutions.jts.geom.Point point : list) {
			processPoint(point);
			if(++counter == multiplier){
				// Report progress.
				log.info("   ...points processed: " + counter);
				multiplier *= 2;
			}
		}
		log.info("   ...points processed: " + counter + " (Done)");
	}

	/**
	 * The method checks if the given <i>Point</i> falls within the envelope of
	 * the polygon, and only processes points within the envelope. At 
	 * instantiation of the <i>MyRaster</i> object the <i>KdeType</i> had to be
	 * specified - indicating how the point is to be processed. The following 
	 * Kernel Density Estimate functions are currently implemented:
	 * <ul>
	 * 		<b>0</b> - Only increase the value of the pixel within which the 
	 * 				   point falls. 
	 * </ul>
	 * @param point that is to be processed.
	 * @return
	 */
	public boolean processPoint(com.vividsolutions.jts.geom.Point point) {
		boolean result = false;
		if(envelope.contains(point)){
			/*
			 * Get raster coordinate of the point. 
			 */
			int x = (int) Math.floor((point.getX() - originX)/resolution);
			int y = (int) Math.floor((originY - point.getY())/resolution);

			double value = 0;
			Point p = null;
			Polygon pixel = null;
			GeometryFactory gf = new GeometryFactory();
			
			switch (this.KdeType) {
			case 0:
				value = 1.0;
				imageMatrix.setQuick(x, y, imageMatrix.getQuick(x, y) + 1);	
				maxValue = Math.max(maxValue,imageMatrix.getQuick(x, y));
				result = true;
				break;
				
			case 1: // Uniform
				int minX = (int) Math.max(0, Math.floor((point.getX() - radius - originX)/resolution));
				int maxX = (int) Math.min(imageMatrix.rows()-1, Math.floor((point.getX() + radius - originX)/resolution));
				int minY = (int) Math.max(0, Math.floor((originY - (point.getY() + radius))/resolution));
				int maxY = (int) Math.min(imageMatrix.columns()-1, Math.floor((originY - (point.getY() - radius))/resolution));
				value = 0.5;
				for(int i = minX; i <= maxX; i++){
					for(int j = minY; j <= maxY; j++){
						p = gf.createPoint(new Coordinate((i + 0.5)*resolution + originX, originY - (j + 0.5)*resolution));
						
						double x1 = originX + i*resolution;
						double x2 = originX + (i+1)*resolution;
						double y1 = originY - (j+1)*resolution;
						double y2 = originY - j*resolution;
						Coordinate c1 = new Coordinate(x1, y1);
						Coordinate c2 = new Coordinate(x2, y1);
						Coordinate c3 = new Coordinate(x2, y2);
						Coordinate c4 = new Coordinate(x1, y2);
						Coordinate [] c = {c1, c2, c3, c4, c1};
						pixel = gf.createPolygon(gf.createLinearRing(c), null);						
						
						double d = point.distance(p);
						if(d <= radius || pixel.contains(point)){
							imageMatrix.setQuick(i, j, imageMatrix.getQuick(i, j) + value);
							maxValue = Math.max(maxValue,imageMatrix.getQuick(i, j));
						}
					}
				}
				result = true;				
				break;
				
			case 2: // Triangular
				minX = (int) Math.max(0, Math.floor((point.getX() - radius - originX)/resolution));
				maxX = (int) Math.min(imageMatrix.rows()-1, Math.floor((point.getX() + radius - originX)/resolution));
				minY = (int) Math.max(0, Math.floor((originY - (point.getY() + radius))/resolution));
				maxY = (int) Math.min(imageMatrix.columns()-1, Math.floor((originY - (point.getY() - radius))/resolution));
				for(int i = minX; i <= maxX; i++){
					for(int j = minY; j <= maxY; j++){
						p = gf.createPoint(new Coordinate((i + 0.5)*resolution + originX, originY - (j + 0.5)*resolution));
						
						double x1 = originX + i*resolution;
						double x2 = originX + (i+1)*resolution;
						double y1 = originY - (j+1)*resolution;
						double y2 = originY - j*resolution;
						Coordinate c1 = new Coordinate(x1, y1);
						Coordinate c2 = new Coordinate(x2, y1);
						Coordinate c3 = new Coordinate(x2, y2);
						Coordinate c4 = new Coordinate(x1, y2);
						Coordinate [] c = {c1, c2, c3, c4, c1};
						pixel = gf.createPolygon(gf.createLinearRing(c), null);						
						
						double d = point.distance(p);
						double u = d / radius;
						if(pixel.contains(point)){
							value = 1.0;
						} else if( d <= radius){
							value = 1 - u;
						}
						imageMatrix.setQuick(i, j, imageMatrix.getQuick(i, j) + value);
						maxValue = Math.max(maxValue,imageMatrix.getQuick(i, j));
					}
				}
								
				result = true;
				break;

			case 3: // Triweight
				minX = (int) Math.max(0, Math.floor((point.getX() - radius - originX)/resolution));
				maxX = (int) Math.min(imageMatrix.rows()-1, Math.floor((point.getX() + radius - originX)/resolution));
				minY = (int) Math.max(0, Math.floor((originY - (point.getY() + radius))/resolution));
				maxY = (int) Math.min(imageMatrix.columns()-1, Math.floor((originY - (point.getY() - radius))/resolution));
				for(int i = minX; i <= maxX; i++){
					for(int j = minY; j <= maxY; j++){
						p = gf.createPoint(new Coordinate((i + 0.5)*resolution + originX, originY - (j + 0.5)*resolution));
						
						double x1 = originX + i*resolution;
						double x2 = originX + (i+1)*resolution;
						double y1 = originY - (j+1)*resolution;
						double y2 = originY - j*resolution;
						Coordinate c1 = new Coordinate(x1, y1);
						Coordinate c2 = new Coordinate(x2, y1);
						Coordinate c3 = new Coordinate(x2, y2);
						Coordinate c4 = new Coordinate(x1, y2);
						Coordinate [] c = {c1, c2, c3, c4, c1};
						pixel = gf.createPolygon(gf.createLinearRing(c), null);						
						
						double d = point.distance(p);
						double u = d / radius;
						if(pixel.contains(point)){
							value = 35.0 / 32.0;
						} else if( d <= radius){
							value = (35.0 / 32.0)*Math.pow(1 - Math.pow(u, 2), 3);
						}
						imageMatrix.setQuick(i, j, imageMatrix.getQuick(i, j) + value);
						maxValue = Math.max(maxValue,imageMatrix.getQuick(i, j));
					}
				}
								
				result = true;
				break;


			default:
				log.warn("A wrong Kernel Density Estimate function type was selected.");
				break;
			}
		}
		return result;
	}
	
	/**
	 * During processing the points, values were increased in an image matrix 
	 * of type <i>cern.colt.matrix.impl.DenseDoubleMatrix2D</i>. This method
	 * translates the image matrix to the object's <i>BufferedImage</i> in 
	 * such a way, using alpha-channel levels, that the most pixel(s) with the
	 * highest activity density is opaque (solid colour), and the pixel(s) with
	 * the lowest activity density is transparent.
	 */
	public void convertMatrixToRaster() {
		log.info("Converting image matrix to raster.");
		if(imageMatrix.rows() == bufferedImage.getWidth() && 
				imageMatrix.columns() == bufferedImage.getHeight() ){
			for (int x = 0; x < imageMatrix.rows(); x++) {
				for (int y = 0; y < imageMatrix.columns(); y++) {
//					if((int) Math.floor((imageMatrix.get(x, y) / maxValue)*255) > 0){
//						log.info("Found positive value.");
//					}
					Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.floor((imageMatrix.get(x, y) / maxValue)*255));
					bufferedImage.setRGB(x, y, c.getRGB());
				}
			}			
		} else {
			throw new RuntimeException("The imageMatrix and BufferedImage raster is not the same size.");
		}
		log.info("Done converting image matrix to raster.");
	}

	public double getMaxImageMatrixValue(){
		return this.maxValue;
	}
	
	public double getImageMatrixValue(int x, int y){
		return this.imageMatrix.get(x, y);
	}

}
