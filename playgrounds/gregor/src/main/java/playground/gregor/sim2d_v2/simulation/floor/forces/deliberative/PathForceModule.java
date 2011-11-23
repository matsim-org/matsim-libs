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
package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.ForceModule;

/**
 * @author laemmel
 * 
 */
public class PathForceModule implements ForceModule {

	private final PhysicalFloor floor;
	private HashMap<Id, LinkInfo> linkGeos;

	int redraws = 0;

	private static final double VIRTUAL_LENGTH = 1000;

	private static final double COS_RIGHT = Math.cos(-Math.PI / 2);
	private static final double SIN_RIGHT = Math.sin(-Math.PI / 2);

	// Mauron constant
	private static final double Apath =25; //*Agent2D.AGENT_WEIGHT;
	private static final double Bpath = 2.;


	/**
	 * @param floor
	 * @param scenario
	 */
	public PathForceModule(PhysicalFloor floor, Scenario scenario) {
		this.floor = floor;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {

		this.linkGeos = new HashMap<Id, LinkInfo>();
		GeometryFactory geofac = new GeometryFactory();
		for (Link link : this.floor.getLinks()) {
			Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
			Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
			Coordinate c = new Coordinate(to.x - from.x, to.y - from.y);
			double length = Math.sqrt(Math.pow(c.x, 2) + Math.pow(c.y, 2));
			c.x /= length;
			c.y /= length;

			Coordinate virtualFrom = new Coordinate(from.x - c.x*VIRTUAL_LENGTH, from.y - c.y * VIRTUAL_LENGTH);


			LineString ls = geofac.createLineString(new Coordinate[] { virtualFrom, to });


			Coordinate perpendicularVec = new Coordinate(COS_RIGHT * c.x + SIN_RIGHT * c.y, -SIN_RIGHT * c.x + COS_RIGHT * c.y);




			LinkInfo li = new LinkInfo();
			li.ls = ls;
			li.perpendicularVector = perpendicularVec;



			this.linkGeos.put(link.getId(), li);

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
	public void run(Agent2D agent, double time) {

		Coordinate pos = agent.getPosition();
		LinkInfo li = this.linkGeos.get(agent.getMentalLink());
		LineString ls = li.ls;

		double pathDist = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y).distance(ls);
		if (pathDist <= 0){
			return;
		}





		double f = Apath * Math.exp(pathDist / Bpath);


		Point orig = ls.getStartPoint();
		Point dest = ls.getEndPoint();
		double x2 = orig.getX() - dest.getX();
		double y2 = orig.getY() - dest.getY();
		double x3 = orig.getX() - pos.x;
		double y3 = orig.getY() - pos.y;
		boolean rightHandSide = x2*y3 - y2*x3 < 0 ? false : true;
		double dx = rightHandSide == true ? -li.perpendicularVector.x : li.perpendicularVector.x;
		double dy = rightHandSide == true ? -li.perpendicularVector.y : li.perpendicularVector.y;

		double fx  = dx * f;
		double fy = dy * f;


		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);

	}

	private static final class LinkInfo {
		LineString ls;
		Coordinate perpendicularVector;
	}
}
