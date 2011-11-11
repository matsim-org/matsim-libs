/* *********************************************************************** *
 * project: org.matsim.*
 * Agent2D.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;


import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Agent2D  {

	private Coordinate currentPosition;
	private final Force force = new Force();
	private final double desiredVelocity;
	private double vx;
	private double vy;
	private final MobsimDriverAgent pda;
	private final Scenario sc;
	private double currentDesiredVelocity;

	private final double maxV = 2.;

	public static final double AGENT_WEIGHT = 80;// * 1000;
	public static final double AGENT_DIAMETER = 0.50;

	private CCWPolygon geometry;
	private final double a_min = .18;
	private final double b_min = .2;
	private final double b_max = .25;
	private final double tau_a = .53;
	private QuadTree<CCWPolygon> geometryQuad;
	private double v;
	private double alpha = 0;

	/**
	 * @param p
	 * @param sim2d
	 */
	public Agent2D(MobsimDriverAgent pda, Scenario sc) {

		this.pda = pda;
		this.sc = sc;
		// TODO think about this
		this.desiredVelocity = 1.34; //+(MatsimRandom.getRandom().nextDouble() - 0.5) / 2;
		this.currentDesiredVelocity = this.desiredVelocity;

		initGeometry();

	}

	private void initGeometry() {

		//x dim velo
		//y dim angle
		this.geometryQuad = new QuadTree<CCWPolygon>(0,0,2,360);

		for (double v = 0; v < this.maxV; v += this.maxV/24 ) {
			double a = this.a_min + this.tau_a * v;
			double b = this.b_max - (this.b_max - this.b_min)*v/this.desiredVelocity;
			Coordinate[] c = Algorithms.getEllipse(a, b);
			for (double angle = 0; angle < 360; angle += 15) {
				Coordinate [] tmp = new Coordinate[c.length];
				for (int i = 0; i < c.length-1; i++) {
					tmp[i] = new Coordinate(c[i]);
				}
				tmp[c.length-1] = tmp[0];
				double alpha = angle / 360. * 2 * Math.PI;
				Algorithms.rotate(alpha, tmp);
				CCWPolygon ccw = new CCWPolygon(tmp, new Coordinate(0,0));
				this.geometryQuad.put(v, angle, ccw);
			}
		}
		this.geometry = this.geometryQuad.get(0, 0);
	}

	public CCWPolygon getGeometry(){
		return this.geometry;
	}

	/**
	 * @return
	 */
	public Coordinate getPosition() {
		return this.currentPosition;
	}

	public void setPostion(Coordinate pos) {
		this.currentPosition = pos;
		this.geometry.translate(pos);
	}

	public Force getForce() {
		return this.force;
	}

	/**
	 * @param newPos
	 */
	@Deprecated //use translate instead!
	public void moveToPostion(Coordinate newPos) {
		// TODO check for choose next link and so on ...
		this.currentPosition.setCoordinate(newPos);
	}

	public void translate(double dx, double dy, double vx2, double vy2) {
		this.currentPosition.x += dx;
		this.currentPosition.y += dy;
		setCurrentVelocity(vx2, vy2);
		this.v = Math.sqrt(vx2*vx2+vy2*vy2);
		if (vx2 != 0 || vy2 != 0) {
			this.alpha = 360*Algorithms.getPolarAngle(this.vx, this.vy)/(2*Math.PI);
		}
		this.geometry = this.geometryQuad.get(this.v, this.alpha);
		this.geometry.translate(this.currentPosition);

	}



	/**
	 * @return
	 */
	public double getDesiredVelocity() {
		return this.currentDesiredVelocity;
	}

	@Deprecated //should be private
	public void setCurrentVelocity(double vx, double vy) {
		this.vx = vx;
		this.vy = vy;

	}

	public double getVx() {
		return this.vx;
	}

	public double getVy() {
		return this.vy;
	}

	public double getWeight() {
		return AGENT_WEIGHT;
	}

	public void notifyMoveOverNode(Id newLinkId, double time) {
		this.pda.notifyMoveOverNode(newLinkId);
		double sp = this.sc.getNetwork().getLinks().get(newLinkId).getFreespeed(time);
		this.currentDesiredVelocity = Math.min(this.desiredVelocity, sp);
	}

	public Id getId() {
		return this.pda.getId();
	}

	public Id getCurrentLinkId() {
		return this.pda.getCurrentLinkId();
	}

	public void endLegAndAssumeControl(double time) {
		this.pda.endLegAndAssumeControl(time);
	}

	public Id chooseNextLinkId() {
		return this.pda.chooseNextLinkId();
	}



}
