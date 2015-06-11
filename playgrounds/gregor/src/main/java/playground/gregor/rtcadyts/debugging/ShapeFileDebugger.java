/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.rtcadyts.debugging;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.rtcadyts.frames2counts.LinkInfo;
import playground.gregor.rtcadyts.io.SensorDataVehicle;

public class ShapeFileDebugger {
	
	private Collection<LinkInfo> lis;
	private String debugDir;

	public ShapeFileDebugger(Collection<LinkInfo> lis, String debugDir) {
		this.lis = lis;
		this.debugDir = debugDir;
	}
	
	public void run() {
		makeLinks();
		makeVeh();
	}

	private void makeVeh() {
		PointFeatureFactory pf = new PointFeatureFactory.Builder().setName("vehicle").setCrs(MGC.getCRS("EPSG:32632")).addAttribute("angle", Double.class).addAttribute("speed",Double.class).create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		int id = 0;
		for (LinkInfo li : this.lis){
			for (SensorDataVehicle veh : li.getVeh()){
				SimpleFeature ft = pf.createPoint(new Coordinate(veh.getX(),veh.getY()), new Object[]{veh.getAngle(),veh.getSpeed()},id+++"");
				features.add(ft);
				
			}
		}
		ShapeFileWriter.writeGeometries(features, this.debugDir+"/vehicles.shp");
	}

	private void makeLinks() {

		PolylineFeatureFactory pl = new PolylineFeatureFactory.Builder().setName("Link").setCrs(MGC.getCRS("EPSG:32632")).addAttribute("nr_veh",Integer.class).
				addAttribute("flow", Double.class).addAttribute("angle", Double.class).create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (LinkInfo li : this.lis){
			Link l = li.getLink();
			Coordinate[] coords = new Coordinate[]{MGC.coord2Coordinate(l.getFromNode().getCoord()),MGC.coord2Coordinate(l.getToNode().getCoord())};
			SimpleFeature ft = pl.createPolyline(coords, new Object[]{li.getVeh().size(),li.getFlow(),li.getAngle()}, li.getLink().getId().toString());
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, this.debugDir+"/links.shp");
		
	}

}
