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
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxibus.run.configuration.TaxibusConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 *
 */
public class KNTaxibusExample {
	
//	also try RunSharedTaxiExample

	public static void main(String[] args) {

		// all vehicles start at same place; all passengers start at same place:

		//TODO: Kai, in case you use this, please update your SVN first, Joschka // Dec 2016
		// passengers live in larger area and want to go to railway station (now with nicer routes), two different algorithms:
		
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/braunschweig/scenario/taxibus-example/input/configClustered.xml", new TaxibusConfigGroup());
		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/braunschweig/scenario/taxibus-example/input/configJsprit.xml", new TaxibusConfigGroup(), new DvrpConfigGroup());
		
		// passengers live in larger area and have distributed destinations, shared taxi for 2 persons:
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/braunschweig/scenario/taxibus-example/input/configShared.xml", new TaxibusConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists) ;
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
		
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
//		visConfig.setMapOverlayMode(true);
		visConfig.setAgentSize(250);
		visConfig.setLinkWidth(5);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setDrawTime(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new org.matsim.contrib.taxibus.run.configuration.ConfigBasedTaxibusLaunchUtils(controler).initiateTaxibusses();
		controler.addOverridingModule( new OTFVisLiveModule() );
		
		controler.run();

	
	}

	
	
}
