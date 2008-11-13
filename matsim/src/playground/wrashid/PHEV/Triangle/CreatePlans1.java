package playground.wrashid.PHEV.Triangle;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Knowledge;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.world.World;

public class CreatePlans1 {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO: am schluss alle meiste pfade in config.xml reintun...
		Population plans = new Population(false);
		Gbl.reset();
		args=new String[1];
		args[0]="C:/data/SandboxCVS/ivt/studies/triangle/config/config.xml";
		Gbl.createConfig(args);
		Gbl.getConfig().plans().setOutputFile("C:/data/SandboxCVS/ivt/studies/triangle/plans/1000plans/plans_hwsh.xml");
		final World world = Gbl.getWorld();

		// read facilities
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile("C:/data/SandboxCVS/ivt/studies/triangle/facilities/facilities.xml");


		// get home and work activity
		Activity home=null;
		Activity work=null;
		Activity shop=null;
		for (Facility f : facilities.getFacilities().values()) {
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {
				Activity a = a_it.next();
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
		for (int i=0;i<1000;i++){
			Person person = new PersonImpl(new IdImpl(i));
			plans.addPerson(person);


			Knowledge k = person.createKnowledge("");
			k.addActivity(home,false);
			k.addActivity(work,false);
			k.addActivity(shop,false);

			Plan plan = person.createPlan(true);
			Facility home_facility = person.getKnowledge().getActivities("home").get(0).getFacility();
			Facility work_facility = person.getKnowledge().getActivities("work").get(0).getFacility();
			Facility shop_facility = person.getKnowledge().getActivities("shop").get(0).getFacility();
			ArrayList<Activity> acts = person.getKnowledge().getActivities();

			double depTimeHome=3600*8;
			double depTimeWork=3600*16;
			double depTimeShop=3600*17.5;
			double mitterNacht=3600*24;
			double duration=3600*8;

			
			// home: 0:00-8:00
			// work: 8-16
			// shop: 16-17.30
			// home: 17.30-0:00
			
			Act a = plan.createAct("home",home_facility.getCenter());
			a.setLink(home_facility.getLink());
			a.setEndTime(depTimeHome);
			Leg l = plan.createLeg(BasicLeg.Mode.car);
			l.setArrivalTime(depTimeHome);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeHome);
			a = plan.createAct("work",work_facility.getCenter());
			a.setLink(work_facility.getLink());
			a.setStartTime(depTimeHome);
			a.setEndTime(depTimeWork);
			a.setDur(depTimeWork-depTimeHome);
			l = plan.createLeg(BasicLeg.Mode.car);
			l.setArrivalTime(depTimeWork);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeWork);
			a = plan.createAct("shop",shop_facility.getCenter());
			a.setLink(shop_facility.getLink());
			a.setStartTime(depTimeWork);
			a.setEndTime(depTimeShop);
			a.setDur(depTimeShop-depTimeWork);
			l = plan.createLeg(BasicLeg.Mode.car);
			l.setArrivalTime(depTimeShop);
			l.setTravelTime(0.0);
			l.setDepartureTime(depTimeShop);
			a = plan.createAct("home",home_facility.getCenter());
			a.setLink(home_facility.getLink());
			// assign home-work-home activities to each person


//			Leg l=null;

		}



		new PopulationWriter(plans).write();
	}

}
