package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;

import java.util.Collection;
import java.util.Collections;

public class QNetsimEnginePlugin extends AbstractQSimPlugin {

	public QNetsimEnginePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			protected void configure() {
				bind(QNetsimEngine.class).asEagerSingleton();
				bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class).asEagerSingleton();
			}
			
			@Provides 
			NetsimNetwork provideNetsimNetwork(QNetsimEngine netsimEngine) {
				return netsimEngine.getNetsimNetwork();
			}
			
			@Provides
			InternalInterface provideInternalInterface(QSim qsim) {
				return qsim.getInternalInterface();
			}
		});
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonList(VehicularDepartureHandler.class);
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.singletonList(QNetsimEngine.class);
	}
}
