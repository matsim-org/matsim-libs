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
package playground.kai.usecases.parkingSearch;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Module;
import com.google.inject.Provides;

/**
 * @author nagel
 *
 */
final class KNParkingSearch {
	private static class ParkingSearchQSimModule extends com.google.inject.AbstractModule {
		@Override 
		protected void configure() {
			bind(Mobsim.class).toProvider(QSimProvider.class);
		}
		@SuppressWarnings("static-method")
		@Provides 
		Collection<AbstractQSimPlugin> provideQSimPlugins(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
			final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
			plugins.add(new MessageQueuePlugin(config));
			plugins.add(new ActivityEnginePlugin(config));
			plugins.add(new QNetsimEnginePlugin(config));
			if (networkConfigGroup.isTimeVariantNetwork()) {
				plugins.add(new NetworkChangeEventsPlugin(config));
			}
			if (transitConfigGroup.isUseTransit()) {
				plugins.add(new TransitEnginePlugin(config));
			}
			plugins.add(new TeleportationPlugin(config));
			plugins.add(new ParkingSearchPopulationPlugin(config));
			return plugins;
		}
	}
	private static class ParkingSearchPopulationPlugin extends AbstractQSimPlugin {
		public ParkingSearchPopulationPlugin(Config config) { super(config); }
		@Override 
		public Collection<? extends Module> modules() {
			Collection<Module> result = new ArrayList<>();
			result.add(new com.google.inject.AbstractModule() {
				@Override
				protected void configure() {
					bind(PopulationAgentSource.class).asEagerSingleton();
					if (getConfig().transit().isUseTransit()) {
						throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
					} else {
						bind(AgentFactory.class).to(ParkingSearchAgentFactory.class).asEagerSingleton(); // (**)
					}
				}
			});
			return result;
		}
		@Override 
		public Collection<Class<? extends AgentSource>> agentSources() {
			Collection<Class<? extends AgentSource>> result = new ArrayList<>();
			result.add(PopulationAgentSource.class);
			return result;
		}
	}
	private static class ParkingSearchAgentFactory implements AgentFactory {
		@Inject Netsim netsim ;
		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			return new MyParkingSearchAgent( p.getSelectedPlan(), netsim ) ;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml") ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.qsim().setSnapshotStyle( SnapshotStyle.queue);

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule(new OTFVisLiveModule() ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.install( new ParkingSearchQSimModule() ) ;
			}
		});

		controler.run();
	}

}
