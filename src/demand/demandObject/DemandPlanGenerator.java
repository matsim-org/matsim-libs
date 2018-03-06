package demand.demandObject;

import java.util.Collection;

import demand.offer.Offer;

public interface DemandPlanGenerator {

	public DemandPlan createDemandPlan(Collection<Offer> offers);
	public void setDemandObject(DemandObject demandObject);
}
