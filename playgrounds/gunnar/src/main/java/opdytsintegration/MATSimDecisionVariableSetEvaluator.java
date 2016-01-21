package opdytsintegration;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import com.google.inject.Inject;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;

/**
 * Identifies the approximately best out of a set of decision variables.
 * 
 * @author Gunnar Flötteröd
 * 
 * @see DecisionVariable
 *
 */
public class MATSimDecisionVariableSetEvaluator<U extends DecisionVariable>
		implements StartupListener, IterationEndsListener, ShutdownListener {

	// -------------------- MEMBERS --------------------

	private final TrajectorySampler<U> trajectorySampler;

	private final MATSimStateFactory<U> stateFactory;

	private final TimeDiscretization timeDiscretization;

	// must be linked to ensure a unique iteration ordering
	private LinkedHashSet<Id<Link>> relevantLinkIds = null;

	private int memory = 1;

	private boolean averageMemory = false;

	// created during runtime:

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Population population;

	@Inject
	private Network network;

	private OccupancyAnalyzer occupancyAnalyzer = null;

	// private SortedSet<Id<Link>> sortedLinkIds = null;

	private LinkedList<Vector> stateList = null;

	private MATSimState finalState = null;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @see MATSimStateFactory
	 */
	public MATSimDecisionVariableSetEvaluator(
			final TrajectorySampler<U> trajectorySampler,
			final MATSimStateFactory<U> stateFactory,
			final TimeDiscretization timeDiscretization,
			final Collection<Id<Link>> relevantLinkIds) {
		this.trajectorySampler = trajectorySampler;
		this.stateFactory = stateFactory;
		this.timeDiscretization = timeDiscretization;
		if (relevantLinkIds == null) {
			this.relevantLinkIds = null;
		} else {
			this.relevantLinkIds = new LinkedHashSet<>(relevantLinkIds);
		}
	}

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

	/**
	 * Where to write standard logging information.
	 */
	// public void setStandardLogFileName(final String logFileName) {
	// this.trajectorySampler.setStandardLogFileName(logFileName);
	// this.trajectorySampler.addStatistic(logFileName,
	// new TransientObjectiveFunctionValue<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new EquilibriumGapWeight<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new EquilibriumGap<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new UniformityGapWeight<U>());
	// this.trajectorySampler
	// .addStatistic(logFileName, new UniformityGap<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new SurrogateObjectiveFunctionValue<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new LastObjectiveFunctionValue<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new LastEquilibriumGap<U>());
	// this.trajectorySampler.addStatistic(logFileName, new
	// TotalMemory<U>());
	// this.trajectorySampler.addStatistic(logFileName, new
	// FreeMemory<U>());
	// this.trajectorySampler.addStatistic(logFileName, new MaxMemory<U>());
	// this.trajectorySampler.addStatistic(logFileName,
	// new LastDecisionVariable<U>());
	// }

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

		// this.sortedLinkIds = new TreeSet<Id<Link>>(this.network.getLinks()
		// .keySet());
		if (this.relevantLinkIds == null) {
			this.relevantLinkIds = new LinkedHashSet<>(this.network.getLinks()
					.keySet());
		}
		this.stateList = new LinkedList<Vector>();

		this.occupancyAnalyzer = new OccupancyAnalyzer(this.timeDiscretization,
				this.relevantLinkIds);
		this.eventsManager.addHandler(this.occupancyAnalyzer);

		this.trajectorySampler.initialize();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		/*
		 * (1) Extract the instantaneous state vector.
		 */
		final Vector newInstantaneousStateVector = new Vector(
				this.relevantLinkIds.size()
						* this.timeDiscretization.getBinCnt());
		int i = 0;
		for (Id<Link> linkId : this.relevantLinkIds) {
			for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
				newInstantaneousStateVector.set(i++,
						this.occupancyAnalyzer.getOccupancy_veh(linkId, bin));
			}
		}

		/*
		 * (2) Add instantaneous state vector to the list of past state vectors
		 * and ensure that the size of this list is equal to what the memory
		 * parameter prescribes.
		 */
		this.stateList.addFirst(newInstantaneousStateVector);
		while (this.stateList.size() < this.memory) {
			this.stateList.addFirst(newInstantaneousStateVector);
		}
		while (this.stateList.size() > this.memory) {
			this.stateList.removeLast();
		}

		/*
		 * (3) Inform the TrajectorySampler that one iteration has been
		 * completed and provide the resulting state.
		 */
		this.trajectorySampler.afterIteration(this.newState(this.population));
	}

	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		this.finalState = this.newState(this.population);
	}
}
