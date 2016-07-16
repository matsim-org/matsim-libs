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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.CountSimComparison;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author dgrether
 *
 */
public class SimSimShapefileWriter {

	private static final Logger log = Logger.getLogger(SimSimShapefileWriter.class);
	private Network network;
	private CoordinateReferenceSystem crs;
	
	public SimSimShapefileWriter(Network network, CoordinateReferenceSystem crs) {
		this.network = network; 
		this.crs = crs;
	}

	public void writeShape(String outfile, Map<Id<Link>, List<CountSimComparison>> countSimCompMap){
		this.writeShape(outfile, countSimCompMap, "sim", "count");
	}

	public void writeShape(String outfile, Map<Id<Link>, List<CountSimComparison>> countSimCompMap,
			String runId, String runId2) {
		PolygonFeatureFactory factory = createFeatureType(this.crs, runId, runId2);
		GeometryFactory geofac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Link link : this.network.getLinks().values()) {
			features.add(this.createFeature(link, geofac, factory, countSimCompMap.get(link.getId())));
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
		String diff = runId2+"-"+runId;
		for (int i = 0; i < 24; i++){
			builder.addAttribute("h" + (i + 1)+"_"+diff, Double.class);
		}
		builder.addAttribute("24h_"+diff, Double.class);
		
		return builder.create();
	}
	
	private SimpleFeature createFeature(Link link, GeometryFactory geofac, PolygonFeatureFactory factory, List<CountSimComparison> countSimComparisonList) {
		Coordinate[] coords = PolygonFeatureGenerator.createPolygonCoordsForLink(link, 20.0);
		Object [] attribs = new Object[34];
		attribs[0] = link.getId().toString();
		attribs[1] = link.getFromNode().getId().toString();
		attribs[2] = link.getToNode().getId().toString();
		attribs[3] = link.getLength();
		attribs[4] = link.getFreespeed();
		attribs[5] = link.getCapacity();
		attribs[6] = link.getNumberOfLanes();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = NetworkUtils.getType(((Link) link));
		int i = 9;
		double sumRelativeError = 0.0;
		double sumRelErrorCount = 0.0;
		double relativeError = 0.0;
		for (CountSimComparison csc : countSimComparisonList){
			if (csc.getHour() != i - 8) throw new IllegalStateException("List not sorted correctly. csc.getHour() returns " +csc.getHour());
			if (csc.getCountValue() == 0.0) {
				relativeError = - csc.getSimulationValue();
			}
			if (csc.getSimulationValue() == 0.0) {
				relativeError = - csc.getCountValue();
			}
			if (csc.getSimulationValue() == 0.0 && csc.getCountValue() == 0.0) {
				relativeError = -1.0;
			}
			if (csc.getSimulationValue() != 0.0 && csc.getCountValue() != 0.0) {
				relativeError = csc.getSimulationValue() / csc.getCountValue();
			}
			attribs[i] = relativeError;
			if (relativeError > 0.0) {
				sumRelativeError += relativeError;
				sumRelErrorCount++;
			}
			i++;
		}
		attribs[33] = sumRelativeError / sumRelErrorCount;
		

		
		return factory.createPolygon(coords, attribs, link.getId().toString());
	}


	
	
}

