package org.matsim.contrib.socnetsim.jointtrips.qsim;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import org.matsim.contrib.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JointQSimPlugin extends AbstractQSimPlugin {
	static public String JOINT_MODES_DEPARTURE_HANDLER = "JointModesDepartureHandler";
	static public String JOINT_PASSENGER_UNBOARDING = "JoinPassengerUnboarding";
	static public String AGENTS_SOURCE_WITH_VEHICLES = "AgentSourceWithVehicles";

	public JointQSimPlugin(Config config) {
		super(config);
	}

	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {

			}

			@Provides
			@Singleton
			JointModesDepartureHandler provideJoinModesDepartureHandler(QNetsimEngine netsimEngine) {
				return new JointModesDepartureHandler(netsimEngine);
			}

			@Provides
			@Singleton
			PopulationAgentSourceWithVehicles providePopulationAgentSourceWithVehicles(Population population,
					PassengerUnboardingAgentFactory passAgentFactory, QSim qsim) {
				return new PopulationAgentSourceWithVehicles(population, passAgentFactory, qsim);
			}

			@Provides
			@Singleton
			PassengerUnboardingAgentFactory providePassengerUnboardingAgentFactory(Config config, QSim qsim,
					QNetsimEngine netsimEngine) {
				return new PassengerUnboardingAgentFactory(
						config.transit().isUseTransit() ? new TransitAgentFactory(qsim) : new DefaultAgentFactory(qsim),
						new NetsimWrappingQVehicleProvider(netsimEngine));
			}

		});
	}

	public Map<String, Class<? extends MobsimEngine>> engines() {
		Map<String, Class<? extends MobsimEngine>> engines = new HashMap<>();
		engines.put(JOINT_MODES_DEPARTURE_HANDLER, JointModesDepartureHandler.class);
		engines.put(JOINT_PASSENGER_UNBOARDING, PassengerUnboardingAgentFactory.class);
		return engines;
	}

	public Map<String, Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonMap(JOINT_MODES_DEPARTURE_HANDLER, JointModesDepartureHandler.class);
	}

	public Map<String, Class<? extends AgentSource>> agentSources() {
		return Collections.singletonMap(AGENTS_SOURCE_WITH_VEHICLES, PopulationAgentSourceWithVehicles.class);
	}
}
