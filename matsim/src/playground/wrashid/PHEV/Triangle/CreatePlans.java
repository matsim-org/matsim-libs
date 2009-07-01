package playground.wrashid.PHEV.Triangle;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.Knowledge;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.matsim.world.World;

public class CreatePlans {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO: am schluss alle meiste pfade in config.xml reintun...
		Population plans = new PopulationImpl();
		Knowledges knowledges = new KnowledgesImpl();
		Gbl.reset();
		args=new String[1];
		args[0]="C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/config.xml";
		Config config = Gbl.createConfig(args);
		config.plans().setOutputFile("C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/5000plan/plans.xml");
		final World world = Gbl.createWorld();

		// read facilities
		ActivityFacilities facilities = (ActivityFacilities)world.createLayer(ActivityFacilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile("C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/facilities/facilities.xml");


		// get home and work activity
		ActivityOption home=null;
		ActivityOption work=null;
		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOption a = a_it.next();
				//System.out.println(a.getType());
				if (a.getType().equals("home")) {
					home=a;
				} else if (a.getType().equals("work")){
					work=a;
				}
			}
		}






		// create 100 persons
		for (int i=0;i<5000;i++){
			PersonImpl person = new PersonImpl(new IdImpl(i));
			plans.addPerson(person);


			Knowledge k = knowledges.getBuilder().createKnowledge(person.getId(), "");
			k.addActivity(home, false);
			k.addActivity(work, false);

			PlanImpl plan = person.createPlan(true);
			ActivityFacility home_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("home").get(0).getFacility();
			ActivityFacility work_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("work").get(0).getFacility();
			ArrayList<ActivityOption> acts = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities();

			double depTime=3600*8;
			double duration=3600*8;

			ActivityImpl a = plan.createActivity("home",home_facility.getCoord());
			a.setLink(home_facility.getLink());
			a.setEndTime(depTime);
			LegImpl l = plan.createLeg(TransportMode.car);
			l.setArrivalTime(depTime);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTime);
			a = plan.createActivity("work",work_facility.getCoord());
			a.setLink(work_facility.getLink());
			a.setStartTime(depTime);
			a.setEndTime(depTime+duration);
			a.setDuration(duration);
			l = plan.createLeg(TransportMode.car);
			l.setArrivalTime(depTime+duration);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTime+duration);
			a = plan.createActivity("home",home_facility.getCoord());
			a.setLink(home_facility.getLink());
			// assign home-work-home activities to each person


//			Leg l=null;

		}



		new PopulationWriter(plans).write();
	}

}
