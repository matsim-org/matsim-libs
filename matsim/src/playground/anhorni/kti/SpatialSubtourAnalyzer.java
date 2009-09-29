package playground.anhorni.kti;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.balmermi.modules.PersonStupidDeleteKnowledgeForStreamingModule;


public class SpatialSubtourAnalyzer extends AbstractPersonAlgorithm {

	private TreeMap<TransportMode, TreeMap<Id, Integer>> subtourStartLocations = 
		new TreeMap<TransportMode, TreeMap<Id, Integer>>();
			
	private final static Logger log = Logger.getLogger(SpatialSubtourAnalyzer.class);
	
	public static void main(final String[] args) {
		
		ScenarioLoader sl = new ScenarioLoader(args[0]);

		log.info("loading facilities...");
		sl.loadActivityFacilities();
		Gbl.printMemoryUsage();
		log.info("done. (loading facilities)");

		log.info("loading network...");
		sl.loadNetwork();
		Gbl.printMemoryUsage();
		log.info("done. (loading network)");

		log.info("adding algorithms...");
		final SpatialSubtourAnalyzer analyzer = new SpatialSubtourAnalyzer();
		PopulationImpl plans = sl.getScenario().getPopulation();
		plans.setIsStreaming(true);
		plans.addAlgorithm(analyzer);
		plans.addAlgorithm(new PersonStupidDeleteKnowledgeForStreamingModule(sl.getScenario().getKnowledges()));
		Gbl.printMemoryUsage();
		log.info("adding algorithms...done.");
		
		Config config = sl.getScenario().getConfig();
		
		Gbl.startMeasurement();
		log.info("Processing plans...");
		final PopulationReader plansReader = new MatsimPopulationReader(sl.getScenario());
		plansReader.readFile(config.plans().getInputFile());
		Gbl.printMemoryUsage();
		log.info("Processing plans...done.");
		Gbl.printElapsedTime();

		String outpath = "output/kti/";
		analyzer.printStartLocationofSubtours(sl.getScenario().getActivityFacilities(), outpath);
	}

	@Override
	public void run(PersonImpl person) {
		PlanImpl selectedPlan = person.getSelectedPlan();
		
		SubtourStartLocations subtourStartLocationsExtractor = new SubtourStartLocations();

		subtourStartLocationsExtractor.run(selectedPlan);
		TreeMap<Id, Integer> subtourStartLocations = subtourStartLocationsExtractor.getSubtourStartLocations();
								
		final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
		
//		if (actslegs.size() < 6) continue;
		if (actslegs.size() >= 6) {
			// do not handle the last two activities as they can not be a start for a subtour
			for (int j = 0; j < actslegs.size() - 4; j = j + 2) {
				final ActivityImpl act = (ActivityImpl) actslegs.get(j);

				if (subtourStartLocations.containsKey(act.getFacilityId())) {
					this.incrementCounter(act.getFacilityId(), selectedPlan
							.getNextLeg(act).getMode());
				}
			}
		}	
	}

	private void incrementCounter(Id facilityId, TransportMode mode) {
		
		if (this.subtourStartLocations.get(mode) == null) {
			this.subtourStartLocations.put(mode, new TreeMap<Id, Integer>());
		}
		
		if (this.subtourStartLocations.get(mode).get(facilityId) == null) {
			this.subtourStartLocations.get(mode).put(facilityId, new Integer(0));
		}
		
		int cnt = this.subtourStartLocations.get(mode).get(facilityId).intValue() + 1;
		this.subtourStartLocations.get(mode).put(facilityId, cnt);
	}
	
	private void printStartLocationofSubtours(ActivityFacilitiesImpl facilities, String outpath) {

		log.info("Generate output files ...");
		
		try {
			BufferedWriter out;			
			
			Iterator<TransportMode> mode_it = subtourStartLocations.keySet().iterator();
			while (mode_it.hasNext()) {
				TransportMode mode = mode_it.next();
				
				log.info("Writing : " + outpath + "subtourStartLocations_" + mode + ".txt");

				out = IOUtils.getBufferedWriter(outpath + "subtourStartLocations_" + mode + ".txt");
				out.write("facility\tx\ty\tnbrofSubtourStarts\n");
				
				TreeMap<Id, Integer> locations = subtourStartLocations.get(mode);
				
				Iterator<Id> locations_it = locations.keySet().iterator();
				while (locations_it.hasNext()) {
					Id facilityId = locations_it.next();
					
					ActivityFacility facility = facilities.getFacilities().get(facilityId);
				
					out.write(facility.getId() + "\t" + 
							facility.getCoord().getX() + "\t" + facility.getCoord().getY() + "\t" +
							locations.get(facilityId).intValue() +  
							"\n");
				}
				out.flush();
				out.close();
			}			
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
}
