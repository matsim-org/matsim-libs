package org.matsim.guice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ExperiencedPlansModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;


class MyModule extends AbstractModule {
	@Override
    public void install() {
        install(new NewControlerModule());
        this.install(new EventsManagerModule());
        this.install(new DefaultMobsimModule());
        this.install(new TravelTimeCalculatorModule());
        this.install(new TravelDisutilityModule());
        this.install(new CharyparNagelScoringFunctionModule());
        this.install(new TripRouterModule());
        this.install(new StrategyManagerModule());
        this.install(new ExperiencedPlansModule());
//        this.install(new LinkStatsModule());
//        this.install(new VolumesAnalyzerModule());
//        this.install(new LegHistogramModule());
//        this.install(new LegTimesModule());
//        this.install(new TravelDistanceStatsModule());
//        this.install(new ScoreStatsModule());
//        this.install(new SnapshotWritersModule());
        install(new ControlerDefaultCoreListenersModule());
        Scenario scenario = ScenarioUtils.createScenario(getConfig());
        install(new ExplodedScenarioModule(scenario));
    }
}
