/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPopulationReaderMatsimV4Runner.java
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

package org.matsim.core.population.io;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.io.ParallelPopulationReaderMatsimV6.*;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Runnable used by ParallelPopulationReaderMatsimV6.
 * Processes xml data taken from a BlockingQueue which is filled
 * in the main class.
 *
 * @author cdobler, steffenaxer
 */
/* deliberately package */  class ParallelPopulationReaderMatsimV6Runner extends PopulationReaderMatsimV6 implements Runnable {

	private final BlockingQueue<List<Tag>> queue;

	public ParallelPopulationReaderMatsimV6Runner(
			final String inputCRS,
			final String targetCRS,
			final Scenario scenario,
			final BlockingQueue<List<Tag>> queue) {
		super(inputCRS ,targetCRS, scenario);
		this.queue = queue;
	}

	@Override
	public void run() {
		/*
		 * The thread will go on with the parsing until an EndProcessingTag is found,
		 * which calls "return".
		 */
		while (true) {
			try {
				List<Tag> tags;
				tags = queue.take();
				for (Tag tag : tags) {
					if (tag instanceof PersonTag) {
						this.currperson = ((PersonTag) tag).person;
					} else if (tag instanceof StartTag) {
						this.startTag(tag.name, ((ParallelPopulationReaderMatsimV6.StartTag) tag).atts, tag.context);
					} else if (tag instanceof EndTag) {
						/*
						 * If its is a person tag, we reset the current person. We do not hand the
						 * tag over to the superclass because the person has already been added
						 * to the population.
						 */
						if (PERSON.equals(tag.name)) {
							this.currperson = null;
						}
						// otherwise hand the tag over to the super class
						else {
							this.endTag(tag.name, ((EndTag) tag).content, tag.context);
						}
					} else if (tag instanceof EndProcessingTag) {
						return;
					}
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
