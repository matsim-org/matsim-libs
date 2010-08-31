package playground.meisterk.eaptus2010;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ExternalModule;
import org.matsim.core.replanning.modules.PlanomatModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PathSizeLogitSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.locationchoice.LocationChoice;

public class MyStrategyManagerConfigLoader {

	private static final Logger log = Logger.getLogger(MyStrategyManagerConfigLoader.class);

	private static int externalCounter = 0;

	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param controler the {@link Controler} that provides miscellaneous data for the replanning modules
	 * @param manager the {@link StrategyManager} to be configured according to the configuration
	 */
	public static void load(final Controler controler, final StrategyManager manager) {
		Config config = controler.getConfig();
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}

			PlanStrategy strategy = loadStrategy(controler, classname, settings);

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
				if (maxIter >= config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeStrategy(strategy, 0.0);
				}
			}
		}
	}

	protected static PlanStrategy loadStrategy(final Controler controler, final String name, final StrategyConfigGroup.StrategySettings settings) {
		Network network = controler.getNetwork();
		PersonalizableTravelCost travelCostCalc = controler.createTravelCostCalculator();
		PersonalizableTravelTime travelTimeCalc = controler.getTravelTimeCalculator();
		Config config = controler.getConfig();

		PlanStrategy strategy = null;
		if (name.equals("KeepLastSelected")) {
			strategy = new PlanStrategy(new KeepSelected());
		} else if (name.equals("ReRoute") || name.equals("threaded.ReRoute")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRoute(controler));
		} else if (name.equals("ReRoute_Dijkstra")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
		} else if (name.equals("ReRoute_Landmarks")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else if (name.equals("TimeAllocationMutator") || name.equals("threaded.TimeAllocationMutator")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			TimeAllocationMutator tam = new TimeAllocationMutator(config);
//			tam.setUseActivityDurations(config.vspExperimental().isUseActivityDurations());
			// functionality moved into TimeAllocationMutator.  kai, aug'10
			strategy.addStrategyModule(tam);
		} else if (name.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new TimeAllocationMutator(config, 7200));
			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else if (name.equals("ExternalModule")) {
			externalCounter++;
			strategy = new PlanStrategy(new RandomPlanSelector());
			String exePath = settings.getExePath();
			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controler, controler.getScenario());
			em.setIterationNumber(controler.getIterationNumber());
			strategy.addStrategyModule(em);
		} else if (name.equals("Planomat")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			PlanStrategyModule planomatStrategyModule = new PlanomatModule(controler, controler.getEvents(), controler.getNetwork(), controler.getScoringFunctionFactory(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator());
			strategy.addStrategyModule(planomatStrategyModule);
		} else if (name.equals("PlanomatReRoute")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			PlanStrategyModule planomatStrategyModule = new PlanomatModule(controler, controler.getEvents(), controler.getNetwork(), controler.getScoringFunctionFactory(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator());
			strategy.addStrategyModule(planomatStrategyModule);
			strategy.addStrategyModule(new ReRoute(controler));
		} else if (name.equals("BestScore")) {
			strategy = new PlanStrategy(new BestPlanSelector());
		} else if (name.equals("SelectExpBeta")) {
			strategy = new PlanStrategy(new ExpBetaPlanSelector(config.charyparNagelScoring()));
		} else if (name.equals("ChangeExpBeta")) {
			strategy = new PlanStrategy(new ExpBetaPlanChanger(config.charyparNagelScoring().getBrainExpBeta()));
		} else if (name.equals("ChangeExpBeta2")) {
			strategy = new PlanStrategy(new ExpBetaPlanChanger2(config.charyparNagelScoring().getBrainExpBeta()));
		} else if (name.equals("SelectRandom")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
		} else if (name.equals("ChangeLegMode")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ChangeLegMode(config));
			strategy.addStrategyModule(new ReRoute(controler));
		} else if (name.equals("SelectPathSizeLogit")) {
			strategy = new PlanStrategy(new PathSizeLogitSelector(controler.getNetwork(), config.charyparNagelScoring()));
		} else if (name.equals("LocationChoice")) {
			strategy = new PlanStrategy(new ExpBetaPlanSelector(config.charyparNagelScoring()));
			strategy.addStrategyModule(new LocationChoice(controler.getNetwork(), controler, (controler.getScenario()).getKnowledges()));
			strategy.addStrategyModule(new ReRoute(controler));
			strategy.addStrategyModule(new TimeAllocationMutator(config));
			/* not really happy about the following line. Imagine what happens if everybody does
			 * this, that one doesn't know at the end which removal-strategy is really used.
			 * The removal strategy must not be tight to a replanning-strategy, but is a general
			 * option that should be set somewhere else.  marcel/9jun2009/CLEANUP
			 */
			// not yet working correctly:
			// compreh. tests needed
			// ah/27jun2009
			// manager.setPlanSelectorForRemoval(new ExpBetaPlanForRemovalSelector());
		}
		//if none of the strategies above could be selected we try to load the class by name
		else {
			//classes loaded by name must not be part of the matsim core
			if (name.startsWith("org.matsim")) {
				log.error("Strategies in the org.matsim package must not be loaded by name!");
			}
			else {
				try {
					Class<? extends PlanStrategy> klas = (Class<? extends PlanStrategy>) Class.forName(name);
					Class[] args = new Class[1];
					args[0] = Scenario.class;
					Constructor<? extends PlanStrategy> c = null;
					try{
						c = klas.getConstructor(args);
						strategy = c.newInstance(controler.getScenario());
					} catch(NoSuchMethodException e){
						log.warn("Cannot find Constructor in PlanStrategy " + name + " with single argument of type Scenario. " +
								"This is not fatal, trying to find other constructor, however a constructor expecting Scenario as " +
								"single argument is recommented!" );
					}
					if (c == null){
						args[0] = Controler.class;
						c = klas.getConstructor(args);
						strategy = c.newInstance(controler);
					}
					log.info("Loaded PlanStrategy from class " + name);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		return strategy;
	}

}
