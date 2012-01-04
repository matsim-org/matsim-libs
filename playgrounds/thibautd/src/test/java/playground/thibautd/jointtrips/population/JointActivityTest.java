/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityTest.java
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
package playground.thibautd.jointtrips.population;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;

/**
 * @author thibautd
 */
public class JointActivityTest {

	@Test
	public void testIds() {
		Set<Id> createdIds = new HashSet<Id>();

		for (int i=0; i <= 100; i++) {
			JointActivity act = new JointActivity(
					"type" ,
					new IdImpl( "link" ),
					new PersonImpl( new IdImpl( "person" ) ) );
			Assert.assertTrue( createdIds.add( act.getId() ) );
		}
	}
}

