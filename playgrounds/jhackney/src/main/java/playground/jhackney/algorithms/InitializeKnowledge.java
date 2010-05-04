package playground.jhackney.algorithms;

import java.io.File;
import java.util.Iterator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.gbl.Gbl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;

import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.socialnetworks.io.ActivityActReader;
import playground.jhackney.socialnetworks.mentalmap.MentalMap;
import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class InitializeKnowledge {
	public InitializeKnowledge(final Population plans, final ActivityFacilities facilities, Knowledges knowledges, Network network){

		ActivityActReader aar = null;


		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)


//		Attempt to open file of mental maps and read it in
		System.out.println("  Opening the file to read in the map of Acts to Facilities");
		aar = new ActivityActReader(Integer.valueOf(((SocNetConfigGroup) Gbl.getConfig().getModule(SocNetConfigGroup.GROUP_NAME)).getInitIter()).intValue());

		String fileName = ((SocNetConfigGroup) Gbl.getConfig().getModule(SocNetConfigGroup.GROUP_NAME)).getInDirName()+ "ActivityActMap"+Integer.valueOf(((SocNetConfigGroup) Gbl.getConfig().getModule(SocNetConfigGroup.GROUP_NAME)).getInitIter()).intValue()+".txt";

		if (new File(fileName).exists()) {
			// File or directory exists
			aar.openFile(fileName);
		} else {
			// File or directory does not exist
			aar=null;
		}


		System.out.println(" ... done");

		Iterator<? extends Person> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person person=p_it.next();

			KnowledgeImpl k = knowledges.getKnowledgesByPersonId().get(person.getId());
			if(k ==null){
				k = knowledges.getFactory().createKnowledge(person.getId(), "created by " + this.getClass().getName());
			}
			for (int ii = 0; ii < person.getPlans().size(); ii++) {
				Plan plan = person.getPlans().get(ii);

				// TODO balmermi: double check if this is the right place to create the MentalMap and the EgoNet
				if (person.getCustomAttributes().get(MentalMap.NAME) == null) { person.getCustomAttributes().put(MentalMap.NAME,new MentalMap(k, network)); }
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
