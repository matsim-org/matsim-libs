package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControllerListener;
import org.matsim.core.scenario.ScenarioUtils;

class SlaveControllerAssemblerTest {

    @Test
    void assemblesTransitDiversityAndIntelligentRouterConfigurationInLegacyOrder() {
        Config config = configWithStrategies();
        config.controller().setOutputDirectory("slave-output");
        config.plans().setInputFile("plans.xml");
        config.transit().setUseTransit(true);
        RecordingEnvironment environment = new RecordingEnvironment();
        SlaveControllerAssembler assembler = assembler(environment);
        SlaveRuntimeSettings settings = new SlaveRuntimeSettings(
                4, 3, 7, 0.25, 90, true, true, true, true, true, true);
        ControllerListener listener = new ControllerListener() { };
        PlanCatcher planCatcher = new PlanCatcher();

        SlaveControllerAssembler.Result result = assembler.assemble(
                config, settings, listener, planCatcher, null);

        assertEquals(List.of("loadScenario", "track:true", "substitute:true:3", "createController",
                "register:true:3", "listener", "mobsim:null", "transit", "intelligent", "travelTime"),
                environment.events);
        assertEquals(90, config.controller().getLastIteration());
        assertEquals("slave-output_4", config.controller().getOutputDirectory());
        assertNull(config.plans().getInputFile());
        assertEquals(7, config.replanning().getMaxAgentPlanMemorySize());
        assertFalse(config.controller().getDumpDataAtEnd());
        assertEquals("DiversityGeneratingPlansRemover", config.replanning().getPlanSelectorForRemoval());
        assertEquals(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles,
                config.controller().getOverwriteFileSetting());
        assertEquals(0.75, strategy(config, 0).getWeight(), 1e-12);
        assertEquals(0.25, strategy(config, 1).getWeight(), 1e-12);
        assertSame(environment.scenario, result.scenario());
        assertSame(environment.controller, result.controller());
        assertSame(environment.travelTime, result.travelTime());
        assertSame(listener, environment.listener);
        assertSame(planCatcher, environment.planCatcher);
    }

    @Test
    void selectsRandomizedRouterAndLeavesWeightsAndDiversityUntouchedWhenDisabled() {
        Config config = configWithStrategies();
        RecordingEnvironment environment = new RecordingEnvironment();
        SlaveRuntimeSettings settings = new SlaveRuntimeSettings(
                2, 5, 4, 0, 12, false, false, false, false, false, false);

        assembler(environment).assemble(config, settings, new ControllerListener() { }, new PlanCatcher(), null);

        assertEquals(List.of("loadScenario", "track:false", "substitute:false:5", "createController",
                "register:false:5", "listener", "mobsim:null", "randomized", "travelTime"),
                environment.events);
        assertEquals(1.0, strategy(config, 0).getWeight(), 1e-12);
        assertEquals(1.0, strategy(config, 1).getWeight(), 1e-12);
        assertEquals("WorstPlanSelector", config.replanning().getPlanSelectorForRemoval());
    }

    private SlaveControllerAssembler assembler(RecordingEnvironment environment) {
        return SlaveControllerAssembler.testing(
                new SlaveConfigPreparer(), new ReplanningWeightUpdater(), environment);
    }

    private Config configWithStrategies() {
        Config config = ConfigUtils.createConfig();
        config.replanning().clearStrategySettings();
        addStrategy(config, "ChangeExpBeta");
        addStrategy(config, "ReRoute");
        return config;
    }

    private void addStrategy(Config config, String name) {
        StrategySettings settings = new StrategySettings();
        settings.setStrategyName(name);
        settings.setWeight(1);
        config.replanning().addStrategySettings(settings);
    }

    private StrategySettings strategy(Config config, int index) {
        return new ArrayList<>(config.replanning().getStrategySettings()).get(index);
    }

    private static final class RecordingEnvironment implements SlaveControllerAssembler.Environment {
        private final List<String> events = new ArrayList<>();
        private Scenario scenario;
        private Controler controller;
        private ControllerListener listener;
        private PlanCatcher planCatcher;
        private ReplaceableTravelTime travelTime;

        public Scenario loadScenario(Config config) {
            events.add("loadScenario");
            scenario = ScenarioUtils.createScenario(config);
            return scenario;
        }

        public void setTrackGenome(boolean trackGenome) {
            events.add("track:" + trackGenome);
        }

        public void substituteStrategies(Config config, boolean quickReplanning, int iterationsPerCycle) {
            events.add("substitute:" + quickReplanning + ":" + iterationsPerCycle);
        }

        public Controler createController(Scenario scenario) {
            events.add("createController");
            controller = new Controler(scenario);
            return controller;
        }

        public void registerStrategies(Controler controller, PlanCatcher planCatcher, boolean quickReplanning,
                int iterationsPerCycle) {
            events.add("register:" + quickReplanning + ":" + iterationsPerCycle);
            this.planCatcher = planCatcher;
        }

        public void addListener(Controler controller, ControllerListener listener) {
            events.add("listener");
            this.listener = listener;
        }

        public void installMobsim(Controler controller, PSimProvider pSimProvider) {
            events.add("mobsim:" + pSimProvider);
        }

        public void enableTransitSerialization() {
            events.add("transit");
        }

        public void installIntelligentRouters(Controler controller) {
            events.add("intelligent");
        }

        public void installRandomizedCarRouter(Controler controller, Config config) {
            events.add("randomized");
        }

        public void installTravelTime(Controler controller, Scenario scenario, ReplaceableTravelTime travelTime) {
            events.add("travelTime");
            this.travelTime = travelTime;
        }
    }
}
