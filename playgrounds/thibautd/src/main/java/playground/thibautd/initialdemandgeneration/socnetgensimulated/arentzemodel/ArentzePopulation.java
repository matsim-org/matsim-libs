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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.IndexedPopulation;
import playground.thibautd.utils.BooleanList;

/**
 * @author thibautd
 */
public class ArentzePopulation extends IndexedPopulation {
	private final char[] ageCategory;
	private final BooleanList isMale;
	private final Coord[] coord;

	public ArentzePopulation(
			final Id[] ids,
			final char[] ageCategory,
			final boolean[] isMale,
			final Coord[] coord ) {
		super(ids);
		this.ageCategory = ageCategory;
		this.isMale = new BooleanList( isMale );
		this.coord = coord;
	}

	public char getAgeCategory(final int agent) {
		return ageCategory[agent];
	}

	public boolean isMale(final int agent) {
		return isMale.get( agent );
	}

	public Coord getCoord(final int agent) {
		return coord[agent];
	}
}
