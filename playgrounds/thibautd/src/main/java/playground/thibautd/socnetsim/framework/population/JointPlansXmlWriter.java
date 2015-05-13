/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansXmlWriter.java
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
package playground.thibautd.socnetsim.framework.population;

import static playground.thibautd.socnetsim.framework.population.JointPlansXmlSchemaNames.JOINT_PLAN_TAG;
import static playground.thibautd.socnetsim.framework.population.JointPlansXmlSchemaNames.PERSON_ATT;
import static playground.thibautd.socnetsim.framework.population.JointPlansXmlSchemaNames.PLAN_NR_ATT;
import static playground.thibautd.socnetsim.framework.population.JointPlansXmlSchemaNames.PLAN_TAG;
import static playground.thibautd.socnetsim.framework.population.JointPlansXmlSchemaNames.ROOT_TAG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class JointPlansXmlWriter extends MatsimXmlWriter {
	private final Set<JointPlan> jointPlansSet = new HashSet<JointPlan>();

	public static void write(
			final Population population,
			final JointPlans jointPlans,
			final String file ) {
		new JointPlansXmlWriter( population , jointPlans ).write( file );
	}

	private JointPlansXmlWriter(
			final Population population,
			final JointPlans jointPlans) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				final JointPlan jp = jointPlans.getJointPlan( plan );
				if (jp != null) jointPlansSet.add( jp );
			}
		}
	}

	private void write(final String file) {
		final Counter counter = new Counter( "[JointPlansXmlWriter] dumped jointPlan # " );
		openFile( file );
		writeXmlHead();
		writeDoctype( ROOT_TAG , "jointplans_v1.dtd" );
		writeStartTag(
				ROOT_TAG,
				Collections.<Tuple<String, String>>emptyList() );
		for (JointPlan jp : jointPlansSet) {
			counter.incCounter();
			writeJointPlan( jp );
		}
		counter.printCounter();
		writeEndTag( ROOT_TAG );
		close();
	}

	private void writeJointPlan(final JointPlan jp) {
		startJointPlan();
		for (Plan plan : jp.getIndividualPlans().values()) {
			writeIndividualPlan( plan );
		}
		endJointPlan();
	}

	private void startJointPlan() {
		writeStartTag(
				JOINT_PLAN_TAG,
				Collections.<Tuple<String, String>>emptyList() );
	}

	private void writeIndividualPlan(final Plan plan) {
		final List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

		atts.add( createTuple( PERSON_ATT , plan.getPerson().getId().toString() ) );
		final int index = plan.getPerson().getPlans().indexOf( plan );
		assert index >= 0;
		atts.add( createTuple( PLAN_NR_ATT , index+"" ) );

		writeStartTag( PLAN_TAG , atts , true );
	}

	private void endJointPlan() {
		writeEndTag( JOINT_PLAN_TAG );
	}
}

