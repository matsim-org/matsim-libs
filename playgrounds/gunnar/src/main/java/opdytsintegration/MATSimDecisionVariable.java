package opdytsintegration;

import optdyts.DecisionVariable;

import org.matsim.core.controler.events.BeforeMobsimEvent;

/**
 * Represents a MATSim decision variable.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public interface MATSimDecisionVariable extends DecisionVariable {

	/**
	 * Implements this decision variable at the start of the iteration indicated
	 * by event.
	 * 
	 * @param event
	 *            representing the start of the iteration in which this decision
	 *            variable is to be implemented
	 */
	public void implement(final BeforeMobsimEvent event);

}
