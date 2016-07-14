package playground.gregor.ctsim.debug;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.gregor.ctsim.run.CTRunner;
import playground.gregor.ctsim.simulation.physics.*;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;

import java.util.ArrayList;
import java.util.List;

public class Debugger2 {

	public static void main(String[] args) {

		CTRunner.DEBUG = true;

		Config c = ConfigUtils.createConfig();

		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		createNetwork(sc);
		EventsManagerImpl em = new EventsManagerImpl();

		if (CTRunner.DEBUG) {
//			Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
//			Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
//
//
//			sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
			EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
			InfoBox iBox = new InfoBox(dbg, sc);
			dbg.addAdditionalDrawer(iBox);
			//		dbg.addAdditionalDrawer(new Branding());
			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
			dbg.addAdditionalDrawer(qDbg);


			em.addHandler(qDbg);
			em.addHandler(dbg);
		}

		CTNetwork net = new CTNetwork(sc.getNetwork(), em, null);
		double coeff = 0.05;
		int nrPeds = 0;
		{
			CTLink link = net.getLinks().get(Id.createLinkId("0"));
			List<Id<Link>> links = new ArrayList<>();
			links.add(Id.createLinkId(0));
			links.add(Id.createLinkId(1));
			links.add(Id.createLinkId(2));
			links.add(Id.createLinkId(3));
			for (CTCell cell : link.getCells()) {

				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("r" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("1"));
			List<Id<Link>> links = new ArrayList<>();

			links.add(Id.createLinkId(1));
			links.add(Id.createLinkId(2));
			links.add(Id.createLinkId(3));
			links.add(Id.createLinkId(0));
			for (CTCell cell : link.getCells()) {

				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("r" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("2"));
			List<Id<Link>> links = new ArrayList<>();


			links.add(Id.createLinkId(2));
			links.add(Id.createLinkId(3));
			links.add(Id.createLinkId(0));
			links.add(Id.createLinkId(1));
			for (CTCell cell : link.getCells()) {

				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("r" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("3"));
			List<Id<Link>> links = new ArrayList<>();


			links.add(Id.createLinkId(3));
			links.add(Id.createLinkId(0));
			links.add(Id.createLinkId(1));
			links.add(Id.createLinkId(2));
			for (CTCell cell : link.getCells()) {

				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("r" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("4"));
			List<Id<Link>> links = new ArrayList<>();


			links.add(Id.createLinkId(4));
			links.add(Id.createLinkId(7));
			links.add(Id.createLinkId(6));
			links.add(Id.createLinkId(5));
			for (CTCell cell : link.getCells()) {
				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("b" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("5"));
			List<Id<Link>> links = new ArrayList<>();

			links.add(Id.createLinkId(5));
			links.add(Id.createLinkId(4));
			links.add(Id.createLinkId(7));
			links.add(Id.createLinkId(6));
			for (CTCell cell : link.getCells()) {
				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("b" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("6"));
			List<Id<Link>> links = new ArrayList<>();
			links.add(Id.createLinkId(6));
			links.add(Id.createLinkId(5));
			links.add(Id.createLinkId(4));
			links.add(Id.createLinkId(7));
			for (CTCell cell : link.getCells()) {
				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("b" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		{
			CTLink link = net.getLinks().get(Id.createLinkId("7"));
			List<Id<Link>> links = new ArrayList<>();
			links.add(Id.createLinkId(7));
			links.add(Id.createLinkId(6));
			links.add(Id.createLinkId(5));
			links.add(Id.createLinkId(4));
			for (CTCell cell : link.getCells()) {
				int n = cell.getN();
				int rnd = (int) (n * coeff * MatsimRandom.getRandom().nextDouble());///4;//n/3;//(int) (MatsimRandom.getRandom().nextInt(n)*.5);
				for (int i = 0; i < rnd; i++) {

					DriverAgent walker = new SimpleCTNetworkWalker(links, Id.createPersonId("b" + nrPeds++));

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}

		}
		System.out.println(nrPeds);
		long start = System.nanoTime();
		net.run();
		long stop = System.nanoTime();
		System.out.println("sim took:" + ((stop - start) / 1000. / 1000.) + " ms");
//		LineSegment ls = new LineSegment();
//		ls.x0 = 0;
//		ls.x1 = 0;
//		ls.y1 = 0;
//		ls.x1 = -10;
//		LineEvent le = new LineEvent(0,ls,true,0,128,128,255,0);
//		em.processEvent(le);



	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		double length = 60;
		double width = 20;

		int id = 0;
		Node n0 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(0, 0));
		Node n1 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(length, 0));
		Node n2 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(length, length));
		Node n3 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(0, length));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		net.addNode(n3);
		id = 0;
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n0, n1);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n1, n2);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n2, n3);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n3, n0);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n1, n0);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n2, n1);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n3, n2);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		{
			Link l0 = fac.createLink(Id.createLinkId(id++), n0, n3);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}

	}

}
