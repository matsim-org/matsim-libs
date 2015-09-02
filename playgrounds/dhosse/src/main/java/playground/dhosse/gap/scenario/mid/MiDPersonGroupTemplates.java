package playground.dhosse.gap.scenario.mid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;

public class MiDPersonGroupTemplates {

	Map<String, List<MiDSurveyPerson>> personGroups = new HashMap<String, List<MiDSurveyPerson>>();
	
	public void handlePerson(MiDSurveyPerson p){
		
		for(PlanElement pe : p.getPlan().getPlanElements()){
			
			if(pe instanceof Activity){
				
				Activity act = (Activity)pe;
				
				if(act.getType().equals("NULL")){
					return;
				}
				
				if(act.getEndTime() == Time.UNDEFINED_TIME && act.getStartTime() == Time.UNDEFINED_TIME && act.getMaximumDuration() == Time.UNDEFINED_TIME){
					return;
				}
				
			} else{
				
				LegImpl leg = (LegImpl)pe;
				if(Double.isNaN(leg.getRoute().getDistance())){
					
					return;
					
				}
				
			}
			
		}
		
		String hash = p.generateHash();
		
		if(!this.personGroups.containsKey(hash)){
			
			this.personGroups.put(hash, new ArrayList<MiDSurveyPerson>());
			
		}
		
		this.personGroups.get(hash).add(p);
		
	}
	
	public Map<String, List<MiDSurveyPerson>> getPersonGroups(){
		
		return this.personGroups;
		
	}
	
}
