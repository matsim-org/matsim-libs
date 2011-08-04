package playground.mzilske.freight;

import java.util.Collection;


public interface TSPPlanBuilder {

	public abstract TSPPlan buildPlan(Collection<TSPContract> contracts, TSPCapabilities tspCapabilities);

}