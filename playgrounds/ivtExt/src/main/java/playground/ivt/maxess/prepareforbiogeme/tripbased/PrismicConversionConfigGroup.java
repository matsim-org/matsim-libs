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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author thibautd
 */
public class PrismicConversionConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "prismicTripChoiceSetGeneration";

	// TODO search reasonable defauls
	private double budget_m = 20000;
	private int choiceSetSize = 150;
	private int nThreads = 4;

	private String outputPath = null;

	private Set<String> modes =
			new TreeSet<>( Arrays.asList(
					TransportMode.car,
					TransportMode.bike,
					TransportMode.pt,
					TransportMode.walk ) );
	private String type = "leisure";

	public PrismicConversionConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "budget_m" )
	public double getBudget_m() {
		return budget_m;
	}

	@StringSetter( "budget_m" )
	public void setBudget_m(final double budget_m) {
		this.budget_m = budget_m;
	}

	@StringGetter( "choiceSetSize" )
	public int getChoiceSetSize() {
		return choiceSetSize;
	}

	@StringSetter( "choiceSetSize" )
	public void setChoiceSetSize(final int choiceSetSize) {
		this.choiceSetSize = choiceSetSize;
	}

	@StringGetter( "outputPath" )
	public String getOutputPath() {
		return outputPath;
	}

	@StringSetter( "outputPath" )
	public void setOutputPath(final String outputPath) {
		this.outputPath = outputPath;
	}

	public Set<String> getModes() {
		return modes;
	}

	public void setModes(final Set<String> modes) {
		this.modes = modes;
	}

	@StringGetter( "modes" )
	private String getModesString() {
		return CollectionUtils.setToString( modes );
	}

	@StringSetter( "modes" )
	private void setModes(final String modes) {
		this.modes = new TreeSet<>( Arrays.asList( CollectionUtils.stringToArray( modes ) ) );
	}

	@StringGetter( "activityType" )
	public String getActivityType() {
		return type;
	}

	@StringSetter( "activityType" )
	public void setActivityType(final String type) {
		this.type = type;
	}

	@StringGetter( "nThreads" )
	public int getNumberOfThreads() {
		return nThreads;
	}

	@StringSetter( "nThreads" )
	public void setNumberOfThreads(int nThreads) {
		this.nThreads = nThreads;
	}
}
