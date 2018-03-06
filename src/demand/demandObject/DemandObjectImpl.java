package demand.demandObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.replanning.GenericStrategyManager;

import demand.decoratedLSP.LSPWithOffers;
import demand.demandAgent.DemandAgent;
import demand.mutualReplanning.DemandReplanner;
import demand.offer.Offer;
import demand.scoring.DemandScorer;
import demand.utilityFunctions.UtilityFunction;
import lsp.functions.Info;
import lsp.shipment.Requirement;

public class DemandObjectImpl implements DemandObject{

	private DemandAgent shipper;
	private DemandAgent recipient;
	private Id<DemandObject> id;
	private ArrayList<DemandPlan> plans;
	private double strengthOfFlow;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private ArrayList <UtilityFunction> utilityFunctions;
	private DemandPlan selectedPlan;
	private DemandScorer scorer;
	private DemandReplanner replanner;
	private Collection<Requirement> requirements;
	private OfferRequester offerRequester;
	private DemandPlanGenerator generator;
	private Collection<Info> infos;
	
	public static class Builder{
		private DemandAgent shipper;
		private DemandAgent recipient;
		private Id<DemandObject> id;
		private double strengthOfFlow;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private ArrayList <UtilityFunction> utilityFunctions;
		private DemandPlan initialPlan;
		private DemandScorer scorer;
		private DemandReplanner replanner;
		private Collection<Requirement> requirements;
		private OfferRequester offerRequester;
		private DemandPlanGenerator generator;
		private Collection<Info> infos;
		
		public static Builder newInstance() {
			return new Builder();
		}
	
	private Builder() {
		this.requirements = new ArrayList<Requirement>();
		this.utilityFunctions = new ArrayList<UtilityFunction>();
		this.infos = new ArrayList<Info>();
	}
	
	public Builder setShipper(DemandAgent shipper) {
		this.shipper = shipper;
		return this;
	}
	
	public Builder setRecipient(DemandAgent recipient) {
		this.recipient = recipient;
		return this;
	}
	
	public Builder setId(Id<DemandObject> id) {
		this.id = id;
		return this;
	}
	
	public Builder setInitialPlan(DemandPlan plan){
		this.initialPlan = plan;
		return this;
	}
	
	public Builder setStrengthOfFlow(double strength){
		this.strengthOfFlow = strength;
		return this;
	}
	
	public Builder setFromLinkId(Id<Link> fromLinkId){
		this.fromLinkId = fromLinkId;
		return this;
	}
	
	public Builder setToLinkId(Id<Link> toLinkId){
		this.toLinkId = toLinkId;
		return this;
	}
	
	public Builder setOfferRequester(OfferRequester offerRequester){
		this.offerRequester = offerRequester;
		return this;
	}
	
	public Builder setDemandPlanGenerator(DemandPlanGenerator generator){
		this.generator = generator;
		return this;
	}

	public Builder addUtilityFunction(UtilityFunction utilityFunction) {
		this.utilityFunctions.add(utilityFunction);
		return this;
	}
	
	public Builder addRequirement(Requirement requirement) {
		this.requirements.add(requirement);
		return this;
	}
	
	public Builder addInfo(Info info) {
		this.infos.add(info);
		return this;
	}
	
	public Builder setScorer(DemandScorer scorer) {
		this.scorer = scorer;
		return this;
	}
	
	public Builder setReplanner(DemandReplanner replanner) {
		this.replanner = replanner;
		return this;
	}
	
	public DemandObject build() {
		return new DemandObjectImpl(this);
	}

	}
	
	private DemandObjectImpl(Builder builder) {
		this.plans = new ArrayList<DemandPlan>();
		this.utilityFunctions = new ArrayList<UtilityFunction>();
		this.shipper = builder.shipper;
		if(this.shipper != null) {
			this.shipper.getDemandObjects().add(this);
		}
		this.recipient = builder.recipient;
		if(this.recipient != null) {
			this.recipient.getDemandObjects().add(this);
		}
		this.id = builder.id;
		this.strengthOfFlow = builder.strengthOfFlow;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.selectedPlan=builder.initialPlan;
		if(this.selectedPlan != null) {
			this.selectedPlan.setDemandObject(this);
			this.selectedPlan.getShipment().setDemandObject(this);
		}
		this.plans.add(builder.initialPlan);
		this.utilityFunctions = builder.utilityFunctions;
		this.scorer = builder.scorer;
		if(this.scorer != null) {
			this.scorer.setDemandObject(this);
		}
		this.replanner = builder.replanner;
		if(this.replanner != null) {
			this.replanner.setDemandObject(this);
		}
		this.requirements = builder.requirements;
		this.offerRequester = builder.offerRequester;
		if(this.offerRequester != null) {
			this.offerRequester.setDemandObject(this);
		}		
		this.infos = builder.infos;
		this.generator = builder.generator;
		if(this.generator != null) {
			generator.setDemandObject(this);
		}
	}
	
	
	@Override
	public boolean addPlan(DemandPlan plan) {
		return plans.add(plan);
	}

	@Override
	public boolean removePlan(DemandPlan plan) {
		if(plans.contains(plan)) {
			plans.remove(plan);
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public DemandPlan getSelectedPlan() {
		return selectedPlan;
	}

	@Override
	public void setSelectedPlan(DemandPlan selectedPlan) {
		if(!plans.contains(selectedPlan)) plans.add(selectedPlan);
		this.selectedPlan = selectedPlan;
	}

	@Override
	public DemandPlan createCopyOfSelectedPlanAndMakeSelected() {
		DemandPlan newPlan = DemandObjectImpl.copyPlan(this.selectedPlan) ;
		this.setSelectedPlan( newPlan ) ;
		return newPlan;
	}

	@Override
	public DemandAgent getShipper() {
		return shipper;
	}

	@Override
	public DemandAgent getRecipient() {
		return recipient;
	}
	
	@Override
	public Id<DemandObject> getId() {
		return id;
	}

	@Override
	public List<? extends DemandPlan> getPlans() {
		return plans;
	}

	@Override
	public double getStrengthOfFlow() {
		return strengthOfFlow;
	}

	@Override
	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	@Override
	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	@Override
	public Collection<UtilityFunction> getUtilityFunctions() {
		return utilityFunctions;
	}

	private static DemandPlan copyPlan(DemandPlan plan2copy) {
		DemandPlanImpl.Builder builder = DemandPlanImpl.Builder.newInstance();
		builder.setLogisticsSolutionId(plan2copy.getSolutionId());
		builder.setLsp(plan2copy.getLsp());
		builder.setShipperShipment(plan2copy.getShipment());
		DemandPlan copiedPlan = builder.build();
		copiedPlan.setScore(plan2copy.getScore());
		return copiedPlan;
	}

	@Override
	public void scoreSelectedPlan() {
		double score = scorer.scoreCurrentPlan(this);
		this.selectedPlan.setScore(score);	
	}

	@Override
	public void setScorer(DemandScorer scorer) {
		this.scorer = scorer;
	}

	@Override
	public DemandScorer getScorer() {
		return scorer;
	}

	@Override
	public DemandReplanner getReplanner() {
		return replanner;
	}

	@Override
	public void setReplanner(DemandReplanner replanner) {
		this.replanner = replanner;
	}

	@Override
	public void setOfferRequester(OfferRequester requester) {
		this.offerRequester = requester;
	}

	@Override
	public OfferRequester getOfferRequester() {
		return offerRequester;
	}

	@Override
	public void setDemandPlanGenerator(DemandPlanGenerator generator) {
		this.generator = generator;
	}

	@Override
	public DemandPlanGenerator getDemandPlanGenerator() {
		return generator;
	}

	@Override
	public Collection<Requirement> getRequirements() {
		return requirements;
	}

	@Override
	public Collection<Info> getInfos() {
		return infos;
	}
}
