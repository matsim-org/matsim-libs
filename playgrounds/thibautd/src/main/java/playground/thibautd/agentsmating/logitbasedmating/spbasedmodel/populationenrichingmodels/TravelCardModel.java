/* *********************************************************************** *
 * project: org.matsim.*
 * TravelCardModel.java
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel.populationenrichingmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.matsim.core.population.PersonImpl;

/**
 * @author thibautd
 */
public class TravelCardModel {
	public static enum TravelCard {
		GENERAL_ABONNEMENT,
		HALBTAX};

	public Collection<TravelCard> getTravelCards(final PersonImpl person) {
		Set<String> cards = person.getTravelcards();
		List<TravelCard> out =  new ArrayList<TravelCard>();

		// currently, just type "unknown:
		// TODO: estimate a model
		// waiting for the model, just consider type "unknown" qs a 1/2 tax
		boolean hasCard = (cards == null) ? false : (cards.size() > 0);

		if (hasCard) out.add( TravelCard.HALBTAX );

		return out;
	}
}

