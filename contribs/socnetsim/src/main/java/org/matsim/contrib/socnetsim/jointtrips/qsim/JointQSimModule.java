package org.matsim.contrib.socnetsim.jointtrips.qsim;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import org.matsim.contrib.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JointQSimModule extends AbstractQSimModule {
	static public String JOINT_MODES_DEPARTURE_HANDLER = "JointModesDepartureHandler";
	static public String JOINT_PASSENGER_UNBOARDING = "JoinPassengerUnboarding";
	static public String AGENTS_SOURCE_WITH_VEHICLES = "AgentSourceWithVehicles";

	@Override
	protected void configureQSim() {
		bindMobsimEngine(JOINT_MODES_DEPARTURE_HANDLER).to(JointModesDepartureHandler.class);
		bindMobsimEngine(JOINT_PASSENGER_UNBOARDING).to(PassengerUnboardingAgentFactory.class);

		bindDepartureHandler(JOINT_MODES_DEPARTURE_HANDLER).to(JointModesDepartureHandler.class);
		bindAgentSource(AGENTS_SOURCE_WITH_VEHICLES).to(PopulationAgentSourceWithVehicles.class);
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
}
