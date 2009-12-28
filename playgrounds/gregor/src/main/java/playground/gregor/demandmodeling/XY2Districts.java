/* *********************************************************************** *
 * project: org.matsim.*
 * XY2Districts.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.demandmodeling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class XY2Districts {

	public static final String ROOT = "/home/laemmel/arbeit/svn/vsp-svn/projects/";
	private final String activity;
	private final String districts;
	private final String output;
	private Envelope envelope;
	private ArrayList<Feature> districtsFeatures;
	private QuadTree<Feature> actFts;
	
	public XY2Districts(final String activity, final String districts, final String output) {
		this.activity = activity;
		this.districts = districts;
		this.output = output;
	}
	private void run() {
		FeatureSource fsDistricts;
		try {
			fsDistricts =  ShapeFileReader.readDataFile(this.districts);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			this.envelope = fsDistricts.getBounds();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	
		this.districtsFeatures = getFeature(fsDistricts);
		
		FeatureSource fsActivities;
		try {
			fsActivities = ShapeFileReader.readDataFile(this.activity);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		ArrayList<Feature> actFts = getFeature(fsActivities);
		this.actFts = generateQuadTree(actFts);
		
		mapActivities();
		
	}

	
	private void mapActivities() {
		CSVFileWriter writer = new CSVFileWriter(this.output);
		writer.writeLine(new String[] {"ID_HH", "STARTTIME", "NAME", "ENDTIME", "X_COORD", "Y_COORD", "MODE", "ID_KELURAHAN"});
		for (Feature ft : this.districtsFeatures) {
			Long districtID = ((Long) ft.getAttribute(1));
			ArrayList<Feature> acts = getFeaturesWithin(ft,districtID);
			for (Feature act : acts){
				Integer hhId = (Integer) act.getAttribute(1);
				Integer startt = (Integer) act.getAttribute(2);
				String name = (String) act.getAttribute(3);
				Integer endt = (Integer) act.getAttribute(4);
				String mode = (String) act.getAttribute(5);
				String x_c = act.getDefaultGeometry().getCoordinate().x +"";
				String y_c = act.getDefaultGeometry().getCoordinate().y +"";
				writer.writeLine(new String[] {hhId.toString(), startt.toString(), name, endt.toString(), mode,x_c, y_c, districtID.toString()});
			}
			
		}
		
		writer.finish();
		
	}
	
	
	private ArrayList<Feature> getFeaturesWithin(final Feature ft, final Long id) {
		ArrayList<Feature> ret = new ArrayList<Feature>();
		Envelope e = ft.getBounds();
		
		Geometry geo = ft.getDefaultGeometry();
		double catchRadius = Math.sqrt(Math.pow((e.getMaxX()-e.getMinX()),2)+Math.pow((e.getMaxY()-e.getMinY()),2));
		Collection<Feature> buildings = this.actFts.get(geo.getCentroid().getX(), geo.getCentroid().getY(), catchRadius);
		for (Feature ftb : buildings) {
			
			try {
				if (geo.contains(ftb.getDefaultGeometry().getCentroid())) {
					ret.add(ftb);
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
		}
		return ret;
	}
	
	
	private QuadTree<Feature> generateQuadTree(final ArrayList<Feature> features) {
		QuadTree<Feature> ret = new QuadTree<Feature>(this.envelope.getMinX(),this.envelope.getMinY(),this.envelope.getMaxX(),this.envelope.getMaxY());
		for (Feature ft : features) {
			Coordinate c = ft.getDefaultGeometry().getCentroid().getCoordinate();
			ret.put(c.x,c.y,ft);
		}
		return ret;
	}
	
	private ArrayList<Feature> getFeature(final FeatureSource fs) {
		
		ArrayList<Feature> ret = new ArrayList<Feature>();
		
		FeatureIterator it = null;
		try {
			it = fs.getFeatures().features();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (it.hasNext()) {
			Feature feature = it.next();
			ret.add(feature);
		}
		
		return ret;
		
	}
	public static void main(final String [] args) {
		String activity = ROOT + "LastMile/demand_generation/FINAL/data_input/activity_data/survey_2008/working-day.shp";
		String districts = ROOT + "LastMile/demand_generation/FINAL/revised_data/census_data/padang_council/keluraha_region_revised.shp";
		String output = ROOT + "LastMile/demand_generation/FINAL/revised_data/activity_data/survey_2008/activity_districts_mapping.csv";
		                       
		new XY2Districts(activity,districts,output).run();
	}

	
}
