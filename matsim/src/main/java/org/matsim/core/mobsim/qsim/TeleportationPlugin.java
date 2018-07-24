package org.matsim.core.mobsim.qsim;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class TeleportationPlugin extends AbstractQSimPlugin {
	public final static String TELEPORATION_ENGINE_NAME = "TeleportationEngine";
	
	public TeleportationPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			protected void configure() {
				bind(DefaultTeleportationEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Map<String, Class<? extends MobsimEngine>> engines() {
		return Collections.singletonMap(TELEPORATION_ENGINE_NAME, DefaultTeleportationEngine.class );
	}
}
