/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.config.groups;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author mrieser
 */
public final class LinkStatsConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "linkStats";

	private static final String WRITELINKSTATSINTERVAL = "writeLinkStatsInterval";
	private static final String AVERAGELINKSTATSOVERITERATIONS = "averageLinkStatsOverIterations";

	private int writeLinkStatsInterval = 50;
	private int averageLinkStatsOverIterations = 5;

	public LinkStatsConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(WRITELINKSTATSINTERVAL, "Specifies how often the link stats should be calculated and written. Use 0 to disable the generation of link stats.");
		comments.put(AVERAGELINKSTATSOVERITERATIONS, "Specifies over how many iterations the link volumes should be averaged that are used for the " +
				"link statistics. Use 1 or 0 to only use the link volumes of a single iteration. This values cannot be larger than the value specified for " + WRITELINKSTATSINTERVAL);
		return comments;
	}

	@StringGetter( WRITELINKSTATSINTERVAL )
	public int getWriteLinkStatsInterval() {
		return this.writeLinkStatsInterval;
	}
	
	@StringSetter( WRITELINKSTATSINTERVAL )
	public void setWriteLinkStatsInterval(int writeCountsInterval) {
		this.writeLinkStatsInterval = writeCountsInterval;
	}
	
	@StringGetter( AVERAGELINKSTATSOVERITERATIONS )
	public int getAverageLinkStatsOverIterations() {
		return this.averageLinkStatsOverIterations;
	}
	
	@StringSetter( AVERAGELINKSTATSOVERITERATIONS )
	public void setAverageLinkStatsOverIterations(int averageLinkStatsOverIterations) {
		this.averageLinkStatsOverIterations = averageLinkStatsOverIterations;
	}
}
