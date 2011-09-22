/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioLoader2DImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.scenario;

import org.matsim.api.core.v01.Scenario;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesReader;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

public class ScenarioLoader2DImpl  {

	private StaticEnvironmentDistancesField sff;

	private final Scenario scenarioData;


	private final Sim2DConfigGroup sim2DConfig;

	public ScenarioLoader2DImpl(Scenario scenario) {
		this.scenarioData = scenario;
		MyDataContainer c = new MyDataContainer();
		this.scenarioData.addScenarioElement(c);
		this.sim2DConfig = (Sim2DConfigGroup) this.scenarioData.getConfig().getModule("sim2d");
	}

	public void load2DScenario() {

		loadStaticEnvironmentDistancesField();
	}

	private void loadStaticEnvironmentDistancesField() {

		if (this.sim2DConfig.getStaticEnvFieldFile() == null) {
			throw new RuntimeException("this mode is not longer supported!");
		} else  {
			loadStaticEnvironmentDistancesField(this.sim2DConfig.getStaticEnvFieldFile());
		}


		this.scenarioData.addScenarioElement(this.sff);

	}

	private void loadStaticEnvironmentDistancesField(String staticEnvFieldFile) {


		EnvironmentDistancesReader r = new EnvironmentDistancesReader();
		r.setValidating(false);
		r.parse(this.sim2DConfig.getStaticEnvFieldFile());
		this.sff = r.getEnvDistField();

	}

}
