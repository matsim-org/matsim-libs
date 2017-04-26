/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.vsp.airPollution.exposure;

import java.io.File;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author benjamin
 *
 */
public class InternalizeEmissionResponsibilityControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(InternalizeEmissionResponsibilityControlerListener.class);

	private final Double timeBinSize;

	private final GridTools gridTools;
	private final EmissionModule emissionModule;
	private final EmissionResponsibilityCostModule emissionCostModule;
	private final ResponsibilityGridTools responsibilityGridTools;

	@Inject
	private MatsimServices controler;

	private EmissionResponsibilityInternalizationHandler emissionInternalizationHandler;
	private IntervalHandler intervalHandler;

	private String emissionEventOutputFile;
	private EventWriterXML emissionEventWriter;

	private int firstIt;
	private int lastIt;

	@Inject
	private InternalizeEmissionResponsibilityControlerListener(EmissionModule emissionModule, EmissionResponsibilityCostModule emissionCostModule, ResponsibilityGridTools rgt, GridTools gridTools) {
		this.timeBinSize = rgt.getTimeBinSize();
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.responsibilityGridTools = rgt;
		this.gridTools = gridTools;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		EventsManager eventsManager = emissionModule.getEmissionEventsManager();

		Double simulationEndtime = controler.getConfig().qsim().getEndTime();
		intervalHandler = new IntervalHandler(timeBinSize, simulationEndtime, gridTools);
		eventsManager.addHandler(intervalHandler);
		
		firstIt = controler.getConfig().controler().getFirstIteration();
		lastIt = controler.getConfig().controler().getLastIteration();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int iteration = event.getIteration();

		logger.info("creating new emission internalization handler...");
		emissionInternalizationHandler = new EmissionResponsibilityInternalizationHandler(controler, emissionCostModule);
		logger.info("adding emission internalization module to emission events stream...");
		emissionModule.getEmissionEventsManager().addHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
			logger.info("creating new emission events writer...");
			emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
			logger.info("adding emission events writer to emission events stream...");
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();

		logger.info("removing emission internalization module from emission events stream...");
		emissionModule.getEmissionEventsManager().removeHandler(emissionInternalizationHandler);

		if(iteration == firstIt || iteration == lastIt){
			logger.info("removing emission events writer from emission events stream...");
			emissionModule.getEmissionEventsManager().removeHandler(emissionEventWriter);
			logger.info("closing emission events file...");
			emissionEventWriter.closeFile();
		}
		
		logger.info("calculating relative duration factors from this/last iteration...");
		// calc relative duration factors/density from last iteration
		responsibilityGridTools.resetAndcaluculateRelativeDurationFactors(intervalHandler.getDuration());
		logger.info("done calculating relative duration factors");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		emissionModule.writeEmissionInformation();

		boolean isWritingOutputEvents = event.getServices().getConfig().vspExperimental().isWritingOutputEvents();
		if( isWritingOutputEvents) {
			dumpOutputEvents();
		}
	}

	private void dumpOutputEvents() {
		try {
			File toFile = new File(	controler.getControlerIO().getOutputFilename("output_emission.events.xml.gz"));
			File fromFile = new File(controler.getControlerIO().getIterationFilename(controler.getConfig().controler().getLastIteration(), "emission.events.xml.gz"));
			IOUtils.copyFile(fromFile, toFile);
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).error("writing output emissions events did not work; probably parameters were such that no events were "
					+ "generated in the final iteration") ;
		}
	}
}