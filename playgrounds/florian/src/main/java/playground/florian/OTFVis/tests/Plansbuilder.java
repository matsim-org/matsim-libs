package playground.florian.OTFVis.tests;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

public class Plansbuilder {
	
	private static double SAMPLE_SIZE = 1.;
	private static String NETWORK = "./src/main/java/playground/florian/OTFVis/tests/network.xml";
	private static String OUTPUT_FOLDER = "./src/main/java/playground/florian/OTFVis/tests/";

	public static void main(String[] args) {
		
		// Öffne Szenario
		ScenarioImpl sc = new ScenarioImpl();
		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(NETWORK);
		PopulationImpl pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();
		
		// Gestalte Pläne
		for(int i=0;i<201;i++){
			Id id =new IdImpl(i);
			PersonImpl person = (PersonImpl) pb.createPerson(id);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			person.addPlan(plan);
			
			// Activity 1
			Id linkId = new IdImpl((i % 4)+1);
			Activity act = plan.createAndAddActivity("h", linkId);
			act.setEndTime(i*216);
			plan.addLeg(pb.createLeg(TransportMode.car));
			
			// Activity 2
			int j = (i % 4) + 3;
			if (j>4){j=j-4;}
			Id linkId2 = new IdImpl(j);
			Activity act2 = plan.createAndAddActivity("w", linkId2);
			act2.setStartTime(i*216);
			act2.setEndTime(i*216+43200);
			plan.addLeg(pb.createLeg(TransportMode.car));
			
			// Activity 3
			Activity act3 = plan.createAndAddActivity("h", linkId);
			act3.setStartTime(i*216+43200);
			
			pop.addPerson(person);
		}
		
		// Output
		Long size = Math.round(200. * SAMPLE_SIZE); 
		String OUTPUT = OUTPUT_FOLDER + "plans" + size.toString() + ".xml";
		new PopulationWriter(pop,net,SAMPLE_SIZE).writeFile(OUTPUT);
		System.out.println("Output in " + OUTPUT + " is done");
		
	}

}
