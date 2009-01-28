package playground.anhorni.locationchoice.cs.choicesetextractors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import playground.anhorni.locationchoice.cs.helper.ChoiceSet;
import playground.anhorni.locationchoice.cs.helper.SpanningTree;
import playground.anhorni.locationchoice.cs.helper.TravelTimeBudget;
import playground.anhorni.locationchoice.cs.helper.ZHFacility;

public abstract class ChoiceSetExtractor {
	
	protected List<ChoiceSet> choiceSets;	
	protected TreeMap<Id, ArrayList<ZHFacility>> zhFacilitiesByLink = null;
	private Controler controler = null;
	private List<TravelTimeBudget> budgets =  new Vector<TravelTimeBudget>();
	
	public ChoiceSetExtractor(Controler controler, List<ChoiceSet> choiceSets) {
		this.choiceSets = choiceSets;
		this.controler = controler;
	} 
	
	
	protected void computeChoiceSets() {

		SpanningTree spanningTree = new SpanningTree(this.controler.getLinkTravelTimes(), this.controler.getTravelCostCalculator());
		String type ="s";
			
		Iterator<ChoiceSet> choiceSet_it = this.choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();			
			
			this.budgets.add(new TravelTimeBudget(choiceSet.getPersonId(), choiceSet.getTravelTimeBudget(), 
					choiceSet.getTrip().getTripNr()));
							
			this.computeChoiceSet(choiceSet, spanningTree, type, controler);
		}		
	}
	
	protected abstract void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type,
			Controler controler);
		
	public List<ChoiceSet> getChoiceSets() {
		return this.choiceSets;
	}
	public void setChoiceSets(List<ChoiceSet> choiceSets) {
		this.choiceSets = choiceSets;
	}
	public List<TravelTimeBudget> getBudgets() {
		return budgets;
	}
	public void setBudgets(List<TravelTimeBudget> budgets) {
		this.budgets = budgets;
	}

}
