/* *********************************************************************** *
 * project: org.matsim.*
 * RunVenkteshScenario
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
package playground.dgrether.signalsystems.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.signalsystems.data.SignalsDataImpl;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;

/**
 * @author dgrether
 * 
 */
public class RunVenkteshScenario {

	public static void main(String[] args) {
		String config = "";

		Controler controler = new Controler(config);
		controler.addControlerListener(new AfterMobsimListener() {

			public void notifyAfterMobsim(AfterMobsimEvent event) {
				Scenario scenario = event.getControler().getScenario();
				int newValue = 0;
				for (SignalSystemControllerData intersectionSignal : scenario
						.getScenarioElement(SignalsDataImpl.class).getSignalControlData()
						.getSignalSystemControllerDataBySystemId().values()) {
					
					for (SignalPlanData plan : intersectionSignal.getSignalPlanData().values()) {
						for (SignalGroupSettingsData data : plan.getSignalGroupSettingsDataByGroupId().values()) {
							data.setDropping(newValue);
							data.setOnset(newValue);
						}
					}
				}
			}
		});

	}

}
