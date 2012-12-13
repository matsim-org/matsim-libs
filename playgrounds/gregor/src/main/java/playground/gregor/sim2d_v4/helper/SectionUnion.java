/* *********************************************************************** *
 * project: org.matsim.*
 * SectionUnion.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import playground.gregor.sim2d_v4.io.Sim2DEnvironmentWriter02;
import playground.gregor.sim2d_v4.io.osmparser.OSM;
import playground.gregor.sim2d_v4.io.osmparser.OSMRelation;
import playground.gregor.sim2d_v4.io.osmparser.OSMRelation.Member;
import playground.gregor.sim2d_v4.io.osmparser.OSMWay;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class SectionUnion {
	
	private final Sim2DEnvironment env;
	private Map<Id, Id> mapping;
	private final Map<Id, Id> rmIdMapping = new HashMap<Id,Id>();
	

	/*package*/ SectionUnion(Sim2DEnvironment env) {
		this.env = env;
	}
	
	/*package*/ void processOSMFile(String file) {
		OSM osm = new OSM();
		osm.addKeyValue("union", "true");
		CustomizedOSM2Sim2D envReader = new CustomizedOSM2Sim2D(this.env, osm);
		envReader.processOSMFile(file);
//		System.out.println(osm);
		//build osm_id --> matsim_id mapping
		this.mapping = new HashMap<Id,Id>();
		for (OSMWay way : osm.getWays()) {
			Id osmId = way.getId();
			String matsimId = way.getTags().get("id");
			if (matsimId != null) {
				this.mapping.put(osmId, new IdImpl(matsimId));
			}
		}
		for (OSMRelation rel : osm.getRelations()) {
			handleRelation(rel);
		}
		
		//revise neighbors - currently the only way to change neighbors is to remove sections and create new ones with revised neighbor relations
		List<Section> secs = new ArrayList<Section>();
		Iterator<Section> it = this.env.getSections().values().iterator();
		while (it.hasNext()) {
			Section sec = it.next();
			secs.add(sec);
			it.remove();
		}
		
		
	}

	
	private void handleRelation(OSMRelation rel) {
		Set<Opening> potentialOpenings = new HashSet<Opening>();
		Set<Id> potentialNeighbors = new HashSet<Id>();
		Iterator<Member> it = rel.getMembers().iterator();
		Member m = it.next();
		Id id = this.mapping.get(m.getRefId());
		Section sec = this.env.getSections().remove(id);
		Geometry geo = sec.getPolygon();
		for (Id n : sec.getNeighbors()) {
			potentialNeighbors.add(n);
		}
		Coordinate[] coords = sec.getPolygon().getExteriorRing().getCoordinates();
		for (int o : sec.getOpenings()){
			Coordinate c0 = coords[o];
			Coordinate c1 = coords[o+1];
			Opening oo = new Opening();
			oo.c0 = c0;
			oo.c1 = c1;
			potentialOpenings.add(oo);
		}
		
		IdImpl newId = new IdImpl(id.toString()+"union");
		this.rmIdMapping.put(id, newId);
		
		
		while (it.hasNext()) {
			m = it.next();
			id = this.mapping.get(m.getRefId());
			sec = this.env.getSections().remove(id);
			for (Id n : sec.getNeighbors()) {
				potentialNeighbors.add(n);
			}
			coords = sec.getPolygon().getExteriorRing().getCoordinates();
			for (int o : sec.getOpenings()){
				Coordinate c0 = coords[o];
				Coordinate c1 = coords[o+1];
				Opening oo = new Opening();
				oo.c0 = c0;
				oo.c1 = c1;
				potentialOpenings.add(oo);
			}
			geo = geo.union(sec.getPolygon());
			this.rmIdMapping.put(id, newId);
		
		}
		if (!(geo instanceof Polygon)) {
			throw new RuntimeException("Invalid relation detected. Revise relation with id:" + rel.getId());
		}
		Polygon p = (Polygon)(geo);
		List<Integer> openingsList = new ArrayList<Integer>();
		coords = p.getExteriorRing().getCoordinates();
		for (int i = 0; i < coords.length; i++) {
			Iterator<Opening> oit = potentialOpenings.iterator();
			while (oit.hasNext()) {
				Opening o = oit.next();
				if (o.c0.equals(coords[i])){
					oit.remove();
					if (o.c1.equals(coords[i+1])) {
						openingsList.add(i);
					}
				}
			}
		}
		int[] openingsA = new int [openingsList.size()];
		for (int i = 0; i < openingsList.size(); i++) {
			openingsA[i] = openingsList.get(i);
		}
		
		Id[] neighborsA = potentialNeighbors.toArray(new Id[0]);
		this.env.createAndAddSection(newId, p, openingsA,neighborsA, sec.getLevel());
	}



	private static final class Opening {
		private Coordinate c0;
		private Coordinate c1;
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Opening) {
				Opening o = (Opening)obj;
				if (o.c0.equals(this.c0) && o.c1.equals(this.c1)) {
					return true;
				} else if (o.c0.equals(this.c1) && o.c1.equals(this.c0)) {
					return true;
				}
			} else {
				return false;
			}
			return false;
		}
		
	}
	
	public static void main(String [] args) throws NoSuchAuthorityCodeException, FactoryException {
		String osmFile = "/Users/laemmel/devel/burgdorf2d/osm/osmEnv.osm";
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setCRS(CRS.decode("EPSG:3395"));
		env.setNetwork(NetworkImpl.createNetwork());
		new SectionUnion(env).processOSMFile(osmFile);
//		CustomizedOSM2Sim2D osm2sim2d = new CustomizedOSM2Sim2D(env);
//		osm2sim2d.processOSMFile(osmFile);
		
		new Sim2DEnvironmentWriter02(env).write("/Users/laemmel/devel/burgdorf2d/osm/sim2dEnv_0.gml.gz");
//		new NetworkWriter(env.getEnvironmentNetwork()).write("/Users/laemmel/devel/burgdorf2d/osm/test.network.xml");
	}
}
