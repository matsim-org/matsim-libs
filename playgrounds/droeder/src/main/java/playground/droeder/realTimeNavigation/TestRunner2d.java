/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.realTimeNavigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.droeder.DaPaths;
import playground.droeder.Vector2D;
import playground.droeder.gis.DaShapeWriter;
import playground.droeder.realTimeNavigation.movingObjects.MovingAgentImpl;
import playground.droeder.realTimeNavigation.movingObjects.MovingObject;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacleImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author droeder
 * use <code> {@link VelocityObstacleTestRunner} </Code>
 */
@Deprecated
public class TestRunner2d {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String DIR = DaPaths.VSP + "2D_Sim/";
		final String OUTDIR = DIR + "output/";
		
		// TODO do not use own Implementation of Vector2D
		//create agentGeometry
		int numOfAgents = 18;
		double agentRadius = 1;
		
		Coordinate[] c = new Coordinate[numOfAgents + 1];
		double angle = Math.PI * 2 / numOfAgents;
		for(int i = 0; i <numOfAgents; i++){
			c[i] = new Coordinate(agentRadius * Math.cos(angle * i), agentRadius * Math.sin(angle * i));
		}
		c[18] = c[0];
		Geometry g = new GeometryFactory().createLinearRing(c);

		// create Agents
		Map<MovingObject, Map<String, Coord>> pos = new HashMap<MovingObject, Map<String, Coord>>();
		numOfAgents = 20;
		double agentDistributionRadius = 20;
		angle = Math.PI /numOfAgents;
		
		MovingObject agent;
		Vector2D position, goal;
		double maxSpeed = 2.0;
		Id id;
		for(int i = 0 ; i < numOfAgents; i++){
			id = new IdImpl(i);
			position = new Vector2D(agentDistributionRadius * Math.cos(angle * i), agentDistributionRadius * Math.sin(angle * i));
			goal = new Vector2D(-1, position);
			agent = new MovingAgentImpl(position, goal, maxSpeed, id, g);
			pos.put(agent, new HashMap<String, Coord>());
		}
		
//		final MovingAgentImpl agent1 = new MovingAgentImpl(new Vector2D(0,15), new Vector2D(15,0), 0.5, new IdImpl("1"), g);
//		final MovingAgentImpl agent2 = new MovingAgentImpl(new Vector2D(0,7.5), new Vector2D(15,7.5), 0.3, new IdImpl("2"), g);
//		@SuppressWarnings("serial")
//		Map<MovingObject, Map<String, Coord>> pos = new HashMap<MovingObject, Map<String, Coord>>(){{
//			put(agent1, new HashMap<String, Coord>());
//			put(agent2, new HashMap<String, Coord>());
//		}};
		
		// agents walk
		Map<Id, Map<String, Geometry>> vos = new HashMap<Id, Map<String,Geometry>>();
		for(int i = 0; i < 200; i++){
			for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
				if(i == 0){
					e.getValue().put(String.valueOf(0), e.getKey().getCurrentPosition().getCoord());
					vos.put(e.getKey().getId(), new HashMap<String, Geometry>());
				}else {
					VelocityObstacle vo = new VelocityObstacleImpl(e.getKey(),pos.keySet());
					vos.get(e.getKey().getId()).put(String.valueOf(i), vo.getGeometry());
					e.getKey().calculateNextStep(1, vo);
				}
			}
			for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
				if(i<=0){
					continue;
				}else if(e.getKey().processNextStep() ){
					e.getValue().put(String.valueOf(i), e.getKey().getCurrentPosition().getCoord());
				}
			}
			System.out.println(i);
		}
		
		// prepare VelocityObstacles for ShapeWriter
		Geometry geo;
		Map<String, Map<String, List<Coord>>> vosForWriter = new HashMap<String, Map<String,List<Coord>>>();
		Map<String, List<Coord>> temp;
		for(Entry<Id, Map<String, Geometry>> e: vos.entrySet()){
			temp = new HashMap<String, List<Coord>>();
			for(Entry<String, Geometry> ee: e.getValue().entrySet()){
				for(int i = 0; i < ee.getValue().getNumGeometries(); i++){
					geo = ee.getValue().getGeometryN(i);
					if(geo.getClass().equals(Polygon.class)){
						temp.put(ee.getKey() + "_" + String.valueOf(i), coordinate2Coord(geo.getCoordinates()));
					}
				}
			}
			vosForWriter.put(e.getKey().toString(), temp);
		}
		
		// dump results
		for(Entry<String, Map<String, List<Coord>>> e: vosForWriter.entrySet()){
			DaShapeWriter.writeDefaultLineStrings2Shape(OUTDIR + "vo_agent_" + e.getKey() + ".shp", e.getKey(), e.getValue());
		}
		
		for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
			DaShapeWriter.writeDefaultPoints2Shape(OUTDIR + "agent_" + e.getKey().getId().toString() + ".shp", e.getKey().getId().toString(), e.getValue(), null);
		}
		
		// dump the position sorted by timeStep
//		Map<String, Map<String, Coord>> sortByTimeStep = new HashMap<String, Map<String,Coord>>();
//		boolean first = true; 
//		for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
//			for(Entry<String, Coord> ee: e.getValue().entrySet()){
//				if(first){
//					sortByTimeStep.put(ee.getKey(), new HashMap<String, Coord>());
//				}
//				sortByTimeStep.get(ee.getKey()).put(e.getKey().getId().toString(), ee.getValue());
//			}
//			first = false;
//		}
//		
//		for(Entry<String, Map<String, Coord>> e: sortByTimeStep.entrySet()){
//			DaShapeWriter.writeDefaultPoints2Shape(OUTDIR + "step_" + e.getKey() + ".shp", e.getKey(), e.getValue(), null);
//		}
	}
	
	private static List<Coord> coordinate2Coord(Coordinate[] c){
		List<Coord> coords = new ArrayList<Coord>();
		for(Coordinate cc: c){
			coords.add(new CoordImpl(cc.x, cc.y));
		}
		return coords;
	}
	
	
//	public static void main(String[] args){
//		final String DIR = DaPaths.VSP + "2D_Sim/";
//		final String INDIR = DIR + "input/";
//		final String OUTDIR = DIR + "output/";
//		final String EVENTS = INDIR + "0.events.xml";
//		
//		RealTimeNavigation navi = new RealTimeNavigation();
//		EventsManager manager = EventsUtils.createEventsManager();
//		manager.addHandler(navi);
//		new XYVxVyEventsFileReader(manager).parse(IOUtils.getInputstream(EVENTS));
//		
//		Map<Id, Map<String, SortedMap<Integer, Coord>>> pos = new HashMap<Id, Map<String,SortedMap<Integer,Coord>>>();
//		
//		
//		for(Entry<Id, Map<String, Coord>> e: navi.getAgentPosition2().entrySet()){
//			DaShapeWriter.writeDefaultPoints2Shape(OUTDIR + e.getKey().toString() + ".shp", e.getKey().toString(), e.getValue(), null);
//		}
//		
//		for(Entry<Id, Map<String, List<Coord>>> e: navi.getAgentPrefferedSpeed().entrySet()){
//			DaShapeWriter.writeDefaultLineStrings2Shape(OUTDIR + e.getKey().toString() + "_v.shp", e.getKey().toString() + "_v", e.getValue());
//		}
//	}
}
