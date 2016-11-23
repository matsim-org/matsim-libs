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
package playground.thibautd.negotiation.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
public class NegotiationAgent<P extends Proposition> implements Identifiable<Person> {
	private final Id<Person> id;

	private final PropositionUtility<P> utility;
	private final AlternativesGenerator<P> alternativesGenerator;
	private final NegotiatingAgents<P> negotiatingAgents;

	private double currentBestUtil;
	private P currentBestProp = null;

	public NegotiationAgent( final Id<Person> id,
			final PropositionUtility<P> utility,
			final AlternativesGenerator<P> alternativesGenerator,
			final NegotiatingAgents<P> negotiatingAgents ) {
		this.id = id;
		this.utility = utility;
		// ie cost of being alone
		// find a way to do without nulls
		this.currentBestUtil = utility.utility( this , null );
		this.alternativesGenerator = alternativesGenerator;
		this.negotiatingAgents = negotiatingAgents;
	}

	private boolean accept( P proposition ) {
		return utility.utility( this , proposition ) > currentBestUtil;
	}

	private void notifyAccepted( P proposition ) {
		this.currentBestUtil = utility.utility( this , proposition );
		this.currentBestProp = proposition;
	}

	public boolean planActivity() {
		boolean found = false;

		for ( P proposition : alternativesGenerator.generateAlternatives( this ) ) {
			if ( !negotiatingAgents.contains( proposition.getProposedIds() ) ) continue;
			final double u = utility.utility( this , proposition );
			if ( u < currentBestUtil ) continue;

			if ( !proposition.getProposedIds().stream()
					.map( negotiatingAgents::get )
					.allMatch( a -> a.accept( proposition ) ) ) continue;

			found = true;
			currentBestUtil = u;
			currentBestProp = proposition;
		}

		if ( found ) {
			currentBestProp.getGroupIds().stream()
					.map( negotiatingAgents::get )
					.forEach( a -> a.notifyAccepted( currentBestProp ) );
		}

		return found;
	}

	public P getBestProposition() {
		return currentBestProp;
	}

	@Override
	public Id<Person> getId() {
		return id;
	}
}

