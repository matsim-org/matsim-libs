/* *********************************************************************** *
 * project: org.matsim.*
 * TextLayer4QGIS.java
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
package playground.yu.utils.qgis;

import java.io.Closeable;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.TollTools;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public abstract class TextLayer4QGIS extends AbstractPersonAlgorithm implements
		PlanAlgorithm, Closeable {
	protected SimpleWriter writer;
	private RoadPricingScheme toll = null;

	/**
	 * dummy constructor, please don't use it.
	 */
	public TextLayer4QGIS() {
	}

	public TextLayer4QGIS(String textFilename) {
		writer = new SimpleWriter(textFilename);
		writer.write("x\ty\t");
	}

	public TextLayer4QGIS(String textFilename, RoadPricingScheme toll) {
		this(textFilename);
		this.toll = toll;
	}

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (toll == null) {
			run(plan);
		} else if (TollTools.isInRange(((PlanImpl) plan).getFirstActivity()
				.getLinkId(), toll)) {
			run(plan);
		}
	}

	public void writeln(String s) {
		writer.writeln(s);
	}

	@Override
	public void close() {
		writer.close();
	}
}
