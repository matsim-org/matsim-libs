/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.socialchoicesetconstraints;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.HashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class UtilsTest {
	@Test
	public void testCliquesOfSize() {
		final Utils.AllCliques cliques = new Utils.AllCliques();

		cliques.addClique( clique( 1 , 3 ) );
		cliques.addClique( clique( 4 , 7 ) );

		Assert.assertEquals(
				"unexpected max clique size",
				4, cliques.getMaxSize() );

		Assert.assertEquals(
				"unexpected number of cliques of size 4",
				1, cliques.getCliquesOfSize( 4 ).size() );

		Assert.assertEquals(
				"unexpected number of cliques of size 1",
				7, cliques.getCliquesOfSize( 1 ).size() );

		Assert.assertEquals(
				"unexpected number of cliques of size 3",
				5, cliques.getCliquesOfSize( 3 ).size() );
	}

	private static Set<Id<Person>> clique( final int min, final int max ) {
		final Set<Id<Person>> set = new HashSet<>();
		for ( int i=min; i <= max; i++ ) {
			set.add( Id.createPersonId( i ) );
		}
		return set;
	}
}
