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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import org.matsim.api.core.v01.Id;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;

/**
 * @author thibautd
 */
public class Alternative<N extends Enum<N>> {
	private final N nestId;
	private final Id<Alternative> alternativeId;
	private final Trip alternative;

	public Alternative( N nestId,
			Id<Alternative> alternativeId,
			Trip alternative ) {
		this.nestId = nestId;
		this.alternativeId = alternativeId;
		this.alternative = alternative;
	}

	public N getNestId() {
		return nestId;
	}

	public Trip getAlternative() {
		return alternative;
	}

	public Id<Alternative> getAlternativeId() {
		return alternativeId;
	}

	@Override
	public String toString() {
		return "[Alternative NestId="+getNestId()+
				", "+alternative+"]";
	}
}
