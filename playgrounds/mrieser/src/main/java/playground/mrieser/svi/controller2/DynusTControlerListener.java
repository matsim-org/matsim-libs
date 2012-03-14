/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.controller2;

import java.io.File;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.mrieser.svi.data.ActivityToZoneMappingWriter;
import playground.mrieser.svi.data.CalculateActivityToZoneMapping;
import playground.mrieser.svi.data.ShapeZonesReader;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;

/**
 * @author mrieser
 */
public class DynusTControlerListener implements StartupListener, IterationStartsListener, AfterMobsimListener {

	private final static Logger log = Logger.getLogger(DynusTControlerListener.class);

	private final DynusTConfig dc;
	private boolean isFirstIteration = true;
	private boolean useOnlyDynusT = false;

	public DynusTControlerListener(final DynusTConfig dc) {
		this.dc = dc;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler c = event.getControler();
		Config config = c.getConfig();
		this.dc.setDynusTDirectory(config.getParam("dynus-t", "dynusTDirectory"));
		this.dc.setModelDirectory(config.getParam("dynus-t", "modelDirectory"));
		this.dc.setZonesShapeFile(config.getParam("dynus-t", "zonesShpFile"));
		this.dc.setZoneIdToIndexMappingFile(config.findParam("dynus-t", "zoneIdToIndexMapping"));
		this.dc.setDemandFactor(Double.parseDouble(config.getParam("dynus-t", "dynusTDemandFactor")));
		this.useOnlyDynusT = Boolean.parseBoolean(config.getParam("dynus-t", "useOnlyDynusT"));

		if (this.useOnlyDynusT) {
			c.setMobsimFactory(new DynusTMobsimFactory(this.dc));
			log.info("DynusT will be used as mobility simulation. Make sure that re-routing is *not* enabled as replanning strategy, as it will have no effect.");
		}
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		Controler c = event.getControler();
		String outDir = c.getControlerIO().getIterationFilename(event.getIteration(), "DynusT");
		new File(outDir).mkdir();
		this.dc.setOutputDirectory(outDir);

		if (this.isFirstIteration) {
			log.info("Reading zones for DynusT...");
			new ShapeZonesReader(this.dc.getZones()).readShapefile(this.dc.getZonesShapeFile());

			log.info("Analyzing zones for Population...");
			new CalculateActivityToZoneMapping(this.dc.getActToZoneMapping(), this.dc.getZones()).run(c.getScenario().getPopulation());
			new ActivityToZoneMappingWriter(this.dc.getActToZoneMapping()).writeFile("actToZoneMapping.txt");

			if (this.dc.getZoneIdToIndexMappingFile() != null) {
				log.info("Reading zone mapping...");
				new ZoneIdToIndexMappingReader(this.dc.getZoneIdToIndexMapping()).readFile(this.dc.getZoneIdToIndexMappingFile());
			} else {
				log.info("No specific zone to id mapping given, assuming same ids are used in Shape file as in DynusT.");
				for (Feature f : this.dc.getZones().getAllZones()) {
					String zoneId = f.getAttribute("id").toString();
					int index = Integer.parseInt(zoneId);
					this.dc.getZoneIdToIndexMapping().addMapping(zoneId, index);
				}
			}

			this.isFirstIteration = false;
		}
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		if (!this.useOnlyDynusT) {
			// run DynusT now
			new DynusTMobsim(this.dc, event.getControler().getScenario(), event.getControler().getEvents()).run();
		}
	}
}
