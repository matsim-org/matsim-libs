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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.name.Names;


/**
 * @author thibautd
 */
public class ListenersInjectionTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDumpDataAtEndIsSingleton() {
		testIsSingleton( DumpDataAtEnd.class );
	}

	@Test
	public void testEvensHandlingIsSingleton() {
		testIsSingleton( EventsHandling.class );
	}

	@Test
	public void testPlansDumpingIsSingleton() {
		testIsSingleton( PlansDumping.class );
	}

	@Test
	public void testPlansReplanningIsSingleton() {
		testIsSingleton( PlansReplanning.class );
	}

	@Test
	public void testPlansScoringIsSingleton() {
		testIsSingleton( PlansScoring.class );
	}

	private void testIsSingleton( final Class<? extends ControlerListener> klass ) {
		final Config config = ConfigUtils.createConfig();
		final String outputDir = utils.getOutputDirectory();
		config.controler().setOutputDirectory( outputDir );

        final Injector injector = Injector.createInjector(
                config,
				// defaults needed as listeners depend on some stuff there
				new ControlerDefaultsModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        // put dummy dependencies to get the listenners happy
						bind(OutputDirectoryHierarchy.class).toInstance( new OutputDirectoryHierarchy( outputDir , OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists ) );
						bind(IterationStopWatch.class).toInstance( new IterationStopWatch() );
						bind(Scenario.class).toInstance( ScenarioUtils.createScenario( config ) );
						bind(ControlerI.class).toInstance(new ControlerI() {

							@Override
							public void run() {

							}

							@Override
							public Integer getIterationNumber() {
								return 1;
							}
						});
                    }
                },
				new ControlerDefaultCoreListenersModule());


		final ControlerListener o1 = injector.getInstance( klass );
		final ControlerListener o2 = injector.getInstance( klass );

		Assert.assertSame(
				"Two different instances of "+klass.getName()+" returned by injector!",
				o1,
				o2 );
	}
}

