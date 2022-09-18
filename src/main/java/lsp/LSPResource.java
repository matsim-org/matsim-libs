/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Collection;

/**
 *
 */
public interface LSPResource extends Identifiable<LSPResource>, HasSimulationTrackers<LSPResource>, Attributable {

	Id<Link> getStartLinkId();

//	Class<?> getClassOfResource();
//	// yyyyyy is it really necessary to use reflection in a code that we fully own?  kai, may'18
//	//One could also leave this method signature out tm, august'18

	Id<Link> getEndLinkId();

	Collection<LogisticsSolutionElement> getClientElements();

	void schedule(int bufferTime);

}
