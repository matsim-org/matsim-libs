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
package playground.jjoubert.projects.wb.tif;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.github.jaiimageio.impl.plugins.tiff.TIFFImageMetadata;

import mil.nga.tiff.FieldTagType;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.FileDirectoryEntry;
import mil.nga.tiff.ImageWindow;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;
import mil.nga.tiff.io.ByteReader;
import mil.nga.tiff.io.IOUtils;
import mil.nga.tiff.util.TiffConstants;
import playground.southafrica.utilities.Header;

/**
 * Class to read the TIF file provided by GeoTerraImage.
 * 
 * @author jwjoubert
 */
public class ReadGtiTiff {
	final private static Logger LOG = Logger.getLogger(ReadGtiTiff.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ReadGtiTiff.class.toString(), args);
		runTiff(args);
//		runGdal(args);
		Header.printFooter();
	}

	public static void runGdal(String[] args){
		String filename = args[0];

		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("tif");
		ImageReader reader = (ImageReader)readers.next();
		LOG.info("Ignoring metadata: " + reader.isIgnoringMetadata());
		File file = new File(filename);
		try {
			ImageInputStream iis = ImageIO.createImageInputStream(file);
			reader.setInput(iis, true, false);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		/* Get the metadata. */
		IIOMetadata metadata = null;
		try {
			metadata = reader.getImageMetadata(0);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		LOG.info("Metadata format names:");
		String[] sa = metadata.getMetadataFormatNames();
		for(String s : sa){
			LOG.info(s);
		}
		Node n = metadata.getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
		displayMetadata(n);


		BufferedImage bi = null;
		try {
			bi = ImageIO.read(new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}

		bi.flush();
	}

	public static void displayMetadata(Node root) {
		displayMetadata(root, 0);
	}

	static void indent(int level) {
		for (int i = 0; i < level; i++) {
			System.out.print("  ");
		}
	} 

	static void displayMetadata(Node node, int level) {
		indent(level); // emit open tag
		System.out.print("<" + node.getNodeName());
		NamedNodeMap map = node.getAttributes();
		if (map != null) { // print attribute values
			int length = map.getLength();
			for (int i = 0; i < length; i++) {
				Node attr = map.item(i);
				System.out.print(" " + attr.getNodeName() +
						"=\"" + attr.getNodeValue() + "\"");
			}
		}

		Node child = node.getFirstChild();
		if (child != null) {
			System.out.println(">"); // close current tag
			while (child != null) { // emit child tags recursively
				displayMetadata(child, level + 1);
				child = child.getNextSibling();
			}
			indent(level); // emit close tag
			System.out.println("</" + node.getNodeName() + ">");
		} else {
			System.out.println("/>");
		}
	}


	public static void runTiff(String[] args){
		String tiffFilename = args[0];
		LOG.info("Reading TIFF file from " + tiffFilename);

		File tiffFile = new File(tiffFilename);
		TIFFImage tiffImage = null;
		try {
			tiffImage = TiffReader.readTiff(tiffFile, true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read TIFF image " + tiffFile);
		}
		LOG.info("Done reading file.");
		
		for(FileDirectory fileDirectory : tiffImage.getFileDirectories()){
			FileDirectoryEntry entry = fileDirectory.get(FieldTagType.GeoKeyDirectory);
			if(entry != null){
				LOG.info("Found the GeoKeyDirectory");
				if(entry.getValues() instanceof ArrayList<?>){
					ArrayList<?> list = (ArrayList<?>)entry.getValues();
					Object o = list.get(0);
				}
			}
		}
		

		FileDirectory fd = tiffImage.getFileDirectories().get(0);
		ImageWindow iw = new ImageWindow(fd);
		LOG.info("Min x: " + iw.getMinX());
		LOG.info("Max x: " + iw.getMaxX());
		LOG.info("Min y: " + iw.getMinY());
		LOG.info("Max y: " + iw.getMaxY());
		LOG.info("Number of samples per pixel: " + fd.getSamplesPerPixel());
		Rasters rasters = fd.readRasters();
		fd.get(FieldTagType.ExifIFD);

		/* Figure out the geo coordinates of the image, or a pixel. */
		LOG.info("Resolution unit: " + fd.getResolutionUnit());		

		LOG.info("IsTiled(): " + fd.isTiled());
		Map<Short,Integer> map = new TreeMap<Short, Integer>();
		for(int x = 0; x < rasters.getWidth(); x++){
			for(int y = 0; y < rasters.getHeight(); y++){
				Number[] num = rasters.getPixel(x, y);
				if(num.length > 1){
					LOG.warn("Pixel value has length " + num.length);
				}
				if(num[0] instanceof Short){
					short s = (short)num[0];
					if(!map.containsKey(s)){
						map.put(s, 1);
					} else{
						int old = map.get(s);
						map.put(s, old+1);
					}
				} else{
					LOG.warn("Pixel does not have a value of type `short`, but " + num[0].getClass().toString());
				}

				/* Get the coordinates of the pixel. */
			}
		}
		LOG.info("Pixel values: value (number of instances)");
		for(short s : map.keySet()){
			LOG.info(String.format("  %3d (%d)", s, map.get(s)));
		}

	}
	
	private class GeoKeys{
		private final short keydirectoryVersion;
		private final short keyRevision;
		private final short minorRevision;
		private final short numberOfKeys;
		
		public GeoKeys(short directoryVersion, short keyRevision, 
				short minorRevision, short numberOfKeys) {
			this.keydirectoryVersion = directoryVersion;
			this.keyRevision = keyRevision;
			this.minorRevision = minorRevision;
			this.numberOfKeys = numberOfKeys;
		}
	}

}
