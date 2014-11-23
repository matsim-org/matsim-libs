/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.copying;

/**
 * @author nagel
 *
 */
public abstract class AbstractObject implements MyCopyable {

	@Override
	protected AbstractObject clone() {
		// this encapsulates the exception for downstream use (makes downstream code easier to read, but 
		// means that downstream implementers do no longer get this runtime hint).
		try {
			return (AbstractObject) super.clone() ;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

}

