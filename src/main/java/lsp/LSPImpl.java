package lsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.ReplanningEvent;

import lsp.replanning.LSPReplanner;
import lsp.scoring.LSPScorer;
import lsp.shipment.LSPShipment;
import org.matsim.utils.objectattributes.attributable.Attributes;

/* package-private */class LSPImpl implements LSP {

	private final Id<LSP> id;
	private final Collection<LSPShipment> shipments;
	private final ArrayList<LSPPlan> plans;
	private final SolutionScheduler solutionScheduler;
	private LSPPlan selectedPlan;
	private final Collection<LSPResource> resources;
	private LSPScorer scorer;
	private LSPReplanner replanner;
	private final Attributes attributes = new Attributes();


	LSPImpl( LSPUtils.LSPBuilder builder ){
		this.shipments = new ArrayList<>();
		this.plans= new ArrayList<>();
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
	public void scheduleSolutions() {
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
		List<LogisticsSolution> copiedSolutions = new ArrayList<>();
		for (LogisticsSolution solution : plan2copy.getSolutions()) {
				LogisticsSolution copiedSolution = LSPUtils.LogisticsSolutionBuilder.newInstance(solution.getId() ).build();
				copiedSolution.getSolutionElements().addAll(solution.getSolutionElements());		
				copiedSolutions.add(copiedSolution);
		}
		LSPPlan copiedPlan = LSPUtils.createLSPPlan();
		copiedPlan.setAssigner(plan2copy.getAssigner());
		copiedPlan.setLSP(plan2copy.getLsp());
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		copiedPlan.getSolutions().addAll(copiedSolutions);
		return copiedPlan;
	}


	@Override
	public Collection<LSPResource> getResources() {
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
		selectedPlan.getAssigner().assignToSolution(shipment);
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

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

}
