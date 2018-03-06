package demand.demandObject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;

import demand.decoratedLSP.LSPDecorator;
import lsp.LogisticsSolution;

public interface DemandPlan extends BasicPlan{
	
	public Double getScore();
	public void setScore(Double arg0);	
	public ShipperShipment getShipment();	
	public LSPDecorator getLsp();
	public Id<LogisticsSolution> getSolutionId();
	public DemandObject getDemandObject();
	public void setDemandObject(DemandObject demandObject);

}
