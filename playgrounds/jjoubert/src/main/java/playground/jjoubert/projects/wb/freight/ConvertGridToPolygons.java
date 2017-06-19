/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertGridToPolygons.java
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

/**
 * 
 */
package playground.jjoubert.projects.wb.freight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to read in a grid file and convert each centroid point into a 
 * sequence of points describing the polygon of the grid cell.
 *  
 * @author jwjoubert
 */
public class ConvertGridToPolygons {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertGridToPolygons.class.toString(), args);
		
		String gridFile = args[0];
		Double width = Double.parseDouble(args[1]);
		String polygonFile = args[2];
		
		BufferedReader br = IOUtils.getBufferedReader(gridFile);
		BufferedWriter bw = IOUtils.getBufferedWriter(polygonFile);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.HARTEBEESTHOEK94_LO29, TransformationFactory.WGS84);
		
		/* Keep track of those cells that have already been processed. */
		List<String> ids = new ArrayList<String>();
		Counter counter = new Counter("  edges # ");
		try{
			bw.write("id,seq,lon,lat");
			bw.newLine();
			
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				
				String fId = sa[0];
				if(!ids.contains(fId)){
					double x = Double.parseDouble(sa[1]);
					double y = Double.parseDouble(sa[2]);
					writePolygonNodes(bw, fId, x, y, width, ct);
					ids.add(fId);
				}
				
				String tId = sa[5];
				if(!ids.contains(tId)){
					double x = Double.parseDouble(sa[6]);
					double y = Double.parseDouble(sa[7]);
					writePolygonNodes(bw, tId, x, y, width, ct);
					ids.add(tId);
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read/write."); 
		} finally{
			try {
				br.close();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close file(s)");
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}
	
	private static void writePolygonNodes(BufferedWriter bw, String id, 
			double x, double y, double width, CoordinateTransformation ct) throws IOException{
		double w = 0.5*width;
		double h = Math.sqrt(3.0)/2.0 * w;
		
		Coord c1 = ct.transform(CoordUtils.createCoord(x-w, y));
		Coord c2 = ct.transform(CoordUtils.createCoord(x-0.5*w, y+h));
		Coord c3 = ct.transform(CoordUtils.createCoord(x+0.5*w, y+h));
		Coord c4 = ct.transform(CoordUtils.createCoord(x+w, y));
		Coord c5 = ct.transform(CoordUtils.createCoord(x+0.5*w, y-h));
		Coord c6 = ct.transform(CoordUtils.createCoord(x-0.5*w, y-h));
		
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 1, c1.getX(), c1.getY()));
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 2, c2.getX(), c2.getY()));
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 3, c3.getX(), c3.getY()));
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 4, c4.getX(), c4.getY()));
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 5, c5.getX(), c5.getY()));
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 6, c6.getX(), c6.getY()));
		bw.write(String.format("%s,%d,%.6f,%.6f\n", id, 7, c1.getX(), c1.getY()));
	}

}
