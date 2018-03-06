package demand.mutualReplanning;

import java.util.Collection;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import demand.decoratedLSP.LSPDecorator;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandPlan;

public interface DemandReplanner {
	public void replan(Collection<LSPDecorator> lsps, ReplanningEvent event);
	public void addStrategy(DemandPlanStrategyImpl strategy);
	public void setDemandObject(DemandObject demandObject);
	public DemandObject getDemandObject();
}
