package org.matsim.core.controler;


import com.google.inject.Provider;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.router.TransitRouter;

import java.util.List;

public interface MatsimServices {

	IterationStopWatch getStopwatch();

	TravelTime getLinkTravelTimes();

	Provider<TripRouter> getTripRouterProvider();

	TravelDisutility createTravelDisutilityCalculator();

	LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory();

	ScoringFunctionFactory getScoringFunctionFactory();

	Config getConfig();

	Scenario getScenario();

	EventsManager getEvents();

	com.google.inject.Injector getInjector();

	CalcLinkStats getLinkStats();

	VolumesAnalyzer getVolumes();

	ScoreStats getScoreStats();

	TravelDisutilityFactory getTravelDisutilityFactory();

	StrategyManager getStrategyManager();

	OutputDirectoryHierarchy getControlerIO();

	void addControlerListener(ControlerListener controlerListener);

	Integer getIterationNumber();
}
