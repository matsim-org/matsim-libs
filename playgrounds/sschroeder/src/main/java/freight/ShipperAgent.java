package freight;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Contract;

import playground.mzilske.freight.TSPContract;

public interface ShipperAgent {
	
	public abstract Id getId();

	public abstract List<TSPContract> createTSPContracts();

	public abstract void scoreSelectedPlan();

	public abstract void informTSPContractAccept(Contract contract);

	public abstract void informTSPContractCanceled(Contract contract);

}