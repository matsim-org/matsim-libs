package playground.mmoyo.algorithms;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;

import playground.mmoyo.utils.DataLoader;

public class ClonMutationReset  {
	
	/**
	 * Set all clones to the original activity times according to an original population file.
	 * Assumes that origonalPop has all original agents inside, and that both plans were "cleaned" */
	public void run (final Population originalPop, Population clonPop){
		
		ClonDetector detector = new ClonDetector("X");
		List<Id> personList = detector.run(clonPop);
		
		for(Id clonId : personList){
			String strOrig = detector.getOriginalId(clonId.toString());
			Id originalId = new IdImpl(strOrig);
			if (originalPop.getPersons().keySet().contains(originalId)){
				Person origPerson = originalPop.getPersons().get(originalId);
				Person clonPerson = clonPop.getPersons().get(clonId);
				int planIndex=0;
				for (Plan plan : origPerson.getPlans()){
					int actIndex=0;
					for (PlanElement pe : plan.getPlanElements()){
						if ((pe instanceof Activity)) {
							Activity origAct = (Activity)pe;
							Activity clonAct = (Activity)clonPerson.getPlans().get(planIndex).getPlanElements().get(actIndex);
							
							clonAct.setStartTime(origAct.getEndTime());
							clonAct.setMaximumDuration(origAct.getMaximumDuration());
							clonAct.setEndTime(origAct.getEndTime());
						}
						actIndex++;
					}
					planIndex++;
				}
			}
		}
		
	}
	
	
	public static void main(String[] args) {
		String popFilePath;
		if (args.length>0){
			popFilePath = args[0];
		}else{
			popFilePath = "../../input/juni/poponlyM44.xml.gz";
		}
		DataLoader dataLoader = new DataLoader (); 
		Population pop= dataLoader.readPopulation(popFilePath);
	}

}
