package playground.anhorni.choiceSetGeneration.choicesetextractors;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.SpanningTree;
import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;

public abstract class ChoiceSetExtractor {
		
	protected ZHFacilities facilities;
	protected Controler controler;
	private List<ChoiceSet> choiceSets;
	private int tt;
	
	private final static Logger log = Logger.getLogger(ChoiceSetExtractor.class);
	
	public ChoiceSetExtractor(Controler controler, List<ChoiceSet> choiceSets, int tt) {
		this.controler = controler;
		this.choiceSets = choiceSets;
		this.tt = tt;
	} 
	
	
	protected void computeChoiceSets() {

		SpanningTree spanningTree = new SpanningTree(this.controler.getTravelTimeCalculator(), this.controler.getTravelCostCalculator());
		String type ="s";
		
		int index = 0;
		List<ChoiceSet> choiceSets2Remove = new Vector<ChoiceSet>();
		
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();										
			this.computeChoiceSet(choiceSet, spanningTree, type, this.controler, this.tt);
			log.info(index + ": Choice set " + choiceSet.getId().toString() + ": " + choiceSet.getFacilities().size() + " alternatives");
			index++;
			
			if (choiceSet.getTravelTimeStartShopEnd(choiceSet.getChosenFacilityId()) > 8 * choiceSet.getTravelTimeBudget()) {	
				choiceSets2Remove.add(choiceSet);			
			}
			
			// remove the trips which end outside of canton ZH:
			/*
			 * change choice set list to TreeMap or similar
			 */
			if (choiceSet.getId().equals(new IdImpl("8160012")) ||
				choiceSet.getId().equals(new IdImpl("58690014")) ||
				choiceSet.getId().equals(new IdImpl("30195012")) ||
				choiceSet.getId().equals(new IdImpl("31953012")) ||
				choiceSet.getId().equals(new IdImpl("55926012")) ||
				choiceSet.getId().equals(new IdImpl("58650012")) ||
				choiceSet.getId().equals(new IdImpl("55443011")) ||
				choiceSet.getId().equals(new IdImpl("44971012")) ) {
				
				choiceSets2Remove.add(choiceSet);				
			}
			
			// remove trips with a walk TTB >= 7200 s:
			if (choiceSet.getId().equals(new IdImpl("27242011")) ||
				choiceSet.getId().equals(new IdImpl("27898011")) ||
				choiceSet.getId().equals(new IdImpl("42444011")) ||
				choiceSet.getId().equals(new IdImpl("65064011")) ||
				choiceSet.getId().equals(new IdImpl("15359011")) ||
				choiceSet.getId().equals(new IdImpl("27691011")) ||
				choiceSet.getId().equals(new IdImpl("65679015"))) {
				
				choiceSets2Remove.add(choiceSet);
			}
			
			
		}
		
		Iterator<ChoiceSet> choiceSets2Remove_it = choiceSets2Remove.iterator();
		while (choiceSets2Remove_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets2Remove_it.next();
			this.choiceSets.remove(choiceSet);
			log.info("Removed choice set: " + choiceSet.getId() + " as travel time was implausible or trip ended outside canton ZH");
		}		
	}
		
	protected abstract void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type,
			Controler controler, int tt);
		
	public List<ChoiceSet> getChoiceSets() {
		return choiceSets;
	}

	public void setChoiceSets(List<ChoiceSet> choiceSets) {
		this.choiceSets = choiceSets;
	}

}
