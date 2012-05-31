package playground.pieter.mentalsim.controler.listeners;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;

/**
 * @author fouriep
 *         <p/>
 *         performs geometric
 * 
 */
public class SimpleAnnealer implements IterationStartsListener,
		ControlerListener {

	final static String START_PROPORTION = "startProportion";
	final static String END_PROPORTION = "endProportion";
	final static String ANNEAL_TYPE = "annealType";
	final static String GEOMETRIC_FACTOR = "geometricFactor";
	final static String modName = "SimpleAnnealer";
	static double startProportion = -1;
	static double endProportion = -1;
	static double currentProportion = -1;
	static double geoFactor = 0.9;
	static double slope = -1;
	static int currentIter = 0;
	static boolean isGeometric = false;
	static boolean annealSwitch = true;
	Logger log = Logger.getLogger(getClass());
	Controler controler;
	Config config;

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		controler = event.getControler();
		config = controler.getConfig();

		if (!annealSwitch) {
			log.error("No simulated annealing of replanning.");
			return;
		}
		// initialize params
		if (startProportion < 0) {
			String sp = config.getParam(modName, START_PROPORTION);
			String ep = config.getParam(modName, END_PROPORTION);
			if (ep != null && sp != null) {
				startProportion = Double.parseDouble(sp);
				endProportion = Double.parseDouble(ep);
			} else {
				log.error("No start/endProportion set for Annealer, "
						+ "so no simulated annealing of replanning.");
				return;
			}
			if (config.getParam(modName, ANNEAL_TYPE).equals("geometric")) {
				isGeometric = true;
				log.warn("Using geometric annealing, so endProportion parameter not used");
				String gf = config.getParam(modName, GEOMETRIC_FACTOR);
				if (gf != null && Double.parseDouble(gf) > 0
						&& Double.parseDouble(gf) <= 1) {
					geoFactor = Double.parseDouble(gf);
				}
			}
			if (!isGeometric)
				slope = (startProportion - endProportion)
						/ (controler.getFirstIteration() - controler
								.getLastIteration());
		}
		// re-planning only starts in the first iteration
		currentIter = event.getIteration() - controler.getFirstIteration();
		if (currentIter <= 1)
			currentProportion = startProportion;
		else {
			if (isGeometric)
				currentProportion *= geoFactor;
			else
				currentProportion = currentIter * slope + startProportion;
		}
		anneal(event);
	}

	/**
	 * @param event
	 *            Goes thru the list of strategies, adjusts the weight of the
	 *            mutating strategies so they form the currentProportion of
	 *            re-planning in total.
	 * @throws InterruptedException
	 */
	private void anneal(IterationStartsEvent event) {
		StrategyManager stratMan = controler.getStrategyManager();
		List<PlanStrategy> strategies = stratMan.getStrategies();
		double totalWeights = 0.0;
		double totalSelectorWeights = 0.0;
		for (PlanStrategy strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();
			double weight = stratMan.getWeights().get(
					strategies.indexOf(strategy));
			if (strategyName.contains("selector")
					&& !strategyName.contains("_")
			// so no other strategies except the selector in the
			// string produced by current planstrategy implementation
			) {
				totalSelectorWeights += weight;
			}
			totalWeights += weight;
		}

		double selectorFactor = (1 - currentProportion)
				/ (totalSelectorWeights / totalWeights);
		double nonSelectorFactor = currentProportion
				/ ((totalWeights - totalSelectorWeights) / totalWeights);
		String outputToWrite = "\t" + event.getIteration() + "\t" + currentIter
				+ "\t" + currentProportion;
		for (PlanStrategy strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();
			double weight = stratMan.getWeights().get(
					strategies.indexOf(strategy));
			double newWeight = weight;
			if (strategyName.contains("selector")
					&& !strategyName.contains("_")
			// selector-only strategy
			) {
				// change the weight of the next selector strategy as recorded
				newWeight = selectorFactor * weight;

			} else {
				newWeight = nonSelectorFactor * weight;
			}
			// log.error("In iter "+ event.getIteration()+ ", strategy " +
			// strategy + " weight set from " + weight
			// + " to " + newWeight);
			outputToWrite += "\t" + strategy + "\t" + weight + "\t" + newWeight;
			stratMan.changeWeightOfStrategy(strategy, newWeight);
		}
		log.error(outputToWrite);

	}
}
