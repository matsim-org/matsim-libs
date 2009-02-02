package playground.anhorni.locationchoice.cs.choicesetextractors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.SpanningTree;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

public abstract class ChoiceSetExtractor {
		
	protected TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink = null;
	private Controler controler = null;
	private List<ChoiceSet> choiceSets;
	
	private final static Logger log = Logger.getLogger(ChoiceSetExtractor.class);
	
	public ChoiceSetExtractor(Controler controler, List<ChoiceSet> choiceSets) {
		this.controler = controler;
		this.choiceSets = choiceSets;
	} 
	
	
	protected void computeChoiceSets() {

		SpanningTree spanningTree = new SpanningTree(this.controler.getLinkTravelTimes(), this.controler.getTravelCostCalculator());
		String type ="s";
			
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();			
			log.info("Computing choice set: " + choiceSet.getId().toString());							
			this.computeChoiceSet(choiceSet, spanningTree, type, this.controler);
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
