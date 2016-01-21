package vind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.Transition;
import floetteroed.opdyts.trajectorysampling.TransitionSequence;
import floetteroed.utilities.math.LeastAbsoluteDeviations;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FinalSensitivitiesAnalyzer {

//	public final double vOpt;
//
//	public final double wOpt;
//
//	public <U extends DecisionVariable> FinalSensitivitiesAnalyzer(
//			final Map<U, TransitionSequence<U>> decisionVariable2transitionSequence,
//			final U finalDecisionVariable, final List<Double> finalWeights) {
//
//		final List<Transition<U>> transitions = new ArrayList<>();
//		final List<Double> alphas = new ArrayList<>();
//		for (Map.Entry<U, TransitionSequence<U>> entry : decisionVariable2transitionSequence
//				.entrySet()) {
//			transitions.addAll(entry.getValue().getTransitions());
//			if (finalDecisionVariable.equals(entry.getKey())) {
//				alphas.addAll(finalWeights);
//			} else {
//				for (int i = 0; i < entry.getValue().size(); i++) {
//					alphas.add(0.0);
//				}
//			}
//		}
//
//		final SurrogateObjectiveFunction<U> surrObjFct = new SurrogateObjectiveFunction<>(
//				transitions, Double.NaN, Double.NaN);
//		final Vector dQdAlpha = surrObjFct.dInterpolObjFctVal_dAlpha(new Vector(alphas));
//		final Vector dEquilibriumGapdAlpha = surrObjFct
//				.dEquilibriumGap_dAlpha(new Vector(alphas));
//		final Vector dUniformityGapdAlpha = surrObjFct
//				.dUniformityGap_dAlpha(new Vector(alphas));
//
//		final LeastAbsoluteDeviations lad = new LeastAbsoluteDeviations();
//		lad.setLowerBounds(0.0, 0.0);
//		for (int i = 0; i < alphas.size(); i++) {
//			lad.add(new Vector(dEquilibriumGapdAlpha.get(i),
//					dUniformityGapdAlpha.get(i)), -dQdAlpha.get(i));
//		}
//		lad.solve();
//		final Vector result = lad.getCoefficients();
//		this.vOpt = result.get(0);
//		this.wOpt = result.get(1);
//	}
}
