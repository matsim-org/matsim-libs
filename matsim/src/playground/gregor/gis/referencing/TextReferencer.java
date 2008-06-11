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
import org.opengis.referencing.FactoryException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.gis.referencing.CRN.CaseNode;
import playground.gregor.gis.utils.ShapeFileReader;
import playground.gregor.gis.utils.ShapeFileWriter;

public class TextReferencer {
	
	
	
	private Collection<Feature> referenced;
	private Collection<Feature> classified;
	private String unclassified;
	private CRN crn;
	private FeatureType ftPoint;
	private FeatureSource featureSource;
	private GeometryFactory geofac;

	public TextReferencer(ArrayList<FeatureSource> fts) {
		this.featureSource = fts.get(0);
		Collection<Feature> ft = new ArrayList<Feature>();
		for (FeatureSource f : fts) {
			ft.addAll(getFeatures(f));
		}

		this.referenced = ft;
		this.crn = new CRN(ft);
		this.geofac = new GeometryFactory();
		this.classified = new ArrayList<Feature>();
		initFeatureGenerator();
	}

	
	private void classify(String unclassified) {
		TextFileReader tfr = new TextFileReader(unclassified);
		TextFileWriter rfw = new TextFileWriter("test.csv");
		
		String [] out = new String [3];
		String [] line = tfr.readLine();
		
		while (line != null) {
			String query = line[6];
			if (query.toLowerCase().contains(", kampung")){
				int i = 0; i++;
			}
			CaseNode resp = this.crn.getCase(query);
			if (resp == null){ 
				line = tfr.readLine();
				continue;
			}
			
			if (resp.getActivation() > 0.97) {
				Feature ft = getPointFeature(resp.getCoordinate(), line);
				this.classified.add(ft);
			}
			
//			System.out.println("Query: " + query.toLowerCase() + " Resp:" + resp.getExpression() + "   Activation:" + resp.getActivation());
			out[0] = query.toLowerCase();
			out[1] = resp.getExpression();
			out[2] = resp.getActivation() + "";
			rfw.writeLine(out);
			line = tfr.readLine();
			
		}
		
		rfw.finish();
		try {
			ShapeFileWriter.writeGeometries(this.classified, "./padang/referencing/classified.shp");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private Feature getPointFeature(Coordinate c, String[] input) {
		
		Object [] obj = new Object [input.length+1];
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
				} catch (NumberFormatException e) {
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
				} catch (NumberFormatException e) {
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
					} catch (NumberFormatException e1) {
						obj[i] = -1.0;
					}
				break;
			case 11:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (NumberFormatException e1) {
					obj[i] = -1.0;
				}
				break;
			case 12:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (NumberFormatException e1) {
					obj[i] = -1.0;
				}
				break;
			case 13:
				try {
					obj[i] = Double.parseDouble(input[i-1]);
				} catch (NumberFormatException e1) {
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
					} catch (NumberFormatException e) {
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
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	
private void initFeatureGenerator(){
		
		AttributeType[] attrib = new AttributeType[20];
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
		
		
		try {
//			this.ftPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {polygon, id, from, to, width, area, length }, "linkShape");
			this.ftPoint = FeatureTypeFactory.newFeatureType(attrib, "pointShape");
//			this.ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {linestring, id, info }, "linString");			
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String [] args) throws Exception {
		String referenced1 =  "./padang/referencing/referenced.shp";
		String referenced2 =  "./padang/referencing/referenced2.shp";
		String referenced3 =  "./padang/referencing/referenced3.shp";
		String referenced4 =  "./padang/referencing/referenced4.shp";
		String referenced5 =  "./padang/referencing/referenced5.shp";
		String referenced6 =  "./padang/referencing/referenced6.shp";
		String referenced7 =  "./padang/referencing/referenced7.shp";
		
		
		String unclassified = "./padang/referencing/unclassified.txt";
	
		
		ArrayList<FeatureSource> fts = new ArrayList<FeatureSource>();
		fts.add(ShapeFileReader.readDataFile(referenced1));
		fts.add(ShapeFileReader.readDataFile(referenced2));
		fts.add(ShapeFileReader.readDataFile(referenced3));
		fts.add(ShapeFileReader.readDataFile(referenced4));
		fts.add(ShapeFileReader.readDataFile(referenced5));
		fts.add(ShapeFileReader.readDataFile(referenced6));
		fts.add(ShapeFileReader.readDataFile(referenced7));
		
		new TextReferencer(fts).classify(unclassified);
		
		
		
	}





	private static Collection<Feature> getFeatures(FeatureSource n) {
		Collection<Feature> features = new ArrayList<Feature>();
		FeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			Feature feature = it.next();
//			int id = (Integer) feature.getAttribute(1);
//			MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
//			if (multiPolygon.getNumGeometries() > 1) {
//				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
//			}
//			Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			features.add(feature);
	}
	
		return features;
	}
}


