/* *********************************************************************** *
 * project: org.matsim.*
 * HomeLocationFilter.java
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

package playground.ikaddoura.analysis.shapes;

import org.apache.log4j.Logger;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.core.utils.misc.Time;

/**
 * @author ikaddoura
 *
 */
public class IKVideoShapeFileWriter {
	
	private final static Logger log = Logger.getLogger(IKVideoShapeFileWriter.class);

	private final String receiverPoints = "...";
	private final String outputPath = "...";

	public static void main(String[] args) {
		
		IKVideoShapeFileWriter main = new IKVideoShapeFileWriter();	
		main.run();		
	}
	
	private void run() {
		// read in receiver point coordinates
		// read in data
		// write out shape file
	}

	public static void writeNoiseImmissionStatsPerHourShapeFile(NoiseContext noiseContext, String fileName){
		
//		PointFeatureFactory factory = new PointFeatureFactory.Builder()
//		.setCrs(MGC.getCRS(noiseContext.getNoiseParams().getTransformationFactory()))
//		.setName("receiver point")
//		.addAttribute("Time", String.class)
//		.addAttribute("immissions", Double.class)
//		.create();
//		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
//		
//		for (ReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
//			for (Double timeInterval : rp.getTimeInterval2immission().keySet()) {
//				if (timeInterval <= 23 * 3600.) {
//					String dateTimeString = convertSeconds2dateTimeFormat(timeInterval);
//					double result = rp.getTimeInterval2immission().get(timeInterval);
//					
//					SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(rp.getCoord()), new Object[] {dateTimeString, result}, null);
//					features.add(feature);
//				}				
//			}
//		}
//		
//		ShapeFileWriter.writeGeometries(features, fileName);
//		log.info("Shape file written to " + fileName);
	}
	
	private static String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2000-01-01 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMMSS);
		String dateTimeString = date + time;
		return dateTimeString;
	}
}
