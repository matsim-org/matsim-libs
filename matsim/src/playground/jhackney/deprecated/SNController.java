/* *********************************************************************** *
 * project: org.matsim.*
 * SNController.java
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

package playground.jhackney.deprecated;

import static org.matsim.controler.Controler.DIRECTORY_ITERS;
import static org.matsim.controler.Controler.outputPath;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.roadpricing.RoadPricingScoringFunctionFactory;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.world.algorithms.WorldBottom2TopCompletion;

import playground.jhackney.controler.SNControlerListener;
import playground.jhackney.interactions.NonSpatialInteractor;
import playground.jhackney.interactions.SocializingOpportunity;
import playground.jhackney.interactions.SpatialInteractor;
import playground.jhackney.interactions.SpatialSocialOpportunityTracker;
import playground.jhackney.io.JUNGPajekNetWriterWrapper;
import playground.jhackney.io.PajekWriter1;
import playground.jhackney.replanning.SNFacilitySwitcher;
import playground.jhackney.scoring.SNScoringFunctionFactory01;
import playground.jhackney.socialnet.SocialNetwork;
import playground.jhackney.statistics.SocialNetworkStatistics;

public class SNController extends Controler {

	protected boolean overwriteFiles = true;
	private final boolean SNFLAG = true;
	private boolean CALCSTATS = true;
	private static final String DIRECTORY_SN = "socialnets";
	public static String SOCNET_OUT_DIR = null;

	SocialNetwork snet;
	SocialNetworkStatistics snetstat;
	PajekWriter1 pjw;
	NonSpatialInteractor plansInteractorNS;//non-spatial (not observed, ICT)
	SpatialInteractor plansInteractorS;//spatial (face to face)
	int max_sn_iter;
	String [] infoToExchange;//type of info for non-spatial exchange is read in
	public static String activityTypesForEncounters[]={"home","work","shop","education","leisure"};

	SpatialSocialOpportunityTracker gen2 = new SpatialSocialOpportunityTracker();
	Collection<SocializingOpportunity> socialPlans=null;

//	Variables for allocating the spatial meetings among different types of activities
	double fractionS[];
	HashMap<String,Double> rndEncounterProbs= new HashMap<String,Double>();
//	New variables for replanning
	int replan_interval;

	private final static Logger log = Logger.getLogger(SNController.class);
	
	public SNController(final String[] args) {
		super(args);
	}

	@Override
	protected void loadData() {
		// this.world
		loadWorld();
		// this.facilities
		loadFacilities();
		this.network = loadNetwork();
		this.population = loadPopulation();

		// Stitch together the world
		if (this.config.world().getInputFile() == null) {
			new WorldBottom2TopCompletion().run(Gbl.getWorld());
		}

		//loadSocialNetwork();
		// if (this.config.socialnet().getInputFile() == null) {
		System.out.println("Loading initial social network");
		// also initializes knowledge.map.egonet
		//}

		System.out.println(" Initializing agent knowledge about geography ...");
		initializeKnowledge(this.population);
		System.out.println("... done");

	}

	@Override
	protected void startup() {
		super.startup();

		System.out.println("----------Initialization of social network -------------------------------------");
		snsetup();
	}

	@Override
	/**
	 * This is a test StrategyManager to see if the replanning works within the social network iterations.
	 * @author jhackney
	 * @return
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();

		String maxvalue = this.config.findParam("strategy", "maxAgentPlanMemorySize");
		manager.setMaxPlansPerAgent(Integer.parseInt(maxvalue));

		// Best-scoring plan chosen each iteration
		PlanStrategy strategy1 = new PlanStrategy(new BestPlanSelector());

		// Social Network Facility Exchange test
		System.out.println("### NOTE THAT FACILITY SWITCHER IS HARD-CODED TO RANDOM SWITCHING OF FACILITIES FROM KNOWLEDGE");
		System.out.println("### NOTE THAT YOU SHOULD EXCHANGE KNOWLEDGE BASED ON ITS VALUE");
		strategy1.addStrategyModule(new SNFacilitySwitcher());
		//strategy1.addStrategyModule(new TimeAllocationMutator());


		// Social Network Facility Exchange for all agents
		manager.addStrategy(strategy1, 1.0);
		return manager;
	}


	@Override
	protected void finishIteration(final int iteration){
		super.finishIteration(iteration);

		System.out.println("finishIteration: Note setting snIter = iteration +1 for now");
		int snIter = iteration+1;

		if(total_spatial_fraction(this.fractionS)>0){ // only generate the map if spatial meeting is important in this experiment
			System.out.println("MAKE A MAP OF SOCIAL EVENTS THAT ACTUALLY OCCURRED IN THE MOBSIM");
			System.out.println("  AND CALCULATE SOCIAL UTILITY FROM THIS");
			System.out.println("  AND/OR USE IT TO COMPARE WITH PLANNED INTERACTIONS");
			System.out.println("  AND USE THIS TO REINFORCE OR DEGRADE LINK STRENGTHS");
			System.out.println("  NEEDS AN EVENT HANDLER");

			System.out.println("  Generating planned [Spatial] socializing opportunities ...");
			System.out.println("   Mapping which agents did what, where, and when");
//			socialEvents = gen2.generate(population);
			System.out.println("...finished.");

			//}// end if

			// Agents' actual interactions
			System.out.println("  Agents' actual interactions at the social opportunities ...");
//			plansInteractorS.interact(socialEvents, rndEncounterProbs, snIter);

		}else{
			System.out.println("     (none)");
		}
		System.out.println(" ... Spatial interactions done\n");

		System.out.println(" Removing social links ...");
		this.snet.removeLinks(snIter);
		System.out.println(" ... done");

<<<<<<< .mine
		if(CALCSTATS){
			System.out.println(" Calculating and reporting network statistics ...");
			snetstat.calculate(snIter, snet, population);
			System.out.println(" ... done");
		}
=======
		System.out.println(" Calculating and reporting network statistics ...");
		this.snetstat.calculate(snIter, this.snet, this.population);
		System.out.println(" ... done");
>>>>>>> .r469

		System.out.println(" Writing out social network for iteration " + snIter + " ...");
		this.pjw.write(this.snet.getLinks(), this.population, snIter);
		System.out.println(" ... done");

		if(iteration == maxIterations){
<<<<<<< .mine
			if(CALCSTATS){
				System.out.println("----------Closing social network statistic files and wrapping up ---------------");
				snetstat.closeFiles();
			}
=======
			System.out.println("----------Closing social network statistic files and wrapping up ---------------");
			this.snetstat.closeFiles();
>>>>>>> .r469
			snwrapup();
		}
	}
	private void snwrapup(){
		JUNGPajekNetWriterWrapper pnww = new JUNGPajekNetWriterWrapper(outputPath,this.snet, this.population);
		pnww.write();

		if(CALCSTATS){
			System.out.println(" Writing the statistics of the final social network to Output Directory...");

<<<<<<< .mine
			SocialNetworkStatistics snetstatFinal=new SocialNetworkStatistics();
			snetstatFinal.openFiles(outputPath);
			snetstatFinal.calculate(maxIterations, snet, population);
=======
		SocialNetworkStatistics snetstatFinal=new SocialNetworkStatistics();
		snetstatFinal.openFiles(outputPath);
		snetstatFinal.calculate(maxIterations, this.snet, this.population);
>>>>>>> .r469

<<<<<<< .mine
			System.out.println(" ... done");
			snetstatFinal.closeFiles();
		}
	}		
=======
		System.out.println(" ... done");
		snetstatFinal.closeFiles();
	}
>>>>>>> .r469

	@Override
	protected void setupIteration(final int iteration) {
		System.out.println("setupIteration: Note setting snIter = iteration for now");
		int snIter = iteration;

		this.fireControlerSetupIterationEvent(iteration);
		// TODO [MR] use events.resetHandlers();
		this.travelTimeCalculator.resetTravelTimes();	// reset, so we can collect the new events and build new travel times for the next iteration

		this.eventwriter = new EventWriterTXT(getIterationFilename(Controler.FILENAME_EVENTS));
		this.events.addHandler(this.eventwriter);
		if (this.planScorer == null) {
			if (Gbl.useRoadPricing()) {
				this.planScorer = new EventsToScore(this.population, new RoadPricingScoringFunctionFactory(this.tollCalc, new CharyparNagelScoringFunctionFactory()));
			}else if (this.SNFLAG){
				this.planScorer = new EventsToScore(this.population, new SNScoringFunctionFactory01());
			} else {
				this.planScorer = new EventsToScore(this.population, new CharyparNagelScoringFunctionFactory());
			}
			this.events.addHandler(this.planScorer);
		} else {
			this.planScorer.reset(iteration);
		}

		// collect and average volumes information in iterations *6-*0, e.g. it.6-10, it.16-20, etc
		if ((iteration % 10 == 0) || (iteration % 10 >= 6)) {
			this.volumes.reset(iteration);
			this.events.addHandler(this.volumes);
		}

		System.out.println("#### Be careful resetting leg times now. You use this in calculating the score later");
		this.legTimes.reset(iteration);

		// dump plans every 10th iteration
		if ((iteration % 10 == 0) || (iteration < 3)) {
			printNote("", "dumping all agents' plans...");
			this.stopwatch.beginOperation("dump all plans");
			String outversion = this.config.plans().getOutputVersion();
			PlansWriter plansWriter = new PlansWriter(this.population, getIterationFilename(Controler.FILENAME_PLANS), outversion);
			plansWriter.setUseCompression(true);
			plansWriter.write();
			this.stopwatch.endOperation("dump all plans");
			printNote("", "done dumping plans.");
		}

		if(total_spatial_fraction(this.fractionS)>0){ // only generate the map if spatial meeting is important in this experiment

			//if(Events have just changed or if no events are yet available and if there is an interest in the planned interactions)
			System.out.println("  Generating planned [Spatial] socializing opportunities ...");
			System.out.println("   Mapping which agents want to do what, where, and when");
			System.out.println("   Note that there is only one structure for each person containing SOCIAL DATES");
			System.out.println("   and that it is cleared before new F2F windows are generated");
			this.socialPlans = this.gen2.generate(this.population);
			System.out.println("...finished.");

			//}// end if

			// Agents' planned interactions
			System.out.println("  Agents planned social interactions ...");
			System.out.println("  Agents' relationships are updated to reflect these interactions! ...");
			this.plansInteractorS.interact(this.socialPlans, this.rndEncounterProbs, snIter);

		}else{
			System.out.println("     (none)");
		}
		System.out.println(" ... Spatial interactions done\n");

		System.out.println(" Non-Spatial interactions ...");
		for (int ii = 0; ii < this.infoToExchange.length; ii++) {
			String facTypeNS = this.infoToExchange[ii];

			//	Geographic Knowledge about all types of places is exchanged
			if (!facTypeNS.equals("none")) {
				System.out.println("  Geographic Knowledge about all types of places is being exchanged ...");
				this.plansInteractorNS.exchangeGeographicKnowledge(facTypeNS, snIter);
			}
		}

		// Exchange of knowledge about people
		double fract_intro=Double.parseDouble(this.config.socnetmodule().getTriangles());
		if (fract_intro > 0) {
			System.out.println("  Knowledge about other people is being exchanged ...");
			this.plansInteractorNS.exchangeSocialNetKnowledge(snIter);
		}

		System.out.println("  ... done");

		if(iteration == minIteration){
			makeSNIterationPath(snIter);
			makeSNIterationPath(iteration, snIter);
		}

	}


	void initializeKnowledge( final Plans plans ){

		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)

		for( Person person : plans.getPersons().values() ){

			Knowledge k = person.getKnowledge();
			if(k ==null){
				k = person.createKnowledge("created by " + this.getClass().getName());
			}
			// Initialize knowledge to the facilities that are in all initial plans
			Iterator<Plan> piter=person.getPlans().iterator();
			while (piter.hasNext()){
				Plan plan = piter.next();
				k.map.matchActsToActivities(plan);
			}
		}
	}

	private void snsetup() {

//		Config config = Gbl.getConfig();

		SOCNET_OUT_DIR = outputPath + "/"+DIRECTORY_SN;
		File snDir = new File(SOCNET_OUT_DIR);
		if (!snDir.mkdir() && !snDir.exists()) {
			Gbl.errorMsg("The iterations directory " + (outputPath + "/" + DIRECTORY_SN) + " could not be created.");
		}


		this.max_sn_iter = Integer.parseInt(this.config.socnetmodule().getNumIterations());
		this.replan_interval = Integer.parseInt(this.config.socnetmodule().getRPInt());
		String rndEncounterProbString = this.config.socnetmodule().getFacWt();
		String xchangeInfoString = this.config.socnetmodule().getXchange();
		this.infoToExchange = getFacTypes(xchangeInfoString);
		this.fractionS = toNumber(rndEncounterProbString);
		this.rndEncounterProbs = mapActivityWeights(activityTypesForEncounters, rndEncounterProbString);

		System.out.println(" Instantiating the Pajek writer ...");

		this.pjw = new PajekWriter1(SOCNET_OUT_DIR, facilities);
		System.out.println("... done");

		System.out.println(" Initializing the social network ...");
		this.snet = new SocialNetwork(this.population);
		System.out.println("... done");

<<<<<<< .mine
		if(CALCSTATS){
			System.out.println(" Calculating the statistics of the initial social network)...");
			snetstat=new SocialNetworkStatistics();
			snetstat.openFiles();
			snetstat.calculate(0, snet, population);
			System.out.println(" ... done");
		}
=======
		System.out.println(" Calculating the statistics of the initial social network)...");
		this.snetstat=new SocialNetworkStatistics();
		this.snetstat.openFiles();
		this.snetstat.calculate(0, this.snet, this.population);
		System.out.println(" ... done");
>>>>>>> .r469

		System.out.println(" Writing out the initial social network ...");
		this.pjw.write(this.snet.getLinks(), this.population, 0);
		System.out.println("... done");

		System.out.println(" Setting up the NonSpatial interactor ...");
		this.plansInteractorNS=new NonSpatialInteractor(this.snet);
		System.out.println("... done");

		System.out.println(" Setting up the Spatial interactor ...");
		this.plansInteractorS=new SpatialInteractor(this.snet);
		System.out.println("... done");
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
		// TODO Auto-generated method stub
		String patternStr = ",";
		String[] s;
		log.info("!!add keyword\"any\" and a new interact method to exchange info of any factility types (compatible with probabilities)");
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
				System.out.println(this.getClass() + ":" + s[i]);
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
			if(w[i]<0.||w[i]>1.){
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
	private HashMap<String,Double> mapActivityWeights(final String[] types, final String longString) {
		String patternStr = ",";
		String[] s;
		HashMap<String,Double> map = new HashMap<String,Double>();
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if(w[i]<0.||w[i]>1.){
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

	private final void makeSNIterationPath(final int iteration) {
		new File(getSNIterationPath(iteration)).mkdir();
	}
	private final void makeSNIterationPath(final int iteration, final int snIter) {
		System.out.println(getSNIterationPath(iteration, snIter));
		File iterationOutFile= new File(getSNIterationPath(iteration, snIter));
		iterationOutFile.mkdir();
//		if (!iterationOutFile.mkdir()) {
//		Gbl.errorMsg("The output directory " + iterationOutFile + " could not be created. Does its parent directory exist?");
//		}
	}

	/**
	 * returns the path to the specified social network iteration directory. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(final int snIter) {
		return outputPath + "/" + DIRECTORY_ITERS + "/"+snIter;
	}
	/**
	 * returns the path to the specified iteration directory,
	 * including social network iteration. The directory path does not include the trailing '/'
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public final static String getSNIterationPath(final int iteration, final int sn_iter) {
		return outputPath + "/" + DIRECTORY_ITERS + "/"+sn_iter + "/it." + iteration;
	}

	public static void main(final String[] args) {
		final Controler controler = new SNController(args);
		controler.addControlerListener(new SNControlerListener());
		controler.run();
		System.exit(0);
	}
}
