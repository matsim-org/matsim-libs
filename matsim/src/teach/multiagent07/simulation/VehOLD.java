/* *********************************************************************** *
 * project: org.matsim.*
 * VehOLD.java
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

package teach.multiagent07.simulation;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.trafficNet.TrafficLinkI;
import org.matsim.utils.vis.netvis.DrawableAgentI;

import teach.multiagent07.population.Person;

public class VehOLD implements  DrawableAgentI {

	protected Person agent;
	protected CAMobSim sim = null;

	public VehOLD() {
		int num = (int)Math.round(Math.random());
		this.agent = new Person(Integer.toString(num));
	}

	public VehOLD(Person agent2, CAMobSim sim) {
		this.sim = sim;
		this.agent = agent2;
	}

	public CAMobSim getSim() {
		return sim;
	}

	public Id getId() {
		return agent.getId();
	}

	public int getDepartureTime_s() {
		// TODO Auto-generated method stub
		return 0;
	}

	public TrafficLinkI getDepartureLink() {
		// TODO Auto-generated method stub
		return null;
	}

	public void leaveActivity() {
		// TODO Auto-generated method stub

	}

	public void reachActivity(){
		// TODO Auto-generated method stub

	}

	public void setCurrentLink(BasicLinkImpl link) {
	}

	public TrafficLinkI chooseNextLink() {
		// TODO Auto-generated method stub
		return null;
	}

	public TrafficLinkI getDestinationLink() {
		// TODO Auto-generated method stub
		return null;
	}

	public void kill() {
		// TODO Auto-generated method stub

	}

	public boolean isDead() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasNextLink() {
		// TODO Auto-generated method stub
		return false;
	}



	// ////////////////////////////////////////////////////////////////////
    // DrawableAgentI Stuff
    // ////////////////////////////////////////////////////////////////////
	public double posInLink_m;
	// private MobsimAgentI driver;

	public void setPosInLink_m(double posInLink_m) {
		this.posInLink_m = posInLink_m;
	}

	public double getPosInLink_m() {
		return posInLink_m;
	}

	public int getLane() {
		return 0;
	}
}
