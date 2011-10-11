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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;
import playground.droeder.realTimeNavigation.movingObjects.MovingAgentImpl;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;

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
		
		Coord start, end;
		start = new CoordImpl(0,0);
		end = new CoordImpl(15,15);
		double vx, vy, maxSpeed;
		vx = 2;
		vy = 2;
		maxSpeed = 1;
		
		MovingAgentImpl agent = new MovingAgentImpl(start, end, vx, vy, maxSpeed);
		
		Map<String, Coord> pos = new HashMap<String, Coord>();
		pos.put(String.valueOf(0), agent.getCurrentPosition());
		for(int i = 1; i < 100; i++){
			if(agent.processTimeStep(1, null)){
				pos.put(String.valueOf(i), agent.getCurrentPosition());
			}else{
				break;
			}
		}
		DaShapeWriter.writeDefaultPoints2Shape(OUTDIR + "test.shp", "test", pos, null);
		
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
