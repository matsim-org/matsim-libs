/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.droeder.southAfrica.scenarios.nmbm;


/**
 * @author droeder
 *
 */
public class PreProcessor {

//	private static final Logger log = Logger.getLogger(PreProcessor.class);
//
//	private PreProcessor() {
//		//Auto-generated constructor stub
//	}
//	private final static double SAMPLESIZE = 0.20;
//	
//	private final static String INPUTDIR = "E:/rsa/Data-nmbm/";
//		private final static String POPULATIONINPUTDIR = INPUTDIR + "population/20121010_100pct/"; 
//		private final static String INPUTNETWORK =  INPUTDIR + "transit/bus/NMBM_Bus_V1.xml.gz";
//		private final static String INPUTSCHEDULE = INPUTDIR + "transit/bus/Transitschedule_Bus_V1_WithVehicles.xml.gz";
//		private final static String INPUTVEHICLES = INPUTDIR + "transit/bus/transitVehicles_Bus_V1.xml.gz";
//	private final static String OUTDIR = "E:/rsa/server/activityTime15min_sample" + String.valueOf(SAMPLESIZE) + "/";
//
//	
//	public static void main(String[] args) {
//		if(!new File(OUTDIR).exists()){
//			new File(OUTDIR ).mkdirs();
//		}
//		String outputNetwork = INPUTNETWORK;
//		
////		outputNetwork = transformNetwork(outputNetwork, OUTDIR);
//		outputNetwork = removeCarFromRailLinks(outputNetwork, OUTDIR);
//		outputNetwork = cleanNetwork(outputNetwork, OUTDIR);
//		outputNetwork = simplifyNetwork(outputNetwork, INPUTSCHEDULE, OUTDIR);
////		sampleHousholds(POPULATIONINPUTDIR, 
////						OUTDIR, 
////						SAMPLESIZE, 
////						outputNetwork);
////		changeLegModes(OUTDIR + "population.xml.gz", OUTDIR + "population.changedLegModes.xml.gz");
////		IOUtils.copyFile(new File(INPUTVEHICLES), new File(OUTDIR + "vehicles.xml.gz"));
//	}
//	
//
//
//	/**
//	 * @param inputnetwork2
//	 * @param outdir2
//	 * @return
//	 */
//	private static String transformNetwork(String inputNetwork, String outdir) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc).readFile(inputNetwork);
//		CoordinateTransformation trans = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_SA_Albers);
//		
//		for(Node n: sc.getNetwork().getNodes().values()){
//			((NodeImpl) n).setCoord(trans.transform(n.getCoord()));
//		}
//		String network = outdir + "network.transformed.xml.gz";
//		new NetworkWriter(sc.getNetwork()).write(network);
//		return network;
//	}
//
//
//
//	/**
//	 * @param outdir 
//	 * @param inputnetwork2
//	 * @return
//	 */
//	private static String removeCarFromRailLinks(String inputNetwork, String outdir) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		sc.getConfig().transit().setUseTransit(true);
//		
//		// remove car and bus from rail-network######
//		new MatsimNetworkReader(sc).readFile(inputNetwork);
//		log.info("making rail links accesible for rails only...");
//		@SuppressWarnings("serial")
//		Set<String> modes = new HashSet<String>(){{
//			add("rail");
//		}};
//		for(Link l: sc.getNetwork().getLinks().values()){
//			if(l.getAllowedModes().contains("rail")){
//				l.setAllowedModes(modes);
//			}
//		}
//		String outputNetwork = new String(inputNetwork).replace(".xml.gz", ".railSeparated.xml.gz");
//		new NetworkWriter(sc.getNetwork()).write(outputNetwork);
//		log.info("new Network written to " + outputNetwork);
//		return outputNetwork;
//	}
//	
//	/**
//	 * @param outputNetwork
//	 * @return
//	 */
//	private static String cleanNetwork(String inputNetwork, String outdir) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc).readFile(inputNetwork);
//		NetworkCleaner cleaner = new NetworkCleaner();
//		cleaner.run(sc.getNetwork());
//		String network = new String(inputNetwork).replace(".xml.gz", ".clean.xml.gz");
//		new NetworkWriter(sc.getNetwork()).write(network);
//		return network;
//	}
//
//	/**
//	 * @param inputschedule2 
//	 * @param inputnetwork2
//	 * @param outputNetwork
//	 */
//	private static String simplifyNetwork(String inputNetwork, String inputschedule, String outdir) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		sc.getConfig().transit().setUseTransit(true);
//		new MatsimNetworkReader(sc).readFile(inputNetwork);
//		new TransitScheduleReader(sc).readFile(inputschedule);
//		PTNetworkSimplifier simpli = new PTNetworkSimplifier(inputNetwork, inputschedule, null, null);
//		@SuppressWarnings("serial")
//		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>(){{
//			add(new Integer(4));
//			add(new Integer(5));
//		}};
//		simpli.setNodesToMerge(nodeTypesToMerge);
//		simpli.run(sc.getNetwork(), sc.getTransitSchedule());
//		// TODO this is just a hack, because otherwise this link is a sink after simplifying!!!
//		sc.getNetwork().getLinks().get(new IdImpl("74239-74241")).setToNode(sc.getNetwork().getNodes().get(new IdImpl("944504976")));
//		//################
//		TransitSchedule schedule = TransitScheduleCleaner.removeAllRoutesWithMissingLinksFromSchedule(sc.getTransitSchedule(), sc.getNetwork());
//		schedule = TransitScheduleCleaner.removeRoutesWithOnlyOneRouteStop(schedule);
//		schedule = TransitScheduleCleaner.removeEmptyLines(schedule);
//		schedule = TransitScheduleCleaner.removeStopsNotUsed(schedule);
//		new TransitScheduleWriter(schedule).writeFile(outdir + "schedule.simple.xml.gz");
//		String network = new String(inputNetwork).replace(".xml.gz", ".simple.xml.gz");
//		new NetworkWriter(sc.getNetwork()).write(network);
//		return network;
//	}
//
//	private static void sampleHousholds(String inputDir, String outputDir, double sampleSize, String networkFile){
//		String[] arg = new String[4];
//		arg[0] = inputDir;
//		arg[1] = outputDir;
//		arg[2] = String.valueOf(sampleSize);
//		arg[3] = networkFile;
//		HouseholdSampler.main(arg);
//	}
//	
//	private static void changeLegModes(String inputPopulation, String outPopulation) {
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		log.info("changing legModes as follows. ride->ride2, pt1->bus, pt2->rail");
//		class LocalPersonAlgorithm implements PersonAlgorithm{
//			
//			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//			Counter removed = new Counter("removed persons: ");
//			
//			@Override
//			public void run(Person person) {
//				if(person.getSelectedPlan() == null || person.getSelectedPlan().getPlanElements() == null){
//					removed.incCounter();
//				}else{
//					for(PlanElement  pe: person.getSelectedPlan().getPlanElements()){
//						if(pe instanceof Leg){
//							if(((Leg) pe).getMode().equals(TransportMode.ride)){
//								((Leg) pe).setMode("ride2");
//							} 
//							if(((Leg) pe).getMode().equals("pt1")){
//								((Leg) pe).setMode("bus");
//							}
//							if(((Leg) pe).getMode().equals("pt2")){
//								((Leg) pe).setMode("rail");
//							}
//						}
//						if(pe instanceof Activity){
//							Double blur = (15*60) * (2 * (0.5 - MatsimRandom.getRandom().nextDouble())); //+- 15 min
//							if(!(((Activity) pe).getEndTime() == Double.NaN)){
//								((Activity) pe).setEndTime(((Activity) pe).getEndTime() + blur);
//							}
//							if(!(((Activity) pe).getStartTime() == Double.NaN)){
//								((Activity) pe).setStartTime(((Activity) pe).getStartTime() + blur);
//							}
//						}
//					}
//					sc.getPopulation().addPerson(person);
//				}
//			}
//			
//			public Population getPopulation(){
//				removed.printCounter();
//				return sc.getPopulation();
//			}
//			
//		}
//		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
//		PersonAlgorithm pa = new LocalPersonAlgorithm();
//		((PopulationImpl) sc.getPopulation()).addAlgorithm(pa);
//		log.info("read " + inputPopulation);
//		new MatsimPopulationReader(sc).parse(inputPopulation);
//		new PopulationWriter(((LocalPersonAlgorithm) pa).getPopulation(), sc.getNetwork()).write(outPopulation);
//		log.info("new Population written to " + outPopulation);
//	}
}

