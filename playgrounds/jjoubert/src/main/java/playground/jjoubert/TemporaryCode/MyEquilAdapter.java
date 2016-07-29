package playground.jjoubert.TemporaryCode;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

public class MyEquilAdapter {
	private static final Logger log = Logger.getLogger(MyEquilAdapter.class);
	private Scenario sc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {	
		MyEquilAdapter mep = new MyEquilAdapter();

		/* Read network */
		mep.readNetwork(args[0]);
		
		/* Reduce the network capacity */
		mep.reduceNetworkCapacity(args[1], Double.parseDouble(args[2]));
		
		/* Create plans */
		mep.createPlans(args[3], Integer.parseInt(args[4]));
		
	}
	

	/**
	 * Changing the network's capacity. Values smaller than 1.0 implies a reduction,
	 * while values greater than 1.0 implies an increase.
	 * @param fraction
	 */
	public void reduceNetworkCapacity(String outputFilename, double fraction) {
		log.info("Changing the network capacity. Multiplying by " + fraction);
		int counter = 0;
		int multiplier = 1;
		
		for(Id<Link> id : sc.getNetwork().getLinks().keySet()){
			double old = sc.getNetwork().getLinks().get(id).getCapacity();
			sc.getNetwork().getLinks().get(id).setCapacity(old*fraction);
			
			/* Report progress */
			if(++counter == multiplier){
				log.info("   Links adapted: " + counter);
				multiplier *= 2;
			}
		}
		log.info("   Links adapted: " + counter + " (Done)");
		
		NetworkWriter nw = new NetworkWriter(sc.getNetwork());
		nw.write(outputFilename);
	}


	private void createPlans(String plansfile, int numberOfPlans) {
        PopulationFactory pf = sc.getPopulation().getFactory();
		for(int i = 0; i < numberOfPlans; i++){
			Plan plan = pf.createPlan();
			Activity h1 = pf.createActivityFromLinkId("home", Id.create("1", Link.class));
			h1.setEndTime(21600); // 06:00:00
			plan.addActivity(h1);
			
			Leg l1 = pf.createLeg("car");
			plan.addLeg(l1);
			
			Activity w = pf.createActivityFromLinkId("work", Id.create("20", Link.class));
			w.setStartTime(25200); // 07:00:00
			w.setEndTime(61200); // 17:00:00
			plan.addActivity(w);
			
			Leg l2 = pf.createLeg("car");
			plan.addLeg(l2);
			
			Activity h2 = pf.createActivityFromLinkId("home", Id.create("1", Link.class));
			h2.setStartTime(64800); // 18:00:00
			plan.addActivity(h2);
			
			Person p = pf.createPerson(Id.create(i, Person.class));
			p.addPlan(plan);
			sc.getPopulation().addPerson(p);
		}
		
		PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		pw.write(plansfile);
		
	}

	public MyEquilAdapter() {
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	public void readNetwork(String networkFilename) {
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc.getNetwork());
		nr.readFile(networkFilename);
	}

}
