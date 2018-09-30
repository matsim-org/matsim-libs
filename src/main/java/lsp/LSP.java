package lsp;

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;

import lsp.resources.Resource;
import lsp.replanning.LSPReplanner;
import lsp.scoring.LSPScorer;
import lsp.shipment.LSPShipment;
import org.matsim.core.controler.events.ReplanningEvent;

public interface LSP extends HasPlansAndId<LSPPlan,LSP>{
	
	/**
	 * @return
	 *
	 * ok
	 */
	Id<LSP> getId();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	Collection<LSPShipment> getShipments();
	
	/**
	 * ok (behavioral method)
	 */
	void scheduleSoultions();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
//	ArrayList<LSPPlan> getPlans();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
	Collection<Resource> getResources();
	
	/**
	 * @return
	 *
	 * probably ok (at some point we either need to expose the next step, or the whole plan)
	 */
//	LSPPlan getSelectedPlan();
	
	/**
	 * @param plan
	 *
	 * yy does it even make sense to expose this (should internally do this).  But probably easy to fix.
	 */
//	void setSelectedPlan( LSPPlan plan );
	
	/**
	 * ok (behavioral method)
	 */
	void scoreSelectedPlan();
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
//	LSPReplanner getReplanner();
	
	/**
	 * @param shipment
	 *
	 * ok (LSP needs to be told that it is responsible for shipment)
	 */
	void assignShipmentToLSP( LSPShipment shipment );
	
	void replan( ReplanningEvent arg0 );
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
//	LSPScorer getScorer();
	
	/**
	 * @param scorer
	 *
	 * yyyy does it make sense to expose this (implies that scorer can be changed during iterations)?
	 */
	void setScorer( LSPScorer scorer );
	
	/**
	 * @param replanner
	 *
	 * yyyy does it make sense to expose this (implies that replanner can be changed during iterations)?
	 */
	void setReplanner( LSPReplanner replanner );
	
	/**
	 * @return
	 *
	 * yyyy does this have to be exposed?
	 */
//	SolutionScheduler getScheduler();
}    
