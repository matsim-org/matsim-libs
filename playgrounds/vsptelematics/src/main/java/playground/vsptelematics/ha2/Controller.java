/* *********************************************************************** *
 * project: org.matsim.*
 * Controller
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsptelematics.ha2;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;


/**
 * @author dgrether
 *
 */
public class Controller {
	
	public static void run(Config config){
		Controler c = new Controler(config);
		c.setOverwriteFiles(true);
        c.getConfig().controler().setCreateGraphs(false);
        addListener(c);
		c.run();
	}

	private static void addListener(Controler c){
		c.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(GuidanceRouteTTObserver.class).in(Singleton.class); // only create one instance of these
				bind(GuidanceMobsimFactory.class).in(Singleton.class); // only create one instance of these
				bindMobsim().toProvider(GuidanceMobsimFactory.class); // bind these instances here
				addControlerListenerBinding().to(GuidanceRouteTTObserver.class);
				addControlerListenerBinding().to(GuidanceMobsimFactory.class);
				addEventHandlerBinding().to(GuidanceRouteTTObserver.class);
			}
		});
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		run(config);
	}

	
}
