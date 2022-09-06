
/* *********************************************************************** *
 * project: org.matsim.*
 * QSimComponentsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.inject.BindingAnnotation;
import com.google.inject.ProvisionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.scenario.ScenarioUtils;

import javax.inject.Inject;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class QSimComponentsTest {
	private static final Logger log = LogManager.getLogger( QSimComponentsTest.class ) ;

	@Test
	public void testAddComponentViaString() {

		// request "abc" component by config:
		Config config = ConfigUtils.createConfig();

		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> list = new ArrayList<>( qsimComponentsConfig.getActiveComponents() ) ; // contains the "standard components" (*)
		list.add( "abc" ) ;
		qsimComponentsConfig.setActiveComponents( list );

		log.warn( "" );
		log.warn( "qsimComponentsConfig=" + qsimComponentsConfig + "; active components:");
		for( String component : qsimComponentsConfig.getActiveComponents() ){
			log.warn( component );
		}
		log.warn( "" );

		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// add event handler to catch event by MockMobsimListener:
		AnalysisEventsHandler handler = new AnalysisEventsHandler() ;
		eventsManager.addHandler( handler );

		// register the "abc" component to the QSim:
		AbstractQSimModule module = new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addQSimComponentBinding( "abc" ).to(  MockMobsimListener.class ) ;
			}
		};

		new QSimBuilder(config) //
								.useDefaultComponents() // uses components from config
								.useDefaultQSimModules() // registers the default modules (needed here because of (*))
								.addQSimModule(module) // registers the additional modules
								.build(scenario, eventsManager) //
								.run();

		Assert.assertTrue( "MockMobsimListener was not added to QSim", handler.hasBeenCalled() ) ;
	}
	/**
	 * this tests what happens when we run the same as in {@link #testAddComponentViaString()}, but without requesting the "abc"
	 * component by config.  "abc" will not be activated (although it is "registered" = added as a qsim module).
	 */
	@Test
	public void testAddModuleOnly() {

		Config config = ConfigUtils.createConfig();

		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> list = new ArrayList<>( qsimComponentsConfig.getActiveComponents() ) ; // contains the "standard components" (*)
//		list.add( "abc" ) ;
		qsimComponentsConfig.setActiveComponents( list );

		log.warn( "" );
		log.warn( "qsimComponentsConfig=" + qsimComponentsConfig + "; active components:");
		for( String component : qsimComponentsConfig.getActiveComponents() ){
			log.warn( component );
		}
		log.warn( "" );

		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		AnalysisEventsHandler handler = new AnalysisEventsHandler() ;
		eventsManager.addHandler( handler );

		AbstractQSimModule module = new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addQSimComponentBinding( "abc" ).to(  MockMobsimListener.class ) ;
			}
		};

		new QSimBuilder(config) //
								.useDefaultComponents() // uses components from config
								.useDefaultQSimModules() // registers the default modules (needed here because of (*))
								.addQSimModule(module) // registers the additional modules
								.build(scenario, eventsManager) //
								.run();

		Assert.assertFalse( "MockMobsimListener was added to QSim although it should not have been added", handler.hasBeenCalled() ) ;
	}

	/**
	 * this tests what happens when we run the same as in {@link #testAddComponentViaString()}, but without registering the "abc"
	 * module.  Evidently, it throws an exception.
	 */
	@Test( expected = ProvisionException.class )
	public void testAddComponentOnly() {
		Config config = ConfigUtils.createConfig();

		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> list = new ArrayList<>( qsimComponentsConfig.getActiveComponents() ) ; // contains the "standard components" (*)
		list.add( "abc" ) ;
		qsimComponentsConfig.setActiveComponents( list );

		log.warn( "" );
		log.warn( "qsimComponentsConfig=" + qsimComponentsConfig + "; active components:");
		for( String component : qsimComponentsConfig.getActiveComponents() ){
			log.warn( component );
		}
		log.warn( "" );

		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		AnalysisEventsHandler handler = new AnalysisEventsHandler() ;
		eventsManager.addHandler( handler );

		AbstractQSimModule module = new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addQSimComponentBinding( "abc" ).to(  MockMobsimListener.class ) ;
			}
		};

		new QSimBuilder(config) //
								.useDefaultComponents() // uses components from config
								.useDefaultQSimModules() // registers the default modules (needed here because of (*))
//								.addQSimModule(module) // registers the additional modules
								.build(scenario, eventsManager) //
								.run();

	}
	/**
	 * this tests what happens when we run the same as in {@link #testAddComponentViaString()}, but request the "abc" component twice.
	 */
	@Test
//			( expected = IllegalStateException.class )
	public void testAddComponentViaStringTwice() {

		// request "abc" component by config:
		Config config = ConfigUtils.createConfig();

		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> list = new ArrayList<>( qsimComponentsConfig.getActiveComponents() ) ; // contains the "standard components" (*)
		list.add( "abc" ) ;
		list.add( "abc" ) ;
		qsimComponentsConfig.setActiveComponents( list );

		log.warn( "" );
		log.warn( "qsimComponentsConfig=" + qsimComponentsConfig + "; active components:");
		for( String component : qsimComponentsConfig.getActiveComponents() ){
			log.warn( component );
		}
		log.warn( "" );

		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// add event handler to catch event by MockMobsimListener:
		AnalysisEventsHandler handler = new AnalysisEventsHandler() ;
		eventsManager.addHandler( handler );

		// register the "abc" component to the QSim:
		AbstractQSimModule module = new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addQSimComponentBinding( "abc" ).to(  MockMobsimListener.class ) ;
			}
		};

		new QSimBuilder(config) //
								.useDefaultComponents() // uses components from config
								.useDefaultQSimModules() // registers the default modules (needed here because of (*))
								.addQSimModule(module) // registers the additional modules
								.build(scenario, eventsManager) //
								.run();

		Assert.assertTrue( "MockMobsimListener was not added to QSim", handler.hasBeenCalled() ) ;
	}


	@Test
	public void testGenericAddComponentMethod() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		AnalysisEventsHandler handler = new AnalysisEventsHandler() ;
		eventsManager.addHandler( handler );

		final QSimComponentsConfigurator configurator = new QSimComponentsConfigurator(){
			@Override public void configure( QSimComponentsConfig components ){
				components.addComponent( MockComponentAnnotation.class );
			}
		};

		AbstractQSimModule module = new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addQSimComponentBinding(MockComponentAnnotation.class).to(MockMobsimListener.class);
			}
		};

		new QSimBuilder(config) //
								.addQSimModule(module) //
								.configureQSimComponents( configurator ) //
								.build(scenario, eventsManager) //
								.run();

		Assert.assertTrue( handler.hasBeenCalled() ) ;
	}

	@Test
	public void testGenericAddComponentMethodWithoutConfiguringIt() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		AnalysisEventsHandler handler = new AnalysisEventsHandler() ;
		eventsManager.addHandler( handler );

		AbstractQSimModule module = new AbstractQSimModule() {
			@Override protected void configureQSim() {
				addQSimComponentBinding(MockComponentAnnotation.class).to(MockMobsimListener.class);
			}
		};

		new QSimBuilder(config) //
								.addQSimModule(module) //
								.build(scenario, eventsManager) //
								.run();

		Assert.assertFalse( handler.hasBeenCalled() ) ;
	}

	@Test
	public void testMultipleBindings() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		MockEngineA mockEngineA = new MockEngineA();
		MockEngineB mockEngineB = new MockEngineB();

		new QSimBuilder(config) //
				.addQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						addQSimComponentBinding(MockComponentAnnotation.class).toInstance(mockEngineA);
						addQSimComponentBinding(MockComponentAnnotation.class).toInstance(mockEngineB);
					}
				}) //
				.configureQSimComponents( components -> {
					components.addComponent(MockComponentAnnotation.class);
				} ) //
				.build(scenario, eventsManager) //
				.run();

		Assert.assertTrue(mockEngineA.isCalled);
		Assert.assertTrue(mockEngineB.isCalled);
	}

	@Test
	public void testExplicitAnnotationConfiguration() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		MockEngine mockEngine = new MockEngine();

		new QSimBuilder(config) //
				.addQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						addQSimComponentBinding(MockComponentAnnotation.class).toInstance(mockEngine);
					}
				}) //
				.configureQSimComponents( components -> {
					components.addComponent(MockComponentAnnotation.class);
				} ) //
				.build(scenario, eventsManager) //
				.run();

		Assert.assertTrue(mockEngine.isCalled);
	}

	@Test
	public void testManualConfiguration() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		MockEngine mockEngine = new MockEngine();

		new QSimBuilder(config) //
				.addQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						addQSimComponentBinding("MockEngine").toInstance(mockEngine);
					}
				}) //
				.configureQSimComponents( components -> {
					components.addNamedComponent("MockEngine");
				} ) //
				.build(scenario, eventsManager) //
				.run();

		Assert.assertTrue(mockEngine.isCalled);
	}

	@Test
	public void testUseConfigGroup() {
		Config config = ConfigUtils.createConfig();

		QSimComponentsConfigGroup componentsConfig = new QSimComponentsConfigGroup();
		config.addModule(componentsConfig);

		componentsConfig.setActiveComponents(Collections.singletonList("MockEngine"));

		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		MockEngine mockEngine = new MockEngine();

		new QSimBuilder(config) //
				.useDefaults() //
				.addQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						addQSimComponentBinding("MockEngine").toInstance(mockEngine);
					}
				}) //
				.build(scenario, eventsManager) //
				.run();

		Assert.assertTrue(mockEngine.isCalled);
	}

	// ---

	private static class MockEngine implements MobsimEngine {
		boolean isCalled = false;

		@Override
		public void doSimStep(double time) {
			isCalled = true;
		}

		@Override
		public void onPrepareSim() {

		}

		@Override
		public void afterSim() {

		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {

		}
	}

	private static class MockEngineA extends MockEngine implements MobsimEngine {

	}

	private static class MockEngineB extends MockEngine implements MobsimEngine {

	}

	private static class MockEvent extends Event {
		MockEvent( double time ){
			super( time );
		}
		@Override public String getEventType(){
			throw new RuntimeException( "not implemented" );
		}
	}

	private static class MockMobsimListener implements MobsimInitializedListener{
		@Inject EventsManager eventsManager ;
		@Override public void notifyMobsimInitialized( MobsimInitializedEvent e ) {
			eventsManager.processEvent( new MockEvent( Double.NEGATIVE_INFINITY ) );
		}
	}

	private static class AnalysisEventsHandler implements BasicEventHandler {
		private boolean hasBeenCalled = false ;
		@Override public void handleEvent( Event event ){
			hasBeenCalled = true ;
		}
		@Override public void reset( int iteration ){
		}
		boolean hasBeenCalled(){
			return hasBeenCalled;
		}
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD, TYPE })
	@Retention(RUNTIME)
	static @interface MockComponentAnnotation {
	}
}
