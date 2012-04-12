/**
 *
 */
package playground.yu.parameterSearch;

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

	private final NelderMead optimizer;
	private final LLhParamFct objectiveFunction;

	public NelderMeadSearcher(String configFilename) {
		objectiveFunction = new LLhParamFct(configFilename);

		optimizer = new NelderMead();
		optimizer.setMaxIterations(objectiveFunction.getMaxIterations());
		optimizer.setMaxEvaluations(objectiveFunction.getMaxEvaluations());
		optimizer.setConvergenceChecker(new SimpleScalarValueChecker(
				objectiveFunction.getRelativeThreshold(), objectiveFunction
						.getAbsoluteThreshold()));
	}

	public void run() throws OptimizationException,
			FunctionEvaluationException, IllegalArgumentException {
		optimizer.optimize(objectiveFunction, GoalType.MAXIMIZE,
				objectiveFunction.getFirstPoint());
	}

	/**
	 * @param args
	 * @throws IllegalArgumentException
	 * @throws FunctionEvaluationException
	 * @throws OptimizationException
	 */
	public static void main(String[] args) throws OptimizationException,
			FunctionEvaluationException, IllegalArgumentException {
		new NelderMeadSearcher(args[0]).run();
	}
}
