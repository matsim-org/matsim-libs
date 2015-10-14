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
import playground.gregor.ctsim.simulation.physics.*;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class Debugger {

	public static void main(String[] args) {


		Config c = ConfigUtils.createConfig();

		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		createNetwork(sc);

		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);


		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
		InfoBox iBox = new InfoBox(dbg, sc);
		dbg.addAdditionalDrawer(iBox);
		//		dbg.addAdditionalDrawer(new Branding());
		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		dbg.addAdditionalDrawer(qDbg);

		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(qDbg);
		em.addHandler(dbg);

		CTNetwork net = new CTNetwork(sc.getNetwork(), em, null);

		int nrPeds = 0;
		for (CTLink link : net.getLinks().values()) {
			List<Id<Link>> links = new ArrayList<>();
			links.add(link.getDsLink().getId());
			links.add(link.getUsLink().getId());
			for (CTCell cell : link.getCells()) {
				int n = cell.getN();
				int rnd = (int) (MatsimRandom.getRandom().nextInt(n) * 0.1);
				for (int i = 0; i < rnd; i++) {
					nrPeds++;
					DriverAgent walker = new SimpleCTNetworkWalker(links);

					CTPed ped = new CTPed(cell, walker);
					cell.jumpOnPed(ped, 0);
				}
				cell.updateIntendedCellJumpTimeAndChooseNextJumper(0);
			}
		}
		System.out.println(nrPeds);
		net.run();
		System.out.println("done.");
//		LineSegment ls = new LineSegment();
//		ls.x0 = 0;
//		ls.x1 = 0;
//		ls.y1 = 0;
//		ls.x1 = -10;
//		LineEvent le = new LineEvent(0,ls,true,0,128,128,255,0);
//		em.processEvent(le);

		try {
			Thread.sleep(10000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static void createNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		NetworkFactory fac = net.getFactory();

		double x1 = 20;
		double y1 = 0;
		double length = 30;
		double offset = 15;
		double width = 6;
		int id = 0;
		for (double rot = 0; rot < 2 * Math.PI; rot += Math.PI / 4)

		{
			double cs = Math.cos(rot);
			double sn = Math.sin(rot);
			double xx = x1 * cs - y1 * sn;
			double yy = x1 * sn + y1 * cs;

			double dx = (xx - x1) / length;
			double dy = (yy - y1) / length;

			Node n0 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(offset * dx, offset * dy));
			net.addNode(n0);
			Node n1 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(xx + offset * dx, yy + offset * dy));
			net.addNode(n1);
			{
				Link l0 = fac.createLink(Id.createLinkId(id++), n0, n1);
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
		}


	}

}
