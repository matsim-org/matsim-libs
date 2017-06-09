/* *********************************************************************** *
 * project: org.matsim.*
 * ReaGtidTif.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.jjoubert.projects.wb.tiff;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import mil.nga.tiff.FieldTagType;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.FileDirectoryEntry;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;
import playground.southafrica.utilities.Header;

/**
 * Class to read the TIF file provided by GeoTerraImage.
 * 
 * @author jwjoubert
 */
public class GeoTiffReader {
	final private static Logger LOG = Logger.getLogger(GeoTiffReader.class);
	private GeoTiffImage gti;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GeoTiffReader.class.toString(), args);
		
		GeoTiffReader gtr = new GeoTiffReader();
		gtr.read(args[0]);
		gtr.convertImageToR(args[1]);
		
		Header.printFooter();
	}
	
	public GeoTiffReader() {
	}

	
	public void read(String geotiffFile){
		this.gti = new GeoTiffImage(new File(geotiffFile));
	}
	
	public GeoTiffImage getImage(){
		return this.gti;
	}
	
	private void convertImageToR(String file){
		this.gti.convertImageToR(file);
	}

	/**
	 * Returns the number of <i>useful</i> pixels. That is, pixels with
	 * values greater than zero. 
	 * @return
	 */
	public int getNumberOfPixels(){
		return this.getImage().getNumberOfUsefulPixels();
	}
	
	public int getWidth(){
		return this.gti.getWidth();
	}

	public int getHeight(){
		return this.gti.getHeight();
	}
	
	public GtiLandCover getLandCover(int x, int y){
		short s = this.gti.getValue(x, y);
		return GtiLandCover.parseLandcoverFromCode(s);
	}
	
	public Coord getPixelCentre(int x, int y){
		double[] da = this.gti.getPixelCentre(x, y);
		return CoordUtils.createCoord(da[0], da[1]);
	}
	
	/**
	 * Calculates the area for the pixel. It is assumed that the given 
	 * {@link CoordinateTransformation} will convert the pixel's coordinates
	 * from (the default) WGS84 to some projected coordinate reference system.
	 * <p></p>
	 * Consequently, the calculated area should be in square meters.
	 * @param x
	 * @param y
	 * @param ct
	 * @return
	 */
	public double calculatePixelAreaSqm(int x, int y, CoordinateTransformation ct){
		double area = 0.0;
		double[] da = this.gti.getPixelCoordArray(x, y);
		Coord bl = ct.transform(CoordUtils.createCoord(da[0], da[1]));
		Coord br = ct.transform(CoordUtils.createCoord(da[2], da[1]));
		Coord tr = ct.transform(CoordUtils.createCoord(da[2], da[3]));
		Coord tl = ct.transform(CoordUtils.createCoord(da[0], da[3]));
		Coordinate c1 = new Coordinate(bl.getX(), bl.getY());
		Coordinate c2 = new Coordinate(br.getX(), br.getY());
		Coordinate c3 = new Coordinate(tr.getX(), tr.getY());
		Coordinate c4 = new Coordinate(tl.getX(), tl.getY());
		Coordinate[]ca = {c1,c2,c3,c4,c1};
		Polygon polygon = new GeometryFactory().createPolygon(ca);
		area = polygon.getArea();
		
		return area;
	}
	
	
	class GeoTiffImage{
		private Double originLongitude;
		private Double originLatitude;
		private Double incrementLongitude;
		private Double incrementLatitude;
		private Map<Short, Color> colorMap;
		private TIFFImage image;
		private final Rasters rasters;
		
		public GeoTiffImage(File geoTiffFile) {
			this.image = null;
			try {
				this.image = TiffReader.readTiff(geoTiffFile);
			} catch (IOException e) {
				throw new RuntimeException("Cannot read GeoTIFF file " + geoTiffFile.getAbsolutePath());
			}  
			
			FileDirectoryEntry modelPixelScale = null;;
			FileDirectoryEntry modelTiePoint = null;
			FileDirectoryEntry colorMapEntry = null;
			for(FileDirectory fileDirectory: this.image.getFileDirectories()){ 
				FileDirectoryEntry geoKeyDirectory = fileDirectory.get(FieldTagType.GeoKeyDirectory); 
				if(geoKeyDirectory != null){ 
					// Parse the keys out of the values 
					// values[3] is the number of keys defined 
					// Similar to https://github.com/constantinius/geotiff.js/blob/master/src/geotiff.js parseGeoKeyDirectory method  
					// Parse those keys similar to https://github.com/constantinius/geotiff.js/blob/master/src/geotiffimage.js  
					// Parsing those keys would use information from these tags 
					modelPixelScale = fileDirectory.get(FieldTagType.ModelPixelScale); 
					modelTiePoint = fileDirectory.get(FieldTagType.ModelTiepoint); 
					colorMapEntry = fileDirectory.get(FieldTagType.ColorMap);
				LOG.info("Done reading field tags");
				}
			}
			
			/* Get the origin. Ignoring elevation. */
			Object o = modelTiePoint.getValues();
			if(o instanceof ArrayList<?>){
				ArrayList<?> mtp = (ArrayList<?>) o;
				
				Object oLon = mtp.get(3);
				if(oLon instanceof Double){
					originLongitude = (double) oLon;
				}
				Object oLat = mtp.get(4);
				if(oLat instanceof Double){
					originLatitude = (double) oLat;
				}
			} else{
				throw new IllegalArgumentException("Model tie points object is not of type ArrayList<?>");
			}
			
			/* Get the pixel increments. */
			Object oo = modelPixelScale.getValues();
			if(oo instanceof ArrayList<?>){
				ArrayList<?> mtp = (ArrayList<?>) oo;
				
				Object oIncLon = mtp.get(0);
				if(oIncLon instanceof Double){
					incrementLongitude = (double)oIncLon;
				}
				
				Object oIncLat = mtp.get(1);
				if(oIncLat instanceof Double){
					incrementLatitude = (double)oIncLat;
				}
			} else{
				throw new IllegalArgumentException("Model pixel scales object is not of type ArrayList<?>");
			}
			
			/* Get the field colors from the colormap. */
			colorMap = new TreeMap<>();
			double intensity = Math.pow(2, 16);
			Object ooo = colorMapEntry.getValues();
			if(ooo instanceof ArrayList<?>){
				ArrayList<?> cma = (ArrayList<?>) ooo;
				for(int i = 0; i < cma.size()/3; i++){
					int r = (int) Math.round(( Double.valueOf(cma.get(i).toString()) / intensity)*255);
					int g = (int) Math.round(( Double.valueOf(cma.get(i+(1*256)).toString()) / intensity)*255);
					int b = (int) Math.round(( Double.valueOf(cma.get(i+(2*256)).toString()) / intensity)*255);
					
					Color c = new Color(r, g, b);
					short index = (short) i;
					if(!colorMap.containsKey(index)){
						colorMap.put(index, c);
					}
				}
			}
			
			/* Get the rasters so that we need not duplicate code so much. */
			this.rasters = this.image.getFileDirectory(0).readRasters();

		}
		
		public Color getPixelColor(short value){
			Color c = null;
			if(!this.colorMap.containsKey(value)){
				LOG.error("There is no color specified for value '" + value + "'");
				LOG.error("Returning 'black'");
				c = new Color(0, 0, 0, 255);
			} else{
				c = this.colorMap.get(value);
			}
			
			return c;
		}
		
		/**
		 * Writes the pixel corners in a way, one point per line, that they
		 * can be visualised in R using the ggplot2 library as an overlay to
		 * a map.
		 * 
		 * @param filename
		 */
		private void convertImageToR(String filename){
			Counter counter = new Counter("  pixels # ");
			
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				bw.write("poly,lon,lat,value,r,g,b");
				bw.newLine();
				
				/* Process each pixel. */
				int index = 0;
				for(int x = 0; x < rasters.getWidth(); x++){
					for(int y = 0; y < rasters.getHeight(); y++){
						short s = getValue(x, y);
						
						/* Only consider pixels with a useful, non-zero value. */
						if(s > 0){
							/* Get the coordinates of the pixel. */
							double[] da = getPixelCoordArray(x, y);
							Color c = getPixelColor(s);
							
							bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[0], da[1], s, c.getRed(), c.getGreen(), c.getBlue()));
							bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[2], da[1], s, c.getRed(), c.getGreen(), c.getBlue()));
							bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[2], da[3], s, c.getRed(), c.getGreen(), c.getBlue()));
							bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[0], da[3], s, c.getRed(), c.getGreen(), c.getBlue()));
							bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[0], da[1], s, c.getRed(), c.getGreen(), c.getBlue()));
						}
						
						counter.incCounter();
						index++;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + filename);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + filename);
				}
			}
			counter.printCounter();
		}
		
		/**
		 * Returns the coordinates of the bottom-left and top-right corners of 
		 * the pixel, in the format [xmin, ymin, xmax, ymax]. 
		 * @param x
		 * @param y
		 * @return
		 */
		private double[] getPixelCoordArray(int x, int y){
			double[] da = {
					this.originLongitude + x*this.incrementLongitude,
					this.originLatitude - (y+1)*this.incrementLatitude,
					this.originLongitude + (x+1)*this.incrementLongitude,
					this.originLatitude - y*this.incrementLatitude,
			};
			return da;
		}
		
		/**
		 * Returns the coordinate of the centre of the pixel in the format
		 * [x, y].
		 * @param x
		 * @param y
		 * @return
		 */
		private double[] getPixelCentre(int x, int y){
			double[] pixelBox = getPixelCoordArray(x, y);
			double xAvg = (pixelBox[0]+pixelBox[2])/2;
			double yAvg = (pixelBox[1]+pixelBox[3])/2;
			double[] da = {xAvg, yAvg};
			return da;
		}
		
		/**
		 * Returns the number of <i>useful</i> pixels. That is, pixels with
		 * values greater than zero. 
		 * @return
		 */
		private int getNumberOfUsefulPixels(){
			int usefulPixels = 0;
			for(int x = 0; x < rasters.getWidth(); x++){
				for(int y = 0; y < rasters.getHeight(); y++){
					short s = getValue(x, y);
					
					/* Only consider pixels with a useful, non-zero value. */
					if(s > 0){
						usefulPixels++;
					}
				}
			}
			LOG.info("Total number of pixels with non-zero values: " + usefulPixels);
			return usefulPixels;
		}
		
		
		private int getWidth(){
			return this.image.getFileDirectory(0).readRasters().getWidth();
		}

		
		private int getHeight(){
			return this.image.getFileDirectory(0).readRasters().getHeight();
		}
		
		private short getValue(int x, int y){
			Number[] num = rasters.getPixel(x, y);
			if(num.length > 1){
				LOG.warn("Pixel value has length " + num.length);
			}
			short s = 0;
			if(num[0] instanceof Short){
				s = (short)num[0];
			} else{
				LOG.warn("Pixel does not have a value of type `short`, but " + num[0].getClass().toString());
			}
			return s;
		}
	}
}
