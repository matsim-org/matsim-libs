package playground.jhackney.controler;

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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.knowledges.Knowledges;
import org.matsim.world.algorithms.WorldConnectLocations;

import playground.jhackney.algorithms.InitializeKnowledge;
import playground.jhackney.algorithms.ParamStringToStringArray;
import playground.jhackney.algorithms.ParamStringsToStringDoubleMap;
import playground.jhackney.kml.EgoNetPlansItersMakeKML;
import playground.jhackney.socialnetworks.algorithms.CompareTimeWindows;
import playground.jhackney.socialnetworks.algorithms.EventsMapStartEndTimes;
import playground.jhackney.socialnetworks.algorithms.PersonForgetKnowledge;
import playground.jhackney.socialnetworks.interactions.NonSpatialInteractor;
import playground.jhackney.socialnetworks.interactions.SpatialInteractorEvents;
import playground.jhackney.socialnetworks.io.ActivityActWriter;
import playground.jhackney.socialnetworks.io.PajekWriter;
import playground.jhackney.socialnetworks.mentalmap.TimeWindow;
import playground.jhackney.socialnetworks.scoring.EventSocScoringFactory;
import playground.jhackney.socialnetworks.scoring.MakeTimeWindowsFromEvents;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;
import playground.jhackney.socialnetworks.statistics.SocialNetworkStatistics;


/**
 * This controler incorporates a social network into MATSIM {@list org.matsim.socialnetworks.SocialNetwork}.<br><br>
 * Social influences can consist of
 * information exchanges, face to face encounters, and interdependent evaluations of plans
 * (Utility).<br><br>
 * The social network can remain static or evolve with travel plans. The evolution
 * of the social network is wrapped around the travel optimization (doIterations). Thus the
 * rate of its evolution with respect to the rate of relaxing the travel plans can be adjusted.<p>
 *
 * The controller structure is as follows:<br><br>
 * 1) Startup:<p>
 * <li>notifyStartup: initialize agent knowledge (facilities, social network), statistical i/o,
 * the time window tracker, and register the social scoring function.</li>
 * <br><br>
 * 2) Iterate:<p>
 *      <li>execute MobSim</li>
 *      <li>notifyScoring: Execute agent encounters and information exchanges</li>
 *      <li>notifyScoring: Enforce cognitive limits: forget friends and information</li>
 *      <li>notifyScoring: Calculate plan scores</li>
 *      <li>notifyIterationEnds: Output results</li>
 *      <li>execute Replanning: Adjust plans through any combination of replanning strategies</li>
 *      <li>notifyIterationStarts: Reset events-based social network helper objects</li><br>
 *<br>
 * 
 * @author jhackney
 *
 */
public class SNSimpleControllerListener implements StartupListener, BeforeMobsimListener, IterationEndsListener,  ScoringListener{

	private int reportInterval=50;
	private static String SOCNET_OUT_DIR = null;
	private SocialNetwork snet;// static? I just need one
	private SocialNetworkStatistics snetstat;// static? I just need one
	private PajekWriter pjw;// static? I just need one
	private NonSpatialInteractor plansInteractorNS;// static? I just need one
	private SpatialInteractorEvents plansInteractorS;// static? I just need one

	private EventsMapStartEndTimes epp=null;//static? I just need one
	private MakeTimeWindowsFromEvents teo=null;// static? I just need one
	private LinkedHashMap<Activity,ArrayList<Double>> actStats=null;// static? I just need one
	private LinkedHashMap<Id,ArrayList<TimeWindow>> twm=null;// static? I just need one
	//
	private LinkedHashMap<String,Double> rndEncounterProbs= new LinkedHashMap<String,Double>();// static? I just need one
	//
	private String [] infoToExchange;

	private int interact_interval;
	private int snIter;

	private EventsToScore scoring = null;
	private Controler controler = null;
	private final Logger log = Logger.getLogger(SNSimpleControllerListener.class);
	private Knowledges knowledges;

	public void notifyStartup(final StartupEvent event) {
		this.controler = event.getControler();
		this.knowledges = (controler.getScenario()).getKnowledges();
		// Complete the world to make sure that the layers all have relevant mapping rules
		new WorldConnectLocations().run(controler.getWorld());

		this.log.info(" Initializing agent knowledge about geography ...");
		initializeKnowledge();
		this.log.info("... done");

		setupSNScoring();

		snsetup();
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
//			double fract_intro=Double.parseDouble(this.controler.getConfig().socnetmodule().getFriendIntroProb());
			this.log.info("  Knowledge about other people is being exchanged ...");
			this.plansInteractorNS.exchangeSocialNetKnowledge(snIter);
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
			this.actStats.putAll(CompareTimeWindows.calculateTimeWindowEventActStats(twm, controler.getFacilities()));

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

			if((reportInterval>0) && (event.getIteration()%reportInterval==0)){//50
				Gbl.printMemoryUsage();
				controler.stopwatch.beginOperation("netstats");
				this.log.info(" Calculating and reporting network statistics ...");
				this.snetstat.calculate(snIter, this.snet, this.controler.getPopulation(), this.knowledges);
				this.log.info(" ... done");
				controler.stopwatch.endOperation("netstats");

				Gbl.printMemoryUsage();


				this.log.info("  Opening the file to write out the map of Acts to Facilities");
				ActivityActWriter aaw=new ActivityActWriter(this.controler.getFacilities());
				aaw.openFile(SOCNET_OUT_DIR+"/ActivityActMap"+snIter+".txt");
				this.log.info(" Writing out the map between Acts and Facilities ...");
				aaw.write(snIter,this.controler.getPopulation());
				aaw.close();
				this.log.info(" ... done");
			}

			if(event.getIteration()%reportInterval==0){
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
	 * The EventHandlers are reset in notifyIterationStarts.<p>
	 * 
	 * However the results of the last MobSim, which are based on the Events,
	 * might still be needed for social network calculations in RePlanning.<p>
	 * 
	 * Clear these results here, after RePlanning, rather than in
	 * notifyIterationStarts, in case they are needed in RePlanning.<p>
	 * 
	 * See {@link org.matsim.core.controler.Controler#doIterations()}
	 */
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {

		this.teo.clearTimeWindowMap();
		this.actStats.clear();
	}

	/* ===================================================================
	 * private methods
	 * =================================================================== */


	private void snsetup() {

		SOCNET_OUT_DIR = this.controler.getConfig().socnetmodule().getOutDirName();
		File snDir = new File(SOCNET_OUT_DIR);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("The iterations directory " + SOCNET_OUT_DIR + " could not be created.");
		}

		this.reportInterval = Integer.parseInt(this.controler.getConfig().socnetmodule().getReportInterval());
		this.interact_interval = Integer.parseInt(this.controler.getConfig().socnetmodule().getRPInt());
		String xchangeInfoString = this.controler.getConfig().socnetmodule().getXchange();
		this.infoToExchange = new ParamStringToStringArray(xchangeInfoString).getArray();
		this.rndEncounterProbs = new ParamStringsToStringDoubleMap(this.controler.getConfig().socnetmodule().getActTypes(), this.controler.getConfig().socnetmodule().getFacWt()).getMap();

		this.log.info(" Instantiating the Pajek writer ...");
		this.pjw = new PajekWriter(SOCNET_OUT_DIR, this.controler.getFacilities(), this.knowledges);
		this.log.info("... done");

		if(this.controler.getConfig().socnetmodule().getSocNetAlgo()==(null)){
			this.log.error("No social network is set. This controller requires you to configure a social network");
		}

		this.log.info(" Initializing the social network ...");
		this.snet = new SocialNetwork(this.controler.getPopulation(), this.controler.getFacilities());
		this.log.info("... done");

		if(reportInterval>0){
			this.log.info(" Opening the files for the social network statistics...");
			this.snetstat=new SocialNetworkStatistics(SOCNET_OUT_DIR, this.controler.getFacilities());
			this.snetstat.openFiles();
			this.log.info(" ... done");
		}

		this.log.info("  Initializing the KML output");

		EgoNetPlansItersMakeKML.setUp(this.controler.getConfig(), this.controler.getNetwork(), this.controler.getFacilities());
		EgoNetPlansItersMakeKML.generateStyles();
		this.log.info("... done");

		this.log.info(" Setting up the NonSpatial interactor ...");
		this.plansInteractorNS=new NonSpatialInteractor(this.snet, this.knowledges);
		this.log.info("... done");

		this.log.info(" Setting up the Spatial interactor ...");
		this.plansInteractorS=new SpatialInteractorEvents(this.snet, teo, this.controler.getFacilities());
		this.log.info("... done");

		this.snIter = this.controler.getFirstIteration();		
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
		actStats = CompareTimeWindows.calculateTimeWindowEventActStats(twm, controler.getFacilities());
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
		new InitializeKnowledge(this.controler.getPopulation(), this.controler.getFacilities(), this.knowledges);
	}
}


