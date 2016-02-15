/* *********************************************************************** *
 * project: org.matsim.*
 * KeepOnlySelectedPlanAndSimplifyTrips.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author thibautd
 */
public class KeepOnlySelectedPlanAndSimplifyTrips {
	public static void main(final String[] args) {
		final String inPopulation = args[ 0 ];
		final String outPopulation = args[ 1 ];
		// if providing an attribute file, only members of the "default"
		// subpopulation will be dumped
		final String inputAttributes = args[ 2 ];

		final ObjectAttributes atts = new ObjectAttributes();
		if ( inputAttributes != null ) {
			new ObjectAttributesXmlReader( atts ).parse( inputAttributes );
		}

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming( true );

		final PopulationWriter writer =
			new PopulationWriter(
					scenario.getPopulation(),
					scenario.getNetwork() );
		writer.writeStartPlans( outPopulation );

		final TripsToLegsAlgorithm trips2legs =
			new TripsToLegsAlgorithm(
					new StageActivityTypesImpl(
						PtConstants.TRANSIT_ACTIVITY_TYPE ),
					new MainModeIdentifierImpl()
					);
		pop.addAlgorithm( new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				if ( atts.getAttribute( person.getId().toString() , "subpopulation" ) != null ) return;
				PersonUtils.removeUnselectedPlans(((PersonImpl) person));
				trips2legs.run( person.getSelectedPlan() );

				for ( PlanElement pe : person.getSelectedPlan().getPlanElements() ) {
					if ( pe instanceof Activity ) {
						((ActivityImpl) pe).setLinkId( null );
					}
					if ( pe instanceof Leg ) {
						((Leg) pe).setRoute( null );
					}
				}
				writer.writePerson( person );
			}
		});

		new MatsimPopulationReader( scenario ).parse( inPopulation );
		writer.writeEndPlans();
	}
}

