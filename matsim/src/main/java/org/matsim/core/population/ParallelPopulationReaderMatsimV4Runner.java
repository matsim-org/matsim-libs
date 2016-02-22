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

package org.matsim.core.population;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4.EndProcessingTag;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4.EndTag;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4.PersonTag;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4.StartTag;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4.Tag;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.xml.sax.Attributes;

/**
 * Runnable used by ParallelPopulationReaderMatsimV4.
 * Processes xml data taken from a BlockingQueue which is filled
 * in the main class.
 * 
 * @author cdobler
 */
public class ParallelPopulationReaderMatsimV4Runner extends PopulationReaderMatsimV4 implements Runnable {
	
	private final BlockingQueue<List<Tag>> queue;
	
	public ParallelPopulationReaderMatsimV4Runner(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario,
			final BlockingQueue<List<Tag>> queue) {
		super(coordinateTransformation , scenario);
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
						// if its is a person tag, we use the startPerson method from this class
						if (PERSON.equals(tag.name)) {
							startPerson(((StartTag) tag).atts);
						}
						// otherwise hand the tag over to the super class
						else {
							this.startTag(tag.name, ((StartTag) tag).atts, tag.context);
						}
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
	
	private void startPerson(final Attributes atts) {
		String ageString = atts.getValue("age");
//		int age = Integer.MIN_VALUE;
		Integer age = null ;
		if (ageString != null) age = Integer.parseInt(ageString);
		PersonUtils.setAge(this.currperson, age);
		PersonUtils.setSex(this.currperson, atts.getValue("sex"));
		PersonUtils.setLicence(this.currperson, atts.getValue("license"));
		PersonUtils.setCarAvail(this.currperson, atts.getValue("car_avail"));
		String employed = atts.getValue("employed");
		if (employed == null) {
			PersonUtils.setEmployed(this.currperson, null);
		} else {
			PersonUtils.setEmployed(this.currperson, "yes".equals(employed));
		}
	}
}
