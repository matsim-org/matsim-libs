/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.scenario;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.ConfigUtils;

/**
 * @author thibautd
 */
public class ScenarioImplTest {
	@Test
	void testAddAndGetScenarioElement() {
		final MutableScenario s = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final Object element1 = new Object();
		final String name1 = "peter_parker";
		final Object element2 = new Object();
		final String name2 = "popeye";

		s.addScenarioElement( name1 , element1 );
		s.addScenarioElement( name2 , element2 );
		Assertions.assertSame(
				element1,
				s.getScenarioElement( name1 ),
				"unexpected scenario element" );
		// just check that it is got, not removed
		Assertions.assertSame(
				element1,
				s.getScenarioElement( name1 ),
				"unexpected scenario element" );

		Assertions.assertSame(
				element2,
				s.getScenarioElement( name2 ),
				"unexpected scenario element" );

	}

	@Test
	void testCannotAddAnElementToAnExistingName() {
		final MutableScenario s = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final String name = "bruce_wayne";

		s.addScenarioElement( name , new Object() );
		try {
			s.addScenarioElement( name , new Object() );
		}
		catch (IllegalStateException e) {
			return;
		}
		catch (Exception e) {
			Assertions.fail( "wrong exception thrown when trying to add an element for an existing name "+e.getClass().getName() );
		}
		Assertions.fail( "no exception thrown when trying to add an element for an existing name" );
	}

	@Test
	void testRemoveElement() {
		final MutableScenario s = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final Object element = new Object();
		final String name = "clark_kent";

		s.addScenarioElement( name , element );
		Assertions.assertSame(
				element,
				s.removeScenarioElement( name ),
				"unexpected removed element" );
		Assertions.assertNull(
				s.getScenarioElement( name ),
				"element was not removed" );

	}

}

