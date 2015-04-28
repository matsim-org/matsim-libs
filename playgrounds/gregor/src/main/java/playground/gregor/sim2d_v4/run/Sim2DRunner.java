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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimInfoBoxDrawer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;
import playground.gregor.sim2d_v4.simulation.HybridQ2DMobsimFactory;

public class Sim2DRunner implements IterationStartsListener{

	private Controler controller;
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
		
//		c.qsim().setEndTime(300);
		c.qsim().setEndTime(9*3600);
//		c.qsim().setEndTime(41*60);//+30*60);

//		for (Person p : sc.getPopulation().getPersons().values()) {
//			
//			for (Plan plan : p.getPlans()) {
//				
//				PlanImpl pp = (PlanImpl) plan;
//				Activity a = pp.getFirstActivity();
//				if (a.getType().contains("origin")) {
//					continue;
//				}
//				for (PlanElement le : pp.getPlanElements()) {
//					if (le instanceof LegImpl) {
//						LegImpl li = (LegImpl) le;
//						li.setRoute(null);
//					}
//				}
//			}
//		}

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
		
		Controler controller = new Controler(sc);

		controller.setOverwriteFiles(true);
		
//		controller.getEvents().addHandler(new SimSpeedObserver());

		final HybridQ2DMobsimFactory factory = new HybridQ2DMobsimFactory(sc, controller.getEvents());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controler().getMobsim().equals("hybridQ2D")) {
					bind(Mobsim.class).toProvider(factory);
				}
			}
		});


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
		if (args[2].equals("true")) {
			
//			QuadTreePath qtp = new QuadTreePath(controller.getEvents());
//			controller.getEvents().addHandler(qtp);
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
//			AgentTracker at2 = new AgentTracker(sc,new IdImpl("carblowup9445"));
//			AgentTracker at = new AgentTracker(sc,new IdImpl("carblowup8888"));
//			controller.getEvents().addHandler(at);
//			controller.getEvents().addHandler(at2);
//			QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
//			QSimVehiclesDrawer qDbgB = new QSimVehiclesDrawer(sc);
			QSimInfoBoxDrawer qDbg2 = new QSimInfoBoxDrawer(sc);
			
//			dbg.addAdditionalDrawer(qDbg);
//			dbg.addAdditionalDrawer(at2);
//			dbg.addAdditionalDrawer(at);
//			dbg.addAdditionalDrawer(qDbgB);
			dbg.addAdditionalDrawer(qDbg2);
			
//			dbg.addAdditionalDrawer(vFND);
//			dbg.addAdditionalDrawer(vFND1);
//			dbg.addAdditionalDrawer(fnd);;
//			dbg.addAdditionalDrawer(iCasion);
//			dbg.addAdditionalDrawer(new GregorsOffice());
//			dbg.addAdditionalDrawer(v);
			controller.getEvents().addHandler(dbg);
//			controller.getEvents().addHandler(qDbg);
//			controller.getEvents().addHandler(qDbgB);
			controller.getEvents().addHandler(qDbg2);
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

		
//		controller.setCreateGraphs(false);
//		controller.setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
//		controller.setTravelDisutilityFactory(new MSAMscbFactory());
		
//		TollHandler tollHandler = new TollHandler(controller.getScenario());
//		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
//		controller.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
//		controller.addControlerListener(new MarginalCostPricing( (ScenarioImpl) controller.getScenario(), tollHandler ));
		controller.run();
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

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if ((event.getIteration()) % 1 == 0 || event.getIteration() > 50) {
//			this.factory.debug(this.visDebugger);
			this.controller.getEvents().addHandler(this.qSimDrawer);
            this.controller.getConfig().controler().setCreateGraphs(true);
        } else {
//			this.factory.debug(null);
			this.controller.getEvents().removeHandler(this.qSimDrawer);
            this.controller.getConfig().controler().setCreateGraphs(false);
        }
//		this.visDebugger.setIteration(event.getIteration());
	}
}
