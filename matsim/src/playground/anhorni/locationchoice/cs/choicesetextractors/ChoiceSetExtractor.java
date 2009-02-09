package playground.anhorni.locationchoice.cs.choicesetextractors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.SpanningTree;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

public abstract class ChoiceSetExtractor {
		
	protected TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink = null;
	protected Controler controler = null;
	private List<ChoiceSet> choiceSets;
	
	private final static Logger log = Logger.getLogger(ChoiceSetExtractor.class);
	
	public ChoiceSetExtractor(Controler controler, List<ChoiceSet> choiceSets) {
		this.controler = controler;
		this.choiceSets = choiceSets;
	} 
	
	
	protected void computeChoiceSets() {

		SpanningTree spanningTree = new SpanningTree(this.controler.getLinkTravelTimes(), this.controler.getTravelCostCalculator());
		String type ="s";
		
		int index = 0;
		List<ChoiceSet> choiceSets2Remove = new Vector<ChoiceSet>();
		
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();										
			this.computeChoiceSet(choiceSet, spanningTree, type, this.controler);
			log.info(index + ": Choice set " + choiceSet.getId().toString() + ": " + choiceSet.getFacilities().size() + " alternatives");
			index++;
			
			if (choiceSet.getTravelTime2ChosenFacility() > 8 * choiceSet.getTravelTimeBudget()) {	
				choiceSets2Remove.add(choiceSet);			
			}
		}
		
		Iterator<ChoiceSet> choiceSets2Remove_it = choiceSets2Remove.iterator();
		while (choiceSets2Remove_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets2Remove_it.next();
			this.choiceSets.remove(choiceSet);
			log.info("Removed choice set: " + choiceSet.getId() + " as travel time was implausible");
		}
		
	}
		
	protected abstract void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type,
			Controler controler);
		
	public List<ChoiceSet> getChoiceSets() {
		return choiceSets;
	}

	public void setChoiceSets(List<ChoiceSet> choiceSets) {
		this.choiceSets = choiceSets;
	}

}
