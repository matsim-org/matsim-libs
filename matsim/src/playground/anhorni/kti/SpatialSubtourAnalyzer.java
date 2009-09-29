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
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.World;


public class SpatialSubtourAnalyzer extends AbstractPersonAlgorithm {

//	private PopulationImpl plans = new PopulationImpl();
//	private ActivityFacilitiesImpl facilities;
//	private NetworkLayer network;
	
	private TreeMap<TransportMode, TreeMap<Id, Integer>> subtourStartLocations = 
		new TreeMap<TransportMode, TreeMap<Id, Integer>>();
			
	private final static Logger log = Logger.getLogger(SpatialSubtourAnalyzer.class);
	
	public static void main(final String[] args) {
		
		Config config = Gbl.createConfig(args);
		
		log.info("reading the network ...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		log.info("reading the network ...done.");
				
		World world = Gbl.getWorld();
		
		log.info("Reading the facilities ...");
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl)world.createLayer(ActivityFacilitiesImpl.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(facilities).readFile(config.facilities().getInputFile());
		log.info("Reading the facilities ...done.");
		
		final SpatialSubtourAnalyzer analyzer = new SpatialSubtourAnalyzer();
		PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		plans.addAlgorithm(analyzer);
		
		Gbl.startMeasurement();
		log.info("Processing plans...");
		final PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		plansReader.readFile(config.plans().getInputFile());
		log.info("Processing plans...done.");
		Gbl.printElapsedTime();

		String outpath = "output/kti/";
		analyzer.printStartLocationofSubtours(facilities, outpath);
	}
	
//	public void run(Config config) {			
//
//		Gbl.startMeasurement();
//		this.init(config);
//		Gbl.printElapsedTime();
//		
////		this.extractSubtours();
////		this.run(this.plans);
//		this.printStartLocationofSubtours(outpath);
//
//	}
		
//	private void init(Config config) {
//		
//		log.info(" \n---------------------------- \nStart init" );
//		log.info("reading the network ...");
//		this.network = new NetworkLayer();
//		new MatsimNetworkReader(this.network).readFile(config.network().getInputFile());
//		log.info("reading the network ...done.");
//				
//		World world = Gbl.getWorld();
//		
//		log.info("Reading the facilities ...");
//		this.facilities =(ActivityFacilitiesImpl)world.createLayer(ActivityFacilitiesImpl.LAYER_TYPE, null);
//		new FacilitiesReaderMatsimV1(this.facilities).readFile(config.facilities().getInputFile());
//		log.info("Reading the facilities ...done.");
//		
//		
//		
//		PopulationImpl plans = new PopulationImpl();
//		plans.setIsStreaming(true);
//		
//		log.info("Reading plans...");
//		final PopulationReader plansReader = new MatsimPopulationReader(this.plans, network);
//		plansReader.readFile(config.plans().getInputFile());
//		log.info("Reading plans...done.");
//		log.info("Init finished \n ----------------------------");
//	}
	
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
	
//	private void extractSubtours() {
//		
//		SubtourStartLocations subtourStartLocationsExtractor = new SubtourStartLocations();
//		
//		Iterator<PersonImpl> person_it = this.plans.getPersons().values().iterator();
//		while (person_it.hasNext()) {
//			PersonImpl person = person_it.next();
//			PlanImpl selectedPlan = person.getSelectedPlan();
//			
//			subtourStartLocationsExtractor.run(selectedPlan);
//			TreeMap<Id, Integer> subtourStartLocations = subtourStartLocationsExtractor.getSubtourStartLocations();
//									
//			final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
//			
//			if (actslegs.size() < 6) continue;
//			// do not handle the last two activities as they can not be a start for a subtour
//			for (int j = 0; j < actslegs.size() - 4; j=j+2) {
//				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
//				
//				if (subtourStartLocations.containsKey(act.getFacilityId())) {
//					this.incrementCounter(act.getFacilityId(), selectedPlan.getNextLeg(act).getMode());
//				}
//			}	
//		}
//	}
//	

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
	
}
