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

package org.matsim.controler.listener;

import org.matsim.analysis.LegHistogram;
import org.matsim.controler.Controler;
import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.events.ControlerSetupIterationEvent;
import org.matsim.events.Events;

public class LegHistogramListener implements ControlerFinishIterationListener, ControlerSetupIterationListener {

	final private Events events;
	final LegHistogram histogram;
	final boolean outputGraph;

	public LegHistogramListener(final Events events, final boolean outputGraph) {
		this.events = events;
		this.histogram = new LegHistogram(300);
		this.outputGraph = outputGraph;
		this.events.addHandler(this.histogram);
	}

	public void notifyIterationSetup(final ControlerSetupIterationEvent event) {
		this.histogram.reset(event.getIteration());
	}

	public void notifyIterationFinished(final ControlerFinishIterationEvent event) {
		this.histogram.write(Controler.getIterationFilename("legHistogram.txt"));
		if (this.outputGraph) {
			this.histogram.writeGraphic(Controler.getIterationFilename("legHistogram.png"));
		}
	}

}
