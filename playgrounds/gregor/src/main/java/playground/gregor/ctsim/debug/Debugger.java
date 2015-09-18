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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.gregor.ctsim.simulation.physics.CTNetwork;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class Debugger {

	public static void main(String [] args) {


		Config c = ConfigUtils.createConfig();

		c.global().setCoordinateSystem("EPSG:3395");
		Scenario sc = ScenarioUtils.createScenario(c);

		createNetwork(sc);

		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);


		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
		//		InfoBox iBox = new InfoBox(dbg, sc);
		//		dbg.addAdditionalDrawer(iBox);
		//		dbg.addAdditionalDrawer(new Branding());
		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
		dbg.addAdditionalDrawer(qDbg);

		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(qDbg);
		em.addHandler(dbg);

		CTNetwork net = new CTNetwork(sc.getNetwork(), em);
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
		double length = 20;
		double offset = 15;
		double width = 3;
		int id = 0;
		for (double rot = 0; rot < 2*Math.PI; rot += Math.PI/4)
			
		{
			double cs = Math.cos(rot);
			double sn = Math.sin(rot);
			double xx = x1*cs - y1 * sn;
			double yy = x1*sn + y1*cs;
			
			double dx = (xx-x1)/length;
			double dy = (yy-y1)/length;
			
			Node n0 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(offset*dx, offset*dy));
			net.addNode(n0);
			Node n1 = fac.createNode(Id.createNodeId(id++), CoordUtils.createCoord(xx+offset*dx, yy+offset*dy));
			net.addNode(n1);
			Link l0 = fac.createLink(Id.createLinkId(id++),n0, n1);
			net.addLink(l0);
			l0.setCapacity(width);
			l0.setLength(length);
		}
		
		
		

	}

}
