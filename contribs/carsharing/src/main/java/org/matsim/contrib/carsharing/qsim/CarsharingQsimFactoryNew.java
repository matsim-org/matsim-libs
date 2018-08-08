package org.matsim.contrib.carsharing.qsim;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSimBuilder;

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
		return new QSimBuilder(config) //
				.useDefaults() //
				.removeModule(PopulationModule.class) //
				.addModule(new CarSharingQSimModule(carsharingSupply, carsharingManager)) //
				.configureComponents(components -> {
					components.activeAgentSources.add(CarSharingQSimModule.CARSHARING_PARKING_VEHICLES_SOURCE);
				}) //
				.build(scenario, eventsManager);
	}

}
