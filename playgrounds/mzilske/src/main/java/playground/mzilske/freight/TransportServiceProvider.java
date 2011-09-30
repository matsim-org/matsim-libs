package playground.mzilske.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.api.Actor;

public interface TransportServiceProvider extends Actor{

	public abstract Collection<TSPContract> getContracts();

	public abstract Collection<TSPPlan> getPlans();

	public abstract TSPPlan getSelectedPlan();

	public abstract void setSelectedPlan(TSPPlan selectedPlan);

	public abstract Id getId();

	public abstract TSPCapabilities getTspCapabilities();

	public abstract void setTspCapabilities(TSPCapabilities tspCapabilities);

	public abstract void setKnowledge(TSPKnowledge knowledge);

	public abstract TSPKnowledge getKnowledge();
	
	public abstract Collection<TSPContract> getNewContracts();
	
	public abstract Collection<TSPContract> getExpiredContracts();

}