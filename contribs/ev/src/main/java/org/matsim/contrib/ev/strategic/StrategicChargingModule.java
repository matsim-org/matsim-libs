package org.matsim.contrib.ev.strategic;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.reservation.ChargerReservability;
import org.matsim.contrib.ev.reservation.ChargerReservationModule;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup.AlternativeSearchStrategy;
import org.matsim.contrib.ev.strategic.access.AnyChargerAccess;
import org.matsim.contrib.ev.strategic.access.AttributeBasedChargerAccess;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.access.SubscriptionRegistry;
import org.matsim.contrib.ev.strategic.analysis.ChargerTypeAnalysisListener;
import org.matsim.contrib.ev.strategic.analysis.ChargingPlanScoringListener;
import org.matsim.contrib.ev.strategic.costs.ChargingCostCalculator;
import org.matsim.contrib.ev.strategic.costs.ChargingCostModule;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.DefaultChargerProvidersModule;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.plan.ChargingPlansConverter;
import org.matsim.contrib.ev.strategic.replanning.StrategicChargingReplanningAlgorithm;
import org.matsim.contrib.ev.strategic.replanning.StrategicChargingReplanningStrategy;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingInnovationParameters;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.replanning.innovator.ConstrainedChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.replanning.innovator.EmptyChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.replanning.innovator.RandomChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.replanning.innovator.chargers.ChargerSelector;
import org.matsim.contrib.ev.strategic.replanning.innovator.chargers.EnergyHelper;
import org.matsim.contrib.ev.strategic.replanning.innovator.chargers.MinimalCostChargerSelector;
import org.matsim.contrib.ev.strategic.replanning.innovator.chargers.RandomChargerSelector;
import org.matsim.contrib.ev.strategic.replanning.selector.BestChargingPlanSelector;
import org.matsim.contrib.ev.strategic.replanning.selector.ChargingPlanSelector;
import org.matsim.contrib.ev.strategic.replanning.selector.ExponentialChargingPlanSelector;
import org.matsim.contrib.ev.strategic.replanning.selector.RandomChargingPlanSelector;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.contrib.ev.strategic.scoring.ScoringTracker;
import org.matsim.contrib.ev.strategic.scoring.StrategicChargingScoringFunction;
import org.matsim.contrib.ev.withinday.ChargingSlotFinder;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.contrib.ev.withinday.analysis.WithinDayChargingAnalysisHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * Main entry-point for startegic electric vehicle charging.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingModule extends AbstractModule {
	static public final String MODE_BINDING = "ev:strategic";

	static public final String ACTIVE_INNOVATOR = "active_innovator";

	@Override
	public void install() {
		WithinDayEvConfigGroup withinDayConfig = WithinDayEvConfigGroup.get(getConfig());

		install(new DefaultChargerProvidersModule());
		installOverridingQSimModule(new StrategicChargingQSimModule());

		addPlanStrategyBinding(StrategicChargingReplanningStrategy.STRATEGY)
				.toProvider(StrategicChargingReplanningStrategy.class);

		addControllerListenerBinding().to(ChargerTypeAnalysisListener.class);
		addEventHandlerBinding().to(ChargerTypeAnalysisListener.class);

		bind(Key.get(TravelTime.class, Names.named(MODE_BINDING)))
				.to(Key.get(TravelTime.class, Names.named(withinDayConfig.getCarMode())));

		StrategicChargingConfigGroup chargingConfig = StrategicChargingConfigGroup.get(getConfig());

		switch (chargingConfig.getSelectionStrategy()) {
			case Best:
				bind(ChargingPlanSelector.class).to(BestChargingPlanSelector.class);
				break;
			case Random:
				bind(ChargingPlanSelector.class).to(RandomChargingPlanSelector.class);
				break;
			case Exponential:
				bind(ChargingPlanSelector.class).to(ExponentialChargingPlanSelector.class);
				break;
			default:
				throw new IllegalStateException();
		}

		ChargingInnovationParameters innovation = chargingConfig.getInnovationParameters();

		if (innovation instanceof RandomChargingPlanInnovator.Parameters) {
			bind(ChargingPlanInnovator.class).to(RandomChargingPlanInnovator.class);
		} else if (innovation == null) {
			bind(ChargingPlanInnovator.class).to(EmptyChargingPlanInnovator.class);
		} else {
			throw new IllegalStateException("Unknown innovation parameters: " + innovation.getClass());
		}

		if (innovation.getConstraintIterations() > 0) {
			bind(Key.get(ChargingPlanInnovator.class, Names.named(ACTIVE_INNOVATOR)))
					.to(ConstrainedChargingPlanInnovator.class);
		} else {
			bind(Key.get(ChargingPlanInnovator.class, Names.named(ACTIVE_INNOVATOR)))
					.to(ChargingPlanInnovator.class);
		}

		addControllerListenerBinding().to(ChargingPlanScoring.class);
		addControllerListenerBinding().to(ChargingPlanScoringListener.class);

		if (chargingConfig.getChargerSelector().equals(RandomChargerSelector.NAME)) {
			bind(ChargerSelector.Factory.class).to(RandomChargerSelector.Factory.class);
		} else if (chargingConfig.getChargerSelector().equals(MinimalCostChargerSelector.NAME)) {
			bind(ChargerSelector.Factory.class).to(MinimalCostChargerSelector.Factory.class);
		}

		if (chargingConfig.getChargingScoreWeight() != 0.0) {
			bind(ScoringFunctionFactory.class).to(StrategicChargingScoringFunction.Factory.class).in(Singleton.class);
		}

		install(new ChargingCostModule());

		addAttributeConverterBinding(ChargingPlans.class).to(ChargingPlansConverter.class);

		bind(ChargerAccess.class).to(AttributeBasedChargerAccess.class);

		install(new ChargerReservationModule(
				chargingConfig.getOnlineSearchStrategy().equals(AlternativeSearchStrategy.ReservationBased)));
	}

	@Provides
	@Singleton
	ChargingPlansConverter provideChargingPlansConverter() {
		return new ChargingPlansConverter();
	}

	@Provides
	@Singleton
	ChargingPlanScoring provideChargingPlanScoring(EventsManager eventsManager, Population population, Network network,
			TimeInterpretation timeInterpretation, ElectricFleetSpecification fleet,
			ChargingInfrastructureSpecification infrastructure,
			ChargingCostCalculator costCalculator,
			StrategicChargingConfigGroup scConfig, WithinDayEvConfigGroup withinConfig, ScoringTracker tracker) {
		return new ChargingPlanScoring(eventsManager, population, network, timeInterpretation, fleet, infrastructure,
				costCalculator,
				scConfig.getScoringParameters(),
				withinConfig.getCarMode(), tracker);
	}

	@Provides
	@Singleton
	ScoringTracker provideScoringTracker(OutputDirectoryHierarchy outputHierarchy,
			StrategicChargingConfigGroup config) {
		return new ScoringTracker(outputHierarchy, config.getScoreTrackingInterval());
	}

	@Provides
	StrategicChargingReplanningStrategy provideChargingPlanStrategy(
			Provider<StrategicChargingReplanningAlgorithm> algorithmProvider) {
		return new StrategicChargingReplanningStrategy(getConfig().global(), algorithmProvider);
	}

	@Provides
	StrategicChargingReplanningAlgorithm provideStrategicReplanningAlgorithm(ChargingPlanSelector selector,
			@Named(ACTIVE_INNOVATOR) ChargingPlanInnovator creator, StrategicChargingConfigGroup config) {
		return new StrategicChargingReplanningAlgorithm(selector, creator, config.getSelectionProbability(),
				config.getMaximumChargingPlans());
	}

	@Provides
	BestChargingPlanSelector provideBestChargingPlanSelector() {
		return new BestChargingPlanSelector();
	}

	@Provides
	RandomChargingPlanSelector provideRandomChargingPlanSelector() {
		return new RandomChargingPlanSelector();
	}

	@Provides
	EmptyChargingPlanInnovator provideEmptyChargingPlanCreator() {
		return new EmptyChargingPlanInnovator();
	}

	@Provides
	ExponentialChargingPlanSelector provideExponentialPlanSelector(StrategicChargingConfigGroup config) {
		return new ExponentialChargingPlanSelector(config.getExponentialSelectionBeta());
	}

	@Provides
	RandomChargingPlanInnovator provideRandomChargingPlanCreator(ChargerProvider chargerProvider,
			Scenario scenario, StrategicChargingConfigGroup config, WithinDayEvConfigGroup withinConfig,
			TimeInterpretation timeInterpretation, ChargerSelector.Factory selectorFactory,
			ChargerReservability chargerReservability) {
		ChargingSlotFinder candidateFinder = new ChargingSlotFinder(scenario, withinConfig.getCarMode());
		return new RandomChargingPlanInnovator(chargerProvider, candidateFinder, timeInterpretation, config,
				(RandomChargingPlanInnovator.Parameters) config.getInnovationParameters(), selectorFactory,
				chargerReservability);
	}

	@Provides
	ConstrainedChargingPlanInnovator provideConstrainedChargingPlanInnovator(
			ChargingPlanInnovator delegate,
			TimeInterpretation timeInterpretation,
			@Named(MODE_BINDING) TravelTime travelTime, Network network, Vehicles vehicles,
			ElectricFleetSpecification electricFleet,
			ChargingInfrastructureSpecification infrastructure, StrategicChargingConfigGroup config,
			WithinDayEvConfigGroup wevcConfig, ChargingPower.Factory chargingPowerFactory,
			DriveEnergyConsumption.Factory driveEnergyConsumptionFactory) {
		ChargingInnovationParameters parameters = config
				.getInnovationParameters();
		return new ConstrainedChargingPlanInnovator(delegate, timeInterpretation, travelTime, network, vehicles,
				electricFleet, infrastructure, wevcConfig.getCarMode(), parameters.getConstraintIterations(),
				parameters.getConstraintErrorMode(), chargingPowerFactory, driveEnergyConsumptionFactory);
	}

	@Provides
	@Singleton
	ChargerTypeAnalysisListener provideChargerTypeAnalysisListener(OutputDirectoryHierarchy outputHierarchy,
			ChargingInfrastructureSpecification infrastructure, EventsManager eventsManager,
			WithinDayChargingAnalysisHandler analysisHandler) {
		return new ChargerTypeAnalysisListener(outputHierarchy, infrastructure, analysisHandler, eventsManager);
	}

	@Provides
	@Singleton
	ChargingPlanScoringListener provideChargingPlanScoringListener(Population population,
			OutputDirectoryHierarchy outputHierarchy) {
		return new ChargingPlanScoringListener(population, outputHierarchy);
	}

	@Provides
	@Singleton
	StrategicChargingScoringFunction.Factory provideStrategicChargingScoringFunctiony(Scenario scenario,
			WithinDayEvConfigGroup withinDayConfig, StrategicChargingConfigGroup chargingConfig,
			ChargingPlanScoring chargingScoring) {
		CharyparNagelScoringFunctionFactory delegate = new CharyparNagelScoringFunctionFactory(scenario);
		return new StrategicChargingScoringFunction.Factory(delegate, chargingScoring,
				chargingConfig.getChargingScoreWeight());
	}

	@Provides
	@Singleton
	AnyChargerAccess provideAnyChargerAccess() {
		return new AnyChargerAccess();
	}

	@Provides
	@Singleton
	AttributeBasedChargerAccess provideAttributeBasedChargerAccess(SubscriptionRegistry subscriptionRegistry) {
		return new AttributeBasedChargerAccess(subscriptionRegistry);
	}

	@Provides
	@Singleton
	SubscriptionRegistry provideSubscriptionRegistry() {
		return new SubscriptionRegistry();
	}

	@Provides
	@Singleton
	RandomChargerSelector.Factory provideRandomChargerSelectorFactory() {
		return new RandomChargerSelector.Factory();
	}

	@Provides
	@Singleton
	MinimalCostChargerSelector.Factory provideMinimalCostChargerSelectorFactory(ChargingCostCalculator costCalculator,
			WithinDayEvConfigGroup config, TimeInterpretation timeInterpretation,
			@Named(MODE_BINDING) TravelTime travelTime, Network network,
			DriveEnergyConsumption.Factory driveFactory,
			ChargingPower.Factory chargingFactory,
			Vehicles vehicles, ElectricFleetSpecification fleet) {
		EnergyHelper.Factory energyFactory = new EnergyHelper.Factory(timeInterpretation, travelTime, network,
				driveFactory, chargingFactory, vehicles, fleet, config.getCarMode());
		return new MinimalCostChargerSelector.Factory(costCalculator, energyFactory);
	}
}
