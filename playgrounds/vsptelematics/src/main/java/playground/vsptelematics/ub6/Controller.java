/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
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

/**
 *
 */
package playground.vsptelematics.ub6;


import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsptelematics.common.IncidentGenerator;
import playground.vsptelematics.common.TelematicsConfigGroup;

/**
 * @author illenberger
 * @author dgrether
 *
 */
public class Controller {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args[0]) ;
		TelematicsConfigGroup telematicsConfigGroup = ConfigUtils.addOrGetModule(config,
				TelematicsConfigGroup.GROUPNAME, TelematicsConfigGroup.class);
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		Controler c = new Controler(scenario);
		
		System.out.println((new File("blub.txt")).getAbsolutePath()); // /Users/gleich/git/matsim/playgrounds/vsptelematics/blub.txt

		c.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		c.getConfig().controler().setCreateGraphs(false);
		c.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				boolean useHomogeneousTravelTimes = false;
				bind(RouteTTObserver.class).asEagerSingleton();
				addControlerListenerBinding().to(RouteTTObserver.class);
				String param = getConfig().getParam("telematics", "useHomogeneousTravelTimes");
				if (param != null) {
					useHomogeneousTravelTimes = Boolean.parseBoolean(param);
				}
				if (useHomogeneousTravelTimes) {
					addControlerListenerBinding().to(Scorer.class);
				}
				if (getConfig().network().isTimeVariantNetwork()) {
					addControlerListenerBinding().to(IncidentGenerator.class); // incidents.xml is not found probably because no absolute path is set and the relative path points into the wrong directory
				}

			}
		});
		c.setScoringFunctionFactory(new NoScoringFunctionFactory());
//		throw new RuntimeException("I removed the overriding of loadCoreListeners() below since that method should become " +
//				"final in Controler.  I am not sure why this was needed; it looks like it was supposed to be a less heavyweight version of the" +
//				" full Controler core listeners.  Thus, it should also work now.  Otherwise, it needs to be derived from AbstractController instead" +
//				" of from Controler.  kai, feb'13 ") ;
		c.run(); // put back into code when runtime exception is removed.
	}
	
	
//	@Override
//	protected void loadCoreListeners() {
//
////		this.addCoreControlerListener(new CoreControlerListener());
//
////		this.addCoreControlerListener(new PlansReplanning());
//		this.addCoreControlerListener(new PlansDumping());
//
//		this.addCoreControlerListener(new EventsHandling(this.events)); // must be last being added (=first being executed)
//	}


}
