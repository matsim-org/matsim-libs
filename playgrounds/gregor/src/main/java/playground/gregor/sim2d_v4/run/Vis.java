/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2D.java
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

package playground.gregor.sim2d_v4.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.experimental.HermesTrajectoryToEventsParser;
import playground.gregor.sim2d_v4.experimental.QuadTreePath;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class Vis {

	private QSimDensityDrawer qSimDrawer;

	public static void main(String [] args) {
		if (args.length != 3) {
			printUsage();
			System.exit(-1);
		}
		String sim2DConf = args[0];
		String qsimConf = args[1];
		Sim2DConfig sim2dc = Sim2DConfigUtils.loadConfig(sim2DConf);
		Sim2DScenario sim2dsc = Sim2DScenarioUtils.loadSim2DScenario(sim2dc);
		Config c = ConfigUtils.loadConfig(qsimConf);
		c.controler().setWriteEventsInterval(1);
//		c.controler().setLastIteration(300);
		Scenario sc = ScenarioUtils.loadScenario(c);
//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sim2dsc);
		sim2dsc.connect(sc);
		
//		c.qsim().setEndTime(120);
		c.qsim().setEndTime(23*3600);
//		c.qsim().setEndTime(41*60);//+30*60);

		
		EventsManager e = new EventsManagerImpl();

		//offsets needed to convert to doubles later in program
		double minX = Double.POSITIVE_INFINITY;
		double minY = minX;
		for (Node n : sc.getNetwork().getNodes().values()) {
			if (n.getCoord().getX() < minX) {
				minX = n.getCoord().getX(); 
			}
			if (n.getCoord().getY() < minY) {
				minY = n.getCoord().getY(); 
			}
		}
//		sim2dc.setOffsets(minX, minY);
		
		
//		controller.getEvents().addHandler(new SimSpeedObserver());


		
//		ShapeFileReader sr = new ShapeFileReader();
//		sr.readFileAndInitialize("/Users/laemmel/devel/gct/analysis/measurement_areas.shp");
//		
//		for (SimpleFeature a : sr.getFeatureSet()) {
//			FlowAreaAnalysis fa = new FlowAreaAnalysis((Geometry)a.getDefaultGeometry(), "/Users/laemmel/devel/gct_TRB/small_single/fnd"+a.getID(),controller.getEvents());
//			controller.getEvents().addHandler(fa);
//		}
//		CrossSectionFlowAnalysis cr = new CrossSectionFlowAnalysis(new IdImpl("sim2d_0_rev_-3499"), new IdImpl("sim2d_0_-3499"), "/Users/laemmel/devel/gct_TRB/small_single/flow", 5);
//		controller.getEvents().addHandler(cr);
		
		
//		FlowAnalysis fa1 = new FlowAnalysis(new Envelope(-8235101, -8235088, 4948062,4948069),"/Users/laemmel/devel/gct_TRB/small_single/fnd01.txt");
//		FlowAnalysis fa2 = new FlowAnalysis(new Envelope(-8235077.88, -8235076.36, 4948033.56,4948034.37),"/Users/laemmel/devel/gct_TRB/small_single/fnd02.txt");
//		FlowAnalysis fa3 = new FlowAnalysis(new Envelope(-8235139, -8235136, 4947984,4947985),"/Users/laemmel/devel/gct_TRB/small_single/fnd03.txt");
//		
//		
//		
//		controller.getEvents().addHandler(fa1);
//		controller.getEvents().addHandler(fa2);
//		controller.getEvents().addHandler(fa3);
		QuadTreePath qtp = new QuadTreePath(e,"/Users/laemmel/devel/tjunction/KO/ko-240-240-240/ko-240-240-240_combined_MB_Quadtree_Path.txt");
		e.addHandler(qtp);
		if (args[2].equals("true")) {
//			VDPath vdp = new VDPath(controller.getEvents());
//			controller.getEvents().addHandler(vdp);
//			
//			QuadTreePath qdp = new QuadTreePath(controller.getEvents());
//			controller.getEvents().addHandler(qdp);
			
//			Sim2DRunner runner = new Sim2DRunner();
//			runner.test = new EventBasedVisDebuggerEngine(sc);
//
//			
//			runner.visDebugger = new VisDebugger( sim2dc.getTimeStepSize(), minX, minY);
////			runner.visDebugger.setTransformationStuff(minX, minY);
//			controller.addControlerListener(runner);
//			runner.controller = controller;
//			runner.factory = factory;
//			runner.qSimDrawer = new QSimDensityDrawer(sc);
////			runner.burgdorfInfoDrawer = new BurgdorfInfoDrawer(sc);
//			
//			runner.visDebugger.addAdditionalDrawer(runner.qSimDrawer);
//			runner.visDebugger.addAdditionalDrawer(runner.burgdorfInfoDrawer);
//			runner.visDebugger.addAdditionalDrawer();
//			runner.visDebugger.addAdditionalDrawer(new ScaleBarDrawer());
//			runner.visDebugger.addAdditionalDrawer(new MousePositionDrawer());
//			FrameSaver fs = new FrameSaver("/Users/laemmel/tmp/processing", "png", 3);
//			runner.visDebugger.setFrameSaver(fs);
			
			
			EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
			InfoBox iBox = new InfoBox(dbg,sc);
//			SeeCasino iCasion = new SeeCasino();
//			LinkFNDDrawer fnd = new LinkFNDDrawer(sc);
//			VoronoiDiagramDrawer v = new VoronoiDiagramDrawer();
//			VoronoiFNDDrawer vFND = new VoronoiFNDDrawer(new Envelope(-3,3,-197.5,-192.5));
//			VoronoiFNDDrawer vFND1 = new VoronoiFNDDrawer(10);
			dbg.addAdditionalDrawer(iBox);
//			dbg.addAdditionalDrawer(new Branding());
			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
//			QSimInfoBoxDrawer qDbg2 = new QSimInfoBoxDrawer(sc);
			
			dbg.addAdditionalDrawer(qDbg);
//			dbg.addAdditionalDrawer(qDbg2);
			
//			dbg.addAdditionalDrawer(vFND);
//			dbg.addAdditionalDrawer(vFND1);
//			dbg.addAdditionalDrawer(fnd);;
//			dbg.addAdditionalDrawer(iCasion);
//			dbg.addAdditionalDrawer(new GregorsOffice());
//			dbg.addAdditionalDrawer(v);
			e.addHandler(dbg);
			e.addHandler(qDbg);
//			controller.getEvents().addHandler(qDbg2);
//			VDTester vdt = new VDTester(new Envelope(-2.5,2.5,-50,-40),controller.getEvents());
//			controller.getEvents().addHandler(vdt);
//			controller.addControlerListener(vdt);
//			controller.getEvents().addHandler(vFND);
//			controller.getEvents().addHandler(vFND1);
//			controller.getEvents().addHandler(fnd);
//			controller.getEvents().addHandler(iCasion);
//			controller.getEvents().addHandler(v);
			
		}
		
//		VDTester vdt = new VDTester(new Envelope(-2.5,2.5,-27.5,-22.5),controller.getEvents());
////		VDTester vdt = new VDTester(new Envelope(19,21,-1,1),controller.getEvents());
//		controller.getEvents().addHandler(vdt);
//		controller.addControlerListener(vdt);

//		EventsReaderXMLv1ExtendedSim2DVersion reader = new EventsReaderXMLv1ExtendedSim2DVersion(e);
//		reader.parse("/Users/laemmel/devel/hhw3/vis/0.events.xml");
		HermesTrajectoryToEventsParser h = new HermesTrajectoryToEventsParser(e);
		h.parse("/Users/laemmel/devel/tjunction/KO/ko-240-240-240/ko-240-240-240_combined_MB.txt");
		qtp.finish();
		
//		controller.setCreateGraphs(false);
//		controller.setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
	}

	protected static void printUsage() {
		System.out.println();
		System.out.println("Controller2D");
		System.out.println("Controller for hybrid sim2d qsim (pedestrian) simulations.");
		System.out.println();
		System.out.println("usage : Controller2D sim2d-config-file qsim-config-file visualize");
		System.out.println();
		System.out.println("sim2d-config-file:  A sim2d config file.");
		System.out.println("qsim-config-file:   A MATSim config file.");
		System.out.println("visualize:   one of {true,false}.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2012, matsim.org");
		System.out.println();
	}

}
