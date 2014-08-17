package playground.johannes.gsv.synPop.sim2;

import java.util.Random;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;

public class ActivityLocationMutatorFactory implements MutatorFactory {

	private final String blacklist;
	
	private final ActivityFacilities facilities;
	
	private final Random random;
	
	public ActivityLocationMutatorFactory(ActivityFacilities facilities, String blacklist, Random random) {
		this.facilities = facilities;
		this.blacklist = blacklist;
		this.random = random;
	}
	
	@Override
	public Mutator newInstance() {
		return new ActivityLocationMutator(facilities, random, blacklist);
	}

}
