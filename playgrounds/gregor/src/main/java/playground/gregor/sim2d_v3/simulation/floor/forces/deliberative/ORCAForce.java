/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAForce.java
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

package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v3.scenario.MyDataContainer;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v3.simulation.floor.forces.DynamicForceModule;

public class ORCAForce implements DynamicForceModule{

	private final QuadTree<Agent2D> agentsQuad;
	private final PhysicalFloor floor;

	//	private boolean debug = false;
	int count = 0;


	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final double tau = 2;

	public ORCAForce(PhysicalFloor floor, Scenario sc) {
		this.floor = floor;
		double maxX = 1000*sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxEasting();
		double minX = -1000 + sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinEasting();
		double maxY = 1000*sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxNorthing();
		double minY = -1000 + sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinNorthing();
		this.agentsQuad = new QuadTree<Agent2D>(minX, minY, maxX, maxY);
	}


	@Override
	public void run(Agent2D agent, double time) {
		// TODO Auto-generated method stub
		List<ORCAabTauDbg> contraints = getORCAs(agent);
		
	}

	private List<ORCAabTauDbg> getORCAs(Agent2D agent) {

		double sensingRange = agent.getSensingRange();
		Collection<Agent2D> l = this.agentsQuad.get(agent.getPosition().x, agent.getPosition().y, sensingRange);

		List<ORCAabTauDbg> ret = new ArrayList<ORCAabTauDbg>(l.size()-1);
		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			ORCAabTauDbg orca = new ORCAabTauDbg(agent,other,this.tau);
			ret.add(orca);
			if (agent.getDelegate().getId().toString().equals("g1")) {
				orca.gisDump();
				String num = null;
				if (this.count < 10) {
					num = "000" + this.count;
				} else if (this.count < 100) {
					num = "00" + this.count;
				}else if (this.count < 1000) {
					num = "0" + this.count;
				} else {
					num = this.count+"";
				}
				this.count++;
				GisDebugger.dump("/Users/laemmel/devel/OCRA/dbg/dbg" + num + ".shp");
			}
		}


		return ret;
	}


	@Override
	public void init() {
		// TODO Auto-generated method stub

	}



	@Override
	public void update(double time) {
		if (time >= this.lastQuadUpdate + this.quadUpdateInterval) {

			updateAgentQuadtree();

			this.lastQuadUpdate = time;
		}

	}

	@Override
	public void forceUpdate() {
		// TODO Auto-generated method stub

	}


	protected void updateAgentQuadtree() {

		this.agentsQuad.clear();
		for (Agent2D agent : this.floor.getAgents()) {
			this.agentsQuad.put(agent.getPosition().x, agent.getPosition().y, agent);
		}

	}
}
