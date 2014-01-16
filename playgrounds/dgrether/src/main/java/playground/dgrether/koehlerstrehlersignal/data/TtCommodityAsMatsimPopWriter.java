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
public class TtCommodityAsMatsimPopWriter {

	private static final Logger log = Logger.getLogger(TtCommodityAsMatsimPopWriter.class);
	
	private Population population;

	private Network network;

	private DgIdConverter idConverter;
	
	public void writePlansFile(Network network, DgIdConverter idConverter, DgCommodities commodities, String outputDirectory,
			String filename, double startTimeSec, double endTimeSec) {

		this.network = network;
		this.idConverter = idConverter;
		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		for (DgCommodity com : commodities.getCommodities().values()){
			for (int i=0; i<com.getFlow(); i++){
				Person person = population.getFactory().createPerson(new IdImpl(com.getId().toString()+i));
				Plan plan = population.getFactory().createPlan();
				plan.addActivity(createSourceAct(com, startTimeSec, endTimeSec));
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				plan.addActivity(createDrainAct(com));
				person.addPlan(plan);
				population.addPerson(person);
			}
		}
		
		//write population as plans file
		String[] fileAttributes = filename.split("_");
		String outputFile = outputDirectory + "plansOfKsCommodities_minFlow" + fileAttributes[2] + "_startTime" + fileAttributes[3];
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write(outputFile);
	}

	private Activity createDrainAct(DgCommodity com) {
		Activity drain = population.getFactory().createActivityFromLinkId("work", convertToCrossingId2LinkId(com.getDrainNodeId()));
		return drain;
	}

	private Activity createSourceAct(DgCommodity com, double startTimeSec, double endTimeSec) {
		double r = Math.random();
		Activity source = population.getFactory().createActivityFromLinkId("home", convertFromCrossingId2LinkId(com.getSourceNodeId()));
		source.setEndTime(startTimeSec + r*(endTimeSec-startTimeSec));
		return source;
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
