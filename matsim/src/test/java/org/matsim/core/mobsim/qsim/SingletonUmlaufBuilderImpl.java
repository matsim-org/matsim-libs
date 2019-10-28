
/* *********************************************************************** *
 * project: org.matsim.*
 * SingletonUmlaufBuilderImpl.java
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

 package org.matsim.core.mobsim.qsim;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufBuilder;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufStueck;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public class SingletonUmlaufBuilderImpl implements UmlaufBuilder {
	
	private Collection<TransitLine> transitLines;
	
	public SingletonUmlaufBuilderImpl(Collection<TransitLine> transitLines) {
		this.transitLines = transitLines;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.pt.queuesim.UmlaufBuilder#build()
	 */
	@Override
	public ArrayList<Umlauf> build() {
		int id = 0;
		ArrayList<Umlauf> umlaeufe = new ArrayList<Umlauf>();
		for (TransitLine line : transitLines) {
			for (TransitRoute route : line.getRoutes().values()) {
				Gbl.assertNotNull(route.getRoute()); // will fail much later if this is null.  kai, may'17
				for (Departure departure : route.getDepartures().values()) {
					UmlaufStueck umlaufStueck = new UmlaufStueck(line, route, departure);
					Umlauf umlauf = new UmlaufImpl(Id.create(id++, Umlauf.class));
					umlauf.getUmlaufStuecke().add(umlaufStueck);
					umlaeufe.add(umlauf);
				}
			}
		}
		return umlaeufe;
	}

}
