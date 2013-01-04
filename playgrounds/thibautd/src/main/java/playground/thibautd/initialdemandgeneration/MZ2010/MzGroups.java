/* *********************************************************************** *
 * project: org.matsim.*
 * MzGroups.java
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
package playground.thibautd.initialdemandgeneration.MZ2010;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author thibautd
 */
public class MzGroups {
	private static final Logger log =
		Logger.getLogger(MzGroups.class);

	private final List<Integer> ages = Arrays.asList( 7 , 15 , 18 , 66 );
	private final List<MzGroup> groups;

	public MzGroups() {
		groups = new ArrayList<MzGroup>();

		List<Integer> minAges = new ArrayList<Integer>();
		minAges.add( -1 );
		minAges.addAll( ages );


		List<Integer> maxAges = new ArrayList<Integer>();
		maxAges.addAll( ages );
		maxAges.add( Integer.MAX_VALUE );

		Iterator<Integer> maxes = maxAges.iterator();

		boolean[] bools = new boolean[]{true,false};
		for (int min : minAges) {
			int max = maxes.next();
			for (Gender gender : Gender.values()) {
				for (boolean employement : bools) {
					for (boolean education : bools) {
						for (boolean license : bools) {
							for (boolean car : bools) {
								groups.add( new MzGroup(
											min,
											max,
											gender,
											employement,
											education,
											license,
											car));
							}
						}
					}
				}
			}
		}
	}

	public boolean add(
			final ObjectAttributes atts,
			final Person p) {
		for (MzGroup g : groups) {
			if (g.add( atts , p )) return true;
		}
		return false;
	}

	public void printInfo() {
		for (MzGroup g : groups) {
			log.info( g+" has size "+g.size() );
		}
	}

}

