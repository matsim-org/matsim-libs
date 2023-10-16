/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.analysis.trainstates;

import ch.sbb.matsim.contrib.railsim.analysis.RailsimCsvWriter;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;


/**
 * Controler to write {@link ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent} csv files.
 */
public final class RailsimTrainStateControlerListener implements IterationEndsListener, IterationStartsListener {

	private TrainStateAnalysis analysis;
	private OutputDirectoryHierarchy controlerIO;
	private final EventsManager eventsManager;
	private final Scenario scenario;

	@Inject
	RailsimTrainStateControlerListener(Scenario scenario, EventsManager eventsManager, OutputDirectoryHierarchy controlerIO) {
		this.eventsManager = eventsManager;
		this.controlerIO = controlerIO;
		this.scenario = scenario;
		this.analysis = new TrainStateAnalysis();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.eventsManager.addHandler(this.analysis);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String railLinkStatesCsvFilename = this.controlerIO.getIterationFilename(event.getIteration(), "railsimTrainStates.csv", this.scenario.getConfig().controller().getCompressionType());
		RailsimCsvWriter.writeTrainStatesCsv(this.analysis.events, this.scenario.getNetwork(), railLinkStatesCsvFilename);
	}

}
