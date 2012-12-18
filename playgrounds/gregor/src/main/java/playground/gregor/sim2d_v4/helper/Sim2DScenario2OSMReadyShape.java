/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DScenario2OSMReadyShape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.spatialschema.geometry.MismatchedDimensionException;

import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

public class Sim2DScenario2OSMReadyShape {


	private final Sim2DScenario scenario;

	private static final String M_T = "sim2d_section";
	private static final String M_T_L = "sim2d_link";
	private static final String H_W = "footway";
	private static final FeatureType ftEnv;
	private static final FeatureType ftNet;
	private static final CoordinateReferenceSystem crs;
	static {
		try {
			crs = CRS.decode("EPSG:4326", true); //WGS84
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		} 

		//sections
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType("Polygon", Polygon.class,true,null,null,crs);
		AttributeType matsimType = DefaultAttributeTypeFactory.newAttributeType("m_type", String.class);
		AttributeType id = DefaultAttributeTypeFactory.newAttributeType("id", String.class);
		AttributeType level = DefaultAttributeTypeFactory.newAttributeType("level", String.class);
		AttributeType openings = DefaultAttributeTypeFactory.newAttributeType("openings", String.class);
		AttributeType neighbors = DefaultAttributeTypeFactory.newAttributeType("neighbors", String.class);
		AttributeType envId = DefaultAttributeTypeFactory.newAttributeType("env_id", String.class);
		//network
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class,true,null,null,crs);
		AttributeType hw = DefaultAttributeTypeFactory.newAttributeType("highway", String.class);
		AttributeType mw = DefaultAttributeTypeFactory.newAttributeType("m_width", String.class);
		AttributeType mf = DefaultAttributeTypeFactory.newAttributeType("m_fspeed", String.class);
		AttributeType mt = DefaultAttributeTypeFactory.newAttributeType("m_tra_mode", String.class);
		try {
			ftEnv = FeatureTypeFactory.newFeatureType(new AttributeType[]{p,matsimType,id,level,openings,neighbors,envId}, "Sim2DEnvironment");
			ftNet = FeatureTypeFactory.newFeatureType(new AttributeType[]{l,matsimType,hw,mw,mf,mt,envId}, "Sim2DNetwork");
		} catch (FactoryRegistryException e) {
			throw new RuntimeException(e);
		} catch (SchemaException e) {
			throw new RuntimeException(e);
		}
	}

	/*package*/ Sim2DScenario2OSMReadyShape(Sim2DScenario scenario) {
		this.scenario = scenario;
	}

	private MathTransform getTransform(CoordinateReferenceSystem c) {
		try {
			return CRS.findMathTransform(c, crs);
		} catch (NoSuchAuthorityCodeException e1) {
			throw new IllegalArgumentException(e1);
		} catch (FactoryException e1) {
			throw new IllegalArgumentException(e1);
		}
	}
	
	
	/*package*/ void writeOSMReadyNetworkShape(String file) {
		GeometryFactory geofac = new GeometryFactory();
		Collection<Feature> fts = new ArrayList<Feature>();
		for (Sim2DEnvironment env : this.scenario.getSim2DEnvironments()) {
			Id envId = env.getId();
			CoordinateReferenceSystem crs = env.getCRS();
			MathTransform transform = getTransform(crs);
			Network net = env.getEnvironmentNetwork();
			Map<Id,Id> handled = new HashMap<Id,Id>();
			for (Link l : net.getLinks().values()){
				Id tmp = handled.get(l.getToNode().getId());
				if (tmp != null && tmp.equals(l.getFromNode().getId())) {
					continue;
				}
				handled.put(l.getFromNode().getId(), l.getToNode().getId());
				Coordinate from = MGC.coord2Coordinate(l.getFromNode().getCoord());
				Coordinate to = MGC.coord2Coordinate(l.getToNode().getCoord());
				LineString ls = geofac.createLineString(new Coordinate[]{from,to});
				try {
					LineString lst = (LineString) JTS.transform(ls, transform);
					Feature ft = createNetFeature(lst, l,envId);
					fts.add(ft);
				} catch (MismatchedDimensionException e) {
					throw new IllegalArgumentException(e);
				} catch (TransformException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, file);
	}

	private Feature createNetFeature(LineString lst, Link l, Id envId) {

		try {
			return ftNet.create(new Object[]{lst,M_T_L,H_W,"1.0","1.34","pedestrian",envId.toString()});
		} catch (IllegalAttributeException e) {
			throw new IllegalArgumentException(e);
		}
		
//		new AttributeType[]{l,matsimType,hw,level,mw,mf,mt}
	}

	/*package*/ void writeOSMReadyEnvironmentShape(String file) {
		Collection<Feature> fts = new ArrayList<Feature>();

		for (Sim2DEnvironment env : this.scenario.getSim2DEnvironments()) {
			CoordinateReferenceSystem crs = env.getCRS();
			MathTransform transform = getTransform(crs);
			Id envId = env.getId();
			for (Section sec : env.getSections().values()) {
				Id id = sec.getId();
				int level = sec.getLevel();
				Id[] n = sec.getNeighbors();
				int[] o = sec.getOpenings();
				Polygon p = sec.getPolygon();
				try {
					Polygon tp = (Polygon) JTS.transform(p, transform);
					Feature ft = createEnvFeature(tp,id,level,n,o,envId);
					fts.add(ft);
				} catch (MismatchedDimensionException e) {
					throw new IllegalArgumentException(e);
				} catch (TransformException e) {
					throw new IllegalArgumentException(e);
				}
			}

		}
		ShapeFileWriter.writeGeometries(fts, file);
	}

	private Feature createEnvFeature(Polygon tp, Id id, int level, Id[] n, int[] o, Id envId) {

		StringBuffer os = new StringBuffer();
		if (o == null) {
			os.append(' ');
		} else {
			for (int oi : o) {
				os.append(oi);
				os.append(' ');
			}
		}
		StringBuffer ns = new StringBuffer();

		if (n == null) {
			ns.append(' ' );
		} else {
			for (Id ni : n) {
				ns.append(ni.toString());
				ns.append(' ');
			}
		}
		try {
			return ftEnv.create(new Object[]{tp,M_T,id.toString(),level+"",os.toString(),ns.toString(),envId.toString()});
		} catch (IllegalAttributeException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static void main(String [] args) {
		String confPath = "/Users/laemmel/devel/burgdorf2d/tmp/sim2dConfig.xml";
		Sim2DConfig conf = Sim2DConfigUtils.loadConfig(confPath);
		Sim2DScenario sc = Sim2DScenarioUtils.loadSim2DScenario(conf);
		Sim2DScenario2OSMReadyShape osm = new Sim2DScenario2OSMReadyShape(sc);
		osm.writeOSMReadyEnvironmentShape("/Users/laemmel/devel/burgdorf2d/osm/osmEnv.shp");
		osm.writeOSMReadyNetworkShape("/Users/laemmel/devel/burgdorf2d/osm/osmNet.shp");
	}

}
