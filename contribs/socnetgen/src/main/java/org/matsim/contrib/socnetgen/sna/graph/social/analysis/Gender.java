/* *********************************************************************** *
 * project: org.matsim.*
 * Gender.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.social.analysis;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.Set;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.core.population.PersonUtils;

/**
 * @author illenberger
 * 
 */
public class Gender extends AbstractLinguisticAttribute {

	public static final String MALE = "m";

	public static final String FEMALE = "f";

	private static Gender instance;

	public static Gender getInstance() {
		if (instance == null)
			instance = new Gender();
		return instance;
	}

	@Override
	protected String attribute(SocialVertex v) {
		String gender = PersonUtils.getSex(v.getPerson().getPerson());
		if (MALE.equalsIgnoreCase(gender))
			return MALE;
		else if (FEMALE.equalsIgnoreCase(gender))
			return FEMALE;
		else
			return null;
	}

	public double correlation(Set<? extends SocialEdge> edges) {
		if (edges.isEmpty())
			return Double.NaN;
		else {
			TDoubleArrayList values1 = new TDoubleArrayList(2 * edges.size());
			TDoubleArrayList values2 = new TDoubleArrayList(2 * edges.size());

			for (SocialEdge edge : edges) {
				String g1 = PersonUtils.getSex(edge.getVertices().getFirst().getPerson().getPerson());
				String g2 = PersonUtils.getSex(edge.getVertices().getSecond().getPerson().getPerson());

				if (g1 != null && g2 != null) {
					int val1 = 0;
					if (g1.equalsIgnoreCase(FEMALE))
						val1 = 1;

					int val2 = 0;
					if (g2.equalsIgnoreCase(FEMALE))
						val2 = 1;

					values1.add(val1);
					values2.add(val2);

					values1.add(val2);
					values2.add(val1);
				}
			}

			return new PearsonsCorrelation().correlation(values1.toArray(), values2.toArray());
		}
	}
}
