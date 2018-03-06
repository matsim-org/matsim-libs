package demand.mutualReplanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;

import demand.demandObject.DemandObject;
import demand.demandObject.DemandPlan;

public class DemandPlanStrategyImpl implements GenericPlanStrategy<DemandPlan, DemandObject> {

	private PlanSelector<DemandPlan, DemandObject> planSelector = null;
	private OfferReplanningStrategyModule firstModule = null;
	private final ArrayList<DemandPlan> plans = new ArrayList<>();
	private DemandObject demandObject;	
	
	public DemandPlanStrategyImpl(final PlanSelector<DemandPlan, DemandObject> planSelector) {
		this.planSelector = planSelector;
	}

	public DemandPlanStrategyImpl(final PlanSelector<DemandPlan, DemandObject> planSelector, DemandObject demandObject) {
		this.planSelector = planSelector;
		this.demandObject = demandObject;
	}
	
	public void addStrategyModule(final GenericPlanStrategyModule<DemandPlan> module) {
		try {
			OfferReplanningStrategyModule offerModule = (OfferReplanningStrategyModule) module;
			offerModule.setDemandObject(demandObject);
			this.firstModule = offerModule;
		}
		catch(ClassCastException e) {
			System.out.println("DemandPlanStrategyImpl expects a module of instance OfferReplanningStrategyModule and not any GenericPlanStrategyModule");
			System.exit(1);
		}
	}
	
	public int getNumberOfStrategyModules() {
			return  1; 
	}
			
	@Override
	public void run(final HasPlansAndId<DemandPlan, DemandObject> person) {
	
		
		// if there is at least one unscored plan, find that one:
		DemandPlan plan = new RandomUnscoredPlanSelector<DemandPlan, DemandObject>().selectPlan(person) ;
		
		// otherwise, find one according to selector (often defined in PlanStrategy ctor):
		if (plan == null) {
			plan = this.planSelector.selectPlan(person);
		}
		
		// "select" that plan:
		if ( plan != null ) {
			person.setSelectedPlan(plan);
		}
		
		// if there is a "module" (i.e. "innovation"):
		if (this.firstModule != null) {
			
			// set the working plan to a copy of the selected plan:
			plan = person.createCopyOfSelectedPlanAndMakeSelected();
			
			// add new plan to container that contains the plans that are handled by this PlanStrategy:
			this.plans.add(plan);

			// start working on this new plan:
			this.firstModule.handlePlan(plan);
		}

	}

	@Override
	public void init(ReplanningContext replanningContext0) {
	
		if (this.firstModule != null) {
			this.firstModule.prepareReplanning(replanningContext0);
		}
	}

	@Override
	public void finish() {
		if (this.firstModule != null) {	
			this.firstModule.finishReplanning();
			
		}
		this.plans.clear();
	}

	public PlanSelector<DemandPlan, DemandObject> getPlanSelector() {
		return planSelector;
	}
	
	public OfferReplanningStrategyModule getModule() {
		return firstModule;
	}
	
	public void setDemandObject(DemandObject demandObject) {
		this.demandObject  = demandObject;
	}
	
	public DemandObject getDemandObject() {
		return demandObject;
	}
}


