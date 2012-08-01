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
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;

import playground.gregor.sim2d_v3.scenario.MyDataContainer;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v3.simulation.floor.forces.DynamicForceModule;

public class ORCAForce implements DynamicForceModule{

	private final QuadTree<Agent2D> agentsQuad;
	private final PhysicalFloor floor;

	private final ORCAProblemSolverII solver = new ORCAProblemSolverII();

	//	private boolean debug = false;
	int count = 0;


	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	private final float tau = 2f;
	private final PathAndDrivingAcceleration driver;
	private final QuadTree<float[]> envSegQuad;
//	private final GuiDebuggerFrame debugger;

//	public static boolean DEBUG = false;

	public ORCAForce(PhysicalFloor floor, Scenario sc) {

//		//DEBUG
//		this.debugger = new GuiDebuggerFrame();
//		this.debugger.setVisible(true);

		this.floor = floor;
		this.driver = new PathAndDrivingAcceleration(floor,sc);
		double maxX = 1000*sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxEasting();
		double minX = -1000 + sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinEasting();
		double maxY = 1000*sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMaxNorthing();
		double minY = -1000 + sc.getScenarioElement(MyDataContainer.class).getDenseCoordsQuadTree().getMinNorthing();
		this.agentsQuad = new QuadTree<Agent2D>(minX, minY, maxX, maxY);

//		this.timeStepSize = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTimeStepSize();
		this.envSegQuad = sc.getScenarioElement(MyDataContainer.class).getFloatSegQuad();
//		for (float[] seg  : this.envSegQuad.values()) {
//			GuiDebugger.addObstacle(seg);
//		}
	}


	@Override
	public void run(Agent2D agent, double time) {


//		//DEBUG
//		GuiDebugger.setReferencePoint((float)agent.getPosition().x, (float)agent.getPosition().y);

		double[] df = this.driver.getDesiredVelocity(agent);
		double vxPref = df[0];//agent.getVx() + this.timeStepSize*(df[0] - agent.getVx())/0.5;
		double vyPref = df[1];//agent.getVy() + this.timeStepSize*(df[1] - agent.getVy())/0.5;
		List<ORCALine> constraints = getORCAs(agent);

		float[] v = this.solver.run(constraints, (float)vxPref, (float)vyPref,2.0f);


//		if (agent.getDelegate().getId().toString().equals("g0")){
//			for (Agent2D a : this.floor.getAgents()) {
//				GuiDebugger.addIgnoredAgent(a);
//			}
//			GuiDebugger.dump = true;
//			while (GuiDebugger.dump) {
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		} else {
//			GuiDebugger.reset();
//		}

		if (!Double.isNaN(v[0])) {
			agent.getForce().reset();
			agent.getForce().setVx(v[0]);
			agent.getForce().setVy(v[1]);
			//			double fx = (v[0]-agent.getVx())/0.5;//todo use tau from config!!
			//			double fy = (v[1]-agent.getVy())/0.5;//todo use tau from config!!
			//			agent.getForce().incrementX(fx);
			//			agent.getForce().incrementY(fy);


		} else {
			agent.getForce().setVy(0);
			agent.getForce().setVx(0);
			agent.getForce().reset();
			//			double fx = 80*(0-agent.getVx())/0.5;//todo use tau from config!!
			//			double fy = 80*(0-agent.getVx())/0.5;//todo use tau from config!!
			//			agent.getForce().incrementX(fx);
			//			agent.getForce().incrementY(fy);
		}

		//		double xb = agent.getPosition().x;
		//		double yb = agent.getPosition().y;
		//		double xa = xb + v[0]*this.timeStepSize;
		//		double ya = yb + v[1]*this.timeStepSize;
		//		for (float[] seg : this.envSegQuad.values()) {
		//			Coordinate sss = new Coordinate();
		//			boolean intersect = Algorithms.computeLineIntersection(new Coordinate(xb,yb), new Coordinate(xa,ya), new Coordinate(seg[0],seg[1]),new Coordinate(seg[2],seg[3]), sss);
		//
		//			
		//			if (intersect) {
		//				System.out.println("got you!");
		//			}
		//		}
	}


	private List<ORCALine> getORCAs(Agent2D agent) {

		double sensingRange = agent.getSensingRange();
		Collection<Agent2D> l = this.agentsQuad.get(agent.getPosition().x, agent.getPosition().y, sensingRange);

		if (l.size() > 8) {
			agent.setSensingRange(sensingRange*0.9);
		} if (l.size() < 6) {
			agent.setSensingRange(sensingRange*1.5);
		}

		List<ORCALine> ret = new ArrayList<ORCALine>();
		for (Agent2D other : l) {





			if (other == agent) {
				continue;
			}
			ORCALine ooo = new ORCALineAgent(agent, other, this.tau);
			ret.add(ooo);

//			GuiDebugger.addORCA(ooo);
			//			if (agent.getDelegate().getId().toString().equals("g6")){
			//				GuiDebugger.addAgent(other);
			//				GuiDebugger.peek = true;
			//				try {
			//					Thread.sleep(1000);
			//				} catch (InterruptedException e) {
			//					// TODO Auto-generated catch block
			//					e.printStackTrace();
			//				}
			//			}
		}

		Collection<float[]> m = new HashSet<float[]>();
		sensingRange=3;
		Rect rect = new Rect(agent.getPosition().x-sensingRange, agent.getPosition().y-sensingRange, agent.getPosition().x+sensingRange, agent.getPosition().y+sensingRange);
		this.envSegQuad.get(rect , m);
		for (float [] mm : m) {
			ORCALine env = new ORCALineEnvironment(agent,mm,this.tau);
			ret.add(env);

//			GuiDebugger.addORCA(env);

//			if (agent.getDelegate().getId().toString().equals("g6")){
//				GuiDebugger.peek = true;
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
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
