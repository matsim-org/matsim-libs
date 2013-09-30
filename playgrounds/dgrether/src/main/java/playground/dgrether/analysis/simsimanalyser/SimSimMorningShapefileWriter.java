/* *********************************************************************** *
 * project: org.matsim.*
 * ShapefileWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.simsimanalyser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.CountSimComparison;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author tthunig
 *
 */
public class SimSimMorningShapefileWriter {

	private static final Logger log = Logger.getLogger(SimSimMorningShapefileWriter.class);
	private Network network;
	private CoordinateReferenceSystem crs;
	
	// time period to analyze (morning peak = between 5 a.m. and 10 a.m.)
	private int firstHour = 5;
	private int lastHour = 10;
	private int numberOfHours = lastHour-firstHour;
					
	
	public SimSimMorningShapefileWriter(Network network, CoordinateReferenceSystem crs) {
		this.network = network; 
		this.crs = crs;
	}

	public void writeShape(String outfile, Map<Id, List<CountSimComparison>> countSimLinkLeaveCompMap){
		this.writeShape(outfile, countSimLinkLeaveCompMap, "sim", "count");
	}

	public void writeShape(String outfile, Map<Id, List<CountSimComparison>> countSimLinkLeaveCompMap, String runId, String runId2) {
		PolygonFeatureFactory factory = createFeatureType(this.crs, runId, runId2);
		GeometryFactory geofac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Link link : this.network.getLinks().values()) {
			features.add(this.createFeature(link, geofac, factory, countSimLinkLeaveCompMap.get(link.getId())));
		}
		ShapeFileWriter.writeGeometries(features, outfile);
	}

	
	private PolygonFeatureFactory createFeatureType(CoordinateReferenceSystem crs, String runId, String runId2) {
		PolygonFeatureFactory.Builder builder = new PolygonFeatureFactory.Builder();
		builder.setCrs(crs);
		builder.setName("link");
		builder.addAttribute("ID", String.class);
		builder.addAttribute("fromID", String.class);
		builder.addAttribute("toID", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("lanes", Double.class);
		builder.addAttribute("visWidth", Double.class);
		builder.addAttribute("type", String.class);
		String diff = "2-1"; //runId2+"-"+runId;
		// flow difference  for the morning peak
		for (int i = firstHour; i < lastHour; i++){
			builder.addAttribute("h" + (i + 1)+"_"+diff, Double.class);
		}
		builder.addAttribute("sum_"+diff, Double.class);
		// flow difference / capacity  for the morning peak
		for (int i = firstHour; i < lastHour; i++){
			builder.addAttribute("h" + (i + 1)+"/c_"+diff, Double.class);
		}
		builder.addAttribute("sum/c_"+diff, Double.class);
		// flow difference * link length  for the morning peak
		for (int i = firstHour; i < lastHour; i++){
			builder.addAttribute("h" + (i + 1)+"*l_"+diff, Double.class);
		}
		builder.addAttribute("sum*l_"+diff, Double.class);
		
		return builder.create();
	}
	
	private SimpleFeature createFeature(Link link, GeometryFactory geofac, PolygonFeatureFactory factory, List<CountSimComparison> countSimLinkLeaveComparisonList) {
		Coordinate[] coords = PolygonFeatureGenerator.createPolygonCoordsForLink(link, 20.0);
		Object [] attribs = new Object[8+3*(numberOfHours+1)+1];
		attribs[0] = link.getId().toString();
		attribs[1] = link.getFromNode().getId().toString();
		attribs[2] = link.getToNode().getId().toString();
		attribs[3] = link.getLength();
		attribs[4] = link.getFreespeed();
		attribs[5] = link.getCapacity();
		attribs[6] = link.getNumberOfLanes();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = ((LinkImpl) link).getType();
		int i = 9;
		
		double sumAbsLinkLeaveDif = 0.0;
		double absLinkLeaveDif = 0.0;
		for (CountSimComparison csc : countSimLinkLeaveComparisonList){			
			// analyse only the 
			if (csc.getHour() > firstHour && csc.getHour() <= lastHour){
				// csc contains the number of link leave events in the time period between csc.getHour()-1 and csc.getHour()
				absLinkLeaveDif = csc.getSimulationValue() - csc.getCountValue();
			
				attribs[i] = absLinkLeaveDif;
				attribs[i+numberOfHours+1] = (absLinkLeaveDif/link.getCapacity())*100; // in percent
				attribs[i+2*(numberOfHours+1)] = absLinkLeaveDif*link.getLength();
				
				sumAbsLinkLeaveDif += absLinkLeaveDif;
				i++;
			}
		}
		attribs[8+numberOfHours+1] = sumAbsLinkLeaveDif; // C: meanAbsLinkLeaveDif = sumAbsLinkLeaveDif/5
		// mean (error/capacity) in percent per hour, i.e flow difference of the morning peak divided by the maximal link capacity in this time period
		attribs[8+2*(numberOfHours+1)] = (sumAbsLinkLeaveDif/(link.getCapacity()*5))*100;
		attribs[8+3*(numberOfHours+1)] = sumAbsLinkLeaveDif*link.getLength(); // alternative: meanAbsLinkLeaveDif*link.getLength();
		
		return factory.createPolygon(coords, attribs, link.getId().toString());
	}


	
	
}

