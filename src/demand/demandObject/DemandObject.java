package demand.demandObject;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;

import demand.demandAgent.DemandAgent;
import demand.mutualReplanning.DemandReplanner;
import demand.scoring.DemandScorer;
import demand.utilityFunctions.UtilityFunction;
import lsp.functions.Info;
import lsp.shipment.Requirement;


public interface DemandObject extends HasPlansAndId<DemandPlan,DemandObject>{
	
	public DemandAgent getShipper();
	public DemandAgent getRecipient();
	public Id<DemandObject> getId();
	public List<? extends DemandPlan> getPlans();
	public double getStrengthOfFlow();
	public Id<Link> getFromLinkId();
	public Id<Link> getToLinkId();
	public Collection<UtilityFunction> getUtilityFunctions();
	public void scoreSelectedPlan();
	public DemandPlan getSelectedPlan();
	public void setSelectedPlan(DemandPlan plan);
	public void setScorer(DemandScorer scorer);
	public DemandScorer getScorer();
	public DemandReplanner getReplanner();
	public void setReplanner(DemandReplanner replanner);
	public void	setOfferRequester(OfferRequester requester);
	public OfferRequester getOfferRequester();
	public void	setDemandPlanGenerator(DemandPlanGenerator generator);
	public DemandPlanGenerator getDemandPlanGenerator();
	public Collection<Requirement> getRequirements();
	public Collection<Info> getInfos();
}
