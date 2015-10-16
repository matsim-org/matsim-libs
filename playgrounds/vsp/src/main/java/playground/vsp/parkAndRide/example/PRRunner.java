/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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

/**
 * 
 */
package playground.vsp.parkAndRide.example;


import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Provider;

import playground.vsp.parkAndRide.PRAdaptiveCapacityControl;
import playground.vsp.parkAndRide.PRConfigGroup;
import playground.vsp.parkAndRide.PRConstants;
import playground.vsp.parkAndRide.PRFacility;
import playground.vsp.parkAndRide.PRFileReader;
import playground.vsp.parkAndRide.scoring.PRScoringFunctionFactory;

/**
 * @author ikaddoura
 *
 */
public class PRRunner {
	
	static String configFile;
		
	public static void main(String[] args) throws IOException {
		
		configFile = "/path-to/config.xml";
		
		PRRunner main = new PRRunner();
		main.run();
	}
	
	private void run() {
		
		Config config = new Config();
		config.addModule(new PRConfigGroup());
		ConfigUtils.loadConfig(config, configFile);
				
		final Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		PRConfigGroup prSettings = (PRConfigGroup) controler.getConfig().getModule(PRConfigGroup.GROUP_NAME);

		ActivityParams prActivityParams = new ActivityParams(PRConstants.PARKANDRIDE_ACTIVITY_TYPE);
		prActivityParams.setTypicalDuration(prSettings.getTypicalDuration());
		controler.getConfig().planCalcScore().addActivityParams(prActivityParams);

        controler.setScoringFunctionFactory(new PRScoringFunctionFactory(controler.getScenario(), prSettings.getIntermodalTransferPenalty()));
		
		PRFileReader prReader = new PRFileReader(prSettings.getInputFile());
		Map<Id<PRFacility>, PRFacility> id2prFacility = prReader.getId2prFacility();
		final PRAdaptiveCapacityControl adaptiveControl = new PRAdaptiveCapacityControl(id2prFacility);

		PRControlerListener prControlerListener = new PRControlerListener(controler, adaptiveControl);
		controler.addControlerListener(prControlerListener);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						final QSim mobsim = QSimUtils.createDefaultQSim(controler.getScenario(), controler.getEvents());
						mobsim.addMobsimEngine(adaptiveControl);
						return mobsim;
					}
				});
			}
		});

		controler.run();
		
	}

}
	
