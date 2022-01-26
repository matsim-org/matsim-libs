package org.matsim.contrib.socnetsim.jointtrips.qsim;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import org.matsim.contrib.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class JointQSimModule extends AbstractQSimModule {
	static final public String JOINT_MODES_DEPARTURE_HANDLER = "JointModesDepartureHandler";
	static final public String JOINT_PASSENGER_UNBOARDING = "JoinPassengerUnboarding";
	static final public String AGENTS_SOURCE_WITH_VEHICLES = "AgentSourceWithVehicles";
	static final public String REPLACEMENT_QNETSIM_ENGINE = "ReplacementQNetsimEngine";

	@Override
	protected void configureQSim() {
		addNamedComponent(JointModesDepartureHandler.class, JOINT_MODES_DEPARTURE_HANDLER);
		addNamedComponent(PassengerUnboardingAgentFactory.class, JOINT_PASSENGER_UNBOARDING);
		addNamedComponent(JointModesDepartureHandler.class, JOINT_MODES_DEPARTURE_HANDLER);
		addNamedComponent(PopulationAgentSourceWithVehicles.class, AGENTS_SOURCE_WITH_VEHICLES);
		addNamedComponent(QNetsimEngineI.class, REPLACEMENT_QNETSIM_ENGINE);
	}

	@Provides
	@Singleton
	JointModesDepartureHandler provideJoinModesDepartureHandler(QNetsimEngineI netsimEngine) {
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
			QNetsimEngineI netsimEngine, TimeInterpretation timeInterpretation) {
		return new PassengerUnboardingAgentFactory(
				config.transit().isUseTransit() ? new TransitAgentFactory(qsim, timeInterpretation) : new DefaultAgentFactory(qsim, timeInterpretation),
				new NetsimWrappingQVehicleProvider(netsimEngine));
	}
	
	static public void configureComponents(QSimComponentsConfig components) {
		components.removeNamedComponent(QNetsimEngineModule.COMPONENT_NAME);
		components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
		
		components.addNamedComponent(REPLACEMENT_QNETSIM_ENGINE);		
		components.addNamedComponent(JOINT_MODES_DEPARTURE_HANDLER);
		components.addNamedComponent(JOINT_PASSENGER_UNBOARDING);
		components.addNamedComponent(AGENTS_SOURCE_WITH_VEHICLES);
	}
}
