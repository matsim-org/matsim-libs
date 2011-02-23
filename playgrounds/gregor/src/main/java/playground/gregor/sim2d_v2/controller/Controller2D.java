/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.controller;

import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v2.simulation.Sim2D;

public class Controller2D extends Controler {

	private Scenario2DImpl scenario2DData;
	private PedVisPeekABot vis;
	private Sim2DConfigGroup sim2dConfig;

	public Controller2D(String[] args) {
		super(args[0]);
		setOverwriteFiles(true);
		this.config.addQSimConfigGroup(new QSimConfigGroup());
		this.config.getQSimConfigGroup().setEndTime(9*3600 + 5* 30);
//		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
	}

	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			initSim2DConfigGroup();
			this.scenario2DData = new Scenario2DImpl(this.config);
			this.loader = new ScenarioLoader2DImpl(this.scenario2DData);
			this.loader.loadScenario();
			this.network = this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			// ((ScenarioLoader2DImpl)
			// this.loader).setPhantomPopulationEventsFile("/home/laemmel/devel/dfg/events.xml");
			this.scenarioLoaded = true;

//			 this.vis = new PedVisPeekABot(1);
//			 Link l = this.network.getLinks().get(new IdImpl(0));
//			 this.vis.setOffsets(l.getCoord().getX(), l.getCoord().getY());
//			 this.vis.setFloorShapeFile(this.sim2dConfig.getFloorShapeFile());
//			 this.vis.drawNetwork(network);
//			 this.events.addHandler(this.vis);
		}

	}
	

	/**
	 * 
	 */
	private void initSim2DConfigGroup() {
		Module module = this.config.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		this.sim2dConfig = s;
		this.config.getModules().put("sim2d", s);
	}

	@Override
	protected void runMobSim() {

		// EventsManager manager = new EventsManagerImpl();
		// EventWriterXML writer = new
		// EventWriterXML(getConfig().controler().getOutputDirectory() +
		// "/ITERS/it." + getIterationNumber() + "/" + getIterationNumber() +
		// ".xyzAzimuthEvents.xml.gz");

		// this.events.addHandler(writer);

		Sim2D sim = new Sim2D(this.events, this.scenario2DData);
//
//		if (this.getIterationNumber() == 0) {
//		 this.vis = new PedVisPeekABot(1);
//		 Link l = this.network.getLinks().get(new IdImpl(1));
//		 this.vis.setOffsets(l.getCoord().getX(), l.getCoord().getY());
//		 this.vis.setFloorShapeFile(this.sim2dConfig.getFloorShapeFile());
//		 this.vis.drawNetwork(network);
//		 this.events.addHandler(this.vis);
//		}
		// }
		sim.run();
		// writer.closeFile();
		if (this.vis != null) {
			this.vis.reset(getIterationNumber());
		}
	}

	public static void main(String[] args) {
		Controler controller = new Controller2D(args);
		controller.run();

	}

}
