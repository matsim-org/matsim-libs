package playground.balac.twowaycarsharingredisigned.membership;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;



public class MembershipAssigner {

	private TwoWayCSVehicleLocation stations;
	private Scenario scenario;
	private ObjectAttributes personAtt;
	
	private Map<Id<Person>, Double> accessHome;
	private Map<Id<Person>, Double> accessWork;
	
	static final double B_Yes_Access_Home = 0.163D;
	static final double B_Yes_Access_Work = 0.0563D;
	static final double B_Yes_Density = 0.0D;
	static final double B_Yes_31_45 = 0.436D;
	static final double B_yes_CAR_NEV = 1.14D;
	static final double B_yes_CAR_SOM = 2.56D;
	static final double B_yes_MALE = 0.197D;
	static final double KONST_NO = 5.23D;
	static final double B_No_18_30 = 0.791D;
	static final double B_No_60 = 0.43D;
	
	public MembershipAssigner (TwoWayCSVehicleLocation stations, Scenario scenario, ObjectAttributes personAtt) {
		
		this.scenario = scenario;
		this.stations = stations;
		this.personAtt = personAtt;
		this.accessHome = new HashMap<Id<Person>, Double>();
		this.accessWork = new HashMap<Id<Person>, Double>();
	
	}
	
	public void init() {
		
		computeAccessHome();
		computeAccessWork();
	}
	
	
	private void computeAccessHome() {
		// TODO Auto-generated method stub		
		
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
		
		double access = 0.0;

			Coord c = new Coord((1.0D / 0.0D), (1.0D / 0.0D));
		for (PlanElement pe : p.getSelectedPlan().getPlanElements())
	    {
	      if ((pe instanceof Activity))
	      {
	        Activity act = (Activity)pe;

	        if (act.getType().equals("home")) {
	          c = ((ActivityFacility)this.scenario.getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
	          break;
	        }
	      }

	    }
		
	    Collection<TwoWayCSStation> nearbyStations = this.stations.getQuadTree().getDisk(c.getX(), c.getY(), 5000.0D);
	    for (TwoWayCSStation station : nearbyStations) {
	      access += station.getNumberOfVehicles() * Math.exp(-2.0D * 
	    		  CoordUtils.calcEuclideanDistance(this.scenario.getNetwork().getLinks().get(station.getLink().getId()).getCoord(), c) / 1000.0D);
	    }
	    
	    if (access == 0.0) {
	    	
	    	this.accessHome.put(p.getId(), access);
	    }
	    else {
	    	
	    	this.accessHome.put(p.getId(), Math.log(access));
	    }
	    	
	    	
	    
	    
		}
		
	}

	private void computeAccessWork() {
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			
			double access = 0.0;

			Coord c = new Coord((1.0D / 0.0D), (1.0D / 0.0D));
			for (PlanElement pe : p.getSelectedPlan().getPlanElements())
		    {
		      if ((pe instanceof Activity))
		      {
		        Activity act = (Activity)pe;

		        if (act.getType().startsWith("work")) {
		          c = ((ActivityFacility)this.scenario.getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
		          break;
		        }
		      }

		    }
			
		    Collection<TwoWayCSStation> nearbyStations = this.stations.getQuadTree().getDisk(c.getX(), c.getY(), 5000.0D);
		    for (TwoWayCSStation station : nearbyStations) {
		      access += station.getNumberOfVehicles() * Math.exp(-2.0D * 
		    		  CoordUtils.calcEuclideanDistance(this.scenario.getNetwork().getLinks().get(station.getLink().getId()).getCoord(), c) / 1000.0D);
		    }
		    
		    if (access == 0.0) {
		    	
		    	this.accessWork.put(p.getId(), access);
		    }
		    else {
		    	
		    	this.accessWork.put(p.getId(), Math.log(access));
		    }
		    	
		    	
		    
		    
			}
		
	}
	
	
	public void run(String name) {
		int count = 0;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			
			if (PersonUtils.getLicense(p).equals("yes")) {
				
				int x = calcMembership(p);
				if (x == 0) {
					this.personAtt.putAttribute(p.getId().toString(), "RT_CARD", "true");
					count++;
				}
				else {
					
					this.personAtt.putAttribute(p.getId().toString(), "RT_CARD", "false");
				}
				
			}
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(this.personAtt);
		betaWriter.writeFile("./desires_rt_memb_100perc_" + name + ".xml.gz");
		System.out.println(count);
		
	}	
	

	public final int calcMembership(Person pi)
	{
	  double[] utils = new double[2];
	  utils[0] = calcYesUtil(pi);
	  utils[1] = calcNoUtil(pi);
	  double[] probs = calcLogitProbability(utils);
	  double r = MatsimRandom.getRandom().nextDouble();

	  double prob_sum = 0.0D;
	  for (int i = 0; i < probs.length; i++) {
	    prob_sum += probs[i];
	    if (r < prob_sum) return i;
	  }
	  return -1;
	}

	private final double[] calcLogitProbability(double[] utils)
	{
	  double exp_sum = 0.0D;
	  for (int i = 0; i < utils.length; i++) exp_sum += Math.exp(utils[i]);
	  double[] probs = new double[utils.length];
	  for (int i = 0; i < utils.length; i++) probs[i] = (Math.exp(utils[i]) / exp_sum);
	  return probs;
	}
	protected final double calcNoUtil(Person pi) {
	  double util = 0.0D;
	  util += 5.23D * 0.924;
	  if (((PersonUtils.getAge(pi) <= 30 ? 1 : 0) & (PersonUtils.getAge(pi) >= 18 ? 1 : 0)) != 0) util += 0.791D;
	  if (PersonUtils.getAge(pi) >= 60) util += 0.43D;

	  return util;
	}

	protected final double calcYesUtil(Person pi)
	{
	  double util = 0.0D;
	  util += 0.163D * this.accessHome.get(pi.getId());
	  util += 0.0563D * this.accessWork.get(pi.getId());
	  util += 0.0D;

	  if (((PersonUtils.getAge(pi) <= 45 ? 1 : 0) & (PersonUtils.getAge(pi) >= 31 ? 1 : 0)) != 0) util += 0.436D;
	  if (PersonUtils.getCarAvail(pi).equals( "never")) util += 1.14D;
	  if (PersonUtils.getCarAvail(pi).equals( "sometimes")) util += 2.56D;
	  if (PersonUtils.getSex(pi).equals( "m")) util += 0.197D;

	  return util;
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Logger.getLogger( "org.matsim.core.population.PopulationReaderMatsimV4" ).setLevel(Level.OFF);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		FacilitiesReaderMatsimV1 facilitiesReader  = new FacilitiesReaderMatsimV1(scenario);
		networkReader.readFile(args[1]);
		facilitiesReader.readFile(args[3]);

		populationReader.readFile(args[2]);
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(args[4]);
		
		TwoWayCSVehicleLocation stations = new TwoWayCSVehicleLocation(args[0], scenario);
		
		MembershipAssigner assigner = new MembershipAssigner(stations, scenario, bla);
		
		assigner.init();
		assigner.run(args[5]);

	}

}
