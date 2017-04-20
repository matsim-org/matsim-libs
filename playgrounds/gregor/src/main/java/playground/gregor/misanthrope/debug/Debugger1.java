package playground.gregor.misanthrope.debug;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.misanthrope.run.CTRunner;
import playground.gregor.misanthrope.simulation.physics.*;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;

import java.util.ArrayList;
import java.util.List;

public class Debugger1 {

    public static void main(String[] args) {

        String netFile = "/Users/laemmel/scenarios/misanthrope/paper/network.xml";

        CTRunner.DEBUG = true;

        Config c = ConfigUtils.createConfig();

        c.global().setCoordinateSystem("EPSG:3395");
        Scenario sc = ScenarioUtils.createScenario(c);

        new MatsimNetworkReader(sc.getNetwork()).readFile(netFile);

        EventsManagerImpl em = new EventsManagerImpl();

        if (CTRunner.DEBUG) {

            EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
            InfoBox iBox = new InfoBox(dbg, sc);
//            dbg.addAdditionalDrawer(iBox);
            //		dbg.addAdditionalDrawer(new Branding());
//			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
//			dbg.addAdditionalDrawer(qDbg);


//			em.addHandler(qDbg);
            em.addHandler(dbg);
        }

        CTNetwork net = new CTNetwork(sc.getNetwork(), em, null);
        double coeff = 0.05;
        int nrPeds = 0;
        {
            CTLink link = net.getLinks().get(Id.createLinkId("2716"));
            List<Id<Link>> links = new ArrayList<>();
            links.add(Id.createLinkId(2716));
            links.add(Id.createLinkId(102716));
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


}
