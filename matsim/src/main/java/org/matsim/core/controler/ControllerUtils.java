/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.controler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.controler.events.AbstractIterationEvent;
import org.matsim.core.scenario.ScenarioByInstanceModule;

/**
 * @author nagel
 *
 */
public final class ControllerUtils{
	private static final Logger log = LogManager.getLogger( ControllerUtils.class ) ;
	/**
	 * This is meant for creating the matsim injector if one does not need/want {@link Controler}.  Technical reason is that {@link Controler} creates
	 * the injector in the run method, and then it is too late to extract material in a direct way.
	 *
	 * @param config
	 * @param scenario
	 * @return
	 */
	public static com.google.inject.Injector createAdhocInjector( Config config, Scenario scenario ){
		return Injector.createInjector( config, adhocModules( scenario ) );
	}
	/**
	 * This is meant for creating the matsim injector if one does not need/want {@link Controler}.
	 * Technical reason is that {@link Controler} creates
	 * the injector in the run method, and then it is too late to extract material in a direct way.
	 *
	 * @param scenario
	 * @return
	 */
	public static com.google.inject.Injector createAdhocInjector( Scenario scenario ){
		return Injector.createInjector( scenario.getConfig(), adhocModules( scenario ) );
	}

	/**
	 * Like {@link #createAdhocInjector(Config, Scenario)}, but the bindings of {@code overridingModules} replace those of
	 * the default modules, as with {@link Controler#addOverridingModule}. Use it to replace a default implementation,
	 * e.g. one that {@link ControlerDefaultsModule} binds; a second binding of the same key would otherwise make the
	 * injector fail. As with several calls of {@link Controler#addOverridingModule}, each module also overrides the
	 * modules before it in the list.
	 */
	public static com.google.inject.Injector createAdhocInjector( Config config, Scenario scenario, List<AbstractModule> overridingModules ){
		AbstractModule modules = adhocModules( scenario );
		for( AbstractModule overridingModule : overridingModules ){
			modules = AbstractModule.override( List.of( modules ), overridingModule );
		}
		return Injector.createInjector( config, modules );
	}

	/**
	 * Like {@link #createAdhocInjector(Scenario)}, with {@code overridingModules} as in
	 * {@link #createAdhocInjector(Config, Scenario, List)}.
	 */
	public static com.google.inject.Injector createAdhocInjector( Scenario scenario, List<AbstractModule> overridingModules ){
		return createAdhocInjector( scenario.getConfig(), scenario, overridingModules );
	}

	/** The modules of the ad-hoc injector: the default modules of the controller, and {@code scenario}. */
	private static AbstractModule adhocModules( Scenario scenario ){
		return new AbstractModule(){
			@Override public void install(){
				install( new NewControlerModule() );
				install( new ControlerDefaultCoreListenersModule() );
				install( new ControlerDefaultsModule() );
				install( new ScenarioByInstanceModule( scenario ) );
			}
		};
	}

	private ControllerUtils() {} // namespace for static methods only should not be instantiated

	/**
	 * Design decisions:
	 * <ul>
	 * <li>I extracted this method since it is now called <i>twice</i>: once
	 * directly after reading, and once before the iterations start. The second
	 * call seems more important, but I wanted to leave the first one there in
	 * case the program fails before that config dump. Might be put into the
	 * "unexpected shutdown hook" instead. kai, dec'10
	 *
	 * Removed the first call for now, because I am now also checking for
	 * consistency with loaded controler modules. If still desired, we can
	 * put it in the shutdown hook.. michaz aug'14
	 *
	 * </ul>
	 *
	 * @param config  TODO
	 * @param message the message that is written just before the config dump
	 */
	public static final void checkConfigConsistencyAndWriteToLog(Config config,
	                                                                final String message) {
	    log.info(message);
	    String newline = System.lineSeparator();// use native line endings for logfile
	    StringWriter writer = new StringWriter();
	    new ConfigWriter(config).writeStream(new PrintWriter(writer), newline);

		Gbl.logDebugOutputSkipped(log, "logging config.xml");
	    log.debug(newline + newline + writer.getBuffer().toString());
	    log.info("Complete config dump done.");
	    log.info("Checking consistency of config...");
	    config.checkConsistency();
	    log.info("Checking consistency of config done.");
	}

	public static void catchLogEntries() {
		OutputDirectoryLogging.catchLogEntries();
	}

	public static Controller createController( Scenario scenario ) {
		return new Controler( scenario );
	}

	/**
	 * Decides if some functionality should be active in the current iteration if it is
	 * configured to run only at a specific iteration-interval.
	 */
	public static boolean isIterationActive(AbstractIterationEvent event, int interval) {
		if (interval <= 0) {
			return false;
		}
		return event.isLastIteration() || (event.getIteration() % interval == 0);
	}

}
