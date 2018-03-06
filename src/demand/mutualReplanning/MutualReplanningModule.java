package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import demand.decoratedLSP.LSPDecorator;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjects;

public abstract class MutualReplanningModule implements ReplanningListener{

	protected Collection<LSPDecorator> lsps;
	protected Collection<DemandObject> demandObjects;
	
	public MutualReplanningModule(Collection<LSPDecorator> lsps, Collection<DemandObject> demandObjects) {
		this.lsps = lsps;
		this.demandObjects = demandObjects;
	}
	
	public void notifyReplanning(ReplanningEvent event) {
		replan(event);
	}
	
	public void replan(ReplanningEvent arg0) {
		replanLSPs(arg0);
		replanDemandObjects(arg0, lsps);
	}
	
	abstract void replanLSPs(ReplanningEvent event);
	
	abstract void replanDemandObjects(ReplanningEvent event, Collection<LSPDecorator> lsps);
	
}
	

