package herbie.running.controler;

import com.google.inject.Singleton;
import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.controler.listeners.ScoreElements;
import herbie.running.replanning.TransitStrategyManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;


public class HerbieModule extends AbstractModule {

	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	private static double reroutingShare = 0.05;

	private static class LegDistanceDistributionWriterProvider implements Provider<ControlerListener> {
		@Inject
		Network network;
		@Override
		public ControlerListener get() {
			return new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME, network);
		}
	}

	private static class HerbieStrategyManagerProvider implements com.google.inject.Provider<StrategyManager> {
		@Inject
		Scenario scenario;
		@Inject Provider<TripRouter> tripRouterProvider;
		@Override
		public StrategyManager get() {
			return new Provider<StrategyManager>() {
				@Override
				public StrategyManager get() {
//					log.info("loading TransitStrategyManager - using rerouting share of " + reroutingShare);
					StrategyManager manager = new TransitStrategyManager(scenario, reroutingShare, tripRouterProvider);
					//	  StrategyManagerConfigLoader.load(this, manager);
					return manager;
				}
			}.get();
		}
	}

	@Override
	public void install() {
		bind(StrategyManager.class).toProvider(new HerbieStrategyManagerProvider()).in(Singleton.class);
		addControlerListenerBinding().toInstance(new ScoreElements(HerbieControler.SCORE_ELEMENTS_FILE_NAME));
		addControlerListenerBinding().toInstance(new CalcLegTimesHerbieListener(HerbieControler.CALC_LEG_TIMES_FILE_NAME, HerbieControler.LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		addControlerListenerBinding().toProvider(LegDistanceDistributionWriterProvider.class);
	}

}
