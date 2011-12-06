/* *********************************************************************** *
 * project: org.matsim.*
 * TestComprehensiveChoiceModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author thibautd
 */
public class TestComprehensiveChoiceModel {
	private static final Id id1 = new IdImpl( 1 );
	private static final Id id2 = new IdImpl( 2 );
	private static final Id id3 = new IdImpl( 3 );

	private List<? extends Plan> plans ;

	// /////////////////////////////////////////////////////////////////////////
	// init
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void initPlans() {
		Person person = new PersonImpl( id1 );

		// simple H-W-H
		PlanImpl plan1 = new PlanImpl( person );
		plan1.createAndAddActivity( "h" , id1 );
		plan1.createAndAddLeg( "car" );
		plan1.createAndAddActivity( "w" , id2 );
		plan1.createAndAddLeg( "car" );
		plan1.createAndAddActivity( "h" , id1 );

		// more sophisticated one-tour plan
		PlanImpl plan2 = new PlanImpl( person );
		plan2.createAndAddActivity( "h" , id1 );
		plan2.createAndAddLeg( "car" );
		plan2.createAndAddActivity( "w" , id2 );
		plan2.createAndAddLeg( "pt" );
		plan2.createAndAddActivity( "s" , id3 );
		plan2.createAndAddLeg( "bike" );
		plan2.createAndAddActivity( "h" , id1 );

		// two home-based tours plan
		PlanImpl plan3 = new PlanImpl( person );
		plan3.createAndAddActivity( "h" , id1 );
		plan3.createAndAddLeg( "car" );
		plan3.createAndAddActivity( "w" , id2 );
		plan3.createAndAddLeg( "pt" );
		plan3.createAndAddActivity( "h" , id1 );
		plan3.createAndAddLeg( "car" );
		plan3.createAndAddActivity( "w" , id2 );
		plan3.createAndAddLeg( "bike" );
		plan3.createAndAddActivity( "h" , id1 );

		// one tour, two subtours
		PlanImpl plan4 = new PlanImpl( person );
		plan4.createAndAddActivity( "h" , id1 );
		plan4.createAndAddLeg( "car" );
		plan4.createAndAddActivity( "w" , id2 );
		plan4.createAndAddLeg( "pt" );
		plan4.createAndAddActivity( "s" , id3 );
		plan4.createAndAddLeg( "car" );
		plan4.createAndAddActivity( "w" , id2 );
		plan4.createAndAddLeg( "bike" );
		plan4.createAndAddActivity( "h" , id1 );

		// long plan
		PlanImpl plan5 = new PlanImpl( person );
		plan5.createAndAddActivity( "h" , id1 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "pt" );
		plan5.createAndAddActivity( "s" , id3 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "bike" );
		plan5.createAndAddActivity( "h" , id1 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "pt" );
		plan5.createAndAddActivity( "s" , id3 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "bike" );
		plan5.createAndAddActivity( "h" , id1 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "pt" );
		plan5.createAndAddActivity( "s" , id3 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "bike" );
		plan5.createAndAddActivity( "h" , id1 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "pt" );
		plan5.createAndAddActivity( "s" , id3 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "bike" );
		plan5.createAndAddActivity( "h" , id1 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "pt" );
		plan5.createAndAddActivity( "s" , id3 );
		plan5.createAndAddLeg( "car" );
		plan5.createAndAddActivity( "w" , id2 );
		plan5.createAndAddLeg( "bike" );
		plan5.createAndAddActivity( "h" , id1 );

		plans = Arrays.asList( plan1 , plan2 , plan3 , plan4 );
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testSumOfProbabilities() {
		ComprehensiveChoiceModel model = new ComprehensiveChoiceModel();
		model.setTripLevelChoiceModel( new DumbModel() );

		for (Plan plan : plans) {
			try {
				Map< List<Alternative> , Double> probs = model.getChoiceProbabilities(
						model.getTripLevelChoiceModel().getDecisionMakerFactory().createDecisionMaker( plan.getPerson() ),
						plan);

				double cumul = 0;

				for ( Double prob : probs.values() ) {
					cumul += prob;
				}

				Assert.assertEquals(
						"probabilities do not sum to 1",
						1d,
						cumul,
						MatsimTestUtils.EPSILON);
			}
			catch ( DecisionMakerFactory.UnelectableAgentException e ) {
				throw new RuntimeException( "test could not be performed" , e );
			}
		}
	}
}
