/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalSim2DSectionTest.java
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class PhysicalSim2DSectionTest {



	private PhysicalSim2DSection psec0;
	private PhysicalSim2DSection psec1;
	private PhysicalSim2DSection psec2;
	private Envelope e;

	@Test
	public void testNeighborsAlgorithm(){
		reset();
		DummyAgent agentInQuestion = new DummyAgent(4.5f, 6.5f);
		this.psec1.getAgents().add(agentInQuestion);
		agentInQuestion.setPSec(this.psec1);

		DummyAgent a0 = new DummyAgent(4.5f, 2f); //dist to agentInQuestion 4.5f
		this.psec0.getAgents().add(a0);
		a0.setPSec(this.psec0);

		DummyAgent a1 = new DummyAgent(2.5f, 6.5f); //dist to agentInQuestion 2.f
		this.psec1.getAgents().add(a1);
		a1.setPSec(this.psec1);

		DummyAgent a2 = new DummyAgent(-2.f, 6.5f); //dist to agentInQuestion 6.5f
		this.psec2.getAgents().add(a2);
		a2.setPSec(this.psec2);

		DummyAgent a5 = new DummyAgent(-3.5f, 6.5f); //dist to agentInQuestion 8.f
		this.psec2.getAgents().add(a5);
		a5.setPSec(this.psec2);
		
		DummyAgent a3 = new DummyAgent(0.5f, 4.f); //not visible from agentInQuestion
		this.psec0.getAgents().add(a3);
		a3.setPSec(this.psec0);

		DummyAgent a4 = new DummyAgent(-3.f, 3f); //not visible from agentInQuestion
		this.psec2.getAgents().add(a4);
		a4.setPSec(this.psec2);

		
		VisDebugger visd = new VisDebugger();
		this.psec0.debug(visd);
		this.psec1.debug(visd);
		this.psec2.debug(visd);
		visd.update();
		
		Neighbors ncalc = new Neighbors();
		ncalc.setRangeAndMaxNrOfNeighbors(10, 3);
		List<Tuple<Float, Sim2DAgent>> neighbors = ncalc.computeNeighbors(agentInQuestion);
		assertEquals(neighbors.size(),3);
		assertEquals(a1,neighbors.get(0).getSecond());
		assertEquals(a0,neighbors.get(1).getSecond());
		assertEquals(a2,neighbors.get(2).getSecond());
	}

	private void reset() {
		GeometryFactory geofac = new GeometryFactory();

		//3 sections
		int level = 0;
		Sim2DConfig conf = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc = Sim2DScenarioUtils.createSim2dScenario(conf);
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(new IdImpl("env0"));
		sc.addSim2DEnvironment(env);

		Id id0 = new IdImpl(0);
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);


		Coordinate c00 = new Coordinate(0,0);
		Coordinate c01 = new Coordinate(0,4);
		Coordinate c02 = new Coordinate(4,5);
		Coordinate c03 = new Coordinate(5,5);
		Coordinate c04 = new Coordinate(5,0);
		Coordinate [] coords0 = {c00,c01,c02,c03,c04,c00};
		LinearRing lr0 = geofac.createLinearRing(coords0);
		Polygon p0 = geofac.createPolygon(lr0, null);
		Section sec0 = env.createAndAddSection(id0, p0, new int []{2}, new Id[]{id1}, level);

		Coordinate c10 = new Coordinate(5,5);
		Coordinate c11 = new Coordinate(4,5);
		Coordinate c12 = new Coordinate(0,6);
		Coordinate c13 = new Coordinate(0,7);
		Coordinate c14 = new Coordinate(2,8);
		Coordinate c15 = new Coordinate(6,8);
		Coordinate c16 = new Coordinate(6,5);
		Coordinate [] coords1 = {c10,c11,c12,c13,c14,c15,c16,c10};
		LinearRing lr1 = geofac.createLinearRing(coords1);
		Polygon p1 = geofac.createPolygon(lr1, null);
		Section sec1 = env.createAndAddSection(id1, p1, new int[]{0,2}, new Id[]{id0,id2}, level);

		Coordinate c20 = new Coordinate(0,7);
		Coordinate c21 = new Coordinate(0,6);
		Coordinate c22 = new Coordinate(-1,5);
		Coordinate c23 = new Coordinate(-4,0);
		Coordinate c24 = new Coordinate(-4,8);
		Coordinate c25 = new Coordinate(0,8);
		Coordinate [] coords2 = {c20,c21,c22,c23,c24,c25,c20};
		LinearRing lr2 = geofac.createLinearRing(coords2);
		Polygon p2 = geofac.createPolygon(lr2, null);
		Section sec2 = env.createAndAddSection(id2, p2, new int[]{0},new Id[]{id1}, level);

		//network
		Config mc = ConfigUtils.createConfig();
		Scenario msc = ScenarioUtils.createScenario(mc );
		NetworkImpl net = (NetworkImpl) msc.getNetwork();
		Node n0 = net.createAndAddNode(new IdImpl(0), new CoordImpl(1,1));
		Node n1 = net.createAndAddNode(new IdImpl(1), new CoordImpl(4.5,5));
		Node n2 = net.createAndAddNode(new IdImpl(2), new CoordImpl(4.5,6.5));
		Node n3 = net.createAndAddNode(new IdImpl(3), new CoordImpl(0,6.5));
		Node n4 = net.createAndAddNode(new IdImpl(4), new CoordImpl(-3,5));
		Link l0a = net.createAndAddLink(new IdImpl("0a"), n0, n1, 0, 0, 0, 0);
		Link l0b = net.createAndAddLink(new IdImpl("0b"), n1, n0, 0, 0, 0, 0);
		Link l1a = net.createAndAddLink(new IdImpl("1a"), n1, n2, 0, 0, 0, 0);
		Link l1b = net.createAndAddLink(new IdImpl("1b"), n2, n1, 0, 0, 0, 0);
		Link l2a = net.createAndAddLink(new IdImpl("2a"), n2, n3, 0, 0, 0, 0);
		Link l2b = net.createAndAddLink(new IdImpl("2b"), n3, n2, 0, 0, 0, 0);
		Link l3a = net.createAndAddLink(new IdImpl("3a"), n3, n4, 0, 0, 0, 0);
		Link l3b = net.createAndAddLink(new IdImpl("3b"), n4, n3, 0, 0, 0, 0);
		sec0.addRelatedLinkId(l0a.getId());
		sec0.addRelatedLinkId(l0b.getId());
		sec1.addRelatedLinkId(l1a.getId());
		sec1.addRelatedLinkId(l1b.getId());
		sec1.addRelatedLinkId(l2a.getId());
		sec1.addRelatedLinkId(l2b.getId());		
		sec2.addRelatedLinkId(l3a.getId());
		sec2.addRelatedLinkId(l3b.getId());
		env.setNetwork(net);

		this.e = new Envelope(-4,6,0,8);
		env.setEnvelope(this.e);

		sc.connect(msc);

		PhysicalSim2DEnvironment penv = new PhysicalSim2DEnvironment(env, sc, null);
		this.psec0 = penv.getPhysicalSim2DSectionAssociatedWithLinkId(l0a.getId());
		this.psec1 = penv.getPhysicalSim2DSectionAssociatedWithLinkId(l1a.getId());
		this.psec2 = penv.getPhysicalSim2DSectionAssociatedWithLinkId(l3a.getId());

	}

	private class DummyAgent implements Sim2DAgent{


		private final float [] pos = {0,0};

		private PhysicalSim2DSection currentPSec;

		public DummyAgent(float spawnX, float spawnY) {
			this.pos[0] = (float) (spawnX - PhysicalSim2DSectionTest.this.e.getMinX());
			this.pos[1] = (float) (spawnY - PhysicalSim2DSectionTest.this.e.getMinY());
		}

		@Override
		public QVehicle getQVehicle() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public void updateVelocity() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
			this.currentPSec = physicalSim2DSection;

		}

		@Override
		public void move(float dx, float dy) {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public float[] getVelocity() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public Id getCurrentLinkId() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public float[] getPos() {
			return this.pos;
		}

		@Override
		public Id chooseNextLinkId() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public Id getId() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public void notifyMoveOverNode(Id nextLinkId) {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public void debug(VisDebugger visDebugger) {
			visDebugger.addCircle(this.getPos()[0], this.getPos()[1], .5f, 192, 0, 64, 128);

		}

		@Override
		public PhysicalSim2DSection getPSec() {
			return this.currentPSec;
		}


	}

}


