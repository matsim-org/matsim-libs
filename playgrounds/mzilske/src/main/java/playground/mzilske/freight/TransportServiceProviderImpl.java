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
public class TransportServiceProviderImpl implements TransportServiceProvider {
	
	private Collection<TSPPlan> plans = new ArrayList<TSPPlan>();
	
	private TSPPlan selectedPlan = null;
	
	private Id id;
	
	private TSPCapabilities tspCapabilities;
	
	private TSPKnowledge knowledge;
	
	private Collection<TSPContract> contracts = new ArrayList<TSPContract>();
	
	private Collection<TSPContract> expiredContracts = new ArrayList<TSPContract>();
	
	private Collection<TSPContract> newContracts = new ArrayList<TSPContract>();
	
	public TransportServiceProviderImpl(Id id) {
		super();
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#getContracts()
	 */
	@Override
	public Collection<TSPContract> getContracts() {
		return contracts;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#getPlans()
	 */
	@Override
	public Collection<TSPPlan> getPlans() {
		return plans;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#getSelectedPlan()
	 */
	@Override
	public TSPPlan getSelectedPlan() {
		return selectedPlan;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#setSelectedPlan(playground.mzilske.freight.TSPPlan)
	 */
	@Override
	public void setSelectedPlan(TSPPlan selectedPlan) {
		this.selectedPlan = selectedPlan;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#getId()
	 */
	@Override
	public Id getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#getTspCapabilities()
	 */
	@Override
	public TSPCapabilities getTspCapabilities() {
		return tspCapabilities;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#setTspCapabilities(playground.mzilske.freight.TSPCapabilities)
	 */
	@Override
	public void setTspCapabilities(TSPCapabilities tspCapabilities) {
		this.tspCapabilities = tspCapabilities;
	}
	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#setKnowledge(playground.mzilske.freight.TSPKnowledge)
	 */
	@Override
	public void setKnowledge(TSPKnowledge knowledge) {
		this.knowledge = knowledge;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.TransportServiceProvider#getKnowledge()
	 */
	@Override
	public TSPKnowledge getKnowledge() {
		return knowledge;
	}

	public String toString(){
		return "[id="+id+"][numberOfPlans="+plans.size()+"]";
	}

	@Override
	public Collection<TSPContract> getNewContracts() {
		return newContracts;
	}

	@Override
	public Collection<TSPContract> getExpiredContracts() {
		return expiredContracts;
	}
}
