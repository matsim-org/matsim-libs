package optdyts.logging;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import optdyts.surrogatesolutions.SurrogateSolution;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface SearchStatistic<X extends SimulatorState, U extends DecisionVariable> {

	public String label();
	
	public String value(final SurrogateSolution<X, U> surrogateSolution);
	
}
