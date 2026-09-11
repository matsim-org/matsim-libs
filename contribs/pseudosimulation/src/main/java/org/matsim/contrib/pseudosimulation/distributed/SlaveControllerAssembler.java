package org.matsim.contrib.pseudosimulation.distributed;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.diversitygeneration.planselectors.DiversityGeneratingPlansRemover;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControllerListener;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

final class SlaveControllerAssembler {
    private static final String DIVERSITY_PLAN_REMOVER = "DiversityGeneratingPlansRemover";

    private final SlaveConfigPreparer configPreparer;
    private final ReplanningWeightUpdater weightUpdater;
    private final Environment environment;

    private SlaveControllerAssembler(SlaveConfigPreparer configPreparer, ReplanningWeightUpdater weightUpdater,
            Environment environment) {
        this.configPreparer = configPreparer;
        this.weightUpdater = weightUpdater;
        this.environment = environment;
    }

    static SlaveControllerAssembler production(SlaveConfigPreparer configPreparer) {
        return new SlaveControllerAssembler(configPreparer, new ReplanningWeightUpdater(), new ProductionEnvironment());
    }

    static SlaveControllerAssembler testing(SlaveConfigPreparer configPreparer,
            ReplanningWeightUpdater weightUpdater, Environment environment) {
        return new SlaveControllerAssembler(configPreparer, weightUpdater, environment);
    }

    Result assemble(Config config, SlaveRuntimeSettings settings, ControllerListener listener,
            PlanCatcher planCatcher, PSimProvider pSimProvider) {
        config.controller().setLastIteration(settings.lastIteration());
        configPreparer.prepareForScenario(config, settings.number());
        if (settings.mutationRate() > 0) {
            weightUpdater.updateSlave(config, settings.mutationRate());
        }

        Scenario scenario = environment.loadScenario(config);
        environment.setTrackGenome(settings.trackGenome());
        environment.substituteStrategies(config, settings.quickReplanning(), settings.iterationsPerCycle());
        Controler controller = environment.createController(scenario);
        environment.registerStrategies(controller, planCatcher, settings.quickReplanning(),
                settings.iterationsPerCycle());
        controller.getConfig().controller().setOverwriteFileSetting(
                OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        environment.addListener(controller, listener);

        ReplaceableTravelTime travelTime = new ReplaceableTravelTime();
        TravelTime initialTravelTime = new FreeSpeedTravelTime();
        travelTime.setTravelTime(initialTravelTime);
        environment.installMobsim(controller, pSimProvider);

        if (config.transit().isUseTransit()) {
            environment.enableTransitSerialization();
        }
        if (settings.intelligentRouters()) {
            environment.installIntelligentRouters(controller);
        } else {
            environment.installRandomizedCarRouter(controller, config);
        }
        environment.installTravelTime(controller, scenario, travelTime);

        if (settings.diversityGeneratingPlanSelection()) {
            controller.getConfig().replanning().setPlanSelectorForRemoval(DIVERSITY_PLAN_REMOVER);
        }
        configPreparer.prepareController(controller.getConfig(), settings.numberOfPlans());
        return new Result(scenario, controller, travelTime, initialTravelTime);
    }

    record Result(Scenario scenario, Controler controller, ReplaceableTravelTime travelTime,
            TravelTime initialTravelTime) { }

    interface Environment {
        Scenario loadScenario(Config config);
        void setTrackGenome(boolean trackGenome);
        void substituteStrategies(Config config, boolean quickReplanning, int iterationsPerCycle);
        Controler createController(Scenario scenario);
        void registerStrategies(Controler controller, PlanCatcher planCatcher, boolean quickReplanning,
                int iterationsPerCycle);
        void addListener(Controler controller, ControllerListener listener);
        void installMobsim(Controler controller, PSimProvider pSimProvider);
        void enableTransitSerialization();
        void installIntelligentRouters(Controler controller);
        void installRandomizedCarRouter(Controler controller, Config config);
        void installTravelTime(Controler controller, Scenario scenario, ReplaceableTravelTime travelTime);
    }

    private static final class ProductionEnvironment implements Environment {
        public Scenario loadScenario(Config config) {
            return ScenarioUtils.loadScenario(config);
        }

        public void setTrackGenome(boolean trackGenome) {
            DistributedPlanStrategyTranslationAndRegistration.TrackGenome = trackGenome;
        }

        public void substituteStrategies(Config config, boolean quickReplanning, int iterationsPerCycle) {
            DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(
                    config, quickReplanning, iterationsPerCycle);
        }

        public Controler createController(Scenario scenario) {
            return new Controler(scenario);
        }

        public void registerStrategies(Controler controller, PlanCatcher planCatcher, boolean quickReplanning,
                int iterationsPerCycle) {
            DistributedPlanStrategyTranslationAndRegistration.registerStrategiesWithControler(
                    controller, planCatcher, quickReplanning, iterationsPerCycle);
        }

        public void addListener(Controler controller, ControllerListener listener) {
            controller.addControllerListener(listener);
        }

        public void installMobsim(Controler controller, PSimProvider pSimProvider) {
            controller.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bindMobsim().toProvider(pSimProvider);
                }
            });
        }

        public void enableTransitSerialization() {
            PlanSerializable.isUseTransit = true;
        }

        public void installIntelligentRouters(Controler controller) {
            controller.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    // The legacy intelligent-router branch intentionally installs an empty module.
                }
            });
        }

        public void installRandomizedCarRouter(Controler controller, Config config) {
            RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory =
                    new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config);
            controller.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(disutilityFactory);
                }
            });
        }

        public void installTravelTime(Controler controller, Scenario scenario, ReplaceableTravelTime travelTime) {
            controller.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(TravelTime.class).toInstance(travelTime);
                    if (scenario.getConfig().replanning().getPlanSelectorForRemoval()
                            .equals(DIVERSITY_PLAN_REMOVER)) {
                        bindPlanSelectorForRemoval().toProvider(DiversityGeneratingPlansRemover.Builder.class);
                    }
                }
            });
        }
    }
}
