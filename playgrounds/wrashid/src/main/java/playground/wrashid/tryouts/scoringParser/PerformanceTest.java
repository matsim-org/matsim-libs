package playground.wrashid.tryouts.scoringParser;

import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.utils.expr.Parser;
import org.matsim.utils.expr.SyntaxException;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

public class PerformanceTest {

	public static void main(String[] args) throws SyntaxException {

		int numberOfTests = 10000000;
		
			Timer timer = new Timer();
			timer.startTimer();

			Parser parser = new Parser("1+log(x)");
			double sum = 0;
			for (int i = 0; i < numberOfTests; i++) {
				
				parser.setVariable("x", i);
				double a = parser.parse();
				sum += a;
			}
			timer.endTimer();
			System.out.println(sum);
			timer.printMeasuredTime("parsed:");
		

		timer = new Timer();
		timer.startTimer();
		sum = 0;
		for (int i = 0; i < numberOfTests; i++) {
			double a = 1 + Math.log(i);
			sum += a;
		}
		timer.endTimer();
		System.out.println(sum);
		timer.printMeasuredTime("non-parsed:");

	}

}
