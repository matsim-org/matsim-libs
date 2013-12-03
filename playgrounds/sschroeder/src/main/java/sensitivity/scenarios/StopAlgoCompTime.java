package sensitivity.scenarios;

import java.util.Collection;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.listener.AlgorithmEndsListener;
import jsprit.core.algorithm.listener.AlgorithmStartsListener;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;

import org.apache.commons.lang.time.StopWatch;


public class StopAlgoCompTime implements AlgorithmStartsListener, AlgorithmEndsListener{

	public final StopWatch stopWatch = new StopWatch();
	
	@Override
	public void informAlgorithmEnds(VehicleRoutingProblem problem,Collection<VehicleRoutingProblemSolution> solutions) {
		stopWatch.stop();
	}

	@Override
	public void informAlgorithmStarts(VehicleRoutingProblem problem,VehicleRoutingAlgorithm algorithm,Collection<VehicleRoutingProblemSolution> solutions) {
		stopWatch.start();
	}

}
