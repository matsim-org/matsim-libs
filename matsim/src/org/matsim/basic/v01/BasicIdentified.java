/* *********************************************************************** *
 * project: org.matsim.*
 * BasicIdentified.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.basic.v01;

import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.identifiers.IdentifiedI;

public class BasicIdentified implements IdentifiedI {
	protected IdI id;

	public BasicIdentified(IdI id) {
		this.id = id;
	}

	public IdI getId() {
		return this.id;
	}

	public void setId(final IdI id) {
		this.id = id;
	}

	public void setId(final String idstring) {
		this.id = new Id(idstring);
	}

}
