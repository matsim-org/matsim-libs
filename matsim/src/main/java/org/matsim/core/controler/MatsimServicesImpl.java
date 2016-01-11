package org.matsim.core.controler;


import com.google.inject.Injector;
import com.google.inject.Provider;
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

	@Inject IterationStopWatch iterationStopWatch;
	@Override
	public IterationStopWatch getStopwatch() {
		return iterationStopWatch;
	}

	@Inject Map<String, TravelTime> travelTimes;
	@Override
	public TravelTime getLinkTravelTimes() {
		return travelTimes.get(TransportMode.car);
	}

	@Inject Provider<TripRouter> tripRouterProvider;
	@Override
	public Provider<TripRouter> getTripRouterProvider() {
		return tripRouterProvider;
	}

	@Inject Map<String, TravelDisutilityFactory> travelDisutilities;
	@Override
	public TravelDisutility createTravelDisutilityCalculator() {
		return getTravelDisutilityFactory().createTravelDisutility(this.injector.getInstance(TravelTime.class), getConfig().planCalcScore());
	}

	@Inject LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	@Override
	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return leastCostPathCalculatorFactory;
	}

	@Inject ScoringFunctionFactory scoringFunctionFactory;
	@Override
	public ScoringFunctionFactory getScoringFunctionFactory() {
		return scoringFunctionFactory;
	}

	@Inject Config config;
	@Override
	public Config getConfig() {
		return config;
	}

	@Inject Scenario scenario;
	@Override
	public Scenario getScenario() {
		return scenario;
	}

	@Inject EventsManager eventsManager;
	@Override
	public EventsManager getEvents() {
		return eventsManager;
	}

	@Inject Injector injector;
	@Override
	public Injector getInjector() {
		return injector;
	}

	@Override
	public CalcLinkStats getLinkStats() {
		return injector.getInstance(CalcLinkStats.class);
	}

	@Inject VolumesAnalyzer volumesAnalyzer;
	@Override
	public VolumesAnalyzer getVolumes() {
		return volumesAnalyzer;
	}

	@Override
	public ScoreStats getScoreStats() {
		return injector.getInstance(ScoreStats.class);
	}

	@Override
	public TravelDisutilityFactory getTravelDisutilityFactory() {
		return travelDisutilities.get(TransportMode.car);
	}

	@Inject
	StrategyManager strategyManager;
	@Override
	public StrategyManager getStrategyManager() {
		return strategyManager;
	}

	@Inject OutputDirectoryHierarchy controlerIO;
	@Override
	public OutputDirectoryHierarchy getControlerIO() {
		return controlerIO;
	}

	@Override
	public void addControlerListener(ControlerListener controlerListener) {
		((NewControler) injector.getInstance(ControlerI.class)).addControlerListener(controlerListener);
	}

	@Inject ReplanningContext replanningContext;
	@Override
	public Integer getIterationNumber() {
		return replanningContext.getIteration();
	}
}
