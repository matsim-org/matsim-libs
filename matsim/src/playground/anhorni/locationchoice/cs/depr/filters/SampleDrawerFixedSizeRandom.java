package playground.anhorni.locationchoice.cs.depr.filters;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;

import playground.anhorni.locationchoice.cs.helper.ChoiceSet;


public class SampleDrawerFixedSizeRandom extends SampleDrawer {
	
	int maxSizeOfChoiceSets = 1;
	private final static Logger log = Logger.getLogger(SampleDrawerFixedSizeRandom.class);
	
	public SampleDrawerFixedSizeRandom(int maxSizeOfChoiceSets) {
		this.maxSizeOfChoiceSets = maxSizeOfChoiceSets;
	}
	
	public void drawSample(List<ChoiceSet> choiceSets) {
		
		log.info("Sample choice sets to the size : " + this.maxSizeOfChoiceSets);
				
		Iterator<ChoiceSet> choiceSets_it = choiceSets.iterator();
		while (choiceSets_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets_it.next();
			while (choiceSet.choiceSetSize() >  this.maxSizeOfChoiceSets) {
//				choiceSet.removeFacilityRandomly();
			}
		}
	}
}
