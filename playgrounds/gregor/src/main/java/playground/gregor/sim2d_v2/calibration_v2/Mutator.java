package playground.gregor.sim2d_v2.calibration_v2;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

public class Mutator {

	public void mutate(double [] params, Tuple<Double, Double>[] ranges){
		int pos = MatsimRandom.getRandom().nextInt(params.length);
		double mut = 1+(MatsimRandom.getRandom().nextDouble()-.5)/10;
		double proposed = params[pos]*mut;
		if (proposed >= ranges[pos].getFirst() && proposed <= ranges[pos].getSecond()) {
			params[pos] = proposed;
		}
	}
}
