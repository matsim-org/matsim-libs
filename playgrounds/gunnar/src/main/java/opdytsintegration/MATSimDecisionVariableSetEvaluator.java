package opdytsintegration;

import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;
import optdyts.algorithms.DecisionVariableSetEvaluator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import floetteroed.utilities.DynamicData;
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

	private int startTime_s = 0;

	private int binSize_s = 3600;

	private int binCnt = 24;

	private int memory = 1;

	private boolean averageMemory = false;

	// RUNTIME VARIABLES

	private LinkOutflowCollectingEventHandler handler = null;

	private SortedSet<Id<Link>> sortedLinkIds = null;

	private final LinkedList<DynamicData<Id<Link>>> dataList = new LinkedList<DynamicData<Id<Link>>>();

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
	 * The earliest time (in seconds) at which the simulated conditions in
	 * MATSim affect the evaluation of a decision variable. Setting this
	 * parameter tightly may save a lot of computer memory.
	 * <p>
	 * The corresponding end time (exclusive, in seconds, also recommended to be
	 * set tightly) is computed as
	 * <code>getStartTime() + getBinSize_s() * getBinCnt()</code>.
	 */
	public void setStartTime_s(final int startTime_s) {
		this.startTime_s = startTime_s;
	}

	public int getStartTime_s() {
		return startTime_s;
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

	private DynamicData<Id<Link>> newDynamicData() {
		final DynamicData<Id<Link>> result = new DynamicData<Id<Link>>(
				this.startTime_s, this.binSize_s, this.binCnt);
		this.dataList.addFirst(result);
		while (this.dataList.size() < this.memory) {
			this.dataList.addFirst(result);
		}
		while (this.dataList.size() > this.memory) {
			this.dataList.removeLast();
		}
		return result;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		if (!this.isInitialized) {
			this.sortedLinkIds = new TreeSet<Id<Link>>(event.getControler()
					.getScenario().getNetwork().getLinks().keySet());
			this.handler = new LinkOutflowCollectingEventHandler(
					this.newDynamicData());
			event.getControler().getEvents().addHandler(this.handler);
			this.evaluator.initialize();
			this.isInitialized = true;
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		final Vector newStateVector;
		if (this.averageMemory) {
			newStateVector = new Vector(this.sortedLinkIds.size()
					* this.dataList.get(0).getBinCnt());
			for (DynamicData<Id<Link>> data : this.dataList) {
				int i = 0;
				for (Id<Link> id : this.sortedLinkIds) {
					for (int bin = 0; bin < data.getBinCnt(); bin++) {
						newStateVector.add(i++, data.getBinValue(id, bin)
								/ this.memory);
					}
				}
			}
		} else {
			newStateVector = new Vector(this.sortedLinkIds.size()
					* this.dataList.get(0).getBinCnt() * this.memory);
			int i = 0;
			for (DynamicData<Id<Link>> data : this.dataList) {
				for (Id<Link> id : this.sortedLinkIds) {
					for (int bin = 0; bin < data.getBinCnt(); bin++) {
						newStateVector.add(i++, data.getBinValue(id, bin));
					}
				}
			}
		}
		final X newState = this.stateFactory.newState(event.getControler()
				.getScenario().getPopulation(), newStateVector,
				this.evaluator.getCurrentDecisionVariable());
		this.evaluator.afterIteration(newState);

		this.handler.setDynamicData(this.newDynamicData());
	}
}
