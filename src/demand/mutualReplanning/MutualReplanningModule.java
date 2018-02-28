package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;
import lsp.LSP;

public abstract class MutualReplanningModule implements ReplanningListener{

	protected Collection<LSP> lsps;
	protected Collection<DemandObject> demandObjects;
	
	public MutualReplanningModule(Collection<LSP> lsps, DemandObjects demandObjects ) {
		this.lsps = lsps;
		this.demandObjects = demandObjects.getDemandObjects().values();
	}
	
	public void notifyReplanning(ReplanningEvent event) {
		replan(event);
	}
	
	public void replan(ReplanningEvent arg0) {
		replanLSPs(arg0);
		replanDemandObjects(arg0);
	}
	
	abstract void replanLSPs(ReplanningEvent event);
	
	abstract void replanDemandObjects(ReplanningEvent event);
	
}
	

