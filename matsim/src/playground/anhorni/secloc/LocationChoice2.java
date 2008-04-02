package playground.anhorni.secloc;

import org.matsim.replanning.modules.StrategyModuleI;
import org.matsim.basic.v01.Id;
import org.matsim.plans.Plan;
import playground.anhorni.secloc.KnowledgeModifierRandom;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;
import org.matsim.gbl.Gbl;



public class LocationChoice2 implements StrategyModuleI {

	/**
	 * KNOWLEGE: 
	 * Mutates (possibly extends) the location set (set of facilities) which is 
	 * given in the knowledge of a person. Extended location set can be limited 
	 * (narrow locations) (kd-tree of facilities) or it can cover all facilities.
	 * The coice of an additional facility can be done as next best choice or randomly
	 * 
	 * LOCATION CHOICE:
	 * 
	 */

	private LocationSelectorI locationSelector;
	private KnowledgeModifier knowledgeModifier;

	public LocationChoice2(){
	}

	/*
	private void modifyKnowledge(Knowledge knowledge) {
		// strategy is set in the specific modifier
		this.knowledgeModifier.modify(knowledge);
	}
	*/

	public void handlePlan(Plan plan){
		// strategy is set in the specific modifier	
		this.knowledgeModifier.modify(plan.getPerson().getKnowledge());
		
		// parameters are set in the specific selector
		this.locationSelector.setLocations(plan);	
	}
	
	public void init(){
		WorldBottom2TopCompletion wc=new WorldBottom2TopCompletion();
		wc.run(Gbl.getWorld());
		
		this.locationSelector=new RandomLocationSelector();
		this.knowledgeModifier=new KnowledgeModifierRandom(Gbl.getWorld().getLayer(new Id("facility")).getLocations());
	}
	public void finish(){}	
}
