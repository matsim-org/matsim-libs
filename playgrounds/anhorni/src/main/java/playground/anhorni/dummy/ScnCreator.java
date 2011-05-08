package playground.anhorni.dummy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class ScnCreator {
	
	private final static Logger log = Logger.getLogger(ScnCreator.class);
	
	private Scenario origScenario;
	private Scenario newScenario;
	
	private static double sampleRate = 0.01;
	
	Random random = new Random();
	
	private String facilitiesFile = "./input/zhcut/facilities.xml.gz";
	private String networkFile = "./input/zhcut/network.xml";
	private String plansFile = "./input/zhcut/plans.xml.gz";
	private String municipalities = "./input/swiss_municipalities.txt";
	
	private String businessCensusOutFile = "C:/l/andreasrep/coding/input/tutorial/business_census.txt";
	private String censusOutFile = "C:/l/andreasrep/coding/input/tutorial/census.txt";
	private String pusPersonsOutFile = "C:/l/andreasrep/coding/input/tutorial/travelsurvey_persons.txt";
	private String pusTripsOutFile = "C:/l/andreasrep/coding/input/tutorial/travelsurvey_trips.txt";
	
	// --------------------------------------------------------------------------
	public static void main(String[] args) {
		ScnCreator creator = new ScnCreator();
		creator.run();		
	}
	
	private void run() {
		this.init();
		this.smearFacilities();
		this.smearPlans();	
		this.writeFacilities();
		List<Id> sampledPersons = this.writePUSPersons();
		this.writePUSTrips(sampledPersons);
		this.writeCensus();
		log.info("Creation finished ......................");
	}
	
	private void init() {
		this.origScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(this.origScenario).readFile(networkFile);
		new FacilitiesReaderMatsimV1((ScenarioImpl)this.origScenario).readFile(this.facilitiesFile);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.origScenario);
		populationReader.readFile(this.plansFile);
		log.info("Original population size " + this.origScenario.getPopulation().getPersons().size());
	}
	
	private void smearPlans() {
		int cnt = 0;
		for (Person p : this.origScenario.getPopulation().getPersons().values()) {
			p.setId(new IdImpl(cnt));
			Plan plan = p.getSelectedPlan();
			
			ActivityImpl homeAct = (ActivityImpl)plan.getPlanElements().get(0);
			Coord homeLocation = homeAct.getCoord();
			Coord newHomeLocation = new CoordImpl(homeLocation.getX() + this.randomize(), homeLocation.getY() + this.randomize());
			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().startsWith("h")) {
						act.setCoord(newHomeLocation);
					}
					else {
						Coord newCoord = new CoordImpl(act.getCoord().getX() + this.randomize(), act.getCoord().getY() + this.randomize());
						act.setCoord(newCoord);
					}
				}
			}
			cnt++;
		}
	}
		
	private void smearFacilities() {	
		for (ActivityFacility facility : ((ScenarioImpl)this.origScenario).getActivityFacilities().getFacilities().values()) {
			// do not add home facilities
			if (facility.getActivityOptions().size() == 1 && facility.getActivityOptions().containsKey("home")) continue;
			Coord coord = facility.getCoord();
			double xNew = coord.getX() + this.randomize();
			double yNew = coord.getY() + this.randomize(); 
			Coord newCoord = new CoordImpl(xNew, yNew);
			
			ActivityFacilityImpl newFacility = (ActivityFacilityImpl)
			((ScenarioImpl)this.newScenario).getActivityFacilities().createFacility(facility.getId(), newCoord);
			
			for (ActivityOption actOption : facility.getActivityOptions().values()) {
				newFacility.createActivityOption(actOption.getType());
			}
		}
	}
	
	private double randomize() {
		double sign = random.nextDouble() - 0.5;
		double deltaL = random.nextInt(2000);
		double dp = random.nextDouble();
		
		return sign * deltaL + dp;
	}
	
	private void writeCensus() {
		int censusOffset = 10000;
		
	}
	
	private List<Id> writePUSPersons() {
		
		// only take every third person! PUS is smaller than census!
		List<Id> sampledPersons = new Vector<Id>();
		
		log.info("Writing PUS persons");
		try {	
			String header = "PersonId\tsurvey_day";
			final BufferedWriter out = IOUtils.getBufferedWriter(this.pusPersonsOutFile);
			out.write(header);
			out.newLine();	
				
			for (Person p : ((ScenarioImpl) this.origScenario).getPopulation().getPersons().values()) {
				if (random.nextDouble() <= sampleRate) {
					sampledPersons.add(p.getId());
					out.write(p.getId().toString() + "\t" + random.nextInt(365));
					out.newLine();
				}
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					Gbl.errorMsg(e);
		}
		log.info("Number of PUS persons after sampling: " + sampledPersons.size());
		return sampledPersons;
	}
			
	private void writePUSTrips(List<Id> sampledPersons) {
		log.info("Writing PUS trips");
		try {	
			String header = "PersonId\tTripId\txCoordOrigin\tyCoordOrigin\txCoordDestination\tyCoordDestination\tactivityDuration\tmode\tactivityType";
			final BufferedWriter out = IOUtils.getBufferedWriter(this.pusTripsOutFile);
			out.write(header);
			out.newLine();	
				
			for (Person p : ((ScenarioImpl) this.origScenario).getPopulation().getPersons().values()) {	
				if (!(sampledPersons.contains(p.getId()))) continue;
				
				PlanImpl plan = (PlanImpl) p.getSelectedPlan();
				
				int cnt = 0;
				for (PlanElement pe : plan.getPlanElements()) {
					String line = p.getId().toString() + "\t" + cnt + "\t";
					if (pe instanceof Leg) {
						LegImpl leg = (LegImpl)pe;
						Activity previousActivity = plan.getPreviousActivity(leg);
						Activity nextAct = plan.getNextActivity(leg);
						line += previousActivity.getCoord().getX() + "\t" + previousActivity.getCoord().getY() + "\t";
						line += nextAct.getCoord().getX() + "\t" + nextAct.getCoord().getY() + "\t";
						
						double duration = nextAct.getMaximumDuration();
						if (!(duration > 0.0)) {
							duration = 24.0 * 3600.0 - previousActivity.getEndTime();
						}
						line += duration + "\t";
						line += leg.getMode() + "\t";
						line += this.convert(nextAct.getType());
						out.write(line);
						out.newLine();
						cnt++;
					}
				}				
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					Gbl.errorMsg(e);
		}
	}
	
	private String convert(String type) {
		if (type.startsWith("h")) return "home";
		else if (type.startsWith("s")) return "shop";
		else if (type.startsWith("l")) return "leisure";
		else if (type.startsWith("w")) return "work";
		else return "education";
	}
		
	private void writeFacilities() {
		log.info("Writing " + ((ScenarioImpl) this.newScenario).getActivityFacilities().getFacilities().values().size() + " facilities");
		try {	
			String header = "Id\txCoord\tyCoord\tTypes";
			final BufferedWriter out = IOUtils.getBufferedWriter(this.businessCensusOutFile);
			out.write(header);
			out.newLine();	
				
			for (ActivityFacility facility : ((ScenarioImpl) this.newScenario).getActivityFacilities().getFacilities().values()) {
				out.write(facility.getId().toString() + "\t" + facility.getCoord().getX() + "\t" + facility.getCoord().getY() + "\t");
				
				for (ActivityOption actOption : facility.getActivityOptions().values()) {
					out.write(actOption.getType() + ",");
				}
				out.newLine();
			}
			out.flush();
			out.flush();			
			out.flush();
			out.close();
		}catch (final IOException e) {
					Gbl.errorMsg(e);
		}
	}
}
