package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizerProvider;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public final class DrtModule extends AbstractModule {

	@Override public void install() {
		bind( Fleet.class ).toProvider( DefaultDrtFleetProvider.class ).asEagerSingleton() ;
		install( new DvrpModule(DrtControlerCreator.createModuleForQSimPlugin(DefaultDrtOptimizerProvider.class), DrtOptimizer.class) ) ;
	}
	
	
	public static final class DefaultDrtFleetProvider implements Provider<Fleet> {
		@Inject @Named(DvrpModule.DVRP_ROUTING) Network network;
		@Inject Config config;
		@Inject DrtConfigGroup drtCfg ;
		@Override public Fleet get() {
			FleetImpl fleet = new FleetImpl();
			new VehicleReader(network, fleet).parse(drtCfg.getVehiclesFileUrl(config.getContext()));
			return fleet;
		}
	}


}