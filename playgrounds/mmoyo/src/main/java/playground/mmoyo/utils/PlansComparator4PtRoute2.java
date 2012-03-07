package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**compares pt connections of a plans file with a set of many plans file and returns similarities*/
public class PlansComparator4PtRoute2 {
	final String strPtInteraction = "pt interaction";
	private Generic2ExpRouteConverter generic2ExpRouteConverter;
	private final Population popBase;
	
	public PlansComparator4PtRoute2(final Population popBase, final TransitSchedule schedule){
		generic2ExpRouteConverter = new Generic2ExpRouteConverter(schedule);
		this.popBase = popBase;
	}
	
	private void run (String comparablePlansDir){
		File comparablePlansDirFile = new File(comparablePlansDir);
		String comparableFilesArray[] = comparablePlansDirFile.list();
		final String ROUTED_PLAN = "routedPlan";
		DataLoader dataLoader = new DataLoader();

		for (Person basePerson: this.popBase.getPersons().values()){
			List<Leg> baseLegList = new ArrayList<Leg>();
			for (PlanElement pe : basePerson.getSelectedPlan().getPlanElements()){
				if ((pe instanceof Leg)) {
					baseLegList.add((Leg)pe);
				}
			}
			
			//compare with all plans
			for (int i=0;i<comparableFilesArray.length; i++){
				String comparableFile= comparableFilesArray[i];
				if (comparableFile.startsWith(ROUTED_PLAN)){
					Population comparablePop = dataLoader.readPopulation(comparablePlansDir + comparableFile);
					Person comparablePerson = comparablePop.getPersons().get(basePerson.getId()); 
					if(comparablePerson !=null){
						comparablePerson.getSelectedPlan();
					}
				}
			}
		}
	}
	
	
	public static void main(String[] args) {
		String scheduleFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String planBaseFilepath = "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.plans.xml.gz";
		String comparablePlansDir = "I:/cluster data/z_alltest5/output/";
		
		//load base data
		DataLoader dataloader = new DataLoader();
		TransitSchedule schedule = dataloader.readTransitSchedule(scheduleFilePath);
		Population popBase = dataloader.readPopulation(planBaseFilepath) ;
		PlansComparator4PtRoute2 plansComparator4PtRoute = new PlansComparator4PtRoute2(popBase, schedule);
		plansComparator4PtRoute.run(comparablePlansDir);
	}
}