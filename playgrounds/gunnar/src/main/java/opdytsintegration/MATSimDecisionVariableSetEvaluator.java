package opdytsintegration;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import optdyts.DecisionVariable;
import optdyts.ObjectiveFunction;
import optdyts.SimulatorState;
import optdyts.algorithms.DecisionVariableSetEvaluator;
import optdyts.surrogatesolutions.TransitionSequence;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
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
 */
public class MATSimDecisionVariableSetEvaluator<X extends SimulatorState<X>, U extends DecisionVariable>
		implements IterationStartsListener, IterationEndsListener {

	// -------------------- MEMBERS --------------------

	// CONSTANTS

	private final DecisionVariableSetEvaluator<X, U> evaluator;

	private final MATSimStateFactory<X, U> stateFactory;

	// PARAMETERS

	private int startTime_s = 0;

	private int binSize_s = 3600;

	private int binCnt = 24;

	// RUNTIME VARIABLES

	private SortedSet<Id<Link>> sortedLinkIds = null;

	private DynamicData<Id<Link>> data = null;

	// -------------------- CONSTRUCTION --------------------

	public MATSimDecisionVariableSetEvaluator(final Set<U> decisionVariables,
			final ObjectiveFunction<X> objectiveFunction,
			final double transitionNoiseVarianceScale,
			final double convergenceNoiseVarianceScale,
			final MATSimStateFactory<X, U> stateFactory) {
		this.evaluator = new DecisionVariableSetEvaluator<X, U>(
				decisionVariables, objectiveFunction,
				transitionNoiseVarianceScale, convergenceNoiseVarianceScale);
		this.stateFactory = stateFactory;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public int getStartTime_s() {
		return startTime_s;
	}

	public void setStartTime_s(final int startTime_s) {
		this.startTime_s = startTime_s;
	}

	public int getBinSize_s() {
		return binSize_s;
	}

	public void setBinSize_s(final int binSize_s) {
		this.binSize_s = binSize_s;
	}

	public int getBinCnt() {
		return binCnt;
	}

	public void setBinCnt(final int binCnt) {
		this.binCnt = binCnt;
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	private boolean isInitialized = false;

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {

		if (!this.isInitialized) {

			this.data = new DynamicData<Id<Link>>(this.startTime_s,
					this.binSize_s, this.binCnt);
			this.sortedLinkIds = new TreeSet<Id<Link>>(event.getControler()
					.getScenario().getNetwork().getLinks().keySet());
			event.getControler().getEvents()
					.addHandler(new LinkEnterEventHandler() {
						@Override
						public void reset(final int iteration) {
						}

						@Override
						public void handleEvent(final LinkEnterEvent event) {
							final int bin = Math.min(
									data.bin((int) event.getTime()),
									data.getBinCnt() - 1);
							data.add(event.getLinkId(), bin, 1.0);
						}
					});

			this.evaluator.initialize();

			this.isInitialized = true;
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		final Vector newStateVector = new Vector(this.sortedLinkIds.size()
				* this.data.getBinCnt());
		int i = 0;
		for (Id<Link> id : this.sortedLinkIds) {
			for (int bin = 0; bin < this.data.getBinCnt(); bin++) {
				newStateVector.set(i++, this.data.getBinValue(id, bin));
			}
		}

		final X newState = this.stateFactory.newState(event.getControler()
				.getScenario().getPopulation(), newStateVector,
				this.evaluator.getCurrentDecisionVariable());
		this.evaluator.afterIteration(newState);

		this.data.clear();
	}

	// TODO experimental
	public Map<U, TransitionSequence<X, U>> getDecisionVariable2TransitionSequence() {
		return this.evaluator.getDecisionVariable2TransitionSequence();
	}
}
