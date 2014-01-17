/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.data;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;

/**
 * @author tthunig
 *
 */
public class TtMorningCommodityAsMatsimPopWriter {

	private static final Logger log = Logger.getLogger(TtMorningCommodityAsMatsimPopWriter.class);
	
	private Population population;

	private Network network; // not necessary

	private DgIdConverter idConverter;
	
	private double startTimeSecMorningPeak = 5.5 * 3600.0;
	private double endTimeSecMorningPeak = 9.5 * 3600.0;
	private double startTimeSecEveningPeak = 13.5 * 3600.0;
	private double endTimeSecEveningPeak = 18.5 * 3600.0;
	
	public void writePlansFile(Network network, DgIdConverter idConverter, DgCommodities commodities, String outputDirectory,
			String filename, double startTimeSecMorningPeak, double endTimeSecMorningPeak) {

		this.network = network;
		this.idConverter = idConverter;
		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		this.startTimeSecMorningPeak = startTimeSecMorningPeak;
		this.endTimeSecMorningPeak = endTimeSecMorningPeak;
		
		// create a person for each flow unit of each commodity (source-drain pairs)
		// assumes that the persons drive home in the evening peak 
		for (DgCommodity com : commodities.getCommodities().values()){
			for (int i=0; i<com.getFlow(); i++){
				Person person = population.getFactory().createPerson(new IdImpl(com.getId().toString()+i));
				Plan plan = population.getFactory().createPlan();
				plan.addActivity(createSourceAct(com, false));
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				plan.addActivity(createDrainAct(com));
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				plan.addActivity(createSourceAct(com, true));
				person.addPlan(plan);
				population.addPerson(person);
			}
		}
		
		//write population as plans file
		String[] fileAttributes = filename.split("_");
		String outputFile = outputDirectory + "all_day_plans_from_morning_peak_ks_commodities_minFlow" + fileAttributes[2] + ".xml";
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write(outputFile);
	}

	private Activity createDrainAct(DgCommodity com) {
		Activity drain = population.getFactory().createActivityFromLinkId("work", convertToCrossingId2LinkId(com.getDrainNodeId()));
		drain.setEndTime(createEndTime(startTimeSecEveningPeak, endTimeSecEveningPeak));
		return drain;
	}

	private Activity createSourceAct(DgCommodity com, boolean endActivity) {
		Activity source = population.getFactory().createActivityFromLinkId("home", convertFromCrossingId2LinkId(com.getSourceNodeId()));
		if (!endActivity){
			source.setEndTime(createEndTime(startTimeSecMorningPeak, endTimeSecMorningPeak));
		}
		return source;
	}

	private double createEndTime(double minTime, double maxTime) {
		// create uniformly distributed activity end time in the given time interval
		double r = Math.random();
		return minTime + r*(maxTime-minTime);
	}

	private Id convertFromCrossingId2LinkId(Id crossingId) {
		Integer integerId = Integer.parseInt(crossingId.toString());
		Id symbolicId = idConverter.getSymbolicId(integerId);
		Id linkId = idConverter.convertFromCrossingNodeId2LinkId(symbolicId);
		return linkId;
	}
	
	private Id convertToCrossingId2LinkId(Id crossingId) {
		Integer integerId = Integer.parseInt(crossingId.toString());
		Id symbolicId = idConverter.getSymbolicId(integerId);
		Id linkId = idConverter.convertToCrossingNodeId2LinkId(symbolicId);
		return linkId;
	}

}
