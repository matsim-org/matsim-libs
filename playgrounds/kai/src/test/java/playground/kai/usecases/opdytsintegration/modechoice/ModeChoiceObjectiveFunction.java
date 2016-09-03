package playground.kai.usecases.opdytsintegration.modechoice;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Kai Nagel based on Gunnar Flötteröd
 * 
 */
public class ModeChoiceObjectiveFunction implements ObjectiveFunction {

	@Override public double value(SimulatorState state) {
//		ModeChoiceState modeChoiceState = (ModeChoiceState) state;
//		final double avgIncome = (1.0 - this.betaPayScale * modeChoiceState.getBetaPay()) * 100.0;
//		return ((-1.0) * (modeChoiceState.getAvgScore() + avgIncome));
		
		throw new RuntimeException("needs to be implemented.  closeness to data") ;
	}

}
