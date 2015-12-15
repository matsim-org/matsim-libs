/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQAdapterLink;
import playground.gregor.sim2d_v4.cgal.TwoDObject;
import playground.gregor.sim2d_v4.cgal.VoronoiCell;
import playground.gregor.sim2d_v4.cgal.VoronoiCenter;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.LinkSwitcher;

						//EXPERIMENTAL [GL Oct'13]
public class Sim2DAgent implements VoronoiCenter, TwoDObject {
	
	//testing only
	@Deprecated
	private final double vCoeff = 1;//1+Math.min(Math.max(-0.25, MatsimRandom.getRandom().nextGaussian()*.1),.25);
	
//	private final double vStd
	private final double [] pos = {0,0};
	private final double [] v = {0,0};
	private final QVehicle veh;
	private final MobsimDriverAgent driver;
	private final double r;
	private final double height = 1.72 + MatsimRandom.getRandom().nextGaussian()*0.1; //TODO find a meaningful value here [gl April '13]
							private final Scenario sc;
	
//	private Id currentLinkId;
//
//	private LinkInfo cachedLi;
	private final LinkSwitcher ls;
	private final PhysicalSim2DEnvironment pEnv;
							private final Id<Person> id;
							/*package*/ int ttl; //workaround - think about this [gl August '13]
							private double v0 = 1.34 * this.vCoeff;
	private boolean hasLeft2DSim = false;
	private VelocityUpdater vu;
	private boolean emitPosEvents = true;
	private VoronoiCell voronoiCell;
	private PhysicalSim2DSection currentPSec;
	
	
	//Dummy
	public Sim2DAgent(Id<Person> id) {
		this.id = id;
		this.r = 0.2;
		this.veh = null;
		this.driver = null;
		this.sc = null;
		this.ls = null;
		this.pEnv = null;
		this.vu = null;
	}
	
	
	public Sim2DAgent(Scenario sc, QVehicle veh, double spawnX, double spawnY, LinkSwitcher ls, PhysicalSim2DEnvironment pEnv) {
		this.r = MatsimRandom.getRandom().nextDouble()*.1 + 0.25;//.25;//.19;//MatsimRandom.getRandom().nextDouble()*.1 + 0.25; //radius//.25; //0.19; //MatsimRandom.getRandom().nextDouble()*.1 + 0.25; //radius
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.id = this.driver.getId();
		this.sc = sc;
		this.ls = ls;
		this.pEnv = pEnv;
		this.vu = new SimpleVelocityUpdater(this, ls, sc);
	}

	public Sim2DAgent(Scenario sc, QVehicle veh, double spawnX,
			double spawnY, LinkSwitcher ls, PhysicalSim2DEnvironment pEnv,
			double radius) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.r = radius;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.id = this.driver.getId();
		this.sc = sc;
		this.ls = ls;
		this.pEnv = pEnv;
		this.vu = new SimpleVelocityUpdater(this, ls, sc);
		
	}

	public void setVelocityUpdater(VelocityUpdater vu) {
		this.vu = vu;
	}
	
	public void updateVelocity() {
//		if (this.getId().toString().equals("a118") && this.getPos()[1] > 3.8) {
//			System.out.println("stop");
//		}
		this.vu.updateVelocity();
	}

							public void setSec(PhysicalSim2DSection pSec) {
								this.currentPSec = pSec;
							}



//	public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
//		this.currentPSec = physicalSim2DSection;
//	}
	
	public boolean move(double dx, double dy, double time) {
		if (this.ls.isSwitchLink(this.pos, dx, dy, this.getCurrentLinkId())) {
			Id<Link> nextLinkId = this.chooseNextLinkId();
			Sim2DQAdapterLink loResLink = this.pEnv.getLowResLink(nextLinkId);
			if (loResLink != null) { //HACK? we are in the agent's mental model but perform a physical sim2D --> qSim transition
				// this should be handled in the link's corresponding PhysicalSim2DSection [gl April '13]
				if (loResLink.isAcceptingFromUpstream()) {
					QVehicle veh = this.getQVehicle();
					veh.setCurrentLink(loResLink.getLink());
					loResLink.addFromUpstream(veh);
					this.hasLeft2DSim = true;
					this.ttl = 100;
					this.pEnv.getEventsManager().processEvent(new LinkLeaveEvent(time, this.veh.getId(), this.getCurrentLinkId()));
					this.notifyMoveOverNode(nextLinkId);
				} else {
					return false;
				}
			} else {
				this.pEnv.getEventsManager().processEvent(new LinkLeaveEvent(time, this.veh.getId(), this.getCurrentLinkId()));
				this.notifyMoveOverNode(nextLinkId);
				this.pEnv.getEventsManager().processEvent(new LinkEnterEvent(time, this.veh.getId(), nextLinkId));
			}
		}


		this.pos[0] += dx;
		this.pos[1] += dy;
		if (this.emitPosEvents) {
//			XYVxVyEventImpl e = new XYVxVyEventImpl(this.id, this.pos[0], this.pos[1], this.v[0], this.v[1], time,this);
			XYVxVyEventImpl e = new XYVxVyEventImpl(this.id, this.pos[0], this.pos[1], this.v[0], this.v[1],time);
			this.pEnv.getEventsManager().processEvent(e);
		}

		return true;
	}

							public QVehicle getQVehicle() {
								return this.veh;
							}

							public Id<Link> getCurrentLinkId() {
								return this.driver.getCurrentLinkId();
							}

							public Id<Link> chooseNextLinkId() {
								Id<Link> id = this.driver.chooseNextLinkId();
								return id;
							}

							public void notifyMoveOverNode(Id<Link> nextLinkId) {
								this.driver.notifyMoveOverNode(nextLinkId);
//		this.v0 = this.sc.getNetwork().getLinks().get(nextLinkId).getFreespeed()+(MatsimRandom.getRandom().nextDouble()*.1)-.05;
								this.setDesiredSpeed(this.sc.getNetwork().getLinks().get(nextLinkId).getFreespeed());
							}

	public void moveGhost(double dx, double dy, double time) {
		this.pos[0] += dx;
		this.pos[1] += dy;
		if (this.emitPosEvents) {
//			XYVxVyEventImpl e = new XYVxVyEventImpl(this.id, this.pos[0], this.pos[1], this.v[0], this.v[1], time,this);
			XYVxVyEventImpl e = new XYVxVyEventImpl(this.id, this.pos[0], this.pos[1], this.v[0], this.v[1], time);
//			this.pEnv.getEventsManager().processEvent(e);
		}
	}

	public double[] getVelocity() {
		return this.v;
	}

	public double[] getPos() {
		return this.pos;
	}

	public Id<Person> getId() {
		return this.id;
	}

	public PhysicalSim2DSection getPSec() {
		return this.currentPSec;
	}

	public double getRadius() {
		return this.r;
	}

	@Override
	public double getX() {
		return this.pos[0];
	}

	@Override
	public double getY() {
		return this.pos[1];
	}

							public double getDesiredSpeed() {
								return this.v0;
							}
	
	public void setDesiredSpeed(double v) {
//		System.out.println(v);
		this.v0 = v*this.vCoeff;

	}

	public boolean hasLeft2DSim() {
		return this.hasLeft2DSim ;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sim2DAgent) {
//			return getId().equals(((Sim2DAgent) obj).getId());
			return this == obj;
		}
		return false;
	}

							@Override
							public String toString() {
								return "id: " + this.driver.getId() + " sec:" + this.currentPSec.getId() + " link:" + this.getCurrentLinkId() + " pos:" + ((int) (this.pos[0] * 100 + .5)) / 100. + ":" + ((int) (this.pos[1] * 100 + .5)) / 100.;
							}
	
	public double getV0() {
		return this.v0;
	}
	
	public double getHeight() {
		return this.height;
	}
	
	public void setFocusOnAgent(boolean focus) {
		this.emitPosEvents = focus;
	}
	
	//DEBUG
	void reDrawAgent(double time){
		XYVxVyEventImpl e = new XYVxVyEventImpl(this.id, this.pos[0], this.pos[1], this.v[0], this.v[1], time);
		this.pEnv.getEventsManager().processEvent(e);
	}

	public double getActualSpeed() {
		return Math.sqrt(this.v[0]*this.v[0]+this.v[1]*this.v[1]);
	}

	//EXPERIMENTAL 
	@Override
	public VoronoiCell getVoronoiCell() {
		return this.voronoiCell;
	}

	@Override
	public void setVoronoiCell(VoronoiCell voronoiCell) {
		this.voronoiCell = voronoiCell;
		
	}
}
