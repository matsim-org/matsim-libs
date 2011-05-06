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
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.CRS;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Buildings2DistrictsMapping {

	
	
	private  ArrayList<Feature> features;
//	private  AttributeType mp;
	private FeatureType ftMultiPolygon;
//	private FeatureType ftPolygon;
//	private FeatureType ftLineString;
//	private FeatureType ftMultiLineString;
//	private FeatureType ftPoint;
//	private FeatureType ftMultiPoint;
	private final static String WGS84_UTM47S = "PROJCS[\"WGS_1984_UTM_Zone_47S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"False_Easting\",500000.0],PARAMETER[\"False_Northing\",10000000.0],PARAMETER[\"Central_Meridian\",99.0],PARAMETER[\"Scale_Factor\",0.9996],PARAMETER[\"Latitude_Of_Origin\",0.0],UNIT[\"Meter\",1.0]]";
	
	
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
		try {
			initFeatureCollection();
		} catch (FactoryRegistryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		
		FeatureSource fsDistricts;
		fsDistricts =  ShapeFileReader.readDataFile(this.districts);

		try {
			this.envelope = fsDistricts.getBounds();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	
		this.districtsFeatures = getFeature(fsDistricts);
		
		
		FeatureSource fsBuildings;
		fsBuildings = ShapeFileReader.readDataFile(this.buildings);
		
		ArrayList<Feature> buildingsPolygons = getFeature(fsBuildings);
		this.buildingsFeatures = generateQuadTree(buildingsPolygons);
		
		mapPolygons();
		
		ShapeFileWriter.writeGeometries(this.features, "./output/buildings_district_mapping.shp");
	}
	
	private void mapPolygons() {
		
		CSVFileWriter writer = new CSVFileWriter(this.outputfile);
		writer.writeLine(new String[] {"DISTRICT","BUILDING"});
		for (Feature ft : this.districtsFeatures) {
			Long districtID = ((Long) ft.getAttribute(1));
			Geometry geo = ft.getDefaultGeometry();
			ArrayList<Feature> buildings = getFeaturesWithin(ft,districtID);
			for (Feature building : buildings){
				String buildingId = ((Integer) building.getAttribute(1)).toString();
				writer.writeLine(new String[] {districtID.toString(),buildingId});
			}
			
		}
		
		writer.finish();
	}

	private ArrayList<Feature> getFeaturesWithin(final Feature ft, final Long id) {
		ArrayList<Feature> ret = new ArrayList<Feature>();
		Envelope e = ft.getBounds();
		
		Geometry geo = ft.getDefaultGeometry();
		double catchRadius = Math.sqrt(Math.pow((e.getMaxX()-e.getMinX()),2)+Math.pow((e.getMaxY()-e.getMinY()),2));
		Collection<Feature> buildings = this.buildingsFeatures.get(geo.getCentroid().getX(), geo.getCentroid().getY(), catchRadius);
		for (Feature ftb : buildings) {
			
			try {
				if (geo.contains(ftb.getDefaultGeometry().getCentroid())) {
					ret.add(ftb);
					Geometry b = ftb.getDefaultGeometry();
					if (b instanceof MultiPolygon) {
						Integer oid = (Integer) ftb.getAttribute(1);
						Integer idz = (Integer) ftb.getAttribute(2);
						String luse = (String) ftb.getAttribute(3);
						Integer night = (Integer) ftb.getAttribute(4);
						Integer day = (Integer) ftb.getAttribute(5);
						
						this.features.add(this.ftMultiPolygon.create(new Object[]{(MultiPolygon)b,id,oid,idz,luse,night,day}));
					} //else if (b instanceof Polygon) {
//						this.features.add(this.ftPolygon.create(new Object[]{(Polygon)b,id}));
//					}
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

	private void initFeatureCollection() throws FactoryRegistryException, SchemaException, FactoryException {
		
		this.features = new ArrayList<Feature>();
		final CoordinateReferenceSystem targetCRS = CRS.parseWKT( WGS84_UTM47S);
		AttributeType mp = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, targetCRS);
//		AttributeType p = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, targetCRS);
//		AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, targetCRS);
//		AttributeType ml = DefaultAttributeTypeFactory.newAttributeType("MultiLineString",MultiLineString.class, true, null, null, targetCRS);
//		AttributeType point = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, targetCRS);
//		AttributeType mpoint = DefaultAttributeTypeFactory.newAttributeType("MultiPoint",MultiPoint.class, true, null, null, targetCRS);
		AttributeType did = AttributeTypeFactory.newAttributeType("DISTRICTID", Long.class);
		AttributeType oid = AttributeTypeFactory.newAttributeType("OBJECTID", Integer.class);
		AttributeType id = AttributeTypeFactory.newAttributeType("ID", Integer.class);
		AttributeType luse = AttributeTypeFactory.newAttributeType("Land_use", String.class);
		AttributeType night = AttributeTypeFactory.newAttributeType("popBdNt", Integer.class);
		AttributeType day = AttributeTypeFactory.newAttributeType("popBd_day", Integer.class);
		

		this.ftMultiPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {mp, did, oid, id, luse, night, day}, "geometry");
//		this.ftPolygon = FeatureTypeFactory.newFeatureType(new AttributeType[] {p, id}, "geometry");
//		this.ftLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {l, id}, "geometry");
//		this.ftMultiLineString = FeatureTypeFactory.newFeatureType(new AttributeType[] {ml, id}, "geometry");
//		this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {point, id}, "geometry");
//		this.ftMultiPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] {mpoint, id}, "geometry");
	}
	
	public static void main(final String [] args) {
		
		String districts = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/data/GIS/keluraha_region.shp";
		String buildings = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/data/population/Population_Padang.shp";
		String outputFile = "./output/mappings.csv";
		Buildings2DistrictsMapping mapper = new Buildings2DistrictsMapping(districts, buildings, outputFile);
		mapper.run();
	}
}
