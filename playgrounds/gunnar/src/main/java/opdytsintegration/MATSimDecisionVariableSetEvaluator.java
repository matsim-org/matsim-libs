package opdytsintegration;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;
import opdytsintegration.car.LinkOccupancyAnalyzer;
import opdytsintegration.pt.PTOccupancyAnalyser;
import opdytsintegration.utils.TimeDiscretization;

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

	private int memory = 1;

	private boolean averageMemory = false;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Population population;

	// created during runtime:

	// must be linked to ensure a unique iteration ordering
	private LinkedHashSet<Id<Link>> relevantLinkIds;

	// must be linked to ensure a unique iteration ordering
	private LinkedHashSet<Id<TransitStopFacility>> relevantStopIds;

	private LinkOccupancyAnalyzer carOccupancyAnalyzer = null;

	private PTOccupancyAnalyser ptOccupancyAnalyzer = null;

	private LinkedList<Vector> stateList = null;

	private MATSimState finalState = null;

	// -------------------- CONSTRUCTION --------------------

	public MATSimDecisionVariableSetEvaluator(final TrajectorySampler<U> trajectorySampler,
			final MATSimStateFactory<U> stateFactory, final TimeDiscretization timeDiscretization) {
		this.trajectorySampler = trajectorySampler;
		this.stateFactory = stateFactory;
		this.timeDiscretization = timeDiscretization;
		this.relevantLinkIds = null;
		this.relevantStopIds = null;
	}

	public void enableCarTraffic(final Collection<Id<Link>> relevantLinkIds) {
		this.relevantLinkIds = new LinkedHashSet<>(relevantLinkIds);
	}

	public void enablePT(final Collection<Id<TransitStopFacility>> relevantStopIds) {
		this.relevantStopIds = new LinkedHashSet<>(relevantStopIds);
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

		if ((this.relevantLinkIds != null) && (this.relevantLinkIds.size() > 0)) {
			this.carOccupancyAnalyzer = new LinkOccupancyAnalyzer(this.timeDiscretization, this.relevantLinkIds);
			this.eventsManager.addHandler(this.carOccupancyAnalyzer);
		}

		if (this.relevantStopIds != null && (relevantStopIds.size() > 0)) {
			this.ptOccupancyAnalyzer = new PTOccupancyAnalyser(this.timeDiscretization, this.relevantStopIds);
			this.eventsManager.addHandler(this.ptOccupancyAnalyzer);
		}

		this.trajectorySampler.initialize();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		/*
		 * (1) Extract the instantaneous state vector.
		 */

		Vector newInstantaneousStateVector = null;

		// car
		if (this.carOccupancyAnalyzer != null) {
			final Vector newInstantaneousStateVectorCar = new Vector(
					this.relevantLinkIds.size() * this.timeDiscretization.getBinCnt());
			int i = 0;
			for (Id<Link> linkId : this.relevantLinkIds) {
				for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
					newInstantaneousStateVectorCar.set(i++, this.carOccupancyAnalyzer.getCount(linkId, bin));
				}
			}
			newInstantaneousStateVector = newInstantaneousStateVectorCar;
		}

		// pt
		if (this.ptOccupancyAnalyzer != null) {
			final Vector newInstantaneousStateVectorPT = new Vector(
					this.relevantStopIds.size() * this.timeDiscretization.getBinCnt());
			int i = 0;
			for (Id<TransitStopFacility> stopId : this.relevantStopIds) {
				for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
					newInstantaneousStateVectorPT.set(i++, this.ptOccupancyAnalyzer.getCount(stopId, bin));
				}
			}
			if (newInstantaneousStateVector != null) {
				newInstantaneousStateVector = Vector.concat(newInstantaneousStateVector, newInstantaneousStateVectorPT);
			} else {
				newInstantaneousStateVector = newInstantaneousStateVectorPT;
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
