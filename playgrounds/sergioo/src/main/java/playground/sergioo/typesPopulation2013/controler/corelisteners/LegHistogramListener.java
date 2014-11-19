/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramListener.java
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

package playground.sergioo.typesPopulation2013.controler.corelisteners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import playground.sergioo.typesPopulation2013.analysis.LegHistogram;
import playground.sergioo.typesPopulation2013.population.PersonImplPops;

/**
 * Integrates the {@link org.matsim.analysis.LegHistogram} into the
 * {@link org.matsim.core.controler.Controler}, so the leg histogram is
 * automatically created every iteration.
 *
 * @author mrieser
 */
public class LegHistogramListener implements IterationEndsListener, IterationStartsListener {

	private final EventsManager events;
	private final Map<Id<Population>, LegHistogram> histograms = new HashMap<Id<Population>, LegHistogram>();
	private final boolean outputGraph;

	static private final Logger log = Logger.getLogger(LegHistogramListener.class);

	public LegHistogramListener(final EventsManager events, final boolean outputGraph, Population population) {
		this.events = events;
		this.outputGraph = outputGraph;
		for(Person person:population.getPersons().values()) {
			Id<Population> popId = ((PersonImplPops)person).getPopulationId();
			if(histograms.get(popId)==null) {
				LegHistogram legHistogram = new LegHistogram(300, popId, population);
				histograms.put(popId, legHistogram);
				this.events.addHandler(legHistogram);
			}
		}
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		for(LegHistogram histogram:histograms.values())
			histogram.reset(event.getIteration());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		for(Entry<Id<Population>, LegHistogram> histogram:histograms.entrySet())
			histogram.getValue().write(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), histogram.getKey()+"_legHistogram.txt"));
		this.printStats();
		if (this.outputGraph) {
			for(Entry<Id<Population>, LegHistogram> histogram:histograms.entrySet()) {
				histogram.getValue().writeGraphic(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), histogram.getKey()+"_legHistogram_all.png"));
				for (String legMode : histogram.getValue().getLegModes())
					histogram.getValue().writeGraphic(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), histogram.getKey()+"_legHistogram_" + legMode + ".png"), legMode);
			}
		}

	}

	public void printStats() {
		int nofLegs = 0;
		for(LegHistogram histogram:histograms.values()) {	
			for (int nofDepartures : histogram.getDepartures()) {
				nofLegs += nofDepartures;
			}
			log.info("number of legs:\t"  + nofLegs + "\t100%");
			for (String legMode : histogram.getLegModes()) {
				int nofModeLegs = 0;
				for (int nofDepartures : histogram.getDepartures(legMode)) {
					nofModeLegs += nofDepartures;
				}
				if (nofModeLegs != 0) {
					log.info("number of " + legMode + " legs:\t"  + nofModeLegs + "\t" + (nofModeLegs * 100.0 / nofLegs) + "%");
					if ( TransportMode.car.equals(legMode) ) {
						log.info("(car legs include legs by pt vehicles)") ;
					}
				}
			}
		}
	}

}
