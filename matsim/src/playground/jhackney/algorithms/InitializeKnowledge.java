package playground.jhackney.algorithms;

import java.io.File;
import java.util.Iterator;

import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledge;
import org.matsim.knowledges.Knowledges;

import playground.jhackney.socialnetworks.io.ActivityActReader;
import playground.jhackney.socialnetworks.mentalmap.MentalMap;
import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class InitializeKnowledge {
	public InitializeKnowledge(final PopulationImpl plans, final ActivityFacilitiesImpl facilities, Knowledges knowledges){

		ActivityActReader aar = null;


		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)


//		Attempt to open file of mental maps and read it in
		System.out.println("  Opening the file to read in the map of Acts to Facilities");
		aar = new ActivityActReader(Integer.valueOf(Gbl.getConfig().socnetmodule().getInitIter()).intValue());

		String fileName = Gbl.getConfig().socnetmodule().getInDirName()+ "ActivityActMap"+Integer.valueOf(Gbl.getConfig().socnetmodule().getInitIter()).intValue()+".txt";

		if (new File(fileName).exists()) {
			// File or directory exists
			aar.openFile(fileName);
		} else {
			// File or directory does not exist
			aar=null;
		}


		System.out.println(" ... done");

		Iterator<PersonImpl> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			PersonImpl person=p_it.next();

			Knowledge k = knowledges.getKnowledgesByPersonId().get(person.getId());
			if(k ==null){
				k = knowledges.getFactory().createKnowledge(person.getId(), "created by " + this.getClass().getName());
			}
			for (int ii = 0; ii < person.getPlans().size(); ii++) {
				PlanImpl plan = person.getPlans().get(ii);

				// TODO balmermi: double check if this is the right place to create the MentalMap and the EgoNet
				if (person.getCustomAttributes().get(MentalMap.NAME) == null) { person.getCustomAttributes().put(MentalMap.NAME,new MentalMap(k)); }
				if (person.getCustomAttributes().get(EgoNet.NAME) == null) { person.getCustomAttributes().put(EgoNet.NAME,new EgoNet()); }

				// JH Hack to make sure act types are compatible with social nets
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).prepareActs(plan);
				// JH If the Acts are not initialized with a Facility they get a random Facility on the Link
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).initializeActActivityMapRandom(plan);
				// JH If there is a user-supplied file of Facilities for the Act, read it in
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).initializeActActivityMapFromFile(plan,facilities, aar);
			}
		}
		if(aar!=null){
			aar.close();//close the file with the input act-activity map
		}
	}
}
