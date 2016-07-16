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

package playground.mrieser.svi.controller;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.io.NetworkWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.mrieser.svi.data.ActivityToZoneMappingReader;
import playground.mrieser.svi.data.ShapeZonesReader;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;
import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;
import playground.mrieser.svi.pt.PtLines;
import playground.mrieser.svi.pt.PtLinesReader;
import playground.mrieser.svi.pt.PtLinesStatistics;

/**
 * @author mrieser
 */
public class DynusTControlerListener implements StartupListener, IterationStartsListener, AfterMobsimListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(DynusTControlerListener.class);

	private final DynusTConfig dc;
	private boolean isFirstIteration = true;
	private boolean useOnlyDynusT = false;
	private PtLines ptLines = null;
	private final DynamicTravelTimeMatrix ttMatrix;
	private final Network dynusTNetwork;

	public DynusTControlerListener(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final Network dynusTNetwork) {
		this.dc = dc;
		this.ttMatrix = ttMatrix;
		this.dynusTNetwork = dynusTNetwork;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		MatsimServices c = event.getServices();
		Config config = c.getConfig();
		this.useOnlyDynusT = Boolean.parseBoolean(config.getParam("dynus-t", "useOnlyDynusT"));
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		MatsimServices c = event.getServices();
		String outDir = c.getControlerIO().getIterationFilename(event.getIteration(), "DynusT");
		new File(outDir).mkdir();
		this.dc.setOutputDirectory(outDir);

		this.ttMatrix.clear();
		
		if (this.isFirstIteration) {
			String filename = c.getControlerIO().getOutputFilename("network.dynust.xml");
			log.info("Writing DynusT-network to " + filename);
			new NetworkWriter(dynusTNetwork).write(filename);

			log.info("Reading zones for DynusT..." + this.dc.getZonesShapeFile());
			new ShapeZonesReader(this.dc.getZones()).readShapefile(this.dc.getZonesShapeFile());

			log.info("Analyzing zones for Population...");
			new ActivityToZoneMappingReader(this.dc.getActToZoneMapping()).readFile(this.dc.getActToZoneMappingFilename());
//			new CalculateActivityToZoneMapping(this.dc.getActToZoneMapping(), this.dc.getZones(), this.dc.getZoneIdAttributeName()).run(c.getScenario().getPopulation());
//			new ActivityToZoneMappingWriter(this.dc.getActToZoneMapping()).writeFile(c.getControlerIO().getOutputFilename("actToZoneMapping.txt"));

			if (this.dc.getZoneIdToIndexMappingFile() != null) {
				log.info("Reading zone mapping..." + this.dc.getZoneIdToIndexMappingFile());
				new ZoneIdToIndexMappingReader(this.dc.getZoneIdToIndexMapping()).readFile(this.dc.getZoneIdToIndexMappingFile());
			} else {
				log.info("No specific zone to id mapping given, assuming same ids are used in Shape file as in DynusT.");
				for (SimpleFeature f : this.dc.getZones().getAllZones()) {
					String zoneId = f.getAttribute(this.dc.getZoneIdAttributeName()).toString();
					int index = Integer.parseInt(zoneId);
					this.dc.getZoneIdToIndexMapping().addMapping(zoneId, index);
				}
			}

			if (this.dc.getPtLinesFile() != null) {
				this.ptLines = new PtLines();
				log.info("reading pt lines from " + this.dc.getPtLinesFile());
				new PtLinesReader(this.ptLines, this.dynusTNetwork).readFile(this.dc.getPtLinesFile());
			}
			
			this.isFirstIteration = false;
		}
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		if (!this.useOnlyDynusT) {
			// run DynusT now
			new DynusTMobsim(this.dc, this.ttMatrix, event.getServices().getScenario(), event.getServices().getEvents(), this.dynusTNetwork, event.getServices(), event.getIteration()).run();
			new ScoreAdaptor().run();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.ptLines != null) {
			log.info("dump pt statistics...");
			new PtLinesStatistics(this.ptLines).writeStatsToFile(
					event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "ptStats.txt"),
					this.dc.getTravelTimeCalculator());
		}
		if (event.getIteration() % 10 != 0) {
			// clean up tmp directory
			log.info("removing unnecessary files.");
			File outDir = new File(this.dc.getOutputDirectory());
			for (File f : outDir.listFiles()) {
				if (f.isFile() && !f.getName().equals("VehTrajectory.dat") && !f.getName().equalsIgnoreCase("node.csv")) {
					log.info("   deleting file " + f.getAbsolutePath());
					f.delete();
				}
			}
		}
	}
	
}
