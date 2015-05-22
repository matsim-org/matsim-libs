/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,  *
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

package playground.sergioo.ptsim2013;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.inject.Provider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sergioo.ptsim2013.qnetsimengine.PTQSimFactory;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;


/**
 * A run Controler for a transit router that depends on the travel times and wait times
 * 
 * @author sergioo
 */

public class ControlerW {

	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, args[0]);
		final Controler controler = new Controler(ScenarioUtils.loadScenario(config));
		if(args.length>1) {
			final StopStopTimeCalculator stopStopTimeCalculatorEvents = new StopStopTimeCalculator(controler.getScenario().getTransitSchedule(), controler.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (controler.getConfig().qsim().getEndTime()-controler.getConfig().qsim().getStartTime()));
			EventsManager eventsManager = EventsUtils.createEventsManager(controler.getConfig());
			eventsManager.addHandler(stopStopTimeCalculatorEvents);
			(new MatsimEventsReader(eventsManager)).readFile(args[1]);
			controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bindMobsim().toProvider(new Provider<Mobsim>() {
                        @Override
                        public Mobsim get() {
                            return new PTQSimFactory(stopStopTimeCalculatorEvents.getStopStopTimes()).createMobsim(controler.getScenario(), controler.getEvents());
                        }
                    });
                }
            });
		}
		else
			controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bindMobsim().toProvider(new Provider<Mobsim>() {
                        @Override
                        public Mobsim get() {
                            return new PTQSimFactory().createMobsim(controler.getScenario(), controler.getEvents());
                        }
                    });
                }
            });
        controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        //controler.addControlerListener(new CalibrationStatsListener(controler.getEvents(), new String[]{args[1], args[2]}, 1, "Travel Survey (Benchmark)", "Red_Scheme", new HashSet<Id<Person>>()));
		controler.run();
	}
	
}
