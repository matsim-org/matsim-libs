package lsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import lsp.resources.Resource;
import lsp.replanning.LSPReplanner;
import lsp.scoring.LSPScorer;
import lsp.shipment.LSPShipment;

public class LSPImpl implements LSP {

	private Id<LSP> id;
	private Collection<LSPShipment> shipments;
	private ArrayList<LSPPlan> plans; 
	private SolutionScheduler solutionScheduler;
	private LSPPlan selectedPlan;
	private Collection<Resource> resources;
	private LSPScorer scorer;
	private LSPReplanner replanner;
	
	public static class Builder{
		private Id<LSP> id;
		private SolutionScheduler solutionScheduler;
		private LSPPlan initialPlan;
		private Collection<Resource> resources;
		private LSPScorer scorer;
		private LSPReplanner replanner;
		

		
		public static Builder getInstance(){
		return new Builder();
	}
		
	private Builder(){
		this.resources = new ArrayList<Resource>();

	}
	
	public Builder setSolutionScheduler(SolutionScheduler solutionScheduler){
		this.solutionScheduler = solutionScheduler;
		return this;
	}
	
	public Builder setSolutionScorer(LSPScorer scorer){
		this.scorer = scorer;
		return this;
	}
	
	public Builder setReplanner(LSPReplanner replanner){
		this.replanner= replanner;
		return this;
	}
	
	
	public Builder setInitialPlan(LSPPlan plan){
		this.initialPlan = plan;
		for(LogisticsSolution solution : plan.getSolutions()) {
			for(LogisticsSolutionElement element : solution.getSolutionElements()) {
				if(!resources.contains(element.getResource())) {
					resources.add(element.getResource());
				}
			}
		}
		return this;
	}
	
	public Builder setId(Id<LSP> id){
		this.id = id;
		return this;
	}
	
	public LSPImpl build(){
		return new LSPImpl(this);
	}
	
	}
	
	
	private LSPImpl(LSPImpl.Builder builder){
		this.shipments = new ArrayList<LSPShipment>();
		this.plans= new ArrayList<LSPPlan>();
		this.id = builder.id;
		this.solutionScheduler = builder.solutionScheduler;
		this.solutionScheduler.setLSP(this);
		this.selectedPlan=builder.initialPlan;
		this.selectedPlan.setLSP(this);
		this.plans.add(builder.initialPlan);
		this.resources = builder.resources;
		this.scorer = builder.scorer;
		if(this.scorer != null) {
			this.scorer.setLSP(this);
		}	
		this.replanner = builder.replanner;
		if(this.replanner != null) {
			this.replanner.setLSP(this);
		}	
	}
	
	
	@Override
	public Id<LSP> getId() {
		return id;
	}

//	@Override
//	public Collection<LSPShipment> getShipments() {
//		return shipments;
//	}

	
	@Override
	public void scheduleSoultions() {
		solutionScheduler.scheduleSolutions();
	}


	@Override
	public boolean addPlan(LSPPlan plan) {
		for(LogisticsSolution solution : plan.getSolutions()) {
			for(LogisticsSolutionElement element : solution.getSolutionElements()) {
				if(!resources.contains(element.getResource())) {
					resources.add(element.getResource());
				}
			}
		}
		return plans.add(plan);
	}


	@Override
	public LSPPlan createCopyOfSelectedPlanAndMakeSelected() {
		LSPPlan newPlan = LSPImpl.copyPlan(this.selectedPlan) ;
		this.setSelectedPlan( newPlan ) ;
		return newPlan ;
	}


	@Override
	public ArrayList<LSPPlan> getPlans() {
		return plans;
	}


	@Override
	public LSPPlan getSelectedPlan() {
		return selectedPlan;
	}


	@Override
	public boolean removePlan(LSPPlan plan) {
		if(plans.contains(plan)) {
			plans.remove(plan);
			return true;
		}
		else {
			return false;
		}
	}


	@Override
	public void setSelectedPlan(LSPPlan selectedPlan) {
		if(!plans.contains(selectedPlan)) {
			plans.add(selectedPlan);
		}
		this.selectedPlan = selectedPlan;
		
	}

	public static LSPPlan copyPlan(LSPPlan plan2copy) {
		List<LogisticsSolution> copiedSolutions = new ArrayList<LogisticsSolution>();
		for (LogisticsSolution solution : plan2copy.getSolutions()) {
				LogisticsSolutionImpl copiedSolution = LogisticsSolutionImpl.Builder.newInstance(solution.getId()).build();
				copiedSolution.getSolutionElements().addAll(solution.getSolutionElements());		
				copiedSolutions.add(copiedSolution);
		}
		LSPPlan copiedPlan = new LSPPlanImpl();
		copiedPlan.setAssigner(plan2copy.getAssigner());
		copiedPlan.setLSP(plan2copy.getLsp());
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		copiedPlan.getSolutions().addAll(copiedSolutions);
		return copiedPlan;
	}


	@Override
	public Collection<Resource> getResources() {
		return resources;
	}

	public void scoreSelectedPlan() {
		if(this.scorer != null) {
			double score = scorer.scoreCurrentPlan(this);
			this.selectedPlan.setScore(score);
		}	
	}
	
//	public LSPReplanner getReplanner() {
//		return replanner;
//	}


	@Override
	public void assignShipmentToLSP(LSPShipment shipment) {
		shipments.add(shipment);
		selectedPlan.getAssigner().assignShipment(shipment);
	}
	
	public void replan( final ReplanningEvent arg0 ) {
		if ( this.replanner!=null ) {
			this.replanner.replan( arg0 );
		}
	}


//	@Override
//	public LSPScorer getScorer() {
//		return scorer;
//	}

	@Override
	public void setScorer(LSPScorer scorer) {
		this.scorer =  scorer;
	}


	@Override
	public void setReplanner(LSPReplanner replanner) {
		this.replanner = replanner;
	}


//	@Override
//	public SolutionScheduler getScheduler() {
//		return solutionScheduler;
//	}
	
	@Override
	public Collection<LSPShipment> getShipments() {
		return this.shipments ;
	}

}
