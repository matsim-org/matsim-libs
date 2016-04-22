/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegMode.java
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

package playground.singapore.springcalibration.run.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.singapore.springcalibration.run.TaxiUtils;

/* 
 * @author anhorni
 */
public class SingaporeModeChangeAlgoGenerator extends AbstractMultithreadedModule {

	private String[] availableModes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;
	private Population population;
	private TaxiUtils taxiUtils;
	private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;

	public SingaporeModeChangeAlgoGenerator(final GlobalConfigGroup globalConfigGroup, 
			ChangeLegModeConfigGroup changeLegModeConfigGroup, 
			SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup,
			Population population, 
			TaxiUtils taxiUtils) {
		super(globalConfigGroup.getNumberOfThreads());
		this.availableModes = changeLegModeConfigGroup.getModes();
		this.ignoreCarAvailability = changeLegModeConfigGroup.getIgnoreCarAvailability();
		this.population = population;
		this.taxiUtils = taxiUtils;
		this.subtourModeChoiceConfigGroup = subtourModeChoiceConfigGroup;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		SingaporeLegModeChange algo = new SingaporeLegModeChange(
				this.availableModes, MatsimRandom.getLocalInstance(), population, this.taxiUtils, subtourModeChoiceConfigGroup);
		
		algo.setIgnoreCarAvailability(this.ignoreCarAvailability);
		return algo;
	}

}
