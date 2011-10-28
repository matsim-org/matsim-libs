/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionMakerFactory.java
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author thibautd
 */
public interface DecisionMakerFactory extends MatsimFactory {
	/**
	 * Creates a decision maker instance representing this agent in the choice
	 * process.
	 *
	 * @param agent the agent to represent
	 * @return the DecisionMaker representation of the agent
	 *
	 * @throws UnelectableAgentException if the agent is not a valid decision
	 * maker (e.g. he has no license whereas the models can only handle driver
	 * agents)
	 */
	public DecisionMaker createDecisionMaker(Person agent) throws UnelectableAgentException;

	public static class UnelectableAgentException extends Exception {
		private static final long serialVersionUID = 1L;

		public UnelectableAgentException() {
			super();
		}

		public UnelectableAgentException( final String msg ) {
			super( msg );
		}

		public UnelectableAgentException( final Throwable cause ) {
			super( cause );
		}

		public UnelectableAgentException( final String msg, final Throwable cause ) {
			super( msg , cause );
		}
	}
}

