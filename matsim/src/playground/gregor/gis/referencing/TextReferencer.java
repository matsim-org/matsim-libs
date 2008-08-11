/* *********************************************************************** *
 * project: org.matsim.*
 * TextReferenzer.java
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

package playground.gregor.gis.referencing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.ShapeFileWriter;

import playground.gregor.gis.referencing.CRN.CaseNode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class TextReferencer {



	private final Collection<Feature> referenced;
	private final Collection<Feature> classified;
	private final String unclassified;
	private final CRN crn;
	private FeatureType ftPoint;
	private final FeatureSource featureSource;
	private final GeometryFactory geofac;
	private final HashMap<String,Feature> others;
	private final Set<Integer> ids = new HashSet<Integer>();
	private final ArrayList<Feature> housholdsFt;
	private final HashMap<Integer,Household> households;
	private FeatureType ftHome;
	private ArrayList<String> homeRelated;
	
	private final  static GeotoolsTransformation GT = new GeotoolsTransformation("WGS84_UTM47S", "WGS84_UTM47S");
	
	public TextReferencer(final ArrayList<FeatureSource> fts, final FeatureSource others, final FeatureSource zonesl, final FeatureSource homelocations, final String unclassified) {
		this.featureSource = fts.get(0);
		final Collection<Feature> ft = new ArrayList<Feature>();
		for (final FeatureSource f : fts) {
			ft.addAll(getFeatures(f));
		}
		this.others = getOthers(others,3);
		this.others.putAll(getOthers(zonesl, 2));
//		this.others.putAll(getOthers(zonesl, 3));

		this.unclassified = unclassified;
		this.referenced = ft;
		this.geofac = new GeometryFactory();
		this.classified = new ArrayList<Feature>();
		this.households = new HashMap<Integer,Household>();
		this.housholdsFt = (ArrayList<Feature>) getFeatures(homelocations);
		initFeatureGenerator();
		final Collection<Feature> revRef = locateHousholds();
		ft.addAll(revRef);
		this.crn = new CRN(ft);
	}


	private HashMap<String, Feature> getOthers(final FeatureSource others2,final int idx) {
		final HashMap<String,Feature> features = new HashMap<String,Feature>();
		final Collection<Feature> fts = getFeatures(others2);
		for (final Feature ft : fts) {
			final String key = (String) ft.getAttribute(idx);
			features.put(key.toLowerCase(),ft);

		}
		return features;
	}

	private Collection<Feature> locateHousholds() {
		final Collection<Feature> revRef = new ArrayList<Feature>();
		final ArrayList<String> excludes = new ArrayList<String>();
		excludes.add("rawang");
		excludes.add("rumah");
		excludes.add("tetangga");
		excludes.add("dekat");
		excludes.add("sekitart");
		excludes.add("depan rumah");
		excludes.add("warung");
		excludes.add("di kelurahan");
		excludes.add("istirahat");
		
		this.homeRelated = excludes;
		final TextFileReader tfr = new TextFileReader(this.unclassified);
		String [] line = tfr.readLine();

		while (line != null) {
			int id;
			try{
				id = Integer.parseInt(line[1]);

			} catch(final Exception e) {
				e.printStackTrace();
				line = tfr.readLine();
				continue;
			}
			if (id < 0) {
				line = tfr.readLine();
				continue;				
			}


			final String activity = line[4];
			if (!activity.equals("home activity")){
				line = tfr.readLine();
				continue;				
			}
			final Feature ft = this.housholdsFt.get(id);
			final Household h = new Household();
			h.id = id;
			h.location = ft;
			this.households.put(id, h);
			final Activity act = getActivity(activity,line,ft);
			
//			h.acts.add(act);
			
			final String location = line[6];
			boolean addFt = true;
			for (final String exclude : excludes) {
				if (location.toLowerCase().contains(exclude)) {
					addFt = false;
					break;
				}
			}

			if (addFt) {
				try {
					final Feature ftN = this.ftHome.create(new Object [] {ft.getDefaultGeometry(),id,id,location,id}, "home locations");
					revRef.add(ftN);
				} catch (final IllegalAttributeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}


			line = tfr.readLine();
		}
		return revRef;
	}

	private Activity getActivity(final String activity, final String[] line, final Feature ft) {
		final Activity act = new Activity();
		act.location = ft;
		act.type = activity;
		act.day = line[2];
		double start = 0;
		double end = 0;
		 try {
			 String tmp1 = line[12];
			 tmp1 = tmp1.replace('.', ',');
			 if (tmp1.contains(",")){
				 final String [] splitted1 = tmp1.split(",");
				 start = Double.parseDouble(splitted1[0]) * 3600;
				 final double frac =  Double.parseDouble(splitted1[1]);
				 start += frac < 10 ? 600 * frac : 60 * frac;
			 } else {
				 start = Double.parseDouble(tmp1) * 3600;
			 }
			 			 
			 String tmp2 = line[13];
			 tmp2 = tmp2.replace('.', ',');
			 if (tmp2.contains(",")){
				 final String [] splitted2 = tmp2.split(",");
				 end = Double.parseDouble(splitted2[0]) * 3600;
				 final double frac =  Double.parseDouble(splitted2[1]);
				 end += frac < 10 ? 600 * frac : 60 * frac;
			 } else {
				 end = Double.parseDouble(tmp2) * 3600;
			 } 
		 } catch (final Exception e) {
//			 e.printStackTrace();
		 }
		
		act.start = start;
		act.end = end;
		return act;
	}


	private void classify() {



		final TextFileReader tfr = new TextFileReader(this.unclassified);
		final TextFileWriter rfw = new TextFileWriter("./padang/referencing/missing_locations.txt");

		String [] line = tfr.readLine();

		while (line != null) {


			int id;
			try {
				id = Integer.parseInt(line[1]);
			} catch (final NumberFormatException e1) {
				e1.printStackTrace();
				rfw.writeLine(line);	
				line = tfr.readLine();
				continue;		
			}

			if (this.households.get(id) == null) {
				createHousehold(id);
			}
			
			final Household hh = this.households.get(id);
			
			final String activity = line[4];
			final String location = line[6].toLowerCase();
			final String location2 = line[7].toLowerCase();
//			System.out.println(location2);
//			if (location2.equals("home or neighbourhood")){
//				System.out.println(location2);
//				
//			}
			
			Feature ft = null;
			if (activity.equals("home activity")){
				ft = classifyAsHome(id,line);
			} else if (isHomeRelated(location)|| location2.equals("home or neighbourhood")) {
				ft = classifyAsHome(id,line);
			} else {
				CaseNode resp = this.crn.getCase(location);
				if (resp == null || resp.getActivation() <= 0.96) {
					resp = this.crn.getCase("jalan " + location);
				}
				
				if (resp != null && resp.getActivation() > 0.96) {
					ft =getPointFeature(resp.getCoordinate(), line);
				} else if (resp == null) {
					for (final String str : this.others.keySet()) {
						if (location.contains(str)){
							ft = getPointFeature(this.others.get(str).getDefaultGeometry().getCoordinate(), line);
							break;
						}
					}
				}
			}
			if (ft != null) {
				
				final Activity act = getActivity(activity,line,ft);
				hh.acts.add(act);
				this.classified.add(ft);
				this.ids.add(id);
			} else {
				rfw.writeLine(line);			
			}
			line = tfr.readLine();

		}
		System.out.println(this.ids.size());
		rfw.finish();
		try {
			ShapeFileWriter.writeGeometries(this.classified, "./padang/referencing/survey.shp");
		} catch (final Exception e) {
			e.printStackTrace();
		} 

	}


	private void createHousehold(final int id) {
		
		if (id < 0) {
			return;
		}
		
		final Feature ft = this.housholdsFt.get(id);
		if (ft != null) {
			final Household hh = new Household();
			hh.id = id;
			hh.location = ft;
			this.households.put(id, hh);
		}
		
		
	}


	private boolean isHomeRelated(final String location) {
		for (final String str : this.homeRelated) {
			if (location.contains(str)) {
				return true;
			}
		}
		return false;
	}


	private Feature classifyAsHome(final int id, final String [] line) {
		if (id < 0) {
			return null;
		}
		final Household hh = this.households.get(id);
		if (hh == null) {
			System.err.println("Houshald does not exisist for:" + line[1]);
			return null;
		}
		return getPointFeature(hh.location.getDefaultGeometry().getCoordinate(),line);

	}


	private Feature getPointFeature(final Coordinate c, final String[] input) {

		final Object [] obj = new Object [input.length+1];
		obj[0] = this.geofac.createPoint(c);
		for (int i = 1; i < input.length+1; i++) {
			if (input[i-1].equals("")) {
				obj[i] = "0";
				continue;
			}

			switch (i) {
			case 1:
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
				obj[i] = input[i-1];
				break;
			case 2:	
				try {
					obj[i] = Integer.parseInt(input[i-1]);
				} catch (final NumberFormatException e) {
					obj[i] = 9999;
				}
				break;
			case 3:
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
				obj[i] = input[i-1];
				break;
			case 4:
				try {
					obj[i] = Integer.parseInt(input[i-1]);
				} catch (final NumberFormatException e) {
					obj[i] = -1;
				}
				break;
			case 5:
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
				obj[i] = input[i-1];
				break;
			case 9:
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
				obj[i] = input[i-1];
				break;
			case 10:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (final NumberFormatException e1) {
					obj[i] = -1.0;
				}
				break;
			case 11:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (final NumberFormatException e1) {
					obj[i] = -1.0;
				}
				break;
			case 12:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (final NumberFormatException e1) {
					obj[i] = -1.0;
				}
				break;
			case 13:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (final NumberFormatException e1) {
					obj[i] = -1.0;
				}
				break;
			case 14 :
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
				obj[i] = input[i-1];
				break;
			case 16 :
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
				obj[i] = input[i-1];
				break;
			case 17:
				try {
					obj[i] = Integer.parseInt(input[i-1]);
				} catch (final NumberFormatException e) {
					obj[i] = -1;
				}
				break;
			default:
				if (input[i-1].contains("NULL")){
					obj[i] = "null";
					break;
				}
			obj[i] = input[i-1];

			}


		}


		try {
			return this.ftPoint.create(obj,"dynamic exposure");
		} catch (final IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}


	private void initFeatureGenerator(){

		final AttributeType[] attrib = new AttributeType[20];
//		AttributeType polygon = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		attrib[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.featureSource.getSchema().getDefaultGeometry().getCoordinateSystem());
//		AttributeType linestring = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.featureSourcePolygon.getSchema().getDefaultGeometry().getCoordinateSystem());
		attrib[1]= AttributeTypeFactory.newAttributeType("ID", String.class);
		attrib[2] = AttributeTypeFactory.newAttributeType("QR15", Integer.class);
		attrib[3] = AttributeTypeFactory.newAttributeType("ACT", String.class);
		attrib[4] = AttributeTypeFactory.newAttributeType("NOACT", Integer.class);
		attrib[5] = AttributeTypeFactory.newAttributeType("QR21", String.class);
		attrib[6] = AttributeTypeFactory.newAttributeType("QR21s", String.class);
		attrib[7] = AttributeTypeFactory.newAttributeType("QR22s", String.class);
		attrib[8] = AttributeTypeFactory.newAttributeType("QR23", String.class);
		attrib[9] = AttributeTypeFactory.newAttributeType("QR23s", String.class);
		attrib[10] = AttributeTypeFactory.newAttributeType("QR241", Double.class);
		attrib[11] = AttributeTypeFactory.newAttributeType("QR242", Double.class);
		attrib[12] = AttributeTypeFactory.newAttributeType("QR251", Double.class);
		attrib[13] = AttributeTypeFactory.newAttributeType("QR252", Double.class);
		attrib[14] = AttributeTypeFactory.newAttributeType("QR26", String.class);
		attrib[15] = AttributeTypeFactory.newAttributeType("QR26s", String.class);
		attrib[16] = AttributeTypeFactory.newAttributeType("QI113", String.class);
		attrib[17] = AttributeTypeFactory.newAttributeType("QI114", Integer.class);
		attrib[18] = AttributeTypeFactory.newAttributeType("QI115", String.class);
		attrib[19] = AttributeTypeFactory.newAttributeType("QI1125", String.class);


		final AttributeType[] homes = new AttributeType[5];
		homes[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.featureSource.getSchema().getDefaultGeometry().getCoordinateSystem());
		homes[1] = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		homes[2] = AttributeTypeFactory.newAttributeType("STARTTIME", Integer.class);
		homes[4] = AttributeTypeFactory.newAttributeType("ENDTIME", Integer.class);
		homes[3] = AttributeTypeFactory.newAttributeType("NAME", String.class);

		try {
//			this.ftPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {polygon, id, from, to, width, area, length }, "linkShape");
			this.ftPoint = FeatureTypeFactory.newFeatureType(attrib, "pointShape");
			this.ftHome = FeatureTypeFactory.newFeatureType(homes, "pointShape");
//			this.ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {linestring, id, info }, "linString");			
		} catch (final FactoryRegistryException e) {
			e.printStackTrace();
		} catch (final SchemaException e) {
			e.printStackTrace();
		}

	}

	private static class Household {
		int id;
		Feature location;
		ArrayList<Activity> acts = new ArrayList<Activity>();
	}
	
	private static class Activity {
		String type;
		Feature location;
		double start;
		double end;
		String day;
		
	}
	

	public static void main(final String [] args) throws Exception {
		final String referenced1 =  "./padang/referencing/referenced.shp";
		final String referenced2 =  "./padang/referencing/referenced2.shp";
		final String referenced3 =  "./padang/referencing/referenced3.shp";
		final String referenced4 =  "./padang/referencing/referenced4.shp";
		final String referenced5 =  "./padang/referencing/referenced5.shp";
		final String referenced6 =  "./padang/referencing/referenced6.shp";
		final String referenced7 =  "./padang/referencing/referenced7.shp";
		final String referenced8 =  "./padang/referencing/referenced8.shp";
		final String referenced9 =  "./padang/referencing/referenced9.shp";
		final String referenced10 =  "./padang/referencing/referenced10.shp";
		final String own =  "./padang/referencing/own.shp";
		final String zones =  "./padang/referencing/zones.shp";
		final String zonesKel =  "./padang/referencing/zonesKel.shp";
		final String homes = "./padang/referencing/homes.shp";

		final String unclassified = "./padang/referencing/input_workday.csv";


		final ArrayList<FeatureSource> fts = new ArrayList<FeatureSource>();
		fts.add(ShapeFileReader.readDataFile(referenced1));
		fts.add(ShapeFileReader.readDataFile(referenced2));
		fts.add(ShapeFileReader.readDataFile(referenced3));
		fts.add(ShapeFileReader.readDataFile(referenced4));
		fts.add(ShapeFileReader.readDataFile(referenced5));
		fts.add(ShapeFileReader.readDataFile(referenced6));
		fts.add(ShapeFileReader.readDataFile(referenced7));
		fts.add(ShapeFileReader.readDataFile(referenced8));
		fts.add(ShapeFileReader.readDataFile(referenced9));
		fts.add(ShapeFileReader.readDataFile(referenced10));
		final FeatureSource others = ShapeFileReader.readDataFile(own);
		final FeatureSource zonesl = ShapeFileReader.readDataFile(zones);
		final FeatureSource zoneslKel = ShapeFileReader.readDataFile(zonesKel);
		final FeatureSource homelocations = ShapeFileReader.readDataFile(homes);
		fts.add(others);
		fts.add(zonesl);
		fts.add(zoneslKel);


		final TextReferencer tr = new TextReferencer(fts,others,zonesl,homelocations,unclassified);
		tr.classify();
		tr.genPlans();



	}





	private void genPlans() {
			this.classified.clear();

			for (final Household h : this.households.values()) {
				Activity a = new Activity();
				a.end = -1;
				a.start = -1;
				a.type = "home activity";
				genAct(h.location.getDefaultGeometry(),h.id,a);
				
				for (final Activity act : h.acts) {
					if (act.day.equals("last weekend day (Sa-Su)")) {
						continue;
					}
					genAct(act.location.getDefaultGeometry(),h.id,act);
//					try {
//						final Geometry g = h.location.getDefaultGeometry();	
//						final Coordinate transformed = MGC.coord2Coordinate(GT.transform(MGC.coordinate2Coord(g.getCoordinate())));
//						this.classified.add(this.ftHome.create(new Object[] {this.geofac.createPoint(transformed),h.id,act.start/3600,act.type,act.end/3600}, "activity"));
//					} catch (final IllegalAttributeException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
					
//					if (act.type.equals("home activity")){
//							try {
//							final Geometry g = h.location.getDefaultGeometry();	
//							final Coordinate transformed = MGC.coord2Coordinate(GT.transform(MGC.coordinate2Coord(g.getCoordinate())));
//							this.classified.add(this.ftHome.create(new Object[] {this.geofac.createPoint(transformed),h.id,0,act.type}, "activity"));
//						} catch (final IllegalAttributeException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						continue;
//					}
//					if (act.start <= 3600*6 && act.end >= 3600*6){
//						genAct(act.location.getDefaultGeometry(),h.id,6,act.type);
//					} 
//					if (act.start <= 3600*9 && act.end >= 3600*9) {
//						genAct(act.location.getDefaultGeometry(),h.id,9,act.type);
//					}
//					if (act.start <= 3600*12 && act.end >= 3600*12) {
//						genAct(act.location.getDefaultGeometry(),h.id,12,act.type);
//					}
//					if (act.start <= 3600*15 && act.end >= 3600*15) {
//						genAct(act.location.getDefaultGeometry(),h.id,15,act.type);
//					}
//					if (act.start <= 3600*18 && act.end >= 3600*18) {
//						genAct(act.location.getDefaultGeometry(),h.id,18,act.type);
//					}
//					
					
				}
				
				
				
				
			}
		
			try {
			ShapeFileWriter.writeGeometries(this.classified, "./padang/referencing/working-day.shp");
		} catch (final Exception e) {
			e.printStackTrace();
		} 
	}


	private void genAct(final Geometry defaultGeometry, final int id, Activity act) {
	
		final Coordinate coord = defaultGeometry.getCoordinate();
		final double r1 = Math.random()*100 - 50;
		coord.x = coord.x + r1;
		final double r2 = Math.random()*100 - 50;
		
		coord.y = coord.y + r2;
		final Coordinate c2 = new Coordinate(coord.x + r1, coord.y + r2);
		final Coordinate transformed = MGC.coord2Coordinate(GT.transform(MGC.coordinate2Coord(c2)));
		
//		final Point p = this.geofac.createPoint(transformed);
		final Point p = this.geofac.createPoint(coord);
		try {
			this.classified.add(this.ftHome.create(new Object[] {p,id,act.start,act.type,act.end}, "activity"));
		} catch (final IllegalAttributeException e) {
			e.printStackTrace();
		}
		
	}


	private static Collection<Feature> getFeatures(final FeatureSource n) {
		final Collection<Feature> features = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
//			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
//			if (multiPolygon.getNumGeometries() > 1) {
//			log.warn("MultiPolygons with more then 1 Geometry ignored!");
//			continue;
//			}
//			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			features.add(feature);
		}

		return features;
	}
}


