
/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationUtilsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;

/**
 * @author thibautd
 */
public class PopulationUtilsTest {

	 @Test
	 void testPlanAttributesCopy() {
		final Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig() );

		final Person person = population.getFactory().createPerson(Id.createPersonId( "Donald Trump"));

		final Plan plan = population.getFactory().createPlan();
		person.addPlan( plan );

		final Activity act = population.getFactory().createActivityFromCoord( "speech" , new Coord( 0 , 0 ) );
		plan.addActivity( act );

		act.getAttributes().putAttribute( "makes sense" , false );
		act.getAttributes().putAttribute( "length" , 1895L );

		final Leg leg = population.getFactory().createLeg( "SUV" );
		plan.addLeg( leg );
		plan.addActivity( population.getFactory().createActivityFromLinkId( "tweet" , Id.createLinkId( 2 )));

		leg.getAttributes().putAttribute( "mpg" , 0.000001d );


		final Plan planCopy = population.getFactory().createPlan();
		PopulationUtils.copyFromTo( plan , planCopy );

		Assertions.assertEquals( plan.getPlanElements().size(),
				planCopy.getPlanElements().size(),
				"unexpected plan length" );

		final Activity activityCopy = (Activity) planCopy.getPlanElements().get( 0 );

		Assertions.assertEquals( act.getAttributes().getAttribute( "makes sense" ),
				activityCopy.getAttributes().getAttribute( "makes sense" ),
				"unexpected attribute" );

		Assertions.assertEquals( act.getAttributes().getAttribute( "length" ),
				activityCopy.getAttributes().getAttribute( "length" ),
				"unexpected attribute" );

		final Leg legCopy = (Leg) planCopy.getPlanElements().get( 1 );

		Assertions.assertEquals( leg.getAttributes().getAttribute( "mpg" ),
				legCopy.getAttributes().getAttribute( "mpg" ),
				"unexpected attribute" );
	}

}
