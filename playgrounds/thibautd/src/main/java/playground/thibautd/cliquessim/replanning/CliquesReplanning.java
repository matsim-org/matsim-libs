/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesReplanning.java
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
package playground.thibautd.cliquessim.replanning;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.thibautd.cliquessim.population.Cliques;
import playground.thibautd.cliquessim.utils.JointControlerUtils;

/**
 * @author thibautd
 */
public class CliquesReplanning implements ReplanningListener {
	private final PopulationCliqueWrapper wrapper = new PopulationCliqueWrapper();

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		wrapper.cliques = JointControlerUtils.getCliques( event.getControler().getScenario() );
		event.getControler().getStrategyManager().run( wrapper , event.getIteration() );
	}

	private static class PopulationCliqueWrapper implements Population {
		private Cliques cliques = null;

		@Override
		public PopulationFactory getFactory() {
			return null;
		}

		@Override
		public String getName() {
			return cliques.getName();
		}

		@Override
		public void setName(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<Id, ? extends Person> getPersons() {
			return cliques.getCliques();
		}

		@Override
		public void addPerson(Person p) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectAttributes getPersonAttributes() {
			throw new UnsupportedOperationException();
		}
	}
}

