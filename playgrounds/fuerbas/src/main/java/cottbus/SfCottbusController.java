/* *********************************************************************** *
 * project: org.matsim.*
 * SfCottbusController.java
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

package cottbus;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;

/**
 * @author fuerbas
 *
 */

public class SfCottbusController {

	public static void main(String[] args) {
		Controler con = new Controler("E:\\Cottbus\\Cottbus_pt\\Cottbus-pt\\config_1.xml");		//args: configfile
		con.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		// NOTE: this is the code for the deleted TransitControlerListener. It was added
		// here, although if the config is properly set (ie the "useTransit" flag in the
		// scenario config group is set to true), it is useless. td, sept. 2012
		ControlerListener lis = new StartupListener() {
				@Override
				public void notifyStartup(final StartupEvent event) {
					final Scenario scenario = event.getControler().getScenario();
					if (event.getControler().getTransitRouterFactory() == null) {
						
						final TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
								scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
								scenario.getConfig().vspExperimental());

						event.getControler().addOverridingModule(new AbstractModule() {
							@Override
							public void install() {
								bind(TransitRouter.class).toProvider(new TransitRouterImplFactory(
										scenario.getTransitSchedule(), transitRouterConfig));
							}
						});
					}

				}
			};
		con.addControlerListener(lis);

	}

}

