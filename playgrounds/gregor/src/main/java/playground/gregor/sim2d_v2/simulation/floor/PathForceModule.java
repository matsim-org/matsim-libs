/* *********************************************************************** *
 * project: org.matsim.*
 * PathForceModule.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;

/**
 * @author laemmel
 * 
 */
public class PathForceModule implements ForceModule {

	private final PhysicalFloor floor;
	private HashMap<Id, Coordinate> drivingDirections;
	private HashMap<Id, Coordinate> fromCoords;
	private HashMap<Id, LineString> linkGeos;

	int redraws = 0;
	private final double deltaT;


	// Mauron constant
	private static final double Apath =1.13*Agent2D.AGENT_WEIGHT;
	private static final double Bpath = 1;


	/**
	 * @param floor
	 * @param scenario
	 */
	public PathForceModule(PhysicalFloor floor, Scenario2DImpl scenario) {
		this.floor = floor;
		this.deltaT = ((Sim2DConfigGroup)scenario.getConfig().getModule("sim2d")).getTimeStepSize();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {

		this.drivingDirections = new HashMap<Id, Coordinate>();
		this.fromCoords = new HashMap<Id, Coordinate>();
		this.linkGeos = new HashMap<Id, LineString>();
		GeometryFactory geofac = new GeometryFactory();
		for (Link link : this.floor.getLinks()) {
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			Coordinate c = new Coordinate(to.x - from.x, to.y - from.y);
			double length = Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;
			this.drivingDirections.put(link.getId(), c);
			this.fromCoords.put(link.getId(), from);
			this.linkGeos.put(link.getId(), geofac.createLineString(new Coordinate[] { from, to }));

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent) {
		Coordinate drivingDir = this.drivingDirections.get(agent.getCurrentLinkId());
		Coordinate fromNode = this.fromCoords.get(agent.getCurrentLinkId());

		double hypotenuse = agent.getPosition().distance(fromNode);


		double pathDist = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y).distance(this.linkGeos.get(agent.getCurrentLinkId()));
		//		if (pathDist < Bpath){
		//			return;
		//		}
		//
		double scale = Math.sqrt(Math.pow(hypotenuse, 2) - Math.pow(pathDist, 2));

		double deltaX = (fromNode.x - agent.getPosition().x) + drivingDir.x * scale;
		double deltaY = (fromNode.y - agent.getPosition().y) + drivingDir.y * scale;

		double f = Math.exp(pathDist / Bpath) * this.deltaT; //deltaT is needed here to make path force independent of temporal resolution
		deltaX *= f / pathDist;
		deltaY *= f / pathDist;

		deltaX = Apath * deltaX;
		deltaY = Apath * deltaY;


		agent.getForce().incrementX(deltaX);
		agent.getForce().incrementY(deltaY);

	}
}
