package playground.artemc.annealing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.DefaultPlanStrategiesModule.Selector;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	final static String HALF_LIFE = "halfLife";
	final static String modName = "SimpleAnnealer";
	static double startProportion = -1;
	static double endProportion = 0.001;
	static double currentProportion = 0.1;
	static double geoFactor = 0.9;
	static int halfLife = 100;
	static double slope = -1;
	static int currentIter = 0;
	static boolean isGeometric = false;
	static boolean isExponential;
	static boolean annealSwitch = true;
	Logger log = Logger.getLogger(getClass());
	private Controler controler;
	private Config config;

	private static Map<Integer, ArrayList<Double>> replaningRates= new HashMap<Integer, ArrayList<Double>>();

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		controler = event.getControler();
		config = controler.getConfig();
		replaningRates.put(event.getIteration(), new ArrayList<Double>());

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
						/ (controler.getConfig().controler().getFirstIteration() - getInnovationStop(controler.getConfig()));
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
	public void anneal(IterationStartsEvent event, double proportion) {
		StrategyManager stratMan = event.getControler().getStrategyManager();

		StrategyConfigGroup strategyConfig = (StrategyConfigGroup) event.getControler().getConfig().getModules().get(StrategyConfigGroup.GROUP_NAME);

		//List<GenericPlanStrategy<Plan>> strategies = stratMan.getStrategiesOfDefaultSubpopulation();

		List<GenericPlanStrategy<Plan, Person>> strategies = stratMan.getStrategies(null);

		double totalWeights = 0.0;
		double totalSelectorWeights = 0.0;
		for (GenericPlanStrategy<Plan, Person> strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();

			//	double weight = stratMan.getWeightsOfDefaultSubpopulation().get(
			//			strategies.indexOf(strategy));
			double weight = stratMan.getWeights(null).get(
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
			//	double weight = stratMan.getWeightsOfDefaultSubpopulation().get(
			//			strategies.indexOf(strategy));
			double weight = stratMan.getWeights(null).get(
					strategies.indexOf(strategy));
			double newWeight = weight;

			if(newWeight > 0){
				if ((strategyName.contains("selector")||strategyName.contains("expbeta"))
						&& !strategyName.contains("_")
						// selector-only strategy
						) {
					// change the weight of the next selector strategy as recorded
					//newWeight = (1-proportion) * weight/totalSelectorWeights;
					newWeight = weight;
				} else {
					newWeight = proportion * weight/(totalWeights-totalSelectorWeights);
				}
			}
			// log.error("In iter "+ event.getIteration()+ ", strategy " +
			// strategy + " weight set from " + weight
			// + " to " + newWeight);
			outputToWrite += "\t" + strategy + "\t" + weight + "\t" + newWeight;
			stratMan.changeWeightOfStrategy(strategy, null, newWeight);
			replaningRates.get(event.getIteration()).add(newWeight);
		}
		Logger.getLogger("ANNEAL").info(outputToWrite);
		writeReplanningRates(strategies);
	}

	private static int getInnovationStop(Config config){
		int globalInnovationDisableAfter = (int) ((config.controler().getLastIteration() - config.controler().getFirstIteration()) 
				* config.strategy().getFractionOfIterationsToDisableInnovation() + config.controler().getFirstIteration());

		int innovationStop = -1;

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {

			String moduleName = settings.getStrategyName();

			// now check if this modules should be disabled after some iterations
			int maxIter = settings.getDisableAfter();
			// --- begin new ---
			if ( maxIter > globalInnovationDisableAfter || maxIter==-1 ) {
				boolean innovative = true ;
				for ( Selector sel : DefaultPlanStrategiesModule.Selector.values() ) {
					System.out.flush();
					if ( moduleName.equals( sel.toString() ) ) {
						innovative = false ;
						break ;
					}
				}
				if ( innovative ) {
					maxIter = globalInnovationDisableAfter;	
				}				

			}

			if(innovationStop == -1){
				innovationStop = maxIter;
			}

			if(innovationStop != maxIter){
				Logger.getLogger("ANNEAL").warn("Different 'Disable After Interation' values are set for different replaning modules." +
						" Annealing doesn't support this function and will be performed according to the 'Disable After Interation' setting of the first replanning module " +
						"or 'globalInnovationDisableAfter', which ever value is lower.");
			}
		}

		return innovationStop;
	}

	private void writeReplanningRates(List<GenericPlanStrategy<Plan, Person>> strategies) {

		String fileName = config.controler().getOutputDirectory() + "/replanningRates.csv";
		File file = new File(fileName);


		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			String header = "it";
			for (GenericPlanStrategy<Plan, Person> strategy : strategies) {
				header = header + "\t"+strategy.toString().toLowerCase();
			}
			bw.write(header);
			bw.newLine();

			for (Integer it : SimpleAnnealer.replaningRates.keySet()){
				String output = it.toString();
				for(Double rate:SimpleAnnealer.replaningRates.get(it)){
					output=output+"\t"+rate;
				}
				bw.write(output);
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

