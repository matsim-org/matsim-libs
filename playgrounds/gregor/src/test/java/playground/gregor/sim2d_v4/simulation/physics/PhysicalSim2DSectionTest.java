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

import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

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

	@Ignore @Test
	public void testNeighborsAlgorithm(){
		reset();
		QVehicle qveh = new QVehicle(new Vehicle() {
			
			@Override
			public Id<Vehicle> getId() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public VehicleType getType() {
				// TODO Auto-generated method stub
				return null;
			}
		});
		DummyAgent agentInQuestion = new DummyAgent(4.5f, 6.5f, qveh);
		this.psec1.getAgents().add(agentInQuestion);
		agentInQuestion.setSec(this.psec1);

		DummyAgent a0 = new DummyAgent(4.5f, 2f,qveh); //dist to agentInQuestion 4.5f
		this.psec0.getAgents().add(a0);
		a0.setSec(this.psec0);

		DummyAgent a1 = new DummyAgent(2.5f, 6.5f,qveh); //dist to agentInQuestion 2.f
		this.psec1.getAgents().add(a1);
		a1.setSec(this.psec1);

		DummyAgent a2 = new DummyAgent(-2.f, 6.5f,qveh); //dist to agentInQuestion 6.5f
		this.psec2.getAgents().add(a2);
		a2.setSec(this.psec2);

		DummyAgent a5 = new DummyAgent(-3.5f, 6.5f,qveh); //dist to agentInQuestion 8.f
		this.psec2.getAgents().add(a5);
		a5.setSec(this.psec2);
		
		DummyAgent a3 = new DummyAgent(0.5f, 4.f,qveh); //not visible from agentInQuestion
		this.psec0.getAgents().add(a3);
		a3.setSec(this.psec0);

		DummyAgent a4 = new DummyAgent(-3.f, 3f,qveh); //not visible from agentInQuestion
		this.psec2.getAgents().add(a4);
		a4.setSec(this.psec2);
		
//		this.psec0.updatedTwoDTree();
//		this.psec1.updatedTwoDTree();
//		
//		this.psec2.updatedTwoDTree();
//		
//		Neighbors ncalc = new Neighbors(agentInQuestion,Sim2DConfigUtils.createConfig());
//		ncalc.setRangeAndMaxNrOfNeighbors(10, 3);
//		List<Tuple<Double, Sim2DAgent>> neighbors = ncalc.getNeighbors();
//		assertEquals(neighbors.size(),3);
//		assertEquals(a1,neighbors.get(0).getSecond());
//		assertEquals(a0,neighbors.get(1).getSecond());
//		assertEquals(a2,neighbors.get(2).getSecond());
//		Log.warn("test disabled!");
	}

	private void reset() {
		GeometryFactory geofac = new GeometryFactory();

		//3 sections
		int level = 0;
		Sim2DConfig conf = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc = Sim2DScenarioUtils.createSim2dScenario(conf);
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setId(Id.create("env0", Sim2DEnvironment.class));
		sc.addSim2DEnvironment(env);

		Id<Section> id0 = Id.create(0, Section.class);
		Id<Section> id1 = Id.create(1, Section.class);
		Id<Section> id2 = Id.create(2, Section.class);


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
		Node n0 = net.createAndAddNode(Id.create(0, Node.class), new Coord((double) 1, (double) 1));
		Node n1 = net.createAndAddNode(Id.create(1, Node.class), new Coord(4.5, (double) 5));
		Node n2 = net.createAndAddNode(Id.create(2, Node.class), new Coord(4.5, 6.5));
		Node n3 = net.createAndAddNode(Id.create(3, Node.class), new Coord((double) 0, 6.5));
		final double x = -3;
		Node n4 = net.createAndAddNode(Id.create(4, Node.class), new Coord(x, (double) 5));
		Link l0a = net.createAndAddLink(Id.create("0a", Link.class), n0, n1, 0, 0, 0, 0);
		Link l0b = net.createAndAddLink(Id.create("0b", Link.class), n1, n0, 0, 0, 0, 0);
		Link l1a = net.createAndAddLink(Id.create("1a", Link.class), n1, n2, 0, 0, 0, 0);
		Link l1b = net.createAndAddLink(Id.create("1b", Link.class), n2, n1, 0, 0, 0, 0);
		Link l2a = net.createAndAddLink(Id.create("2a", Link.class), n2, n3, 0, 0, 0, 0);
		Link l2b = net.createAndAddLink(Id.create("2b", Link.class), n3, n2, 0, 0, 0, 0);
		Link l3a = net.createAndAddLink(Id.create("3a", Link.class), n3, n4, 0, 0, 0, 0);
		Link l3b = net.createAndAddLink(Id.create("3b", Link.class), n4, n3, 0, 0, 0, 0);
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

	private class DummyAgent extends Sim2DAgent{


		private final double [] pos = {0,0};

		private PhysicalSim2DSection currentPSec;

		public DummyAgent(double spawnX, double spawnY, QVehicle veh) {
			super(null, veh, spawnX, spawnY, null, null);
			this.pos[0] = (spawnX - PhysicalSim2DSectionTest.this.e.getMinX());
			this.pos[1] = (spawnY - PhysicalSim2DSectionTest.this.e.getMinY());
		}

		@Override
		public QVehicle getQVehicle() {
			throw new RuntimeException("don't call this method!");
		}


		@Override
		public void setSec(PhysicalSim2DSection physicalSim2DSection) {
			this.currentPSec = physicalSim2DSection;

		}

		@Override
		public boolean move(double dx, double dy, double time) {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public double[] getVelocity() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public Id<Link> getCurrentLinkId() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public double[] getPos() {
			return this.pos;
		}

		@Override
		public Id<Link> chooseNextLinkId() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public Id<Person> getId() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public void notifyMoveOverNode(Id<Link> nextLinkId) {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public PhysicalSim2DSection getPSec() {
			return this.currentPSec;
		}

		@Override
		public double getRadius() {
			throw new RuntimeException("don't call this method!");
		}

		@Override
		public double getX() {
			return this.pos[0];
		}

		@Override
		public double getY() {
			return this.pos[1];
		}

		@Override
		public void setDesiredSpeed(double v) {
			// TODO Auto-generated method stub
			
		}


	}

}


