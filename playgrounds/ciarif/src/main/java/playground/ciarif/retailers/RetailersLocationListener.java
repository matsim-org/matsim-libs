package playground.ciarif.retailers;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;

import playground.ciarif.retailers.IO.FileRetailerReader;
import playground.ciarif.retailers.IO.LinksRetailerReader;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;
import playground.ciarif.retailers.utils.ReRoutePersons;

public class RetailersLocationListener implements StartupListener, IterationEndsListener, BeforeMobsimListener{
	
	private final static Logger log = Logger.getLogger(RetailersLocationListener.class);
	
	//Base_Config arguments
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_RETAILERS = "retailers";
	public final static String CONFIG_STRATEGY_TYPE = "strategyType";
	public final static String CONFIG_MODEL_ITERATION = "modelIteration";
	//public final static String CONFIG_POP_SUM_TABLE = "populationSummaryTable";
	//public final static String CONFIG_RET_SUM_TABLE = "retailersSummaryTable";
	
	//private variables
	
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRoute pcrl = null;
	private final boolean parallel = false;
	private String facilityIdFile = null;
	private Retailers retailers;
	private Controler controler;
	private LinksRetailerReader lrr;
	
	// public methods
	
	public void notifyStartup(StartupEvent event) {
		
		this.controler = event.getControler();
		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRoute(controler.getNetwork(),timeCostCalc, timeCostCalc, new AStarLandmarksFactory(preprocess));
		
		//The characteristics of retailers are read
		this.facilityIdFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		if (this.facilityIdFile == null) {throw new RuntimeException("In config file, param = "+CONFIG_RETAILERS+" in module = "+CONFIG_GROUP+" not defined!");}
		else { 
			this.retailers = new FileRetailerReader (controler.getFacilities().getFacilities(), this.facilityIdFile).readRetailers(this.controler);
		}
		
		this.lrr = new LinksRetailerReader (controler, retailers);
		lrr.init();
		
	}
	
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		if (parallel) {
			
		}
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		int gravityModelIter =0;
		String modelIterParam = (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_MODEL_ITERATION));
		if (modelIterParam == null) {
			log.warn("The iteration in which the model should be run has not been set, the model will be performed at the last iteration");
			gravityModelIter = controler.getLastIteration();
		}
		else {
			gravityModelIter = Integer.parseInt (modelIterParam);
		}
		// The model is run only every "n" iterations
		//if (controler.getIteration()%gravityModelIter ==0 && controler.getIteration()>0){
		if (controler.getIteration()==gravityModelIter){
			// TODO maybe need to add if sequential statement
			for (Retailer r : this.retailers.getRetailers().values()) {
				r.runStrategy(lrr.getFreeLinks());
				lrr.updateFreeLinks();
				new ReRoutePersons().run(r.getMovedFacilities(), controler.getNetwork(), controler.getPopulation().getPersons(), pcrl, controler.getFacilities());  
			}
		}
	}
}
