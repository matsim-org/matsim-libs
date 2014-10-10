package playground.acmarmol.matsim2030.microcensus2010;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.acmarmol.Avignon.PersonSubTourExtractor;
import playground.acmarmol.Avignon.SubtourInfo;



public class CarSharingSubtourCounter{


		private static void countCarSharingSubtours(Population population, ObjectAttributes wegeAttributes){
			
			Set<Id<Person>> ids;
			ids = MZPopulationUtils.identifyPlansWithoutActivities(population);
			MZPopulationUtils.removePlans(population, ids);
			System.out.println("      Total persons removed: " + ids.size());
			System.out.println("      Remaining population size: " + population.getPersons().size()+" (" + (double)population.getPersons().size()/(double)62868*100 + "%)");
			
			ids = MZPopulationUtils.identifyNonRoundPlans(population);
			MZPopulationUtils.removePlans(population, ids);
			System.out.println("      Total persons removed: " + ids.size());
			System.out.println("      Remaining population size: " + population.getPersons().size()+" (" + (double)population.getPersons().size()/(double)62868*100 + "%)");
			
			
			PersonSubTourExtractor pse= new PersonSubTourExtractor();
			pse.run(population);
			TreeMap<Id, ArrayList<SubtourInfo>> subtours = pse.getPopulationSubtours();
			
			int subtourCounter = 0;
			int carSharingSubtourCounter = 0;
			boolean cont;
			
			
			for(Person person: population.getPersons().values()){//iterates over all persons
				
				ArrayList<SubtourInfo> personSubtours = subtours.get(person.getId());
				List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
				
				for(SubtourInfo subtourInfo: personSubtours){//iterates over all subtours
					cont = true;
					subtourCounter++;
					
					List<Integer> a = subtourInfo.getSubtour().subList(1, subtourInfo.getSubtour().size());
					
					for(Integer position : subtourInfo.getSubtour().subList(1, subtourInfo.getSubtour().size())){//iterates over elements of subtour (indexes of activities)
						
						LegImpl leg = (LegImpl) planElements.get(position-1);
						String wid =  person.getId().toString() + "-" + (position/2); 
						
						if(leg.getMode().equals("car")){
							
							for(int i=1; i<= (Integer) wegeAttributes.getAttribute(wid.toString(), "number of etappen"); i++){//iterates over ettapen
								
								Etappe etappe = (Etappe) wegeAttributes.getAttribute(wid.toString(), "etappe".concat(String.valueOf(i)));
								
								if(etappe.getCarType().equals("car sharing")){
									carSharingSubtourCounter++;
									cont = false;
									break;
									
								}
								
								
				
							}	
							
							if(!cont){
								break;
							}
							
						}
						
						
					}
					
					
					
					
					
					
				}
				
				
				
			}
			
			System.out.println("NUmber of persons: " + population.getPersons().size());
			System.out.println("Number of subtours: " + subtourCounter );
			System.out.println("Number of car-sharing subtours: " + carSharingSubtourCounter + "(" + ((carSharingSubtourCounter/subtourCounter*100)) + ")");
			
			
		}
		

	

}
