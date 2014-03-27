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

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

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
	private static final PolygonFeatureFactory polygonFactory;
	private static final PolylineFeatureFactory polylineFactory;
	private static final CoordinateReferenceSystem crs;
	static {
		try {
			crs = CRS.decode("EPSG:4326", true); //WGS84
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		} 

		polygonFactory = new PolygonFeatureFactory.Builder().
				setCrs(crs).
				addAttribute("m_type", String.class).
				addAttribute("id", String.class).
				addAttribute("level", String.class).
				addAttribute("openings", String.class).
				addAttribute("neighbors", String.class).
				addAttribute("env_id", String.class).
				create();
		
		polylineFactory = new PolylineFeatureFactory.Builder().
				setCrs(crs).
				addAttribute("m_type", String.class).
				addAttribute("highway", String.class).
				addAttribute("m_width", String.class).
				addAttribute("m_fspeed", String.class).
				addAttribute("m_tra_mode", String.class).
				addAttribute("env_id", String.class).
				create();
		
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
		Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
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
					SimpleFeature ft = createNetFeature(lst, l,envId);
					fts.add(ft);
				} catch (TransformException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, file);
	}

	private SimpleFeature createNetFeature(LineString lst, Link l, Id envId) {

		return polylineFactory.createPolyline(lst, new Object[]{M_T_L,H_W,"1.0","1.34","pedestrian",envId.toString()}, null);
		
//		new AttributeType[]{l,matsimType,hw,level,mw,mf,mt}
	}

	/*package*/ void writeOSMReadyEnvironmentShape(String file) {
		Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();

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
					SimpleFeature ft = createEnvFeature(tp,id,level,n,o,envId);
					fts.add(ft);
				} catch (TransformException e) {
					throw new IllegalArgumentException(e);
				}
			}

		}
		ShapeFileWriter.writeGeometries(fts, file);
	}

	private SimpleFeature createEnvFeature(Polygon tp, Id id, int level, Id[] n, int[] o, Id envId) {

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
		return polygonFactory.createPolygon(tp, new Object[]{M_T,id.toString(),level+"",os.toString(),ns.toString(),envId.toString()}, null);
	}

	public static void main(String [] args) {
		String baseName = "nextgen_floorplan";
		String confPath = "/Users/laemmel/devel/hhw3/raw/sim2dConfigenv.xml";
		Sim2DConfig conf = Sim2DConfigUtils.loadConfig(confPath);
		Sim2DScenario sc = Sim2DScenarioUtils.loadSim2DScenario(conf);
		Sim2DScenario2OSMReadyShape osm = new Sim2DScenario2OSMReadyShape(sc);
		osm.writeOSMReadyEnvironmentShape("/Users/laemmel/devel/hhw3/env_gen_stage2/osmEnv" + baseName +".shp");
		osm.writeOSMReadyNetworkShape("/Users/laemmel/devel/hhw3/env_gen_stage2/osmNet"+ baseName + ".shp");
	}

}
