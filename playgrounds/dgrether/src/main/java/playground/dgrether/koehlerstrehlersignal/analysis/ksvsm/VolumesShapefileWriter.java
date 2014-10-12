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
package playground.dgrether.koehlerstrehlersignal.analysis.ksvsm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class VolumesShapefileWriter{

	private static final Logger log = Logger.getLogger(VolumesShapefileWriter.class);
	private Network network;
	private CoordinateReferenceSystem crs;
	
	public VolumesShapefileWriter(Network network, CoordinateReferenceSystem crs) {
		this.network = network; 
		this.crs = crs;
	}

	public void writeShape(String outfile, Map<Id<Link>, Double> ks2010Volumes, Map<Id<Link>, Double> matsimVolumes, double scalingFactor){
		PolygonFeatureFactory factory = createFeatureType(this.crs);
		GeometryFactory geofac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (Link link : this.network.getLinks().values()) {
			double ks2010LinkVolume = 0;
			double matsimLinkVolume = 0;
			if (ks2010Volumes.containsKey(link.getId()))
				ks2010LinkVolume = ks2010Volumes.get(link.getId());
			if (matsimVolumes.containsKey(link.getId()))
				matsimLinkVolume = matsimVolumes.get(link.getId());
			
			features.add(this.createFeature(link, geofac, factory, ks2010LinkVolume, matsimLinkVolume, scalingFactor));
		}
		ShapeFileWriter.writeGeometries(features, outfile + ".shp");
	}
	
	private PolygonFeatureFactory createFeatureType(CoordinateReferenceSystem crs) {
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
		builder.addAttribute("ks2010Flow", Double.class);
		builder.addAttribute("scaledKsFl", Double.class);
		builder.addAttribute("matsimFlow", Double.class);
		builder.addAttribute("mats-scaKs", Double.class);
		return builder.create();
	}
	
	private SimpleFeature createFeature(Link link, GeometryFactory geofac, PolygonFeatureFactory factory, double ks2010Volume, double matsimVolume, double scalingFactor) {
		Coordinate[] coords = PolygonFeatureGenerator.createPolygonCoordsForLink(link, 20.0);
//		coords = new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())};
		
		double scaledKs2010Volume = ks2010Volume * (1+scalingFactor);
		
		Object [] attribs = new Object[13];
		attribs[0] = link.getId().toString();
		attribs[1] = link.getFromNode().getId().toString();
		attribs[2] = link.getToNode().getId().toString();
		attribs[3] = link.getLength();
		attribs[4] = link.getFreespeed();
		attribs[5] = link.getCapacity();
		attribs[6] = link.getNumberOfLanes();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = ((LinkImpl) link).getType();
		attribs[9] = ks2010Volume;
		attribs[10] = scaledKs2010Volume;
		attribs[11] = matsimVolume;
		attribs[12] = matsimVolume - scaledKs2010Volume;
			
		return factory.createPolygon(coords, attribs, link.getId().toString());
	}

	
	
}

