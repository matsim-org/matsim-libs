package org.matsim.contrib.ev;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.List;

class EvMainModule extends AbstractModule {
	@Override public void install(){
		{
			QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( this.getConfig(), QSimComponentsConfigGroup.class );
			List<String> cmps = qsimComponentsConfig.getActiveComponents();
			cmps.add( EvModule.EV_COMPONENT );
			qsimComponentsConfig.setActiveComponents( cmps );
		}

		addRoutingModuleBinding( TransportMode.car ).toProvider(new EvNetworkRoutingProvider(TransportMode.car) );

		installQSimModule(new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						addMobsimScopeEventHandlerBinding().to(VehicleChargingHandler.class).asEagerSingleton();
					}
		});

	}
}
