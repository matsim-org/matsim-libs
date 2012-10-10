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
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.mrieser.svi.converters.DynusTNetworkReader;
import playground.mrieser.svi.data.ActivityToZoneMappingWriter;
import playground.mrieser.svi.data.CalculateActivityToZoneMapping;
import playground.mrieser.svi.data.ShapeZonesReader;
import playground.mrieser.svi.data.ZoneIdToIndexMappingReader;
import playground.mrieser.svi.data.vehtrajectories.DynamicTravelTimeMatrix;
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
	private final DynamicTravelTimeMatrix ttMatrix = new DynamicTravelTimeMatrix(600, 30*3600.0); // 10min time bins, at most 30 hours
	private PtLines ptLines = null;
	private Network dynusTNetwork = null;

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
		this.dc.setZoneIdAttributeName(config.getParam("dynus-t", "zoneIdAttributeName"));
		if (config.findParam("dynus-t", "timeBinSize_min") != null) {
			this.dc.setTimeBinSize_min(Integer.parseInt(config.getParam("dynus-t", "timeBinSize_min")));
		}
		this.dc.setPtLinesFile(config.findParam("dynus-t", "ptLinesFile"));
		this.useOnlyDynusT = Boolean.parseBoolean(config.getParam("dynus-t", "useOnlyDynusT"));

		log.info("Reading DynusT-network..." + this.dc.getModelDirectory());
		this.dynusTNetwork = NetworkImpl.createNetwork();
		new DynusTNetworkReader(this.dynusTNetwork).readFiles(this.dc.getModelDirectory() + "/xy.dat", this.dc.getModelDirectory()+ "/network.dat");
		
		if (this.useOnlyDynusT) {
			c.setMobsimFactory(new DynusTMobsimFactory(this.dc, this.ttMatrix, this.dynusTNetwork));
			log.info("DynusT will be used as exclusive mobility simulation. Make sure that re-routing is *not* enabled as replanning strategy, as it will have no effect.");
			c.setScoringFunctionFactory(new DynusTScoringFunctionFactory(this.dc, this.ttMatrix, this.dc.getActToZoneMapping(), new CharyparNagelScoringParameters(config.planCalcScore())));
		} else {
			c.setScoringFunctionFactory(new MixedScoringFunctionFactory(this.dc, this.ttMatrix, this.dc.getActToZoneMapping(), new CharyparNagelScoringParameters(config.planCalcScore())));
		}
			}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		Controler c = event.getControler();
		String outDir = c.getControlerIO().getIterationFilename(event.getIteration(), "DynusT");
		new File(outDir).mkdir();
		this.dc.setOutputDirectory(outDir);

		this.ttMatrix.clear();
		
		if (this.isFirstIteration) {
			log.info("Reading zones for DynusT..." + this.dc.getZonesShapeFile());
			new ShapeZonesReader(this.dc.getZones()).readShapefile(this.dc.getZonesShapeFile());

			log.info("Analyzing zones for Population...");
			new CalculateActivityToZoneMapping(this.dc.getActToZoneMapping(), this.dc.getZones(), this.dc.getZoneIdAttributeName()).run(c.getScenario().getPopulation());
			new ActivityToZoneMappingWriter(this.dc.getActToZoneMapping()).writeFile(c.getControlerIO().getOutputFilename("actToZoneMapping.txt"));

			if (this.dc.getZoneIdToIndexMappingFile() != null) {
				log.info("Reading zone mapping..." + this.dc.getZoneIdToIndexMappingFile());
				new ZoneIdToIndexMappingReader(this.dc.getZoneIdToIndexMapping()).readFile(this.dc.getZoneIdToIndexMappingFile());
			} else {
				log.info("No specific zone to id mapping given, assuming same ids are used in Shape file as in DynusT.");
				for (Feature f : this.dc.getZones().getAllZones()) {
					String zoneId = f.getAttribute(this.dc.getZoneIdAttributeName()).toString();
					int index = Integer.parseInt(zoneId);
					this.dc.getZoneIdToIndexMapping().addMapping(zoneId, index);
				}
			}

			if (this.dc.getPtLinesFile() != null) {
				this.ptLines = new PtLines();
				log.info("reading pt lines from " + this.dc.getPtLinesFile());
				new PtLinesReader(this.ptLines, this.dynusTNetwork);
			}
			
			this.isFirstIteration = false;
		}
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		if (!this.useOnlyDynusT) {
			// run DynusT now
			new DynusTMobsim(this.dc, this.ttMatrix, event.getControler().getScenario(), event.getControler().getEvents(), this.dynusTNetwork).run();
			new ScoreAdaptor().run();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.ptLines != null) {
			log.info("dump pt statistics...");
			new PtLinesStatistics(this.ptLines).writeStatsToFile(
					event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "ptStats.txt"),
					this.dc.getTravelTimeCalculator());
		}
		// clean up tmp directory
		log.info("removing unnecessary files.");
		File outDir = new File(this.dc.getOutputDirectory());
		for (File f : outDir.listFiles()) {
			if (f.isFile() && !f.getName().equals("VehTrajectory.dat")) {
				log.info("   deleting file " + f.getAbsolutePath());
				f.delete();
			}
		}
	}
	
}
