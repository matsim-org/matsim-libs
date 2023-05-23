/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package lsp.events;

import org.matsim.contrib.freight.events.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utils for {@link LogisticEventCreator}s
 *
 * @author kturner
 */
public final class LogisticsEventCreatorUtils {

	private LogisticsEventCreatorUtils(){
	}

	/**
	 * @return a collection of the standard freightEvent creators
	 */
	public static Collection<LogisticEventCreator> getStandardEventCreators(){
		List<LogisticEventCreator> creators = new ArrayList<>();
		creators.add(new HandlinInHubStartEventCreator());
		creators.add(new HandlinInHubEndEventCreator());
		return creators;
	}
	
}
