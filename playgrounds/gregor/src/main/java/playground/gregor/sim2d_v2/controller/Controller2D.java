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

import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v2.simulation.Sim2D;

public class Controller2D extends Controler {

	private Scenario2DImpl scenario2DData;
	private PedVisPeekABot vis;

	public Controller2D(String[] args) {
		super(args);
		setOverwriteFiles(true);
		this.config.setQSimConfigGroup(new QSimConfigGroup());
		this.config.getQSimConfigGroup().setEndTime(2 * 60);
	}

	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			this.scenario2DData = new Scenario2DImpl(this.config);
			this.loader = new ScenarioLoader2DImpl(this.scenario2DData);
			this.loader.loadScenario();
			this.network = this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			((ScenarioLoader2DImpl) this.loader).setPhantomPopulationEventsFile("/home/laemmel/devel/dfg/events.xml");
			this.scenarioLoaded = true;

			this.vis = new PedVisPeekABot(0.2);
			this.vis.setFloorShapeFile(Sim2DConfig.FLOOR_SHAPE_FILE);
			this.events.addHandler(this.vis);
		}

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

		// }
		sim.run();
		// writer.closeFile();
		this.vis.reset(getIterationNumber());
	}

	public static void main(String[] args) {
		Controler controller = new Controller2D(args);
		controller.run();

	}

}
