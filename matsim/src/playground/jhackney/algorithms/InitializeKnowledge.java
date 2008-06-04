package playground.jhackney.algorithms;

import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.socialnetworks.io.ActivityActReader;

public class InitializeKnowledge {
	public InitializeKnowledge(final Plans plans){

		ActivityActReader aar = null;


		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)


//		If the user has an existing file that maps activities to acts, open it and read it in
		if(Boolean.valueOf(Gbl.getConfig().socnetmodule().getReadMentalMap())){
			System.out.println("  Opening the file to read in the map of Acts to Facilities");
			aar = new ActivityActReader(Integer.valueOf(Gbl.getConfig().socnetmodule().getInitIter()).intValue());

			String fileName = Gbl.getConfig().socnetmodule().getInDirName()+ "/ActivityActMap"+Integer.valueOf(Gbl.getConfig().socnetmodule().getInitIter()).intValue()+".txt";
			aar.openFile(fileName);
			System.out.println(" ... done");
		}

		Iterator<Person> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person person=p_it.next();

			Knowledge k = person.getKnowledge();
			if(k ==null){
				k = person.createKnowledge("created by " + this.getClass().getName());
			}
			for (int ii = 0; ii < person.getPlans().size(); ii++) {
				Plan plan = person.getPlans().get(ii);

				k.map.prepareActs(plan); // Always call this first, to make sure the Acts have a reference Id
				k.map.initializeActActivityMapRandom(plan);
				k.map.initializeActActivityMapFromFile(plan,aar);
			}
		}
		if(Boolean.valueOf(Gbl.getConfig().socnetmodule().getReadMentalMap())){
			aar.close();//close the file with the input act-activity map
		}
	}
}
