/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2D.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.controller;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;

import playground.gregor.sim2d.events.XYZEventsGenerator;
import playground.gregor.sim2d.events.XYZEventsManager;
import playground.gregor.sim2d.peekabot.PeekABotClient;
import playground.gregor.sim2d.peekabot.Sim2DVis;
import playground.gregor.sim2d.simulation.SegmentedStaticForceField;
import playground.gregor.sim2d.simulation.Sim2D;
import playground.gregor.sim2d.simulation.StaticForceField;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Controller2D extends Controler {

	private Map<MultiPolygon, List<Link>> mps;

	private SegmentedStaticForceField sff;

	private final boolean vis = false;

	private Sim2DVis sim2DVis = null;

	private Map<Id, LineString> lsmp;

	private Scenario2DImpl scenario2DData;

	public Controller2D(String[] args) {
		super(args);
		setOverwriteFiles(true);

	}

	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			this.scenario2DData = new Scenario2DImpl(this.config);
			this.loader = new ScenarioLoader2DImpl(this.scenario2DData);
			this.loader.loadScenario();
			this.mps = this.scenario2DData.getFloorLinkMapping();
			this.sff = this.scenario2DData.getSegmentedStaticForceField();
			this.lsmp = this.scenario2DData.getLineStringMap();
			this.network = this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			this.scenarioLoaded = true;
		}
		if (this.vis) {
			intiSim2DVis();
		}
	}

	private void intiSim2DVis() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (MultiPolygon mp : this.mps.keySet()) {
			Geometry geo = mp.getBoundary();
			Coordinate[] coords = geo.getCoordinates();
			for (int i = 0; i < coords.length; i++) {
				if (coords[i].x < minX) {
					minX = coords[i].x;
				}
				if (coords[i].y < minY) {
					minY = coords[i].y;
				}

			}

		}
		this.sim2DVis = new Sim2DVis(minX, minY);
		this.sim2DVis.drawFloorPlans(this.mps);

	}

	@Override
	protected void runMobSim() {

		EventsManager manager = new EventsManagerImpl();
		EventWriterXML writer = new EventWriterXML(getConfig().controler().getOutputDirectory() + "/ITERS/it." + getIterationNumber() + "/" + getIterationNumber() + ".xyzAzimuthEvents.xml.gz");
		manager.addHandler(writer);
		XYZEventsGenerator gen = new XYZEventsGenerator(manager);
		Sim2D sim = new Sim2D(this.network, this.mps, this.lsmp, this.population, this.events, this.sff, this.config);
		if (this.vis) {
			sim.setSim2DVis(this.sim2DVis);
		}
		sim.setXYZEventsManager(gen);
		// }
		sim.run();
		writer.closeFile();
	}

	public static void main(String[] args) {
		Controler controller = new Controller2D(args);
		controller.run();

	}

}
