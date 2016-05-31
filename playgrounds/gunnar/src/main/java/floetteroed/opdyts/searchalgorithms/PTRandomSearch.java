package floetteroed.opdyts.searchalgorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.opdyts.trajectorysampling.ParallelTrajectorySampler;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.opdyts.trajectorysampling.SingleTrajectorySampler;
import floetteroed.opdyts.trajectorysampling.Transition;
import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

public class PTRandomSearch<U extends DecisionVariable> extends RandomSearch<U>{
	private final Simulator<U> simulator;
	
	private Scenario scenario;
	
	private final DecisionVariableRandomizer<U> randomizer;

	private final U initialDecisionVariable;

	private final ConvergenceCriterion convergenceCriterion;

	private final int maxIterations;

	private final int maxTransitions;

	private final int populationSize;

	private final Random rnd;

	private final boolean interpolate;

	private final ObjectiveFunction objectBasedObjectiveFunction;

	private final boolean includeCurrentBest;

	private int maxTotalMemory = Integer.MAX_VALUE;

	private int maxMemoryPerTrajectory = Integer.MAX_VALUE;

	private boolean maintainAllTrajectories = true;

	// -------------------- MEMBERS --------------------

	private String logFileName = null;

	private String convergenceTrackingFileName = null;

	private String outerIterationLogFileName = null;

	// -------------------- CONSTRUCTION --------------------
	public PTRandomSearch(final Simulator<U> simulator,
			final DecisionVariableRandomizer<U> randomizer,
			final U initialDecisionVariable,
			final ConvergenceCriterion convergenceCriterion,
			final int maxIterations, final int maxTransitions,
			final int populationSize, final Random rnd,
			final boolean interpolate,
			final ObjectiveFunction objectBasedObjectiveFunction,
			final boolean includeCurrentBest) {
		super(simulator,randomizer,initialDecisionVariable,
				convergenceCriterion,maxIterations, maxTransitions,
				populationSize, rnd, interpolate, objectBasedObjectiveFunction,includeCurrentBest);
		this.simulator = simulator;
		this.randomizer = randomizer;
		this.initialDecisionVariable = initialDecisionVariable;
		this.convergenceCriterion = convergenceCriterion;
		this.maxIterations = maxIterations;
		this.maxTransitions = maxTransitions;
		this.populationSize = populationSize;
		this.rnd = rnd;
		this.interpolate = interpolate;
		this.objectBasedObjectiveFunction = objectBasedObjectiveFunction;
		this.includeCurrentBest = includeCurrentBest;
	}
	private void deleteIfPossible(final String fileName) {
		if (fileName != null) {
			final File file = new File(fileName);
			if (file.exists()) {
				file.delete();
			}
		}
	}
	public void setLogFileName(final String logFileName) {
		this.logFileName = logFileName;
	}

	public void setConvergenceTrackingFileName(
			final String convergenceTrackingFileName) {
		this.convergenceTrackingFileName = convergenceTrackingFileName;
	}

	public void setMaxTotalMemory(final int maxTotalMemory) {
		this.maxTotalMemory = maxTotalMemory;
	}

	public void setMaxMemoryPerTrajectory(final int maxMemoryPerTrajectory) {
		this.maxMemoryPerTrajectory = maxMemoryPerTrajectory;
	}

	public void setMaintainAllTrajectories(final boolean maintainAllTrajectories) {
		this.maintainAllTrajectories = maintainAllTrajectories;
	}

	public void setOuterIterationLogFileName(
			final String outerIterationLogFileName) {
		this.outerIterationLogFileName = outerIterationLogFileName;
	}
	public void setScenario(final Scenario scenario){
		this.scenario=scenario;
	}
	
	public void run(final SelfTuner selfTuner) {
		this.run(0.0, 0.0, selfTuner);
	}
	public ArrayList<?> toArrayList(Iterator<?> iter){
		ArrayList<Object> arraylist = new ArrayList<Object>();
		while(iter.hasNext()){
			arraylist.add((Object)(iter.next()));
		}
		return arraylist;
	}
	@SuppressWarnings("unchecked")
	private void removePTLinksAndNodes(Network network){
		Map<Id<Node>, ? extends Node> nodes = network.getNodes();
		List<?> nodeids = toArrayList(nodes.keySet().iterator());
		for(int i=0;i<nodeids.size();i++){
			Id<Node> nodeid = (Id<Node>)nodeids.get(i);
			if(nodeid.toString().startsWith("tr")){
				network.removeNode(nodeid);
			}
		}
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		List<?> linkids = toArrayList(links.keySet().iterator());
		for(int i=0;i<linkids.size();i++){
			Id<Link> linkid = (Id<Link>)linkids.get(i);
			if(linkid.toString().startsWith("tr")){
				network.removeLink(linkid);
			}
		}
	}
	private void run(double equilibriumGapWeight, double uniformityGapWeight,
		SelfTuner weightOptimizer) {

		int totalTransitionCnt = 0;

		this.deleteIfPossible(this.logFileName);
		this.deleteIfPossible(this.convergenceTrackingFileName);
		this.deleteIfPossible(this.outerIterationLogFileName);

		final StatisticsWriter<OuterIterationStatistics> outerIterationStatsWriter;
		if (this.outerIterationLogFileName != null) {
			outerIterationStatsWriter = new StatisticsWriter<OuterIterationStatistics>(
					this.outerIterationLogFileName, false);
			OuterIterationStatistics
					.initializeWriter(outerIterationStatsWriter);
		} else {
			outerIterationStatsWriter = null;
		}

		U bestDecisionVariable = this.initialDecisionVariable;
		Double bestObjectiveFunctionValue = null;
		SimulatorState newInitialState = null;

		for (int it = 0; it < this.maxIterations
				&& totalTransitionCnt < this.maxTransitions; it++) {

			Logger.getLogger(this.getClass().getName()).info(
					"Iteration " + (it + 1) + " of " + this.maxIterations
							+ ", transitions " + totalTransitionCnt + " of "
							+ this.maxTransitions + " ====================");

			final Set<U> candidates = new LinkedHashSet<U>();
			if (this.includeCurrentBest) {
				candidates.add(bestDecisionVariable);
			}
			while (candidates.size() < this.populationSize) {
				candidates.addAll(this.randomizer
						.newRandomVariations(bestDecisionVariable));
			}

			int transitionsPerIteration = 0;
			U newBestDecisionVariable;
			double newBestObjectiveFunctionValue;

			final OuterIterationStatistics outerIterationStats;

			if (this.interpolate) {

				/*
				 * >>>>>>>>>>>>>>>>>>>> PARALLEL SAMPLING >>>>>>>>>>>>>>>>>>>>
				 */

				outerIterationStats = new OuterIterationStatistics(
						System.currentTimeMillis(), it + 1, totalTransitionCnt,
						equilibriumGapWeight, uniformityGapWeight);

				final ParallelTrajectorySampler<U> sampler;
				sampler = new ParallelTrajectorySampler<>(candidates,
						this.objectBasedObjectiveFunction,
						this.convergenceCriterion, this.rnd,
						equilibriumGapWeight, uniformityGapWeight, (it > 0),
						this.maxTotalMemory, this.maxMemoryPerTrajectory,
						this.maintainAllTrajectories);

				if (this.logFileName != null) {
					sampler.addStatistic(this.logFileName,
							new TimeStampStatistic<SamplingStage<U>>());
					// sampler.addStatistic(this.logFileName,
					// new Statistic<SamplingStage<U>>() {
					// @Override
					// public String label() {
					// return TIMESTAMP;
					// }
					//
					// @Override
					// public String value(final SamplingStage<U> data) {
					// return (new SimpleDateFormat(
					// "yyyy-MM-dd HH:mm:ss"))
					// .format(new Date(System
					// .currentTimeMillis()));
					// }
					// });
					final int currentIt = it; // inner class requires final
					sampler.addStatistic(this.logFileName,
							new Statistic<SamplingStage<U>>() {
								@Override
								public String label() {
									return RANDOM_SEARCH_ITERATION;
								}

								@Override
								public String value(final SamplingStage<U> data) {
									return Integer.toString(currentIt);
								}
							});
					final Double currentBestObjectiveFunctionValue = bestObjectiveFunctionValue;
					sampler.addStatistic(this.logFileName,
							new Statistic<SamplingStage<U>>() {
								@Override
								public String label() {
									return BEST_OVERALL_SOLUTION;
								}

								@Override
								public String value(final SamplingStage<U> data) {
									if (currentBestObjectiveFunctionValue == null) {
										return "";
									} else {
										return Double
												.toString(currentBestObjectiveFunctionValue);
									}
								}
							});
					sampler.setStandardLogFileName(this.logFileName);
				}
//				Network network = this.scenario.getNetwork();
//				removePTLinksAndNodes(network);
//				TransitSchedule schedule = scenario.getTransitSchedule();
////				new TransitScheduleWriterV1(schedule).write("./ihop2/matsim-input/transitschedule.xml");
////				new TransitScheduleReaderV1(schedule, network).readFile(scenario.getConfig().getParam("transit", "transitScheduleFile"));
//				new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
				newInitialState = this.simulator.run(sampler, newInitialState);
				newBestDecisionVariable = sampler
						.getDecisionVariable2convergenceResultView().keySet()
						.iterator().next();
				newBestObjectiveFunctionValue = sampler
						.getDecisionVariable2convergenceResultView().get(
								newBestDecisionVariable).finalObjectiveFunctionValue;
				transitionsPerIteration = sampler.getTotalTransitionCnt();

				if (this.convergenceTrackingFileName != null) {
					try {
						if (!new File(this.convergenceTrackingFileName)
								.exists()) {
							final PrintWriter writer = new PrintWriter(
									this.convergenceTrackingFileName);
							writer.println("Raw Objective Function Value\tAveraged Objective Function Value\tConverged");
							writer.flush();
							writer.close();
						}
						final BufferedWriter writer = new BufferedWriter(
								new FileWriter(
										this.convergenceTrackingFileName, true));
						final List<Transition<U>> transitions = sampler
								.getTransitions(newBestDecisionVariable);
						for (int i = 0; i < transitions.size(); i++) {
							final ConvergenceCriterionResult convRes = this.convergenceCriterion
									.evaluate(
											transitions.subList(0, i + 1),
											sampler.additionCnt(newBestDecisionVariable));
							writer.write(transitions.get(i)
									.getToStateObjectiveFunctionValue()
									+ "\t"
									+ (convRes.finalObjectiveFunctionValue != null ? convRes.finalObjectiveFunctionValue
											: "") + "\t" + convRes.converged);
							writer.newLine();
						}
						writer.flush();
						writer.close();
					} catch (IOException e) {
						Logger.getLogger(this.getClass().getName()).warn(
								e.getMessage());
					}
				}

				if (weightOptimizer != null) {
					weightOptimizer
							.update(sampler.getSamplingStages(),
									sampler.getDecisionVariable2convergenceResultView()
											.get(newBestDecisionVariable).finalObjectiveFunctionValue);
					equilibriumGapWeight = weightOptimizer
							.getEquilibriumGapWeight();
					uniformityGapWeight = weightOptimizer
							.getUniformityGapWeight();
				}

				/*
				 * <<<<<<<<<<<<<<<<<<<< PARALLEL SAMPLING <<<<<<<<<<<<<<<<<<<<
				 */

			} else {

				/*
				 * >>>>>>>>>>>>>>>>>>>> SEQUENTIAL SAMPLING >>>>>>>>>>>>>>>>>>>>
				 */

				outerIterationStats = new OuterIterationStatistics(
						System.currentTimeMillis(), it + 1, totalTransitionCnt,
						null, null);

				if (bestObjectiveFunctionValue != null) {
					try {
						final PrintWriter logWriter = new PrintWriter(
								new BufferedWriter(new FileWriter(
										this.logFileName, true)));
						logWriter.print((new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss")).format(new Date(System
								.currentTimeMillis()))
								+ "\t");
						logWriter.print(it + "\t");
						logWriter.print(totalTransitionCnt + "\t");
						logWriter.print(bestObjectiveFunctionValue + "\t");
						logWriter.println(bestDecisionVariable);
						logWriter.flush();
						logWriter.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				final SimulatorState thisRoundsInitialState = newInitialState;

				newBestDecisionVariable = null;
				newBestObjectiveFunctionValue = Double.POSITIVE_INFINITY;

				for (U candidate : candidates) {
					
					final SingleTrajectorySampler<U> singleSampler;
					singleSampler = new SingleTrajectorySampler<>(candidate,
							this.objectBasedObjectiveFunction,
							this.convergenceCriterion);
					Network network = this.scenario.getNetwork();
					NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(network);
					reader.parse("./ihop2/network-output/emptynetwork.xml");
					reader.parse("./ihop2/network-output/network.xml");
					TransitSchedule schedule = scenario.getTransitSchedule();
//					cleanSchedule(schedule);
					new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
					final SimulatorState candidateInitialState = this.simulator
							.run(singleSampler, thisRoundsInitialState);
					final double candidateObjectiveFunctionValue = singleSampler
							.getDecisionVariable2convergenceResultView().get(
									candidate).finalObjectiveFunctionValue;
					if (candidateObjectiveFunctionValue < newBestObjectiveFunctionValue) {
						newBestDecisionVariable = candidate;
						newBestObjectiveFunctionValue = candidateObjectiveFunctionValue;
						newInitialState = candidateInitialState;
					}
					transitionsPerIteration += singleSampler
							.getTotalTransitionCnt();
				}

				/*
				 * <<<<<<<<<<<<<<<<<<<< SEQUENTIAL SAMPLING <<<<<<<<<<<<<<<<<<<<
				 */
			}

			if (bestObjectiveFunctionValue == null
					|| newBestObjectiveFunctionValue < bestObjectiveFunctionValue) {
				bestDecisionVariable = newBestDecisionVariable;
				bestObjectiveFunctionValue = newBestObjectiveFunctionValue;
			}
			totalTransitionCnt += transitionsPerIteration;

			outerIterationStats.finalize(bestObjectiveFunctionValue,
					transitionsPerIteration, System.currentTimeMillis());
			if (outerIterationStatsWriter != null) {
				outerIterationStatsWriter.writeToFile(outerIterationStats);
			}
		}
	}
}
