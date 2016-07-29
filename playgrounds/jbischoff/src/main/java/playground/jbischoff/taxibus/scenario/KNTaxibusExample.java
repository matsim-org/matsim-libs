/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.jbischoff.taxibus.run.configuration.ConfigBasedTaxibusLaunchUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;
import playground.jbischoff.taxibus.scenario.analysis.quick.TaxiBusTravelTimesAnalyzer;
import playground.jbischoff.taxibus.scenario.analysis.quick.TraveltimeAndDistanceEventHandler;

/**
 * @author jbischoff
 *
 */
public class KNTaxibusExample {
	
//	also try RunTaxibusExample
//	also try RunSharedTaxiExample

	public static void main(String[] args) {

		// all vehicles start at same place; all passengers start at same place:
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/vw_rufbus/scenario/input/example/configVWTB.xml", new TaxibusConfigGroup());
		
		// passengers live in larger area and want to go to railway station:
		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/braunschweig/scenario/taxibus-example/input/config.xml", new TaxibusConfigGroup());
		
		// passengers live in larger area and have distributed destinations (which are, however, all called "train" :-( ):
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/braunschweig/scenario/taxibus-example/input/configShared.xml", new TaxibusConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists) ;
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
//		visConfig.setMapOverlayMode(true);
		visConfig.setAgentSize(250);
		visConfig.setLinkWidth(5);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setDrawTime(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new ConfigBasedTaxibusLaunchUtils(controler).initiateTaxibusses();

		final TaxiBusTravelTimesAnalyzer taxibusTravelTimesAnalyzer = new TaxiBusTravelTimesAnalyzer();
		final TraveltimeAndDistanceEventHandler ttEventHandler = new TraveltimeAndDistanceEventHandler(scenario.getNetwork());
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				addEventHandlerBinding().toInstance(taxibusTravelTimesAnalyzer);
				addEventHandlerBinding().toInstance(ttEventHandler);
			}
		});
		controler.addOverridingModule( new OTFVisLiveModule() );
		
		controler.run();

		taxibusTravelTimesAnalyzer.printOutput();
		ttEventHandler.printOutput();
	}

	
	
}
