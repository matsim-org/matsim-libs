package org.matsim.contrib.carsharing.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;

import com.google.inject.Provider;

/** 
 * 
 * @author balac
 */
public class CarsharingQsimFactoryNew implements Provider<Mobsim>{

	@Inject Config config;
	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;
	@Inject CarsharingSupplyInterface carsharingSupply;
	@Inject private CarsharingManagerInterface carsharingManager;

	@Override
	public Mobsim get() {
		/*final QSim qsim = new QSim(scenario, eventsManager);
		//QSimUtils.createDefaultQSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qsim.getAgentCounter());
		qsim.addMobsimEngine(activityEngine);
		qsim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new QNetsimEngine(qsim);
		qsim.addMobsimEngine(netsimEngine);
		qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager);
		qsim.addMobsimEngine(teleportationEngine);
		qsim.addDepartureHandler(teleportationEngine) ;
		AgentFactory agentFactory = new CSAgentFactory(qsim, carsharingManager);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qsim);
        qsim.addAgentSource(agentSource);		
	
		ParkCSVehicles parkSource = new ParkCSVehicles( qsim,
				carsharingSupply);
		qsim.addAgentSource(parkSource);*/
		
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (config.network().isTimeVariantNetwork()) {
		    plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
		    plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new CarSharingQSimPlugin(config, carsharingSupply, carsharingManager));
		
		List<AbstractModule> modules = Collections.singletonList(new AbstractModule() {
			@Override
			public void install() {
				QSimComponents components = new QSimComponents();
				new StandardQSimComponentsConfigurator(config).configure(components);
				components.activeAgentSources.add(CarSharingQSimPlugin.CARSHARING_PARKING_VEHICLES_SOURCE);
				bind(QSimComponents.class).toInstance(components);
			}
		});
		
		QSim qsim = QSimUtils.createQSim(scenario, eventsManager, modules, plugins);
		return qsim;
	}

}
