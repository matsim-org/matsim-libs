/**
 *
 */
package playground.yu.parameterSearch.NelderMead;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleScalarValueChecker;
import org.apache.commons.math.optimization.direct.NelderMead;

/**
 * An attempt to use Nelder-Mead Method to search the best parameters in scoring
 * function
 *
 * @author C
 *
 */
public class NelderMeadSearcher {

	/**
	 * @param args
	 * @throws IllegalArgumentException
	 * @throws FunctionEvaluationException
	 * @throws OptimizationException
	 */
	public static void main(String[] args) throws OptimizationException,
			FunctionEvaluationException, IllegalArgumentException {
		NelderMead optimizer = new NelderMead();
		optimizer.setMaxIterations(1000);
		optimizer.setMaxEvaluations(1000);
		/* path from (1,1) to (-6,0) */
		// optimizer.setStartConfiguration(new double[] { -7d, -1d });
		// optimizer.setStartConfiguration

		optimizer
				.setConvergenceChecker(new SimpleScalarValueChecker(0.01, 0.01));
		optimizer.optimize(new LLhParamFct(args[0]), GoalType.MAXIMIZE,
				new double[] { -6d, 0d });
	}
}
