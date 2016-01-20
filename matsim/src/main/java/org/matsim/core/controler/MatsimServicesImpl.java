package org.matsim.core.controler;


import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

import javax.inject.Inject;
import java.util.Map;

class MatsimServicesImpl implements MatsimServices {

	@Override
	public IterationStopWatch getStopwatch() {
		return injector.getInstance(IterationStopWatch.class);
	}

	@Override
	public final TravelTime getLinkTravelTimes() {
		return this.injector.getInstance(com.google.inject.Injector.class).getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() {})).get(TransportMode.car);
	}

	@Override
	public final Provider<TripRouter> getTripRouterProvider() {
		return this.injector.getProvider(TripRouter.class);
	}

	@Override
	public final TravelDisutility createTravelDisutilityCalculator() {
		return getTravelDisutilityFactory().createTravelDisutility(this.injector.getInstance(TravelTime.class), getConfig().planCalcScore());
	}

	@Override
	public final LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return this.injector.getInstance(LeastCostPathCalculatorFactory.class);
	}

	@Override
	public final ScoringFunctionFactory getScoringFunctionFactory() {
		return this.injector.getInstance(ScoringFunctionFactory.class);
	}

	@Override
	public Config getConfig() {
		return this.injector.getInstance(Config.class);
	}

	@Override
	public Scenario getScenario() {
		return this.injector.getInstance(Scenario.class);
	}

	@Override
	public EventsManager getEvents() {
		return this.injector.getInstance(EventsManager.class);
	}

	@Inject Injector injector;
	@Override
	public Injector getInjector() {
		return injector;
	}

	@Override
	public final CalcLinkStats getLinkStats() {
		return this.injector.getInstance(CalcLinkStats.class);
	}

	@Override
	public final VolumesAnalyzer getVolumes() {
		return this.injector.getInstance(VolumesAnalyzer.class);
	}

	@Override
	public final ScoreStats getScoreStats() {
		return this.injector.getInstance(ScoreStats.class);
	}

	@Override
	public final TravelDisutilityFactory getTravelDisutilityFactory() {
		return this.injector.getInstance(com.google.inject.Injector.class).getInstance(Key.get(new TypeLiteral<Map<String, TravelDisutilityFactory>>(){}))
				.get(TransportMode.car);
	}

	@Override
	public final StrategyManager getStrategyManager() {
		return this.injector.getInstance(StrategyManager.class);
	}

	@Override
	public OutputDirectoryHierarchy getControlerIO() {
		return injector.getInstance(OutputDirectoryHierarchy.class);
	}

	@Override
	public void addControlerListener(ControlerListener controlerListener) {
		((NewControler) injector.getInstance(ControlerI.class)).addControlerListener(controlerListener);
	}

	@Override
	public Integer getIterationNumber() {
		return injector.getInstance(ReplanningContext.class).getIteration();
	}
}
