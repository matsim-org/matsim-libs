package playground.wrashid.PHV.Triangle;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.population.Knowledge;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.world.World;

public class CreatePlans {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO: am schluss alle meiste pfade in config.xml reintun...
		Population plans = new Population(false);
		Gbl.reset();
		args=new String[1];
		args[0]="C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/config.xml";
		Gbl.createConfig(args);
		Gbl.getConfig().plans().setOutputFile("C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/5000plan/plans.xml");
		final World world = Gbl.getWorld();

		// read facilities
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile("C:/data/SandboxCVS/ivt/studies/wrashid/Energy and Transport/triangle/facilities/facilities.xml");


		// get home and work activity
		Activity home=null;
		Activity work=null;
		for (Facility f : facilities.getFacilities().values()) {
			Iterator<Activity> a_it = f.getActivities().values().iterator();
			while (a_it.hasNext()) {
				Activity a = a_it.next();
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
			Person person = new Person(new IdImpl(i));
			plans.addPerson(person);


			Knowledge k = person.createKnowledge("");
			k.addActivity(home);
			k.addActivity(work);

			Plan plan = person.createPlan(true);
			Facility home_facility = person.getKnowledge().getActivities("home").get(0).getFacility();
			Facility work_facility = person.getKnowledge().getActivities("work").get(0).getFacility();
			ArrayList<Activity> acts = person.getKnowledge().getActivities();

			double depTime=3600*8;
			double duration=3600*8;

			plan.createAct("home",home_facility.getCenter().getX(),home_facility.getCenter().getY(),home_facility.getLink(),0.0,depTime,duration,false);
			plan.createLeg(BasicLeg.Mode.car,depTime,0.0,depTime);
			plan.createAct("work",work_facility.getCenter().getX(),work_facility.getCenter().getY(),work_facility.getLink(),depTime,depTime+duration,duration,false);
			plan.createLeg(BasicLeg.Mode.car,depTime+duration,0.0,depTime+duration);
			plan.createAct("home",home_facility.getCenter().getX(),home_facility.getCenter().getY(),home_facility.getLink(),depTime+duration,3600*24,duration,false);
			// assign home-work-home activities to each person


			Leg l=null;

		}



		new PopulationWriter(plans).write();
	}

}
