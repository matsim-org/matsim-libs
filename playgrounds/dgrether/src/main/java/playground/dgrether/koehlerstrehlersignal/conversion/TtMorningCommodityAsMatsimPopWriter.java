/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.conversion;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;

/**
 * Class to write the BTU commodities (which came from agents 
 * traveling in the morning peak) as MATSim population.
 * 
 * Uniformly distributes the agents activity end times in the 
 * given time interval.
 * 
 * @author tthunig
 */
public class TtMorningCommodityAsMatsimPopWriter {

	private static final Logger log = Logger.getLogger(TtMorningCommodityAsMatsimPopWriter.class);
	
	private Population population;

	private Network network;
	
	private double startTimeSecMorningPeak = 5.5 * 3600.0;
	private double endTimeSecMorningPeak = 9.5 * 3600.0;
	private double startTimeSecEveningPeak = 13.5 * 3600.0;
	private double endTimeSecEveningPeak = 18.5 * 3600.0;
	
	/**
	 * Writes the BTU commodities as MATSim population with single
	 * dummy-dummy trips.
	 * 
	 * @param network the MATSim network
	 * @param commodities the BTU commodities
	 * @param outputDirectory
	 * @param filename
	 * @param startTimeSecMorningPeak the second where the morning peak starts
	 * @param endTimeSecMorningPeak the second where the morning peak ends
	 */
	public void writeTripPlansFile(Network network, DgCommodities commodities, 
			String outputDirectory, String filename, double startTimeSecMorningPeak, 
			double endTimeSecMorningPeak) {

		this.network = network;
		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		this.startTimeSecMorningPeak = startTimeSecMorningPeak;
		this.endTimeSecMorningPeak = endTimeSecMorningPeak;
		
		// create a person for each flow unit of each commodity (source-drain pairs)
		// in the morning peak as single dummy-dummy trip
		for (DgCommodity com : commodities.getCommodities().values()){
			for (int i=0; i<com.getFlow(); i++){
				Person person = population.getFactory().createPerson(
						Id.create(com.getId().toString()+i, Person.class));
				Plan plan = population.getFactory().createPlan();
				plan.addActivity(createDummySourceAct(com));
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				plan.addActivity(createDummyDrainAct(com));
				person.addPlan(plan);
				population.addPerson(person);
			}
		}
		
		//write population as plans file
		String[] fileAttributes = filename.split("_");
		String outputFile = outputDirectory + "trip_plans_from_morning_peak_ks_commodities_minFlow"
				+ fileAttributes[2] + ".xml";
		MatsimWriter popWriter = new PopulationWriter(population, this.network);
		popWriter.write(outputFile);
		log.info("plans file of simplified population written to " + outputFile);
	}
	
	/**
	 * Writes the BTU commodities as MATSim population with home-work-home trips.
	 * 
	 * @param network the MATSim network
	 * @param commodities the BTU commodities
	 * @param outputDirectory
	 * @param filename
	 * @param startTimeSecMorningPeak the second where the morning peak starts
	 * @param endTimeSecMorningPeak the second where the morning peak ends
	 */
	public void writeHomeWorkHomePlansFile(Network network, DgCommodities commodities, 
			String outputDirectory, String filename, double startTimeSecMorningPeak, 
			double endTimeSecMorningPeak) {

		this.network = network;
		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		this.startTimeSecMorningPeak = startTimeSecMorningPeak;
		this.endTimeSecMorningPeak = endTimeSecMorningPeak;
		
		// create a person for each flow unit of each commodity (source-drain pairs)
		// assume that the persons drive home in the evening peak 
		for (DgCommodity com : commodities.getCommodities().values()){
			for (int i=0; i<com.getFlow(); i++){
				Person person = population.getFactory().createPerson(
						Id.create(com.getId().toString()+i, Person.class));
				Plan plan = population.getFactory().createPlan();
				plan.addActivity(createHomeAct(com, false));
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				plan.addActivity(createWorkAct(com));
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				plan.addActivity(createHomeAct(com, true));
				person.addPlan(plan);
				population.addPerson(person);
			}
		}
		
		//write population as plans file
		String[] fileAttributes = filename.split("_");
		String outputFile = outputDirectory + "all_day_plans_from_morning_peak_ks_commodities_minFlow"
				+ fileAttributes[2] + ".xml";
		MatsimWriter popWriter = new PopulationWriter(population, this.network);
		popWriter.write(outputFile);
		log.info("plans file of simplified population written to " + outputFile);
	}

	private Activity createWorkAct(DgCommodity com) {
		Activity drain = population.getFactory().createActivityFromLinkId("work", com.getDrainLinkId());
		drain.setEndTime(createEndTime(startTimeSecEveningPeak, endTimeSecEveningPeak));
		return drain;
	}

	private Activity createHomeAct(DgCommodity com, boolean endActivity) {
		Activity source = population.getFactory().createActivityFromLinkId("home", com.getSourceLinkId());
		if (!endActivity){
			source.setEndTime(createEndTime(startTimeSecMorningPeak, endTimeSecMorningPeak));
		}
		return source;
	}
	
	private Activity createDummySourceAct(DgCommodity com) {
		Activity source = population.getFactory().createActivityFromLinkId("dummy", com.getSourceLinkId());
		source.setEndTime(createEndTime(startTimeSecMorningPeak, endTimeSecMorningPeak));
		return source;
	}
	
	private Activity createDummyDrainAct(DgCommodity com) {
		Activity source = population.getFactory().createActivityFromLinkId("dummy", com.getDrainLinkId());
		return source;
	}

	/**
	 * creates an uniformly distributed activity end time in the given time interval
	 * 
	 * @param minTime the interval start in seconds
	 * @param maxTime the interval end in seconds
	 * @return
	 */
	private double createEndTime(double minTime, double maxTime) {
		double r = Math.random();
		return minTime + r*(maxTime-minTime);
	}
}
