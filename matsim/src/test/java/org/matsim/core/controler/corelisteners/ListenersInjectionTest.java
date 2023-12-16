/* *********************************************************************** *
 * project: org.matsim.*
 * ListenersInjectionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.core.controler.corelisteners;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author thibautd
 */
public class ListenersInjectionTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDumpDataAtEndIsSingleton() {
		testIsSingleton( DumpDataAtEnd.class );
	}

	@Test
	void testEvensHandlingIsSingleton() {
		testIsSingleton( EventsHandling.class );
	}

	@Test
	void testPlansDumpingIsSingleton() {
		testIsSingleton( PlansDumping.class );
	}

	@Test
	void testPlansReplanningIsSingleton() {
		testIsSingleton( PlansReplanning.class );
	}

	@Test
	void testPlansScoringIsSingleton() {
		testIsSingleton( PlansScoring.class );
	}

	private void testIsSingleton( final Class<? extends ControlerListener> klass ) {
		final Config config = ConfigUtils.createConfig();
		final String outputDir = utils.getOutputDirectory();
		config.controller().setOutputDirectory( outputDir );

        final com.google.inject.Injector injector = Injector.createInjector(
                config,
				// defaults needed as listeners depend on some stuff there
				new ControlerDefaultsModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
						// put dummy dependencies to get the listenners happy
						bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);
						bind(OutputDirectoryHierarchy.class).toInstance(new OutputDirectoryHierarchy(outputDir,
								OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists,
								config.controller().getCompressionType()));
						bind(IterationStopWatch.class).toInstance(new IterationStopWatch());
						bind(IterationCounter.class).toInstance(() -> 0);
						install(new ScenarioByInstanceModule(ScenarioUtils.createScenario(config)));
					}
                },
				new ControlerDefaultCoreListenersModule());


		final ControlerListener o1 = injector.getInstance( klass );
		final ControlerListener o2 = injector.getInstance( klass );

		Assertions.assertSame(
				o1,
				o2,
				"Two different instances of "+klass.getName()+" returned by injector!" );
	}
}

