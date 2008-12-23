/* *********************************************************************** *
 * project: org.matsim.*
 * SNControlerListener2.java
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ScoringEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ScoringListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
//import org.matsim.scoring.EventsToScore;
import org.matsim.scoring.EventsToScore;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.algorithms.EventsMapStartEndTimes;
import org.matsim.socialnetworks.algorithms.PersonForgetKnowledge;
import org.matsim.socialnetworks.interactions.NonSpatialInteractor;
import org.matsim.socialnetworks.interactions.SpatialInteractorEvents;
import org.matsim.socialnetworks.io.ActivityActReader;
import org.matsim.socialnetworks.io.ActivityActWriter;
import org.matsim.socialnetworks.io.PajekWriter;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import org.matsim.socialnetworks.scoring.EventSocScoringFactory;
import org.matsim.socialnetworks.socialnet.SocialNetwork;
import org.matsim.socialnetworks.statistics.SocialNetworkStatistics;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.algorithms.ParamStringToNumberArray;
import playground.jhackney.algorithms.ParamStringToStringArray;
import playground.jhackney.algorithms.ParamStringsToStringDoubleMap;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;


/**
 * This controler initializes a social network which permits the exchange of influence
 * and information between agents within the MobSim iterations. The agents'
 * plans are modified according to the new information within the iterations of the MobSim.
 * Thus the social network replanning occurs in parallel to the normal replanning and not
 * serial to it. <p>
 *
 * Contrast this functionality to <a href= <a> playground/jhackney/controler/SNControllerListenerSecLoc.java</a>, which replans outside the
 * MobSim loop and generates new initial demand (100% of agents replan with social network
 * and a portion of the plans are optimized subsequently in MobSim).<p>
 *
 * It is likely that neither implementation is right or wrong, but that different
 * experiments will use different Controllers: e.g. secondary location choice still needs
 * a route and/or departure time optimization either for the new secondary activities or
 * for the primary activities.<p>
 *
 * The fraction of agents socially interacting is set in the config.xml variable
 * "fract_ns_interact" for simulating
 * non-spatail interactions which occur/have occured outside the framework of the plans
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
public class SNControllerListener2 implements StartupListener, BeforeMobsimListener, IterationEndsListener,  ScoringListener{
//	public class SNControllerListener2 implements StartupListener, IterationStartsListener, IterationEndsListener,  ScoringListener{
//	public class SNControllerListenerRePlanSecLoc implements StartupListener, IterationStartsListener, IterationEndsListener,  AfterMobsimListener{
//	public class SNControllerListenerRePlanSecLoc implements StartupListener, IterationStartsListener, IterationEndsListener{


	private int dummyReportInterval=50;
	private static String SOCNET_OUT_DIR = null;
	private SocialNetwork snet;// static? I just need one
	private SocialNetworkStatistics snetstat;// static? I just need one
	private PajekWriter pjw;// static? I just need one
	private NonSpatialInteractor plansInteractorNS;// static? I just need one
	private SpatialInteractorEvents plansInteractorS;// static? I just need one

	private EventsMapStartEndTimes epp=null;//static? I just need one
	private MakeTimeWindowsFromEvents teo=null;// static? I just need one
	private Hashtable<Act,ArrayList<Double>> actStats=null;// static? I just need one
	private Hashtable<Facility,ArrayList<TimeWindow>> twm=null;// static? I just need one
	//
	private HashMap<String,Double> rndEncounterProbs= new HashMap<String,Double>();// static? I just need one
	//
	private String [] infoToExchange;

	private int interact_interval;
	private int snIter;

	private EventsToScore scoring = null;
	private Controler controler = null;
	private final Logger log = Logger.getLogger(SNControllerListener2.class);

	public void notifyStartup(final StartupEvent event) {
		this.controler = event.getControler();

		// Complete the world to make sure that the layers all have relevant mapping rules
		new WorldConnectLocations().run(Gbl.getWorld());

		this.log.info(" Initializing agent knowledge about geography ...");
		initializeKnowledge();
		this.log.info("... done");

		setupSNScoring();

		snsetup();
	}

	private void setupSNScoring() {
		// 
		this.log.info("   Instantiating a new social network scoring factory with new Event Tracker");
		epp=new EventsMapStartEndTimes(this.controler.getPopulation());
		this.controler.getEvents().addHandler(this.epp);
		//TODO null in 0th iteration. Scoring function needs to know the memory address (can null be passed and still set up the right memory address?)
		teo=new MakeTimeWindowsFromEvents();
		teo.makeTimeWindows(epp);
		twm=teo.getTimeWindowMap();
		this.log.info("... done");

		this.log.info(" Setting up scoring factory ...");
		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm);
		EventSocScoringFactory factory = new EventSocScoringFactory("leisure", controler.getScoringFunctionFactory(),actStats);
//		SocScoringFactoryEvent factory = new playground.jhackney.scoring.SocScoringFactoryEvent("leisure", actStats);
		this.controler.setScoringFunctionFactory(factory);
		this.log.info("... done");

		this.log.info("  Instantiating social network EventsToScore for scoring the plans");
//		scoring = new playground.jhackney.scoring.EventsToScoreAndReport(this.controler.getPopulation(), factory);
		scoring = new EventsToScore(this.controler.getPopulation(),factory);
		this.controler.getEvents().addHandler(scoring);
		this.log.info(" ... Instantiation of social network scoring done");
	}

	protected void initializeKnowledge() {
		new InitializeKnowledge(this.controler.getPopulation(), this.controler.getFacilities());
	}

	public void notifyScoring(final ScoringEvent event){

		this.log.info("scoring");
		if( event.getIteration()%interact_interval==0){
//			Got new events from mobsim
//			Now make new TimeWindows and update the Map of Agent<->TimeWindow (uses old plans and new events)
			Gbl.printMemoryUsage();

			controler.stopwatch.beginOperation("timewindowmap");			
			this.log.info(" Making time Windows and Map from Events");
			teo.makeTimeWindows(epp);
			twm= teo.getTimeWindowMap();
			this.log.info(" ... done making time windows and map");
			controler.stopwatch.endOperation("timewindowmap");

//			Spatial interactions can change the social network and/or knowledge
			controler.stopwatch.beginOperation("spatialencounters");
			this.log.info(" Beginning spatial encounters");
			this.plansInteractorS.interact(this.controler.getPopulation(), this.rndEncounterProbs, snIter, twm);
			this.log.info(" ... Spatial interactions done\n");
			controler.stopwatch.endOperation("spatialencounters");

			Gbl.printMemoryUsage();
//			Execute nonspatial interactions (uses new social network). Can change social network and/or knowledge
			this.log.info(" Non-Spatial interactions ...");
			controler.stopwatch.beginOperation("infoexchange");
			for (int ii = 0; ii < this.infoToExchange.length; ii++) {
				String facTypeNS = this.infoToExchange[ii];
				if (!facTypeNS.equals("none")) {
					this.log.info("  Geographic Knowledge about all types of places is being exchanged ...");
					this.plansInteractorNS.exchangeGeographicKnowledge(facTypeNS, snIter);
				}
			}
			controler.stopwatch.endOperation("infoexchange");

//			Exchange of knowledge about people. Changes social network (makes new friends)
			this.log.info("Introducing people");
			double fract_intro=Double.parseDouble(this.controler.getConfig().socnetmodule().getFriendIntroProb());
			this.log.info("  Knowledge about other people is being exchanged ...");
			this.plansInteractorNS.exchangeSocialNetKnowledge(snIter, fract_intro);
			this.log.info("  ... introducing people done");

//			forget knowledge
			this.log.info("Forgetting knowledge");
			double multiple=Double.parseDouble(this.controler.getConfig().socnetmodule().getMemSize());
			new PersonForgetKnowledge(multiple).run(controler.getPopulation());
			this.log.info(" ... forgetting knowledge done");

			Gbl.printMemoryUsage();

			//dissolve social ties
			this.log.info(" Removing social links ...");
			controler.stopwatch.beginOperation("dissolvelinks");
			this.snet.removeLinks(snIter);
			this.log.info(" ... removing social links done");
			controler.stopwatch.endOperation("dissolvelinks");

//			Update activity statistics with pruned social network
			this.log.info(" Remaking actStats from events");
			this.actStats.putAll(CompareTimeWindows.calculateTimeWindowEventActStats(twm));

			Gbl.printMemoryUsage();

			this.log.info("SSTEST Finish Scoring with actStats "+snIter);
			scoring.finish();
			this.log.info(" ... scoring with actStats finished");
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {

		this.log.info("finishIteration ... "+event.getIteration());

		Gbl.printMemoryUsage();

		if( event.getIteration()%interact_interval==0){

			if(dummyReportInterval>0 && event.getIteration()%dummyReportInterval==0){//50
				Gbl.printMemoryUsage();
				controler.stopwatch.beginOperation("netstats");
				this.log.info(" Calculating and reporting network statistics ...");
				this.snetstat.calculate(snIter, this.snet, this.controler.getPopulation());
				this.log.info(" ... done");
				controler.stopwatch.endOperation("netstats");

				Gbl.printMemoryUsage();


				this.log.info("  Opening the file to write out the map of Acts to Facilities");
				ActivityActWriter aaw=new ActivityActWriter();
				aaw.openFile(SOCNET_OUT_DIR+"/ActivityActMap"+snIter+".txt");
				this.log.info(" Writing out the map between Acts and Facilities ...");
				aaw.write(snIter,this.controler.getPopulation());
				aaw.close();
				this.log.info(" ... done");
			}

//			if(event.getIteration()%dummyReportInterval==0){
			if(event.getIteration()==this.controler.getLastIteration()){
				this.log.info(" Writing out social network for iteration " + snIter + " ...");
				this.pjw.write(this.snet.getLinks(), this.controler.getPopulation(), snIter);
				this.pjw.writeGeo(this.controler.getPopulation(), this.snet, snIter);
				this.log.info(" ... done");

//				Write out the KML for the EgoNet of a chosen agent BROKEN 12.2008
//				this.log.info(" Writing out KMZ activity spaces and day plans for agent's egoNet");
//				Person testP=this.controler.getPopulation().getPerson("21924270");//1pct
////				Person testP=this.controler.getPopulation().getPerson("21462061");//10pct
//				EgoNetPlansItersMakeKML.loadData(testP,event.getIteration());
//				this.log.info(" ... done");
			}
			snIter++;
		}
		if (event.getIteration() == this.controler.getLastIteration()) {
			this.log.info("----------Closing social network statistic files ---------------");
			this.snetstat.closeFiles();
			this.log.info("----------Closing KMZ files ---------------");
			EgoNetPlansItersMakeKML.write();
		}
	}

	/**
	 * The EventHandlers are usually only used in Scoring, the second-to-last step
	 * in the iteration, and then are reset at the top of the next iteration in
	 * notifyIterationStarts.<p>
	 * 
	 * However the results of the last MobSim, which are based on the Events,
	 * might still be needed for certain
	 * social network calculations two steps later
	 * in RePlanning.<p>
	 * 
	 * Clear these results here, after RePlanning, rather than in
	 * notifyIterationStarts, in case they are needed in RePlanning.<p>
	 * 
	 * See Controler.doIterations()
	 */
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {

		this.teo.clearTimeWindowMap();
		this.actStats.clear();
	}

	/* ===================================================================
	 * private methods
	 * =================================================================== */


	private void snsetup() {

//		Config config = Gbl.getConfig();

		SOCNET_OUT_DIR = this.controler.getConfig().socnetmodule().getOutDirName();
		File snDir = new File(SOCNET_OUT_DIR);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("The iterations directory " + SOCNET_OUT_DIR + " could not be created.");
		}

		this.dummyReportInterval = Integer.parseInt(this.controler.getConfig().socnetmodule().getReportInterval());
		this.interact_interval = Integer.parseInt(this.controler.getConfig().socnetmodule().getRPInt());
		String xchangeInfoString = this.controler.getConfig().socnetmodule().getXchange();
		this.infoToExchange = new ParamStringToStringArray(xchangeInfoString).getArray();
		this.rndEncounterProbs = new ParamStringsToStringDoubleMap(this.controler.getConfig().socnetmodule().getActTypes(), this.controler.getConfig().socnetmodule().getFacWt()).getMap();

		this.log.info(" Instantiating the Pajek writer ...");
		this.pjw = new PajekWriter(SOCNET_OUT_DIR, (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE));
		this.log.info("... done");

		if(this.controler.getConfig().socnetmodule().getSocNetAlgo()==(null)){
			this.log.error("No social network is set. This controller requires you to configure a social network");
		}

		this.log.info(" Initializing the social network ...");
		this.snet = new SocialNetwork(this.controler.getPopulation());
		this.log.info("... done");

		if(dummyReportInterval>0){
			this.log.info(" Opening the files for the social network statistics...");
			this.snetstat=new SocialNetworkStatistics(SOCNET_OUT_DIR);
			this.snetstat.openFiles();
			this.log.info(" ... done");
		}

		this.log.info("  Initializing the KML output");

		EgoNetPlansItersMakeKML.setUp(this.controler.getConfig(), this.controler.getNetwork());
		EgoNetPlansItersMakeKML.generateStyles();
		this.log.info("... done");

		this.log.info(" Setting up the NonSpatial interactor ...");
		this.plansInteractorNS=new NonSpatialInteractor(this.snet);
		this.log.info("... done");

		this.log.info(" Setting up the Spatial interactor ...");
		this.plansInteractorS=new SpatialInteractorEvents(this.snet, teo);
		this.log.info("... done");

		this.snIter = this.controler.getFirstIteration();		
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

