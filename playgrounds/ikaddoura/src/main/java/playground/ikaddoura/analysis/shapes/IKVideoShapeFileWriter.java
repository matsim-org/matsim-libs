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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

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
