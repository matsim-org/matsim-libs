/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonMapper.java
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

package playground.gregor.gis.polygonmapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;



public class PolygonMapper {
	
	public static final String SVN_ROOT = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/demand_generation/FINAL/revised_data/";

	private final List<Feature> zFts;

	private final List<Feature> kFts; 
	
	private static final String DELIMITER = "\t"; 
	
	BufferedWriter bw;
	
	public PolygonMapper(final FeatureSource zones, final FeatureSource kel, final String outFile) throws IOException {
		this.zFts = getFts(zones);
		this.kFts = getFts(kel);
		this.bw = new BufferedWriter(new FileWriter(new File(outFile)));
	}

	public void run() throws IOException {
		writeHeader();
		process();
		this.bw.close();
		
	}
	
	private void process() throws IOException{
		for (Feature ft : this.kFts) {
			Long id1 = (Long) ft.getAttribute(1);
			Long id2 = (Long) ft.getAttribute(2);
			String kel = (String) ft.getAttribute(3);
			String kec = (String) ft.getAttribute(4);
			this.bw.write(id1 + DELIMITER + id2 + DELIMITER + kel + DELIMITER + kec);
			List<Polygon> ps = new ArrayList<Polygon>();
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof Polygon) {
				ps.add((Polygon) geo);
			} else if (geo instanceof MultiPolygon) {
				for (int i = 0; i < geo.getNumGeometries(); i++) {
					ps.add((Polygon) geo.getGeometryN(i));
				}
			}
			
			handleKel(ps);
		}
	}

	private void handleKel(final List<Polygon> ps) throws IOException {

		for (Feature zFt : this.zFts) {
//			Long id = (Long) zFt.getAttribute(1);
			Polygon p = (Polygon) ((MultiPolygon) zFt.getDefaultGeometry()).getGeometryN(0);
			double overlap = 0;
			for (Polygon tmp : ps) {
				try {
					Geometry g = tmp.intersection(p);
//					Geometry g1 = p.union(tmp);
//					Geometry g2 = g1.difference(p);
//					double oA = tmp.getArea() - g2.getArea();
//					
//					overlap += oA/p.getArea();
				overlap += g.getArea()/p.getArea();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.bw.write(DELIMITER+overlap);
		}
		this.bw.write("\n");
		
	}

	private void writeHeader() throws IOException {
		this.bw.write("ID"+ DELIMITER + "ID" + DELIMITER + "KEL" + DELIMITER + "KEC");
		for (int i = 0; i < this.zFts.size(); i++) {
			Long id = (Long) this.zFts.get(i).getAttribute(1);
			this.bw.write(DELIMITER+id);
		}
		this.bw.write("\n");
	}

	private List<Feature> getFts(final FeatureSource fs) throws IOException {
		
		Iterator<Feature> it = fs.getFeatures().iterator();
		List<Feature> fc = new ArrayList<Feature>();
		while (it.hasNext()){
			Feature ft = it.next();
			fc.add(ft);
		}


		return fc;
	}


	public static void main(final String [] args) throws IOException {
		
		String trafficZones = SVN_ROOT + "traffic_data/INTERPLAN/trafficZones.shp";
		String keluraha = SVN_ROOT + "census_data/padang_council/keluraha_region_revised.shp";
		String outFile = "TZKellMapping.txt";
		
		
		FeatureSource fZones = ShapeFileReader.readDataFile(trafficZones);
		FeatureSource fKel = ShapeFileReader.readDataFile(keluraha);
		
		new PolygonMapper(fZones,fKel,outFile).run();
		
		
	}
	
	
	
}
