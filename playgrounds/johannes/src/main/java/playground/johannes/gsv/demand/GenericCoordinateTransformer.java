/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.demand;

import org.geotools.referencing.CRS;
import org.matsim.contrib.common.gis.CRSUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.*;

/**
 * @author johannes
 *
 */
public class GenericCoordinateTransformer {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FactoryException 
	 */
	public static void main(String[] args) throws IOException, FactoryException {
		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.matched.xml"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.xml"));

		CoordinateReferenceSystem inCRS = CRSUtils.getCRS(Integer.parseInt("4326"));
		CoordinateReferenceSystem outCRS = CRSUtils.getCRS(Integer.parseInt("31467"));
		
		MathTransform transform = CRS.findMathTransform(inCRS, outCRS);
//		return CRSUtils.transformPoint(point, transform);
		
		String inLine = null;
		String outLine = null;

		while((inLine = reader.readLine()) != null) {
			int xStartPos = inLine.indexOf(" x=\"");
			int yStartPos = inLine.indexOf(" y=\"");
			
			
			if(xStartPos >= 0 &&  yStartPos >= 0) {
//				if(inLine.contains("id=\"106324567\"")){
//					System.err.println();
//				}
				xStartPos += 4;
				yStartPos += 4;
				
				int xEndPos = inLine.indexOf("\"", xStartPos);
				int yEndPos = inLine.indexOf("\"", yStartPos);
				
				if(xEndPos >= 0 && yEndPos >= 0) {
				String xStr = inLine.substring(xStartPos, xEndPos);
				String yStr = inLine.substring(yStartPos, yEndPos);
				
				double x = Double.parseDouble(xStr);
				double y = Double.parseDouble(yStr);
				
				double[] points = new double[] { x, y };
				try {
					transform.transform(points, 0, points, 0, 1);
				} catch (TransformException e) {
					e.printStackTrace();
				}
				
				outLine = inLine;
				outLine = outLine.replace(xStr, String.valueOf(points[0]));
				outLine = outLine.replace(yStr, String.valueOf(points[1]));
				}
			} else {
				outLine = inLine;
			}
			
			writer.write(outLine);
			writer.newLine();
		}
		
		reader.close();
		writer.close();
	}

}
