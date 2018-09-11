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
	
	/**
	 * @return
	 *
	 * ok
	 */
	public Id<LSP> getId();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed
	 */
	public Collection<LSPShipment> getShipments();
	
	/**
	 * ok (behavioral method)
	 */
	public void scheduleSoultions();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	public ArrayList<LSPPlan> getPlans();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	public Collection<Resource> getResources();
	
	/**
	 * @return
	 *
	 * probably ok (at some point we either need to expose the next step, or the whole plan)
	 */
	public LSPPlan getSelectedPlan();
	
	/**
	 * @param plan
	 *
	 * yy does it even make sense to expose this (should internally do this).  But probably easy to fix.
	 */
	public void setSelectedPlan(LSPPlan plan);
	
	/**
	 * ok (behavioral method)
	 */
	public void scoreSelectedPlan();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	public LSPReplanner getReplanner();
	
	/**
	 * @param shipment
	 *
	 * ok (LSP needs to be told that it is responsible for shipment)
	 */
	public void assignShipmentToLSP(LSPShipment shipment);
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	public LSPScorer getScorer();
	
	/**
	 * @param scorer
	 *
	 * yyyy does it make sense to expose this (implies that scorer can be changed during iterations)?
	 */
	public void setScorer(LSPScorer scorer);
	
	/**
	 * @param replanner
	 *
	 * yyyy does it make sense to expose this (implies that replanner can be changed during iterations)?
	 */
	public void setReplanner(LSPReplanner replanner);
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	public SolutionScheduler getScheduler();
}    
