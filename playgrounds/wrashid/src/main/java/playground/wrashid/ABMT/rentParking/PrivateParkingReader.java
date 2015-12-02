package playground.wrashid.ABMT.rentParking;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class PrivateParkingReader {

	public static void main(String[] args) {
		
		double populationScalingFactor=0.01;
		
		Config config = ConfigUtils.createConfig();
		
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		
		
		PopulationReaderMatsimV5 pReader=new PopulationReaderMatsimV5(scenario);
		
		pReader.readFile("c:/data/abmt/input/plans_1perc.xml");
		
		
		LinkedList<PParking> inputCollection=new LinkedList<>();
		
		LinkedList<PParking> outputCollection=new LinkedList<>();
		
		ParkingHerbieControler.readParkings(1.0, "C:\\data\\ABMT\\privateParkings_v1_kti.xml", inputCollection);
		
		
		for (Person person:scenario.getPopulation().getPersons().values()){
			
			for (PlanElement pe:person.getSelectedPlan().getPlanElements()){
				if (pe instanceof ActivityImpl){
					ActivityImpl act=(ActivityImpl) pe;
					
					System.out.println(act.getType());
					System.out.println(act.getCoord());
					System.out.println();
				}
			}
			
		}
		
		
		
		
		
		
		for (PParking parking:inputCollection){
			
			
			PrivateParking privateParking=(PrivateParking) parking;
			
			System.out.println(privateParking.getActInfo().getActType());
			
			
			
		//	PersonalParking personalParking=new PersonalParking(new Coord(0, 1), personId)
		}
		
		ParkingImpl newParking=new ParkingImpl(new Coord(0, 1));
		
		
		newParking.setParkingId(Id.create("personalParking_1", PParking.class));
		
		outputCollection.add(newParking);
		
		
	}

}
