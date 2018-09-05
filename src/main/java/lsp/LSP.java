package lsp;

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;

import lsp.resources.Resource;
import lsp.replanning.LSPReplanner;
import lsp.scoring.LSPScorer;
import lsp.shipment.LSPShipment;

public interface LSP extends HasPlansAndId<LSPPlan,LSP>{

	public Id<LSP> getId();
	
	public Collection<LSPShipment> getShipments();
	
	public void scheduleSoultions();
	
	public ArrayList<LSPPlan> getPlans();
	
	public Collection<Resource> getResources();
	
	public LSPPlan getSelectedPlan();
	
	public void setSelectedPlan(LSPPlan plan);
	
	public void scoreSelectedPlan();
	
	public LSPReplanner getReplanner();
	
	public void assignShipmentToLSP(LSPShipment shipment);
	
	public LSPScorer getScorer();
	
	public void setScorer(LSPScorer scorer);
	
	public void setReplanner(LSPReplanner replanner);
	
	public SolutionScheduler getScheduler();
}    