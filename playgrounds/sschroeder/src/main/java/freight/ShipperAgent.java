package freight;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.api.Contract;
import playground.mzilske.freight.TSPContract;

import java.util.List;

public interface ShipperAgent {
	
	public abstract Id getId();

	public abstract List<TSPContract> createTSPContracts();

	public abstract void scoreSelectedPlan();

	public abstract void informTSPContractAccept(Contract contract);

	public abstract void informTSPContractCanceled(Contract contract);

}