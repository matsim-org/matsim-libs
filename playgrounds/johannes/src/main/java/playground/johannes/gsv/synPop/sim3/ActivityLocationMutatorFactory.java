package playground.johannes.gsv.synPop.sim3;

import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.synpop.gis.DataPool;

import java.util.Random;

public class ActivityLocationMutatorFactory implements MutatorFactory {

	private final String blacklist;
	
	private final DataPool dataPool;
	
	private final Random random;
	
	public ActivityLocationMutatorFactory(DataPool dataPool, String blacklist, Random random) {
		this.dataPool = dataPool;
		this.blacklist = blacklist;
		this.random = random;
	}
	
	@Override
	public Mutator newInstance() {
		Random rnd = new XORShiftRandom(random.nextLong());
		return new RandomSelector(new ActivityLocationMutator(dataPool, rnd, blacklist), rnd);
	}

}
