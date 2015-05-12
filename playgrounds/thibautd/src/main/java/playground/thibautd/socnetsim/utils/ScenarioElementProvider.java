/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioElementProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.utils;

import org.matsim.api.core.v01.Scenario;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public final class ScenarioElementProvider<T> implements Provider<T> {
	private final String elementName;
	@Inject Scenario sc;

	public ScenarioElementProvider( final String elementName ) {
		this.elementName = elementName;
	}

	@Override
	public T get() {
		return (T) sc.getScenarioElement( elementName );
	}
}

