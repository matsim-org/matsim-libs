package opdytsintegration;

import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;
import optdyts.algorithms.DecisionVariableSetEvaluator;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import floetteroed.utilities.math.Vector;

/**
 * Identifies the approximately best out of a set of decision variables.
 * 
 * @author Gunnar Flötteröd
 *
 * @param <X>
 *            the simulator state type
 * @param <U>
 *            the decision variable type
 * 
 * @see SimulatorState
 * @see DecisionVariable
 * @see DecisionVariableSetEvaluator
 */
public class MATSimDecisionVariableSetEvaluator<X extends SimulatorState, U extends DecisionVariable>
		implements IterationStartsListener, IterationEndsListener {

	// -------------------- MEMBERS --------------------

	// CONSTANTS

	private final DecisionVariableSetEvaluator<X, U> evaluator;

	private final MATSimStateFactory<X, U> stateFactory;

	// PARAMETERS

	private int binSize_s = 3600;

	private int startBin = 0;

	private int binCnt = 24;

	private int memory = 1;

	private boolean averageMemory = false;

	// RUNTIME VARIABLES

	private VolumesAnalyzer volumesAnalyzer;

	private SortedSet<Id<Link>> sortedLinkIds = null;

	private final LinkedList<Vector> stateList = new LinkedList<Vector>();

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @see DecisionVariableSetEvaluator#DecisionVariableSetEvaluator
	 */
	public MATSimDecisionVariableSetEvaluator(final Set<U> decisionVariables,
			final ObjectiveFunction<X> objectiveFunction,
			final MATSimStateFactory<X, U> stateFactory,
			final int minimumAverageIterations, final double maximumRelativeGap) {
		this.evaluator = new DecisionVariableSetEvaluator<X, U>(
				decisionVariables, objectiveFunction, minimumAverageIterations,
				maximumRelativeGap);
		this.stateFactory = stateFactory;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	/**
	 * The vector representation of MATSim's instantaneous state omits some
	 * memory effects, meaning that it is not precise. Setting the memory
	 * parameter to more than one (its default value) will increase the
	 * precision at the cost of a larger computer memory usage. Values larger
	 * than 10 do probably not make sense.
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
	 * Setting this to true saves computer memory by averaging the process
	 * memory instead of completely keeping track of it. This only works if the
	 * right (and problem-specific!) memory length is chosen.
	 * 
	 * @param averageMemory
	 */
	public void setAverageMemory(final boolean averageMemory) {
		this.averageMemory = averageMemory;
	}

	public boolean getAverageMemory() {
		return this.averageMemory;
	}

	/**
	 * Where to write logging information.
	 */
	public void setLogFileName(final String logFileName) {
		this.evaluator.setLogFileName(logFileName);
	}

	public String getLogFileName() {
		return this.evaluator.getLogFileName();
	}

	/**
	 * The time discretization (in seconds) according to which the simulated
	 * conditions in MATSim affect the evaluation of a decision variable.
	 * 
	 * @param binSize_s
	 */
	public void setBinSize_s(final int binSize_s) {
		this.binSize_s = binSize_s;
	}

	public int getBinSize_s() {
		return binSize_s;
	}


	/**
	 * The earliest time bin in which the simulated conditions in MATSim affect
	 * the evaluation of a decision variable. Setting this parameter tightly may
	 * save a lot of computer memory.
	 */
	public void setStartBin(final int startBin) {
		this.startBin = startBin;
	}

	public int getStartBin() {
		return this.startBin;
	}
	
	/**
	 * The number of time bins within which the simulated conditions in MATSim
	 * affect the evaluation of a decision variable.
	 * 
	 * @param binCnt
	 */
	public void setBinCnt(final int binCnt) {
		this.binCnt = binCnt;
	}

	public int getBinCnt() {
		return binCnt;
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	private boolean isInitialized = false;

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		if (!this.isInitialized) {
			this.sortedLinkIds = new TreeSet<Id<Link>>(event.getControler()
					.getScenario().getNetwork().getLinks().keySet());

			this.volumesAnalyzer = new VolumesAnalyzer(this.binSize_s,
					this.binSize_s * (this.startBin + this.binCnt), event
							.getControler().getScenario().getNetwork());
			event.getControler().getEvents().addHandler(this.volumesAnalyzer);

			this.evaluator.initialize();
			this.isInitialized = true;
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		{
			final Vector newInstantaneousState = new Vector(
					this.sortedLinkIds.size() * this.binCnt);
			int i = 0;
			for (Id<Link> linkId : this.sortedLinkIds) {
				final int[] volumes = this.volumesAnalyzer
						.getVolumesForLink(linkId);
				if (volumes == null) {
					for (int j = this.startBin; j < (this.startBin + this.binCnt); j++) {
						newInstantaneousState.set(i++, 0.0);
					}
				} else {
					for (int j = this.startBin; j < (this.startBin + this.binCnt); j++) {
						newInstantaneousState.set(i++, volumes[j]);
					}
				}
			}
			this.stateList.addFirst(newInstantaneousState);
			while (this.stateList.size() < this.memory) {
				this.stateList.addFirst(newInstantaneousState);
			}
			while (this.stateList.size() > this.memory) {
				this.stateList.removeLast();
			}
		}

		{
			final Vector newSummaryState;
			if (this.averageMemory) {
				newSummaryState = this.stateList.getFirst().copy();
				for (int i = 1; i < this.memory; i++) {
					newSummaryState.add(this.stateList.get(i));
				}
				newSummaryState.mult(1.0 / this.memory);
			} else {
				newSummaryState = Vector.concat(this.stateList);
				System.out.println(newSummaryState);
			}
			final X newState = this.stateFactory.newState(event.getControler()
					.getScenario().getPopulation(), newSummaryState,
					this.evaluator.getCurrentDecisionVariable());
			this.evaluator.afterIteration(newState);
		}
	}
}
