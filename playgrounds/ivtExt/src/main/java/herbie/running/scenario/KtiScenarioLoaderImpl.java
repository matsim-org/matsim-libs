/* *********************************************************************** *
 * project: org.matsim.*
 * KtiScenarioLoaderImpl.java
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

package herbie.running.scenario;

import herbie.running.config.KtiConfigGroup;
import herbie.running.router.PlansCalcRouteKtiInfo;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class KtiScenarioLoaderImpl extends ScenarioLoaderImpl {

	private static final Logger log = Logger.getLogger(KtiScenarioLoaderImpl.class);
	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private KtiConfigGroup ktiConfigGroup;
	
	public KtiScenarioLoaderImpl(Scenario scenario, PlansCalcRouteKtiInfo plansCalcRouteKtiInfo, KtiConfigGroup ktiConfigGroup) {
		super(scenario);
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
		this.ktiConfigGroup = ktiConfigGroup;
	}

	@Override
	public Scenario loadScenario() {
		String currentDir = new File("tmp").getAbsolutePath();
		currentDir = currentDir.substring(0, currentDir.length() - 3);
		log.info("loading scenario from base directory: " + currentDir);
//		this.loadWorld();
		this.loadNetwork();
		if (this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			this.loadPlansCalcRouteKtiInfo();
		}
		this.loadActivityFacilities();
		this.loadPopulation();
		
		return getScenario();

	}

	private void loadPlansCalcRouteKtiInfo() {
		this.plansCalcRouteKtiInfo.prepare(this.getScenario().getNetwork());
	}
	
}
