package opdytsintegration;

import java.util.*;
import com.google.inject.Inject;
import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;
import opdytsintegration.utils.OpdytsConfigGroup;
import opdytsintegration.utils.TimeDiscretization;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.StackedBarChart;

/**
 * Identifies the approximately best out of a set of decision variables.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see DecisionVariable
 *
 */
public class MATSimDecisionVariableSetEvaluator2<U extends DecisionVariable>
		implements StartupListener, BeforeMobsimListener, ShutdownListener, IterationEndsListener {

	// -------------------- MEMBERS --------------------

	private static final Logger LOGGER = Logger.getLogger(MATSimDecisionVariableSetEvaluator2.class);

	private final TrajectorySampler<U> trajectorySampler;

	private final MATSimStateFactory<U> stateFactory;

	// private final TimeDiscretization timeDiscretization;

	private int memory = 1;

	private boolean averageMemory = false;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Population population;

	// created during runtime:

	// // must be linked to ensure a unique iteration ordering
	// private LinkedHashSet<String> relevantNetworkModes;
	//
	// // must be linked to ensure a unique iteration ordering
	// private LinkedHashSet<Id<Link>> relevantLinkIds;
	//
	// // must be linked to ensure a unique iteration ordering
	// private LinkedHashSet<Id<TransitStopFacility>> relevantStopIds;
	//
	// private DifferentiatedLinkOccupancyAnalyzer networkOccupancyAnalyzer =
	// null;
	//
	// private PTOccupancyAnalyser ptOccupancyAnalyzer = null;

	// a list because the order matters in the state space vector
	private final List<SimulationStateAnalyzerProvider> simulationStateAnalyzers = new ArrayList<>();

	private LinkedList<Vector> stateList = null;

	private MATSimState finalState = null;

	private boolean justStarted = true;

	// -------------------- CONSTRUCTION --------------------

	public MATSimDecisionVariableSetEvaluator2(final TrajectorySampler<U> trajectorySampler,
			final MATSimStateFactory<U> stateFactory, final TimeDiscretization timeDiscretization) {
		// TODO timeDiscretization argument is no longer needed
		this.trajectorySampler = trajectorySampler;
		this.stateFactory = stateFactory;
		// this.timeDiscretization = timeDiscretization;
		// the following need to be explicitly set
		// this.relevantNetworkModes = null;
		// this.relevantLinkIds = null;
		// this.relevantStopIds = null;
	}

	public void addSimulationStateAnalyzer(final SimulationStateAnalyzerProvider analyzer) {
		if (this.simulationStateAnalyzers.contains(analyzer)) {
			throw new RuntimeException("Analyzer " + analyzer + " has already been added.");
		}
		this.simulationStateAnalyzers.add(analyzer);
	}

	// public void enableNetworkTraffic(final Collection<String>
	// relevantNetworkModes,
	// final Collection<Id<Link>> relevantLinkIds) {
	// this.relevantNetworkModes = new LinkedHashSet<>(relevantNetworkModes);
	// this.relevantLinkIds = new LinkedHashSet<>(relevantLinkIds);
	// }

	// public void enablePT(final Collection<Id<TransitStopFacility>>
	// relevantStopIds) {
	// this.relevantStopIds = new LinkedHashSet<>(relevantStopIds);
	// }

	// -------------------- SETTERS AND GETTERS --------------------

	public boolean foundSolution() {
		return this.trajectorySampler.foundSolution();
	}

	/**
	 * The vector representation of MATSim's instantaneous state omits some
	 * memory effects, meaning that it is not perfectly precise. Setting the
	 * memory parameter to more than one (its default value) will increase the
	 * precision at the cost of a larger computer memory usage. Too large values
	 * (perhaps larger than ten) may again impair the optimization performance.
	 */
	public void setMemory(final int memory) {
		this.memory = memory;
	}

	public int getMemory() {
		return this.memory;
	}

	/**
	 * <b>WARNING Use this option only if you know what you are doing.</b>
	 * <p>
	 * Setting this to true saves <em>computer</em> memory by averaging the
	 * <em>simulation process</em> memory instead of completely keeping track of
	 * it. This only works if the right (and problem-specific!) memory length is
	 * chosen.
	 * 
	 * @param averageMemory
	 */
	public void setAverageMemory(final boolean averageMemory) {
		this.averageMemory = averageMemory;
	}

	public boolean getAverageMemory() {
		return this.averageMemory;
	}

	public MATSimState getFinalState() {
		return finalState;
	}

	// -------------------- INTERNALS --------------------

	private MATSimState newState(final Population population) {
		final Vector newSummaryStateVector;
		if (this.averageMemory) {
			// average state vectors
			newSummaryStateVector = this.stateList.getFirst().copy();
			for (int i = 1; i < this.memory; i++) {
				// TODO Why iterate up to memory and not up to stateList.size()?
				newSummaryStateVector.add(this.stateList.get(i));
			}
			newSummaryStateVector.mult(1.0 / this.memory);
		} else {
			// concatenate state vectors
			newSummaryStateVector = Vector.concat(this.stateList);
		}
		return this.stateFactory.newState(population, newSummaryStateVector,
				this.trajectorySampler.getCurrentDecisionVariable());
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.stateList = new LinkedList<Vector>();

		if (this.simulationStateAnalyzers.isEmpty()) {
			throw new RuntimeException("No simulation state analyzers have been added.");
		}

		for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
			this.eventsManager.addHandler(analyzer.newEventHandler());
		}

		// if ((this.relevantLinkIds != null) && (this.relevantLinkIds.size() >
		// 0) && (this.relevantNetworkModes != null)
		// && (this.relevantNetworkModes.size() > 0)) {
		// this.networkOccupancyAnalyzer = new
		// DifferentiatedLinkOccupancyAnalyzer(this.timeDiscretization,
		// this.relevantNetworkModes, this.relevantLinkIds);
		// this.eventsManager.addHandler(this.networkOccupancyAnalyzer);
		// }

		// if (this.relevantStopIds != null && (relevantStopIds.size() > 0)) {
		// this.ptOccupancyAnalyzer = new
		// PTOccupancyAnalyser(this.timeDiscretization, this.relevantStopIds);
		// this.eventsManager.addHandler(this.ptOccupancyAnalyzer);
		// }

		this.trajectorySampler.initialize();

		// TODO NEW
		this.justStarted = true;

		//NEW: amit
		this.stateVectorSizePlotter = new StateVectorSizePlotter();
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {

		/*
		 * (0) The mobsim must have been run at least once to allow for the
		 * extraction of a vector-valued system state. The "just started" MATSim
		 * iteration is hence run through without Opdyts in the loop.
		 */
		if (this.justStarted) {
			this.justStarted = false;
		} else {

			/*
			 * (1) Extract the instantaneous state vector.
			 */

			Vector newInstantaneousStateVector = null;
			for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
				if (newInstantaneousStateVector == null) {
					newInstantaneousStateVector = analyzer.newStateVectorRepresentation();

					stateVectorSizePlotter.addStateSize(newInstantaneousStateVector.size(),analyzer.getStringIdentifier()); // amit
				} else {
					newInstantaneousStateVector = Vector.concat(newInstantaneousStateVector,
							analyzer.newStateVectorRepresentation());
					stateVectorSizePlotter.addStateSize(newInstantaneousStateVector.size(),analyzer.getStringIdentifier()); // amit
				}
			}

			// car
			// if (this.networkOccupancyAnalyzer != null) {
			// final Vector newInstantaneousStateVectorCar = new
			// Vector(this.relevantNetworkModes.size()
			// * this.relevantLinkIds.size() *
			// this.timeDiscretization.getBinCnt());
			// int i = 0;
			// for (String mode : this.relevantNetworkModes) {
			// final MATSimCountingStateAnalyzer<Link> analyzer =
			// this.networkOccupancyAnalyzer
			// .getNetworkModeAnalyzer(mode);
			// for (Id<Link> linkId : this.relevantLinkIds) {
			// for (int bin = 0; bin < this.timeDiscretization.getBinCnt();
			// bin++) {
			// newInstantaneousStateVectorCar.set(i++, analyzer.getCount(linkId,
			// bin));
			// }
			// }
			// }
			// newInstantaneousStateVector = newInstantaneousStateVectorCar;
			// }

			// pt
			// if (this.ptOccupancyAnalyzer != null) {
			// final Vector newInstantaneousStateVectorPT = new Vector(
			// this.relevantStopIds.size() *
			// this.timeDiscretization.getBinCnt());
			// int i = 0;
			// for (Id<TransitStopFacility> stopId : this.relevantStopIds) {
			// for (int bin = 0; bin < this.timeDiscretization.getBinCnt();
			// bin++) {
			// newInstantaneousStateVectorPT.set(i++,
			// this.ptOccupancyAnalyzer.getCount(stopId, bin));
			// }
			// }
			// if (newInstantaneousStateVector != null) {
			// newInstantaneousStateVector =
			// Vector.concat(newInstantaneousStateVector,
			// newInstantaneousStateVectorPT);
			// } else {
			// newInstantaneousStateVector = newInstantaneousStateVectorPT;
			// }
			// }

			/*
			 * (2) Add instantaneous state vector to the list of past state
			 * vectors and ensure that the size of this list is equal to what
			 * the memory parameter prescribes.
			 */
			this.stateList.addFirst(newInstantaneousStateVector);
			while (this.stateList.size() < this.memory) {
				this.stateList.addFirst(newInstantaneousStateVector);
			}
			while (this.stateList.size() > this.memory) {
				this.stateList.removeLast();
			}

			// BEGIN_NEW: amit June'17
			Config config = event.getServices().getConfig();
			OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(config,OpdytsConfigGroup.GROUP_NAME, OpdytsConfigGroup.class);
			int numberOfDecisionVariableTrials = opdytsConfigGroup.getNumberOfDecisionVariableTrials();
			int iterationsToUpdateDecisionVariableTrial = opdytsConfigGroup.getIterationsToUpdateDecisionVariableTrial();
			int totalIterationForUpdation = numberOfDecisionVariableTrials * iterationsToUpdateDecisionVariableTrial;

			/*
			 * (3) Inform the TrajectorySampler that one iteration has been
			 * completed and provide the resulting state.
			 */
			if (event.getIteration() == config.controler().getFirstIteration() ||
					event.getIteration() > totalIterationForUpdation
					) { //i.e., for first iteration and after all decision variables are tried out.

				LOGGER.info("Informing trajectory sampler at iteration Nr "+ event.getIteration() + "; this will provide the resulting state.");
				this.trajectorySampler.afterIteration(this.newState(this.population));

			} else if( event.getIteration() % iterationsToUpdateDecisionVariableTrial == 1
					) { // i.e., update after every iterationsToUpdateDecisionVariableTrial

				LOGGER.info("Informing trajectory sampler at iteration Nr "+ event.getIteration() + "; this will provide the resulting state.");
				this.trajectorySampler.afterIteration(this.newState(this.population));

			} else {
				// nothing to do for intermediate iterations.
			}
			// END_NEW: amit June'17
		}

		for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
			analyzer.beforeIteration();
		}

		// if (this.networkOccupancyAnalyzer != null) {
		// this.networkOccupancyAnalyzer.beforeIteration();
		// }
		// if (this.ptOccupancyAnalyzer != null) {
		// this.ptOccupancyAnalyzer.beforeIteration();
		// }

	}

	/*
	 * TODO Given that an iteration is assumed to end before the
	 * "mobsim execution" step, the final state is only approximately correctly
	 * computed because it leaves out the last iteration's "replanning" step.
	 * 
	 */
	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		this.finalState = this.newState(this.population);
	}

	//NEW: Amit
	private StateVectorSizePlotter stateVectorSizePlotter ;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.stateVectorSizePlotter.plotData(event.getServices().getControlerIO().getOutputFilename("stateVectorSize.png"));
	}

	class StateVectorSizePlotter {

		StateVectorSizePlotter () {
			identifierToVectorSizes = new HashMap<>();
		}

		private final Map<String, List<Integer>> identifierToVectorSizes ;

		public void addStateSize(final int vectorSize, final String identifier) {
			List<Integer> sizes = this.identifierToVectorSizes.get(identifier);
			if (sizes == null) {
				sizes = new ArrayList<>();
				sizes.add(vectorSize);
			} else {
				sizes.add(vectorSize);
			}
			this.identifierToVectorSizes.put(identifier, sizes);
		}

		public void plotData(final String outFile){
			StackedBarChart chart = new StackedBarChart("Size of the state vector element","iteration", "size");
			for (String mode : this.identifierToVectorSizes.keySet()) {
				double[] ys = new double[ identifierToVectorSizes.get(mode).size()];

				List<Integer> sizes = this.identifierToVectorSizes.get(mode);
				Collections.sort(sizes, Collections.reverseOrder());

				for (int index = 0; index < ys.length ; index++) {
					ys[index] = sizes.get(index);
				}

				chart.addSeries(mode, ys);
			}
			chart.saveAsPng(outFile,1200,800);
		}
	}

}

// @Override
// public void notifyIterationEnds(final IterationEndsEvent event) {
//
// /*
// * (1) Extract the instantaneous state vector.
// */
//
// Vector newInstantaneousStateVector = null;
//
// // car
// if (this.networkOccupancyAnalyzer != null) {
// final Vector newInstantaneousStateVectorCar = new
// Vector(this.relevantNetworkModes.size()
// * this.relevantLinkIds.size() * this.timeDiscretization.getBinCnt());
// int i = 0;
// for (String mode : this.relevantNetworkModes) {
// final MATSimCountingStateAnalyzer<Link> analyzer =
// this.networkOccupancyAnalyzer
// .getNetworkModeAnalyzer(mode);
// for (Id<Link> linkId : this.relevantLinkIds) {
// for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
// newInstantaneousStateVectorCar.set(i++, analyzer.getCount(linkId, bin));
// }
// }
// }
// newInstantaneousStateVector = newInstantaneousStateVectorCar;
// }
//
// // pt
// if (this.ptOccupancyAnalyzer != null) {
// final Vector newInstantaneousStateVectorPT = new Vector(
// this.relevantStopIds.size() * this.timeDiscretization.getBinCnt());
// int i = 0;
// for (Id<TransitStopFacility> stopId : this.relevantStopIds) {
// for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
// newInstantaneousStateVectorPT.set(i++,
// this.ptOccupancyAnalyzer.getCount(stopId, bin));
// }
// }
// if (newInstantaneousStateVector != null) {
// newInstantaneousStateVector = Vector.concat(newInstantaneousStateVector,
// newInstantaneousStateVectorPT);
// } else {
// newInstantaneousStateVector = newInstantaneousStateVectorPT;
// }
// }
//
// /*
// * (2) Add instantaneous state vector to the list of past state vectors
// * and ensure that the size of this list is equal to what the memory
// * parameter prescribes.
// */
// this.stateList.addFirst(newInstantaneousStateVector);
// while (this.stateList.size() < this.memory) {
// this.stateList.addFirst(newInstantaneousStateVector);
// }
// while (this.stateList.size() > this.memory) {
// this.stateList.removeLast();
// }
//
// /*
// * (3) Inform the TrajectorySampler that one iteration has been
// * completed and provide the resulting state.
// */
// this.trajectorySampler.afterIteration(this.newState(this.population));
// }
