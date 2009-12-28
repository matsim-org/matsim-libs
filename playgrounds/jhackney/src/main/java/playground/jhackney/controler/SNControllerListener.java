/* *********************************************************************** *
 * project: org.matsim.*
 * SNControlerListenerRePlanSecLoc.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.controler;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.activitySpaces.ActivitySpaces;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.socialnetworks.algorithms.CompareTimeWindows;
import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;
import playground.jhackney.socialnetworks.interactions.NonSpatialInteractor;
import playground.jhackney.socialnetworks.interactions.SpatialInteractorEvents;
import playground.jhackney.socialnetworks.io.ActivityActReader;
import playground.jhackney.socialnetworks.io.ActivityActWriter;
import playground.jhackney.socialnetworks.io.PajekWriter;
import playground.jhackney.socialnetworks.mentalmap.MentalMap;
import playground.jhackney.socialnetworks.mentalmap.TimeWindow;
import playground.jhackney.socialnetworks.scoring.EventSocScoringFactory;
import playground.jhackney.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import playground.jhackney.socialnetworks.socialnet.EgoNet;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;
import playground.jhackney.socialnetworks.statistics.SocialNetworkStatistics;



/**
 * This controler initializes a social network which permits the exchange of influence
 * and information between agents within the MobSim iterations. The agents'
 * plans are modified according to the new information within the iterations of the MobSim.
 * Thus the social network replanning occurs in parallel to the normal replanning and not
 * serial to it. <p>
 *
 * Contrast this functionality to {@link playground/jhackney/controler/SNControllerListenerSecLoc}, which replans outside the
 * MobSim loop and generates new initial demand (100% of agents replan with social network
 * and a portion of the plans are optimized subsequently in MobSim).<p>
 *
 * It is likely that neither implementation is right or wrong, but that different
 * experiments will use different Controllers: e.g. secondary location choice still needs
 * a route and/or departure time optimization either for the new secondary activities or
 * for the primary activities.<p>
 *
 * The fraction of agents socially interacting is set in the config.xml variables,
 * "fract_s_interact" for spatial interactions, and "fract_ns_interact" for simulating
 * other interactions which occur/have occured outside the framework of the plans
 * under consideration.<p>
 *
 * After these interactions occur, a percent of agents adapt their plans to their social
 * group. This is done in a PlanAlgorithm written to make the desired kind of changes
 * one wants to make to the plans as a result of social interactions. The PlanAlgorithm
 * must be added to the StrategyManager.<p>
 *
 * Initialization of social networks can use the initial plans and/or
 * other algorithms to generate relationships.
 *
 * @author jhackney
 *
 */
public class SNControllerListener implements StartupListener, IterationStartsListener, IterationEndsListener,  ScoringListener{
//	public class SNControllerListenerRePlanSecLoc implements StartupListener, IterationStartsListener, IterationEndsListener,  AfterMobsimListener{
//	public class SNControllerListenerRePlanSecLoc implements StartupListener, IterationStartsListener, IterationEndsListener{


	private static final boolean CALCSTATS = true;
	public static String SOCNET_OUT_DIR = null;

	SocialNetwork snet;
	SocialNetworkStatistics snetstat;
	ActivityActWriter aaw;
	ActivityActReader aar = null;
	PajekWriter pjw;
	NonSpatialInteractor plansInteractorNS;//non-spatial (not observed, ICT)
	//InteractorTest
//	SpatialInteractorActs plansInteractorS;//spatial (face to face)
	SpatialInteractorEvents plansInteractorS;
	int max_sn_iter;
	int snIter;
	private String [] infoToExchange;//type of info for non-spatial exchange is read in
	public static String activityTypesForEncounters[]={"home","work","shop","education","leisure"};

//	private TrackEventsOverlap teo=null;
	private EventsMapStartEndTimes epp=null;
	private MakeTimeWindowsFromEvents teo=null;
	private LinkedHashMap<Activity,ArrayList<Double>> actStats=null;
	private LinkedHashMap<ActivityFacility,ArrayList<TimeWindow>> twm=null;
	private EventsToScore scoring =null;

	private final Logger log = Logger.getLogger(SNControllerListener.class);

//	Variables for allocating the spatial meetings among different types of activities
	double fractionS[];
	LinkedHashMap<String,Double> rndEncounterProbs= new LinkedHashMap<String,Double>();
//	New variables for replanning
	int replan_interval;

	private Controler controler = null;
	private Knowledges knowledges;
	
	public void notifyStartup(final StartupEvent event) {
		this.controler = event.getControler();
		this.knowledges = ((ScenarioImpl)controler.getScenario()).getKnowledges();
		// Make a new zone layer (Raster)
//		if(!(this.controler.getConfig().socnetmodule().getGridSpace().equals(null))){
//		int gridSpacing = Integer.valueOf(this.controler.getConfig().socnetmodule().getGridSpace());
//		if(Gbl.getWorld().getLayers().size()>2){
//		System.out.println("World already contains a zone layer");
//		new WorldCreateRasterLayer(gridSpacing).run(Gbl.getWorld());
//		}else{
//		new WorldCreateRasterLayer2(gridSpacing).run(Gbl.getWorld());
////		new WorldCreateRasterLayer(3000).run(Gbl.getWorld());
//		}
//		// Stitch together the world
//		new WorldBottom2TopCompletion().run(Gbl.getWorld());
//		}
//		int gridSpacing = Integer.valueOf(this.controler.getConfig().socnetmodule().getGridSpace());
//		new WorldCreateRasterLayer2(gridSpacing).run(Gbl.getWorld());

		// Complete the world to make sure that the layers all have relevant mapping rules
		new WorldConnectLocations().run(event.getControler().getWorld());

		this.log.info(" Initializing agent knowledge about geography ...");

		initializeKnowledge(this.controler.getPopulation(), this.controler.getFacilities(), this.knowledges);
		this.log.info("... done");

		this.log.info("   Instantiating a new social network scoring factory with new SocialActs");

		//teo = new TrackEventsOverlap();
		epp=new EventsMapStartEndTimes(this.controler.getPopulation());

//		this.controler.getEvents().addHandler(this.teo);
		this.controler.getEvents().addHandler(this.epp);
		
		//TODO superfluous in 0th iteration and not necessary anymore except that scoring runction needs it (can null be passed?)
		teo=new MakeTimeWindowsFromEvents();
		teo.makeTimeWindows(epp);
		twm=teo.getTimeWindowMap();
		
		this.log.info(" ... Instantiation of events overlap tracking done");
//		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(teo.getTimeWindowMap());
		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm);
		EventSocScoringFactory factory = new EventSocScoringFactory("leisure", controler.getScoringFunctionFactory(),actStats);
		
		this.controler.setScoringFunctionFactory(factory);
		this.log.info("... done");

		this.log.info("  Instantiating social network EventsToScore for scoring the plans");
		scoring = new EventsToScore(this.controler.getPopulation(), factory);
		this.controler.getEvents().addHandler(scoring);
		this.log.info(" ... Instantiation of social network scoring done");

		snsetup();
	}

	public void notifyScoring(final ScoringEvent event){

		this.log.info("scoring");
		
		//TODO: put in the spatial interactions here. The TimeWindowMap was updated in MobSim and is current, here.
		// Do not call the teo.clear method. Instead, let
		// the controler clear the teo in the notifyIterationStarts method
		
		Gbl.printMemoryUsage();
		
		//SSTEST this.spatialScorer.scoreActs(this.controler.getPopulation(), snIter);
		log.info("SSTEST Clearing and recalculating actStats "+snIter);
		this.actStats.clear();
		
		Gbl.printMemoryUsage();
		teo=new MakeTimeWindowsFromEvents();
		teo.makeTimeWindows(epp);
		twm= teo.getTimeWindowMap();

		this.actStats.putAll(CompareTimeWindows.calculateTimeWindowEventActStats(twm));
		log.info("SSTEST Finish Scoring with actStats "+snIter);
		scoring.finish();
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {

		this.log.info("finishIteration ... "+event.getIteration());

		Gbl.printMemoryUsage();
		
		if( event.getIteration()%replan_interval==0){
			
			// Removing the social links here rather than before the
			//replanning and assignment lets you use the actual encounters in a social score
			this.log.info(" Removing social links ...");
			this.snet.removeLinks(snIter);
			this.log.info(" ... done");
			
			Gbl.printMemoryUsage();

//			You could forget activities here, after the replanning and assignment

			if(CALCSTATS && (event.getIteration()%1==0)){
				Gbl.printMemoryUsage();
				this.log.info(" Calculating and reporting network statistics ...");
				this.snetstat.calculate(snIter, this.snet, this.controler.getPopulation(), this.knowledges);
				this.log.info(" ... done");

				Gbl.printMemoryUsage();
				
				this.log.info("  Opening the file to write out the map of Acts to Facilities");
				aaw=new ActivityActWriter();
				aaw.openFile(SOCNET_OUT_DIR+"ActivityActMap"+snIter+".txt");
				this.log.info(" Writing out the map between Acts and Facilities ...");
				aaw.write(snIter,this.controler.getPopulation());
				aaw.close();
				this.log.info(" ... done");
			}

			if(event.getIteration()%1==0){
				this.log.info(" Writing out social network for iteration " + snIter + " ...");
				this.pjw.write(this.snet.getLinks(), this.controler.getPopulation(), snIter);
				this.pjw.writeGeo(this.controler.getPopulation(), this.snet, snIter);
				this.log.info(" ... done");

//				Write out the KML for the EgoNet of a chosen agent
				this.log.info(" Writing out KMZ activity spaces and day plans for agent's egoNet");
				Person testP=this.controler.getPopulation().getPersons().get(new IdImpl("21924270"));//1pct
//				Person testP=this.controler.getPopulation().getPerson("21462061");//10pct
				EgoNetPlansItersMakeKML.loadData(testP,event.getIteration(), this.knowledges);
				this.log.info(" ... done");
			}
		}
		if (event.getIteration() == this.controler.getLastIteration()) {
			if(CALCSTATS){
				this.log.info("----------Closing social network statistic files and wrapping up ---------------");
				this.snetstat.closeFiles();
			}
		}

		if (event.getIteration() == this.controler.getLastIteration()){

			EgoNetPlansItersMakeKML.write();
		}

	}

	public void notifyIterationStarts(final IterationStartsEvent event) {
//		Controler controler = event.getControler();

		/* code previously in setupIteration() */

		if( (event.getIteration()%replan_interval==0) && (event.getIteration()!=this.controler.getFirstIteration())){

			// only generate the map if spatial meeting is important in this experiment
			if (total_spatial_fraction(this.fractionS) > 0) {

				// Agents' planned interactions
				this.log.info("  Agents planned social interactions, respectively their meetings based on last MobSim iteration ...");
				this.log.info("  Agents' relationships are updated to reflect these interactions! ...");
				this.plansInteractorS.interact(this.controler.getPopulation(), this.rndEncounterProbs, snIter, twm);
				
				// Agents' actual interactions
				// TrackEventsOverlap must be passed to initialization of the interactor
				// timeWindowMap is updated in the scoringListener after the MobSim
				// this.plansInteractorS.interact(this.controler.getPopulation(), this.rndEncounterProbs, snIter);
				// 
			} else {
				this.log.info("     (none)");
			}
			this.epp.reset(snIter);
			this.teo.clearTimeWindowMap();
			this.log.info(" ... Spatial interactions done\n");

			Gbl.printMemoryUsage();
			
			this.log.info(" Non-Spatial interactions ...");
			for (int ii = 0; ii < this.infoToExchange.length; ii++) {
				String facTypeNS = this.infoToExchange[ii];

				//	Geographic Knowledge about all types of places is exchanged
				if (!facTypeNS.equals("none")) {
					this.log.info("  Geographic Knowledge about all types of places is being exchanged ...");
					this.plansInteractorNS.exchangeGeographicKnowledge(facTypeNS, snIter);
				}
			}

			// Exchange of knowledge about people
			double fract_intro=Double.parseDouble(this.controler.getConfig().socnetmodule().getFriendIntroProb());
			if (fract_intro > 0) {
				this.log.info("  Knowledge about other people is being exchanged ...");
				this.plansInteractorNS.exchangeSocialNetKnowledge(snIter);
			}

			this.log.info("  ... done");

			this.log.info(" Forgetting excess activities (locations) OR SHOULD THIS HAPPEN EACH TIME AN ACTIVITY IS LEARNED ...");
			this.log.info("  Should be an algorithm");
			for (Person p : this.controler.getPopulation().getPersons().values()) {
//				Remember a number of activities equal to at least the number of
//				acts per plan times the number of plans in memory
				int max_memory = (int) (p.getSelectedPlan().getPlanElements().size()/2*p.getPlans().size()*1.5);
//				this.log.info("NOTE that manageMemory is turned off");
				((MentalMap)p.getCustomAttributes().get(MentalMap.NAME)).manageMemory(max_memory, p.getPlans());
			}
			this.log.info(" ... done");

			Gbl.printMemoryUsage();
			
			snIter++;
		}
	}

	/* ===================================================================
	 * private methods
	 * =================================================================== */

	void initializeKnowledge(final Population plans, ActivityFacilities facilities, Knowledges knowledges ) {

		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)


//		If the user has an existing file that maps activities to acts, open it and read it in
//		Attempt to open file of mental maps and read it in
		System.out.println("  Opening the file to read in the map of Acts to Facilities");
		aar = new ActivityActReader(Integer.valueOf(controler.getConfig().socnetmodule().getInitIter()).intValue());

		String fileName = controler.getConfig().socnetmodule().getInDirName()+ "ActivityActMap"+Integer.parseInt(controler.getConfig().socnetmodule().getInitIter())+".txt";
		aar.openFile(fileName);
		System.out.println(" ... done");

		for (Person person : plans.getPersons().values()) {
			KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(person.getId());
			if(k ==null){
				k =  this.knowledges.getFactory().createKnowledge(person.getId(), "created by " + this.getClass().getName());
			}
			for (int ii = 0; ii < person.getPlans().size(); ii++) {
				Plan plan = person.getPlans().get(ii);

				// TODO balmermi: double check if this is the right place to create the MentalMap and the EgoNet
				if (person.getCustomAttributes().get(MentalMap.NAME) == null) { person.getCustomAttributes().put(MentalMap.NAME,new MentalMap(k)); }
				if (person.getCustomAttributes().get(EgoNet.NAME) == null) { person.getCustomAttributes().put(EgoNet.NAME,new EgoNet()); }

				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).prepareActs(plan);// // JH Hack to make sure act types are compatible with social nets
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).initializeActActivityMapRandom(plan);
				((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).initializeActActivityMapFromFile(plan,facilities,aar);
//				Reset activity spaces because they are not read or written correctly
				ActivitySpaces.resetActivitySpaces(person);
			}
		}
		aar.close();//close the file with the input act-activity map
	}

	private void snsetup() {

//		Config config = Gbl.getConfig();

		SOCNET_OUT_DIR = this.controler.getConfig().socnetmodule().getOutDirName();
		File snDir = new File(SOCNET_OUT_DIR);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("The iterations directory " + SOCNET_OUT_DIR + " could not be created.");
		}

		this.max_sn_iter = Integer.parseInt(this.controler.getConfig().socnetmodule().getNumIterations());
		this.replan_interval = Integer.parseInt(this.controler.getConfig().socnetmodule().getRPInt());
		String rndEncounterProbString = this.controler.getConfig().socnetmodule().getFacWt();
		String xchangeInfoString = this.controler.getConfig().socnetmodule().getXchange();
		this.infoToExchange = getFacTypes(xchangeInfoString);
		this.fractionS = toNumber(rndEncounterProbString);
		this.rndEncounterProbs = mapActivityWeights(activityTypesForEncounters, rndEncounterProbString);

		this.log.info(" Instantiating the Pajek writer ...");
		this.pjw = new PajekWriter(SOCNET_OUT_DIR, controler.getFacilities(), this.knowledges);
		this.log.info("... done");

		this.log.info(" Initializing the social network ...");
		this.snet = new SocialNetwork(this.controler.getPopulation());
		this.log.info("... done");

		if(CALCSTATS){
//			this.log.info(" Calculating the statistics of the initial social network)...");
			this.log.info(" Opening the files for the social network statistics...");
			this.snetstat=new SocialNetworkStatistics(SOCNET_OUT_DIR);
			this.snetstat.openFiles();
//			Social networks do not change until the first iteration of Replanning,
//			so we can skip writing out this initial state because the networks will still be unchanged after the first assignment
//			this.snetstat.calculate(0, this.snet, this.controler.getPopulation());
			this.log.info(" ... done");

		}

		this.log.info("  Initializing the KML output");

		EgoNetPlansItersMakeKML.setUp(this.controler.getConfig(), this.controler.getNetwork());
		EgoNetPlansItersMakeKML.generateStyles();
		this.log.info("... done");

		this.log.info(" Writing out the initial social network ...");
		this.pjw.write(this.snet.getLinks(), this.controler.getPopulation(), this.controler.getFirstIteration());
		this.log.info("... done");

		this.log.info(" Setting up the NonSpatial interactor ...");
		this.plansInteractorNS=new NonSpatialInteractor(this.snet, this.knowledges);
		this.log.info("... done");

		this.log.info(" Setting up the Spatial interactor ...");
		//InteractorTest
//		this.plansInteractorS=new SpatialInteractorActs(this.snet);
		this.plansInteractorS=new SpatialInteractorEvents(this.snet, teo);
		this.log.info("... done");

		this.snIter = this.controler.getFirstIteration();
	}

	/**
	 * A method for decyphering the config codes. Part of configuration
	 * reader. Replace eventually with a routine that runs all of the
	 * facTypes but uses a probability for each one, summing to 1.0. Change
	 * the interactors accordingly.
	 *
	 * @param longString
	 * @return
	 */
	private String[] getFacTypes(final String longString) {
		String patternStr = ",";
		String[] s;
		log.info(this.getClass()+ "getFacTypes:	!!add keyword\"any\" and a new interact method to exchange info of any factility types (compatible with probabilities)");
		if (longString.equals("all-p")) {
			s = new String[5];
			s[0] = "home";
			s[1] = "work";
			s[2] = "education";
			s[3] = "leisure";
			s[4] = "shop";
		} else if (longString.equals("all+p")) {
			s = new String[6];
			s[0] = "home";
			s[1] = "work";
			s[3] = "education";
			s[4] = "leisure";
			s[5] = "shop";
			s[6] = "person";
		} else {
			s = longString.split(patternStr);
		}
		for (int i = 0; i < s.length; i++) {
			// if(s[i]!="home"&&s[i]!="work"&&s[i]!="education"&&s[i]!="leisure"&&s[i]!="shop"&&s[i]!="person"&&s[i]!="none"){
			if (!s[i].equals("home") && !s[i].equals("work") && !s[i].equals("education") && !s[i].equals("leisure")
					&& !s[i].equals("shop") && !s[i].equals("person") && !s[i].equals("none")) {
				this.log.info(this.getClass() + ":" + s[i]);
				Gbl.errorMsg("Error on type of info to exchange. Check config file. Use commas with no spaces");
			}
		}
		return s;
	}

	private double[] toNumber(final String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if((w[i]<0.)||(w[i]>1.)){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}
	private LinkedHashMap<String,Double> mapActivityWeights(final String[] types, final String longString) {
		String patternStr = ",";
		String[] s;
		LinkedHashMap<String,Double> map = new LinkedHashMap<String,Double>();
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if((w[i]<0.)||(w[i]>1.)){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
			map.put(types[i],w[i]);
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return map;
	}

	private double total_spatial_fraction(final double[] fractionS2) {
//		See if we use spatial interaction at all: sum of these must > 0 or else no spatial
//		interactions take place
		double total_spatial_fraction=0;
		for (int jjj = 0; jjj < fractionS2.length; jjj++) {
			total_spatial_fraction = total_spatial_fraction + fractionS2[jjj];
		}
		return total_spatial_fraction;
	}

	/**
	 * returns the path to the specified social network iteration directory. The directory path does not include the trailing '/'
	 * @param snIter the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(final int snIter) {
		return Controler.getOutputFilename("ITERS/" + snIter);
	}

	/**
	 * returns the path to the specified iteration directory,
	 * including social network iteration. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @param snIter
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(final int iteration, final int snIter) {
		return Controler.getOutputFilename("ITERS/" + snIter + "/it." + iteration);
	}
}

