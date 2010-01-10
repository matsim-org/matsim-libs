package playground.wrashid.PHEV.Triangle;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.World;

public class CreatePlans1 {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Gbl.reset();
		args=new String[1];
		args[0]="C:/data/SandboxCVS/ivt/studies/triangle/config/config.xml";
		Config config = Gbl.createConfig(args);
		config.plans().setOutputFile("C:/data/SandboxCVS/ivt/studies/triangle/plans/100Kplans/plans_hwsh.xml");

		ScenarioImpl scenario = new ScenarioImpl(config);
		PopulationImpl plans = scenario.getPopulation();
		Knowledges knowledges = scenario.getKnowledges();
		final World world = scenario.getWorld();

		// read facilities
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(facilities).readFile("C:/data/SandboxCVS/ivt/studies/triangle/facilities/facilities.xml");


		// get home and work activity
		ActivityOptionImpl home=null;
		ActivityOptionImpl work=null;
		ActivityOptionImpl shop=null;
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			Iterator<ActivityOptionImpl> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOptionImpl a = a_it.next();
				//System.out.println(a.getType());
				if (a.getType().equals("home")) {
					home=a;
				} else if (a.getType().equals("work")){
					work=a;
				} else if (a.getType().equals("shop")){
					shop=a;
				}
			}
		}






		// create persons
		for (int i=0;i<100000;i++){
			PersonImpl person = new PersonImpl(new IdImpl(i));
			plans.addPerson(person);


			KnowledgeImpl k = knowledges.getFactory().createKnowledge(person.getId(), "");
			k.addActivity(home,false);
			k.addActivity(work,false);
			k.addActivity(shop,false);

			PlanImpl plan = person.createAndAddPlan(true);
			ActivityFacilityImpl home_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("home").get(0).getFacility();
			ActivityFacilityImpl work_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("work").get(0).getFacility();
			ActivityFacilityImpl shop_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("shop").get(0).getFacility();
			ArrayList<ActivityOptionImpl> acts = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities();

			double depTimeHome=3600*8;
			double depTimeWork=3600*16;
			double depTimeShop=3600*17.5;
			double mitterNacht=3600*24;
			double duration=3600*8;


			// home: 0:00-8:00
			// work: 8-16
			// shop: 16-17.30
			// home: 17.30-0:00

			ActivityImpl a = plan.createAndAddActivity("home",home_facility.getCoord());
			a.setLink(home_facility.getLink());
			a.setEndTime(depTimeHome);
			LegImpl l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTimeHome);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeHome);
			a = plan.createAndAddActivity("work",work_facility.getCoord());
			a.setLink(work_facility.getLink());
			a.setStartTime(depTimeHome);
			a.setEndTime(depTimeWork);
			a.setDuration(depTimeWork-depTimeHome);
			l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTimeWork);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeWork);
			a = plan.createAndAddActivity("shop",shop_facility.getCoord());
			a.setLink(shop_facility.getLink());
			a.setStartTime(depTimeWork);
			a.setEndTime(depTimeShop);
			a.setDuration(depTimeShop-depTimeWork);
			l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTimeShop);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeShop);
			a = plan.createAndAddActivity("home",home_facility.getCoord());
			a.setLink(home_facility.getLink());
			// assign home-work-home activities to each person


//			Leg l=null;

		}



		new PopulationWriter(plans).writeFile(config.plans().getOutputFile());
	}

}
