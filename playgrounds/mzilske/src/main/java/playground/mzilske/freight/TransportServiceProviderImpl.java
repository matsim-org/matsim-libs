/**
 * 
 */
package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;

/**
 * @author schroeder
 *
 */
public class TransportServiceProviderImpl {
	
	private Collection<TSPPlan> plans = new ArrayList<TSPPlan>();
	
	private TSPPlan selectedPlan = null;
	
	private Id id;
	
	private TSPCapabilities tspCapabilities;
	
	private TSPKnowledge knowledge;
	
	private Collection<TSPContract> contracts = new ArrayList<TSPContract>();
	
	public TransportServiceProviderImpl(Id id) {
		super();
		this.id = id;
	}

	public Collection<TSPContract> getContracts() {
		return contracts;
	}

	public Collection<TSPPlan> getPlans() {
		return plans;
	}

	public TSPPlan getSelectedPlan() {
		return selectedPlan;
	}

	public void setSelectedPlan(TSPPlan selectedPlan) {
		this.selectedPlan = selectedPlan;
	}

	public Id getId() {
		return id;
	}

	public TSPCapabilities getTspCapabilities() {
		return tspCapabilities;
	}

	public void setTspCapabilities(TSPCapabilities tspCapabilities) {
		this.tspCapabilities = tspCapabilities;
	}
	
	public void setKnowledge(TSPKnowledge knowledge) {
		this.knowledge = knowledge;
	}

	public TSPKnowledge getKnowledge() {
		return knowledge;
	}

	public String toString(){
		return "[id="+id+"][numberOfPlans="+plans.size()+"]";
	}
}
