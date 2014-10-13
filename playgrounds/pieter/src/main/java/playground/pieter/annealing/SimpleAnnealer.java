package playground.pieter.annealing;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;

/**
 * @author fouriep
 *         <p/>
 *         performs geometric
 * 
 */
class SimpleAnnealer implements IterationStartsListener,
		ControlerListener {

	private final static String START_PROPORTION = "startProportion";
	private final static String END_PROPORTION = "endProportion";
	private final static String ANNEAL_TYPE = "annealType";
	private final static String GEOMETRIC_FACTOR = "geometricFactor";
	private final static String HALF_LIFE = "halfLife";
	private final static String modName = "SimpleAnnealer";
	private static double startProportion = -1;
	private static double endProportion = 0.001;
	private static double currentProportion = 0.1;
	private static double geoFactor = 0.9;
	private static int halfLife = 100;
	private static double slope = -1;
	private static int currentIter = 0;
	private static boolean isGeometric = false;
	private static boolean isExponential;
	private static boolean annealSwitch = true;
	private final Logger log = Logger.getLogger(getClass());

    @Override
	public void notifyIterationStarts(IterationStartsEvent event) {
        Controler controler = event.getControler();
        Config config = controler.getConfig();

		if (!annealSwitch) {
			log.error("No simulated annealing of replanning.");
			return;
		}
		// initialize params
		if (startProportion < 0) {
			String sp = config.getParam(modName, START_PROPORTION);
			String ep = config.getParam(modName, END_PROPORTION);
			if (sp != null) {
				startProportion = Double.parseDouble(sp);
			} else {
				log.error("No startProportion set for Annealer, "
						+ "so no simulated annealing of replanning.");
				annealSwitch = false;
				return;
			}
			if (ep != null) {
				endProportion = Double.parseDouble(ep);
			}

			if (config.getParam(modName, ANNEAL_TYPE).equals("geometric")) {
				isGeometric = true;
				if (ep != null)
					log.warn("Using geometric annealing, so endProportion parameter becomes a minimum");
				String gf = config.getParam(modName, GEOMETRIC_FACTOR);
				if (gf != null && Double.parseDouble(gf) > 0
						&& Double.parseDouble(gf) <= 1) {
					geoFactor = Double.parseDouble(gf);
				} else {
					log.error("No geometric factor set for geometric simulated Annealer, "
							+ "so using default of 0.9.");

				}
			} else if (config.getParam(modName, ANNEAL_TYPE).equals(
					"exponential")) {
				isExponential = true;
				if (ep != null)
					log.warn("Using exponential annealing, so " + END_PROPORTION
							+ " parameter becomes a minimum");
				String ef = config.getParam(modName, HALF_LIFE);
				if (ef != null && Integer.parseInt(ef) > 0) {
					halfLife = Integer.parseInt(ef);
				} else {
					log.error("Invalid " + HALF_LIFE
							+ " for simulated Annealer, "
							+ "so using default of " + halfLife + " iters.");

				}
			} else if (config.getParam(modName, ANNEAL_TYPE).equals("linear")) {
				if (ep == null)
					log.warn("No " + END_PROPORTION
							+ " set, so using default of " + endProportion);
				slope = (startProportion - endProportion)
						/ (controler.getConfig().controler().getFirstIteration() - controler.getConfig().controler().getLastIteration());
			} else {
				log.error("Incorrect anneal type \""
						+ config.getParam(modName, ANNEAL_TYPE)
						+ "\". Turning off simulated annealing)");
				annealSwitch = false;
				return;
			}
		}
		// re-planning only starts in the first iteration
		currentIter = event.getIteration() - controler.getConfig().controler().getFirstIteration();
		if (currentIter <= 1)
			currentProportion = startProportion;
		else {
			if (isGeometric)
				currentProportion *= geoFactor;
			else if (isExponential)
				currentProportion = startProportion
						/ (Math.pow(2, (double) currentIter / halfLife));
			else
				currentProportion = currentIter * slope + startProportion;
		}
		currentProportion = Math.max(currentProportion,endProportion);
		anneal(event, currentProportion);
	}

	/**
	 * @param event
	 *            Goes thru the list of strategies, adjusts the weight of the
	 *            mutating strategies so they form the currentProportion of
	 *            re-planning in total.
	 * @throws InterruptedException
	 */
	private static void anneal(IterationStartsEvent event, double proportion) {
		StrategyManager stratMan = event.getControler().getStrategyManager();
		List<GenericPlanStrategy<Plan, Person>> strategies = stratMan.getStrategiesOfDefaultSubpopulation();
		double totalWeights = 0.0;
		double totalSelectorWeights = 0.0;
		for (GenericPlanStrategy<Plan, Person> strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();
			double weight = stratMan.getWeightsOfDefaultSubpopulation().get(
					strategies.indexOf(strategy));
			if ((strategyName.contains("selector")||strategyName.contains("expbeta"))
					&& !strategyName.contains("_")
			// so no other strategies except the selector in the
			// string produced by current planstrategy implementation
			) {
				totalSelectorWeights += weight;
			}
			totalWeights += weight;
		}

//		double selectorFactor = (1 - proportion)
//				/ (totalSelectorWeights / totalWeights);
//		double nonSelectorFactor = proportion
//				/ ((totalWeights - totalSelectorWeights) / totalWeights);
		String outputToWrite = "\t" + event.getIteration() + "\t" + currentIter
				+ "\t" + proportion;
		for (GenericPlanStrategy<Plan, Person> strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();
			double weight = stratMan.getWeightsOfDefaultSubpopulation().get(
					strategies.indexOf(strategy));
			double newWeight = weight;
			if ((strategyName.contains("selector")||strategyName.contains("expbeta"))
					&& !strategyName.contains("_")
			// selector-only strategy
			) {
				// change the weight of the next selector strategy as recorded
				newWeight = (1-proportion) * weight/totalSelectorWeights;

			} else {
				newWeight = proportion * weight/(totalWeights-totalSelectorWeights);
			}
			// log.error("In iter "+ event.getIteration()+ ", strategy " +
			// strategy + " weight set from " + weight
			// + " to " + newWeight);
			outputToWrite += "\t" + strategy + "\t" + weight + "\t" + newWeight;
			stratMan.changeWeightOfStrategyForDefaultSubpopulation(strategy, newWeight);
		}
		Logger.getLogger("ANNEAL").info(outputToWrite);

	}
}
