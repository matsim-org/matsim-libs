package playground.acmarmol.Avignon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

public abstract class SubtourDistances {
	
	
	
	public static LinkedHashMap<Id, String> calcDistancesForPurpose(TreeMap<Id, ArrayList<SubtourInfo>> subtours, Scenario scenario){
	
		
		final  Logger log = Logger.getLogger(SubtourDistances.class);
		
		log.info("Calculating distances by purpose...");
		
		final  DecimalFormat df = new DecimalFormat("#.##");
		LinkedHashMap<Id, String> distancesByPurpose = new LinkedHashMap<Id, String>();
		
		for(Id id : subtours.keySet()){
			
			Person person = scenario.getPopulation().getPersons().get(id);
			
			double[] distances = new double[MyTransportMode.PURPOSES.size()];	
			
			for(SubtourInfo  personSubtour: subtours.get(id)){
					
				int index = MyTransportMode.PURPOSES.indexOf(getSubtourHierarchicAct(personSubtour));
				ArrayList<Integer> subtour = personSubtour.getSubtour();
				
				for (int position = 1; position < subtour.size(); position++){
						
					Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(subtour.get(position)-1);
					if(index!=-1){
							if(leg.getMode().equals("car")){
								//i.e home-home trips
								distances[index] += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), scenario.getNetwork());
							}else{ 
								distances[index] += leg.getRoute().getDistance();
							}
								
						
					}else{
						//never goes in here
						try{distances[0] += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), scenario.getNetwork());}
						catch(java.lang.ClassCastException e){ }
					}
				}
				
							
			}
			
				
			String strDistance = Arrays.toString(distances);
			strDistance = strDistance.replace( '[', ' ');
			strDistance = strDistance.replace( ']', ' ');
			
			distancesByPurpose.put(person.getId(), strDistance);
			
			
		}
		log.info("...done");
		log.info("number of subtours: " + subtours.size());
		return distancesByPurpose;
	}

	
	private static String getSubtourHierarchicAct(SubtourInfo personSubtour){
		
		String actSequence = personSubtour.getActSequence();
		
		if(actSequence.charAt(0) == 'w'){//activity sequence starts at work
							
					if(actSequence.contains("e")){
						return "toEducation";
					}
					else if(actSequence.contains("s")){
						return "toShop";
					}
					else if(actSequence.contains("l")){
						return "toLeisure";
					}
					else{throw new RuntimeException("error handling activity sequence: " + actSequence); }
					//subtours H-H assigned to distance to work for completeness
		
		}else{
					if(actSequence.contains("w")){
						return "toWork";
					}
					else if(actSequence.contains("e")){
						return "toEducation";
					}
					else if(actSequence.contains("s")){
						return "toShop";
					}
					else if(actSequence.contains("l")){
						return "toLeisure";
					}
					else{return "toLeisure";}
					//"subtours" H-H assigned to distance to leisure for completeness
					// shouldn't be a problem since dist H to H should be 0
			
		}
		
		
		
	}



}



	 

