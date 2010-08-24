package playground.ciarif.retailers;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;

import playground.ciarif.retailers.IO.FileRetailerReader;
import playground.ciarif.retailers.IO.LinksRetailerReader;
import playground.ciarif.retailers.IO.RetailersSummaryWriter;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;
import playground.ciarif.retailers.utils.CountFacilityCustomers;
import playground.ciarif.retailers.utils.ReRoutePersons;

public class RetailersLocationListener implements StartupListener, IterationEndsListener, BeforeMobsimListener{
	
	private final static Logger log = Logger.getLogger(RetailersLocationListener.class);
	
	//Base_Config arguments
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_RETAILERS = "retailers";
	public final static String CONFIG_STRATEGY_TYPE = "strategyType";
	public final static String CONFIG_MODEL_ITERATION = "modelIteration";
	public static final String CONFIG_ANALYSIS_FREQUENCY = "analysisFrequency"; 
	public final static String CONFIG_RSW_OUTPUT_FILE = "rswOutputFile";
	//public final static String CONFIG_POP_SUM_TABLE = "populationSummaryTable"; //TODO Look if they make sense anymore, try to use them again or delete it
	//public final static String CONFIG_RET_SUM_TABLE = "retailersSummaryTable";
	
	//private variables
	
	private PlansCalcRoute pcrl = null;
	private final boolean parallel = false;
	private String facilityIdFile = null;
	private Retailers retailers;
	private Controler controler;
	private LinksRetailerReader lrr;
	private RetailersSummaryWriter rsw;
	private CountFacilityCustomers cfc;

	
	// public methods
	
	public void notifyStartup(StartupEvent event) {
		
		this.controler = event.getControler();
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(controler.getConfig().charyparNagelScoring());
		pcrl = new PlansCalcRoute(controler.getConfig().plansCalcRoute(), controler.getNetwork(),timeCostCalc, timeCostCalc, new AStarLandmarksFactory(controler.getNetwork(), timeCostCalc));
		
		//The characteristics of retailers are read
		this.facilityIdFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		if (this.facilityIdFile == null) {throw new RuntimeException("In config file, param = "+CONFIG_RETAILERS+" in module = "+CONFIG_GROUP+" not defined!");}
		else { 
			this.retailers = new FileRetailerReader (controler.getFacilities().getFacilities(), this.facilityIdFile).readRetailers(this.controler);
		}
		
		this.lrr = new LinksRetailerReader (controler, retailers);
		lrr.init();
		
		String rswOutputFile = (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_RSW_OUTPUT_FILE));
		if (rswOutputFile == null) {
			throw new RuntimeException("The file to which the Retailers Summary should be written has not been set");
		}
		else {
			this.rsw = new RetailersSummaryWriter (rswOutputFile);
		}
		this.cfc = new CountFacilityCustomers(controler.getPopulation().getPersons());
		
	}
	
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
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
		
		int analysisFrequency =0;
		String AnalysisFrequencyParam = (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_ANALYSIS_FREQUENCY));
		if (AnalysisFrequencyParam == null) {
			log.warn("The frequency with which the analysis should be run has not been set, the analysis will be only performed when the model will run and at the last iteration");
			analysisFrequency = controler.getLastIteration();
		}
		else {
			analysisFrequency = Integer.parseInt (AnalysisFrequencyParam);
		}
		
		if (event.getIteration() == gravityModelIter){
			// TODO maybe need to add if sequential statement
			//TODO The file name below should be read from the config file
			
			for (Retailer r : this.retailers.getRetailers().values()) {
				this.rsw.write(r, event.getIteration(), this.cfc);
				r.runStrategy(lrr.getFreeLinks());
				lrr.updateFreeLinks();
				Map<Id, PersonImpl> persons = (Map<Id, PersonImpl>)controler.getPopulation().getPersons();
				new ReRoutePersons().run(r.getMovedFacilities(), controler.getNetwork(), persons, pcrl, controler.getFacilities());  
			}
		}
		if (controler.getIterationNumber()!=0 && controler.getIterationNumber()%analysisFrequency==0 && controler.getIterationNumber()!= gravityModelIter && controler.getIterationNumber()!= controler.getLastIteration() ) {
		
			for (Retailer r : this.retailers.getRetailers().values()) {
			
				rsw.write(r, controler.getIterationNumber(),this.cfc);  
			
			}
		}
		
		if (controler.getIterationNumber()== controler.getLastIteration()) {
			
			for (Retailer r : this.retailers.getRetailers().values()) {

				rsw.write(r, controler.getIterationNumber(),this.cfc);  

			}
			rsw.close();
		}
	}
}
