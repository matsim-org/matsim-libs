/* *********************************************************************** *
 * project: org.matsim.*
 * Buildings2DistrictsMapping.java
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
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class Buildings2DistrictsMapping {

	
	
	private final String districts;
	private final String buildings;
	
	private ArrayList<Feature> districtsFeatures;
	private QuadTree<Feature> buildingsFeatures;
	private Envelope envelope;
	private final String outputfile;

	public Buildings2DistrictsMapping(final String districts, final String buildings, final String outputFile) {
		this.districts = districts;
		this.buildings = buildings;
		this.outputfile = outputFile;
	}

	public void run() {
		
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
		
		
		FeatureSource fsBuildings;
		try {
			fsBuildings = ShapeFileReader.readDataFile(this.buildings);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		ArrayList<Feature> buildingsPolygons = getFeature(fsBuildings);
		this.buildingsFeatures = generateQuadTree(buildingsPolygons);
		
		mapPolygons();
		
		
	}
	
	private void mapPolygons() {
		
		CSVFileWriter writer = new CSVFileWriter(this.outputfile);
		writer.writeLine(new String[] {"DISTRICT","BUILDING"});
		for (Feature ft : this.districtsFeatures) {
			String districtID = ((Long) ft.getAttribute(1)).toString();
			Geometry geo = ft.getDefaultGeometry();
			ArrayList<Feature> buildings = getFeaturesWithin(ft);
			for (Feature building : buildings){
				String buildingId = ((Integer) building.getAttribute(1)).toString();
				writer.writeLine(new String[] {districtID,buildingId});
			}
			
		}
		
		writer.finish();
	}

	private ArrayList<Feature> getFeaturesWithin(final Feature ft) {
		ArrayList<Feature> ret = new ArrayList<Feature>();
		Envelope e = ft.getBounds();
		
		Geometry geo = ft.getDefaultGeometry();
		double catchRadius = Math.sqrt(Math.pow((e.getMaxX()-e.getMinX()),2)+Math.pow((e.getMaxY()-e.getMinY()),2));
		Collection<Feature> buildings = this.buildingsFeatures.get(geo.getCentroid().getX(), geo.getCentroid().getY(), catchRadius);
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
//			if (geo instanceof Polygon) {
//				ret.add((Polygon) geo);
//			} else if (geo instanceof MultiPolygon) {
//				MultiPolygon mp = (MultiPolygon) geo;
//				for (int i = 0; i < mp.getNumGeometries(); i++) {
//					ret.add((Polygon) mp.getGeometryN(i));
//				}
//				
//			} else {
//				throw new RuntimeException("Feature does not contain a  polygon!!");
//			}

		}
		
		return ret;
		
	}
	
	public static void main(final String [] args) {
		
		String districts = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/data/GIS/keluraha_region.shp";
		String buildings = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/data/population/Population_Padang.shp";
		String outputFile = "./output/mappings.csv";
		Buildings2DistrictsMapping mapper = new Buildings2DistrictsMapping(districts, buildings, outputFile);
		mapper.run();
	}
}
