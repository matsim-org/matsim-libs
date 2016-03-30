/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.data.SignalsData;

import playground.dgrether.xvis.vismodel.VisScenario;
import playground.dgrether.xvis.vismodel.VisScenarioBuilder;


/**
 * @author dgrether
 *
 */
public class DataManager {

	private VisScenario visScenario = null;

	private Scenario scenario = null;

	private SignalsData signalsData;

	public DataManager(Scenario sc){
		this.scenario = sc;
		this.signalsData = (SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME);
		
	}

	public void createVisScenario() {
		if (this.visScenario == null){
			VisScenarioBuilder visScenarioBuilder = new VisScenarioBuilder();
			this.visScenario = visScenarioBuilder.createVisScenario(scenario);
		}
	}

	public VisScenario getVisScenario() {
		return this.visScenario;
	}

	public SignalsData getSignalsData() {
		return this.signalsData;
	}

	
}
