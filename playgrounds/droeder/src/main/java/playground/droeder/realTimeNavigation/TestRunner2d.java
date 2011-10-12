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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;

import playground.droeder.DaPaths;
import playground.droeder.Vector2D;
import playground.droeder.gis.DaShapeWriter;
import playground.droeder.realTimeNavigation.movingObjects.MovingAgentImpl;
import playground.droeder.realTimeNavigation.movingObjects.MovingObject;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacle;
import playground.droeder.realTimeNavigation.velocityObstacles.VelocityObstacleImpl;
import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author droeder
 *
 */
public class TestRunner2d {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String DIR = DaPaths.VSP + "2D_Sim/";
		final String INDIR = DIR + "input/";
		final String OUTDIR = DIR + "output/";
		Coordinate[] c = new Coordinate[19];
		
		int angle = 0 ;
		for(int i = 0; i <18; i++){
			c[i] = new Coordinate(1 * Math.cos(i*Math.PI*2), 1 * Math.sin(i*Math.PI*2));
		}
		c[18] = c[0];
		
		Geometry g = new GeometryFactory().createLinearRing(c);
		final MovingAgentImpl agent1 = new MovingAgentImpl(new Vector2D(0,15), new Vector2D(15,0), 0.5, new IdImpl("1"), g);
		final MovingAgentImpl agent2 = new MovingAgentImpl(new Vector2D(0,0), new Vector2D(15,15), 1.5, new IdImpl("2"), g);
		final MovingAgentImpl agent3 = new MovingAgentImpl(new Vector2D(0,7), new Vector2D(15,7), 1, new IdImpl("3"), g);
		
		
		@SuppressWarnings("serial")
		Map<MovingObject, Map<String, Coord>> pos = new HashMap<MovingObject, Map<String, Coord>>(){{
			put(agent1, new HashMap<String, Coord>());
			put(agent2, new HashMap<String, Coord>());
			put(agent3, new HashMap<String, Coord>());
		}};
		
		for(int i = 0; i < 100; i++){
			for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
				if(i == 0){
					e.getValue().put(String.valueOf(0), e.getKey().getCurrentPosition().getCoord());
				}else{
					VelocityObstacle vo = new VelocityObstacleImpl(e.getKey(), pos.keySet());
					e.getKey().calculateNextStep(1, vo);
				}
			}
			for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
				if(i<=0) continue;
				if(e.getKey().processNextStep() ){
					e.getValue().put(String.valueOf(i), 
							e.getKey().
							getCurrentPosition().
							getCoord());
				}
			}
			
		}
			
		GisDebugger.dump(OUTDIR + "vo.shp");
		for(Entry<MovingObject, Map<String, Coord>> e: pos.entrySet()){
			DaShapeWriter.writeDefaultPoints2Shape(OUTDIR + "agent_" + e.getKey().getId().toString() + ".shp", e.getKey().getId().toString(), e.getValue(), null);
		}
		
//		final String EVENTS = INDIR + "0.events.xml";
//		
//		RealTimeNavigation navi = new RealTimeNavigation();
//		EventsManager manager = EventsUtils.createEventsManager();
//		manager.addHandler(navi);
//		new XYVxVyEventsFileReader(manager).parse(IOUtils.getInputstream(EVENTS));
//		
//		DaShapeWriter.writeDefaultLineStrings2Shape(OUTDIR + "agentPosition.shp", "agentPosition", navi.getAgentPosition());
//		
//		for(Entry<Id, Map<String, Coord>> e: navi.getAgentPosition2().entrySet()){
//			DaShapeWriter.writeDefaultPoints2Shape(OUTDIR + e.getKey().toString() + ".shp", e.getKey().toString(), e.getValue(), null);
//		}
//		
//		for(Entry<Id, Map<String, List<Coord>>> e: navi.getAgentPrefferedSpeed().entrySet()){
//			DaShapeWriter.writeDefaultLineStrings2Shape(OUTDIR + e.getKey().toString() + "_v.shp", e.getKey().toString() + "_v", e.getValue());
//		}
		
	}

}
