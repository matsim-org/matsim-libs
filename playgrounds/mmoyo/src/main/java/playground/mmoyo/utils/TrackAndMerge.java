package playground.mmoyo.utils;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
//import org.matsim.pt.transitSchedule.api.TransitRoute;
//import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.algorithms.PassengerTracker2;
import playground.mmoyo.utils.calibration.OverDemandPlanCreator;

public class TrackAndMerge {
	
	/**to track passengers from an array of population files and fusion them in a new population where coincident agents plans are merged, the first plan remains as selected*/ 
	public Population run(final String[] popFilesArray, final TransitSchedule schedule, final String networkFile, final TransitLine line){
		//-> read the network only one not 4 times!
		//create popArry
		Population[] popArray = new Population[popFilesArray.length];
		DataLoader dataLoader = new DataLoader();
		for (int i=0; i <popFilesArray.length;i++){
			popArray[i]  = dataLoader.readNetwork_Population(networkFile, popFilesArray[i]).getPopulation();
		}		

		Network net = dataLoader.readNetwork(networkFile);
		
		//track passengers
		List<Id> travelPersonList = new PassengerTracker2(line, net, schedule).getTrackedPassengers(popArray);
		
		//create new Population extracting the agents from the pop array
		Scenario scn = dataLoader.createScenario();
		Population newPop = scn.getPopulation();
		scn= null;

		//merge supposing that all agents are in all plans
		for (Id id : travelPersonList){
			Person newPerson = popArray[0].getPersons().get(id);   //take the pop[0] as base
			for (int i=1; i <popArray.length;i++){      //start from 1 because the 0 was taken as base
				newPerson.addPlan(popArray[i].getPersons().get(id).getSelectedPlan());
			}
			newPop.addPerson(newPerson);
		}
		return newPop;
	}
	
	public static void main(String[] args) {
		String[] popFilesArray = new String[3];
		String trScheduleFile;
		String netFilePath;
		String strLine;
		//String[] strStops = new String[3];
		if(args.length>0){
			popFilesArray[0]= args[0];
			popFilesArray[1]= args[1];
			popFilesArray[2]= args[2];
			trScheduleFile =  args[3];
			netFilePath =   args[4];
			strLine =    args[5];
			//strStops[0]= args[6];
			//strStops[1]= args[7];
			//strStops[2]= args[8];
		}else{
			popFilesArray = new String[1];
			popFilesArray[0]= "../../input/juni/overEstimatedDemandPlans.xml.gz";
			//popFilesArray[1]= "../../input/persTrack/routedPlan_walk6.0_dist0.0_tran1200.0.xml.gz";
			//popFilesArray[2]= "../../input/persTrack/routedPlan_walk8.0_dist0.5_tran720.0.xml.gz";
			trScheduleFile =  "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
			netFilePath =   "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			strLine =    "B-M44";
			/*
			strStops[0]= "812020.3";
			strStops[1]= "812550.1";
			strStops[2]= "812013.1";
			*/
		}
		//load data
		DataLoader dataLoader = new DataLoader();
		NetworkImpl net = (NetworkImpl)dataLoader.readNetwork(netFilePath);
		TransitSchedule schedule = dataLoader.readTransitSchedule(net, trScheduleFile);
		TransitLine line = schedule.getTransitLines().get(new IdImpl(strLine));		
		
		//execute
		Population pop =  new TrackAndMerge().run(popFilesArray, schedule, netFilePath, line);

		//add home plan
		pop= new OverDemandPlanCreator(pop).run(1, 0);
		
		//write population
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(pop, net);
		File file = new File(popFilesArray[0]);
		popwriter.write(file.getParent() + "/" + file.getName() + "TrackedAndMergedAndHomPlan.xml.gz") ;
		System.out.println("done");
	}

}
