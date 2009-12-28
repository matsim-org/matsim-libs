package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

import playground.anhorni.locationchoice.preprocess.helper.BinsOld;
import playground.anhorni.locationchoice.preprocess.plans.modifications.helper.DesiredDurationPerson;

public class DistanceBinsAssigner {
	
	private final static Logger log = Logger.getLogger(DistanceBinsAssigner.class);
	private double linearLimitDuration;
	private String mode;
	private Population plans;
	private DistanceBins distanceBins;
	private BinsOld durationDistanceBins;
	
	public DistanceBinsAssigner(double linearLimitDuration, String mode, Population plans, 
			DistanceBins distanceBins, BinsOld durationDistanceBins) {
		this.linearLimitDuration = linearLimitDuration;
		this.mode = mode;
		this.plans = plans;
		this.distanceBins = distanceBins;
		this.durationDistanceBins = durationDistanceBins;
		
	}
	
	public void assignDistanceBins(ArrayList<DesiredDurationPerson> desiredDurationsPerson) {
		ArrayList<DesiredDurationPerson> linearBins = new ArrayList<DesiredDurationPerson>();
		ArrayList<DesiredDurationPerson> uniformBins = new ArrayList<DesiredDurationPerson>();
		
		Iterator<DesiredDurationPerson> desiredDurationPerson_it = desiredDurationsPerson.iterator();
		while (desiredDurationPerson_it.hasNext()) {
			DesiredDurationPerson desiredDurationPerson = desiredDurationPerson_it.next();
						
			if (desiredDurationPerson.getDuration() <= linearLimitDuration) {
				linearBins.add(desiredDurationPerson);
			}
			else {
				uniformBins.add(desiredDurationPerson);
			}
		}
		linearBins = this.movePlansWithLeisurePriorToWorkToHead(linearBins);
		this.assignDistanceBinsLinear(linearBins);
		this.assignDistanceBinsUniformly(uniformBins);
	}
	
	private ArrayList<DesiredDurationPerson> movePlansWithLeisurePriorToWorkToHead(ArrayList<DesiredDurationPerson> linearBins) {
		
		ArrayList<DesiredDurationPerson> linearBinsOrdered = new ArrayList<DesiredDurationPerson>();
		Collections.reverse(linearBins); // long duration -------- short duration
		
		int cnt = 0;
		
		Iterator<DesiredDurationPerson> bins_it = linearBins.iterator();	
		while (bins_it.hasNext()) {
			DesiredDurationPerson desiredDurationPerson = bins_it.next();
			if (desiredDurationPerson.planContainsLeisurePriorToWork()) {
				linearBinsOrdered.add(0, desiredDurationPerson);
				cnt++;
			}
			else {
				linearBinsOrdered.add(cnt, desiredDurationPerson);
			}	
		}
		return linearBinsOrdered;
	}
	
	private void assignDistanceBinsUniformly(ArrayList<DesiredDurationPerson> bins) {
		int numberOfElements2Assign = bins.size();	
		int i = 0;
		while (numberOfElements2Assign > 0) {
			
			i++;
			int index = MatsimRandom.getRandom().nextInt(numberOfElements2Assign);
			if (bins.get(index) == null) {
				continue;
			}
			else {
				DesiredDurationPerson desiredDurationPerson = bins.get(index);	
				this.assignDistanceBinsPerPerson(desiredDurationPerson);
				bins.remove(index);
				numberOfElements2Assign--;
			}
			if (i % 100 == 0 && i > 0) {
				log.info(i + " trials to assign distance bins uniformly");
			}
		}
	}
	
	private void assignDistanceBinsLinear(ArrayList<DesiredDurationPerson> bins) {
		Iterator<DesiredDurationPerson> desiredDurationPerson_it = bins.iterator();
		while (desiredDurationPerson_it.hasNext()) {
			DesiredDurationPerson desiredDurationPerson = desiredDurationPerson_it.next();
			this.assignDistanceBinsPerPerson(desiredDurationPerson);			
		}
	}
	
	private void assignDistanceBinsPerPerson(DesiredDurationPerson desiredDurationPerson) {
		
		Id personId = desiredDurationPerson.getPersonId();		
		PlanImpl plan = (PlanImpl)this.plans.getPersons().get(personId).getSelectedPlan();
		
		List<? extends PlanElement> actslegs = plan.getPlanElements();			
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Activity act = (Activity)actslegs.get(j);
			
			if (act.getType().startsWith("leisure")) {
				final LegImpl leg = (LegImpl)actslegs.get(j+1);
				
				if (leg.getMode().toString().equals(this.mode)) {					
					int randomDistance = 
						(int)Math.round(this.distanceBins.getRandomDistance(desiredDurationPerson.planContainsLeisurePriorToWork()));
					String newActType = act.getType() + "_" + randomDistance;
					act.setType(newActType);
					
					// minimum duration is 30 min! -> else division by zero while scoring
					plan.getPerson().getDesires().putActivityDuration(newActType, Math.max(30 * 60, desiredDurationPerson.getDuration()));	
					
					// for plot
					this.durationDistanceBins.addVal(desiredDurationPerson.getDuration(), randomDistance);
				}
			}
		}
	}	

}
