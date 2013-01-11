/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansXmlReader.java
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
package playground.thibautd.socnetsim.population;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.utils.AbstractParsePullXmlReader;
import static playground.thibautd.socnetsim.population.JointPlansXmlSchemaNames.*;

/**
 * @author thibautd
 */
public class JointPlansXmlReader extends AbstractParsePullXmlReader<List<JointPlan>> {
	public static List<JointPlan> readJointPlans(
			final Population population,
			final String fileName) {
		return new JointPlansXmlReader( population ).readFile( fileName );
	}

	private final Population population;

	private JointPlansXmlReader(final Population population) {
		this.population = population;
	}

	@Override
	protected List<JointPlan> parse(
			final XMLStreamReader streamReader)
			throws XMLStreamException {
		final Counter counter = new Counter( "parsing joint plan # " );
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();

		while ( streamReader.next() != XMLStreamConstants.START_ELEMENT );
		while (streamReader.hasNext()) {
			if ( !streamReader.getLocalName().equals( JOINT_PLAN_TAG ) ) continue;
			counter.incCounter();
			jointPlans.add( parseJointPlan( streamReader ) );
		}

		return jointPlans;
	}

	private JointPlan parseJointPlan(
			final XMLStreamReader streamReader)
			throws XMLStreamException {
		final Map<Id, Plan> plans = new LinkedHashMap<Id, Plan>();

		while ( streamReader.next() != XMLStreamConstants.START_ELEMENT );
		while ( streamReader.getLocalName().equals( PLAN_TAG ) ) {
			final Id id = new IdImpl(
					streamReader.getAttributeValue(
						null,
						PERSON_ATT ) );

			final int planIndex = Integer.parseInt(
					streamReader.getAttributeValue(
						null,
						PLAN_NR_ATT ) );

			final Person person = population.getPersons().get( id );
			final Plan plan = person.getPlans().get( planIndex );
			// use the ID from person on purpose to minimize memory consumption
			plans.put( person.getId() , plan );

			while ( streamReader.next() != XMLStreamConstants.START_ELEMENT );
		}

		return JointPlanFactory.createJointPlan( plans );
	}
}

