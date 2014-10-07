package playground.johannes.gsv.synPop.sim3;

import java.util.Random;

import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

public class ActivityLocationMutatorFactory implements MutatorFactory {

	private final String blacklist;
	
	private final DataPool facilities;
	
	private final Random random;
	
	public ActivityLocationMutatorFactory(DataPool facilities, String blacklist, Random random) {
		this.facilities = facilities;
		this.blacklist = blacklist;
		this.random = random;
	}
	
	@Override
	public Mutator newInstance() {
		Random rnd = new XORShiftRandom(random.nextLong());
		return new RandomSelector(new ActivityLocationMutator(facilities, rnd, blacklist), rnd);
	}

}
