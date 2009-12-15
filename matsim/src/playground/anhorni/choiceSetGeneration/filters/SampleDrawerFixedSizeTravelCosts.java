package playground.anhorni.choiceSetGeneration.filters;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
//import org.matsim.interfaces.basic.v01.Coord;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;


public class SampleDrawerFixedSizeTravelCosts extends SampleDrawer {
	
	int maxSizeOfChoiceSets = 1;
	boolean crowFly = true;
	private final static Logger log = Logger.getLogger(SampleDrawerFixedSizeTravelCosts.class);
	
	public SampleDrawerFixedSizeTravelCosts(int maxSizeOfChoiceSets, boolean crowFly) {
		this.maxSizeOfChoiceSets = maxSizeOfChoiceSets;
		this.crowFly = crowFly;
	}
	
	@Override
	public void drawSample(List<ChoiceSet> choiceSets) {
		
		log.info("Sample choice sets to max. size : " + this.maxSizeOfChoiceSets);
				
		Iterator<ChoiceSet> choiceSets_it = choiceSets.iterator();
		while (choiceSets_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets_it.next();
			
			while (choiceSet.choiceSetSize() > this.maxSizeOfChoiceSets) {
//				Coord referencePoint = choiceSet.getReferencePoint();					
//				int index = choiceSet.getFacilityIndexProbDependendOnTravelCost(referencePoint, this.crowFly);
//				choiceSet.removeFacility(index);
			}
		}
	}
}
