package org.matsim.core.mobsim.qsim;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.ArrayList;
import java.util.Collection;

public class TeleportationPlugin extends AbstractQSimPlugin {
//	private boolean withDistanceCheck = true ;

	public TeleportationPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		Collection<Module> result = new ArrayList<>();
		result.add(new AbstractModule() {
			@Override
			protected void configure() {
//				if ( withDistanceCheck ) {
//					bind(TeleportationEngineWDistanceCheck.class).asEagerSingleton();
//				} else {
					bind(TeleportationEngine.class).asEagerSingleton();
				}
//			}
		});
		return result;
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		Collection<Class<? extends MobsimEngine>> result = new ArrayList<>();
//		if ( withDistanceCheck ) {
//			result.add(TeleportationEngineWDistanceCheck.class);
//		} else {
			result.add(TeleportationEngine.class);
//		}
		return result;
	}
}
