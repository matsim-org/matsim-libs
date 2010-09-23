package playground.wrashid.PHEV.Triangle;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;

public class CreatePlans1 {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		args=new String[1];
		args[0]="C:/data/SandboxCVS/ivt/studies/triangle/config/config.xml";
		Config config = ConfigUtils.loadConfig(args[0]);
//		config.plans().setOutputFile("C:/data/SandboxCVS/ivt/studies/triangle/plans/100Kplans/plans_hwsh.xml");

		ScenarioImpl scenario = new ScenarioImpl(config);
		Population plans = scenario.getPopulation();
		Knowledges knowledges = scenario.getKnowledges();

		// read facilities
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile("C:/data/SandboxCVS/ivt/studies/triangle/facilities/facilities.xml");


		// get home and work activity
		ActivityOption home=null;
		ActivityOption work=null;
		ActivityOption shop=null;
		for (ActivityFacility f : facilities.getFacilities().values()) {
			Iterator<ActivityOption> a_it = f.getActivityOptions().values().iterator();
			while (a_it.hasNext()) {
				ActivityOption a = a_it.next();
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
			k.addActivityOption(home,false);
			k.addActivityOption(work,false);
			k.addActivityOption(shop,false);

			PlanImpl plan = person.createAndAddPlan(true);
			ActivityFacilityImpl home_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("home").get(0).getFacility();
			ActivityFacilityImpl work_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("work").get(0).getFacility();
			ActivityFacilityImpl shop_facility = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities("shop").get(0).getFacility();
			ArrayList<? extends ActivityOption> acts = knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities();

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
			a.setLinkId(home_facility.getLinkId());
			a.setEndTime(depTimeHome);
			LegImpl l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTimeHome);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeHome);
			a = plan.createAndAddActivity("work",work_facility.getCoord());
			a.setLinkId(work_facility.getLinkId());
			a.setStartTime(depTimeHome);
			a.setEndTime(depTimeWork);
			a.setDuration(depTimeWork-depTimeHome);
			l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTimeWork);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeWork);
			a = plan.createAndAddActivity("shop",shop_facility.getCoord());
			a.setLinkId(shop_facility.getLinkId());
			a.setStartTime(depTimeWork);
			a.setEndTime(depTimeShop);
			a.setDuration(depTimeShop-depTimeWork);
			l = plan.createAndAddLeg(TransportMode.car);
			l.setArrivalTime(depTimeShop);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeShop);
			a = plan.createAndAddActivity("home",home_facility.getCoord());
			a.setLinkId(home_facility.getLinkId());
			// assign home-work-home activities to each person


//			Leg l=null;

		}



		new PopulationWriter(plans, scenario.getNetwork()).write(null);//config.plans().getOutputFile());
	}

}
