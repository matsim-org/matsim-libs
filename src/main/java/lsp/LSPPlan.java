package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.population.BasicPlan;

public interface LSPPlan extends BasicPlan{
	
	public void addSolution (LogisticsSolution solution);
	
	public Collection<LogisticsSolution> getSolutions();

	public ShipmentAssigner getAssigner();
	
	public void setAssigner(ShipmentAssigner assigner);

	public void setLSP(LSP lsp);
	
	public LSP getLsp();

}
