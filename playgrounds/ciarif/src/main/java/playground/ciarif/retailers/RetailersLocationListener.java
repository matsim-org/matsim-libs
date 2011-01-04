package playground.ciarif.retailers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
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
import playground.ciarif.retailers.IO.FileRetailerReader;
import playground.ciarif.retailers.IO.LinksRetailerReader;
import playground.ciarif.retailers.IO.RetailersSummaryWriter;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;
import playground.ciarif.retailers.utils.CountFacilityCustomers;
import playground.ciarif.retailers.utils.ReRoutePersons;
import playground.ciarif.retailers.utils.Utils;

public class RetailersLocationListener
  implements StartupListener, IterationEndsListener, BeforeMobsimListener
{
  private static final Logger log = Logger.getLogger(RetailersLocationListener.class);
  public static final String CONFIG_GROUP = "Retailers";
  public static final String CONFIG_RETAILERS = "retailers";
  public static final String CONFIG_STRATEGY_TYPE = "strategyType";
  public static final String CONFIG_MODEL_ITERATION = "modelIteration";
  public static final String CONFIG_ANALYSIS_FREQUENCY = "analysisFrequency";
  public static final String CONFIG_RSW_OUTPUT_FILE = "rswOutputFile";
  private PlansCalcRoute pcrl = null;
  private final boolean parallel = false;
  private String facilityIdFile = null;
  private Retailers retailers;
  private Controler controler;
  private LinksRetailerReader lrr;
  private RetailersSummaryWriter rsw;
  private CountFacilityCustomers cfc;

  public void notifyStartup(StartupEvent event)
  {
    this.controler = event.getControler();
    FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(this.controler.getConfig().charyparNagelScoring());
    this.pcrl = new PlansCalcRoute(this.controler.getConfig().plansCalcRoute(), this.controler.getNetwork(), timeCostCalc, timeCostCalc, new AStarLandmarksFactory(this.controler.getNetwork(), timeCostCalc));

    this.facilityIdFile = this.controler.getConfig().findParam("Retailers", "retailers");
    if (this.facilityIdFile == null) throw new RuntimeException("In config file, param = retailers in module = Retailers not defined!");

    this.retailers = new FileRetailerReader(this.controler.getFacilities().getFacilities(), this.facilityIdFile).readRetailers(this.controler);

    this.cfc = new CountFacilityCustomers(this.controler.getPopulation().getPersons());
    Utils.setFacilityQuadTree(Utils.createFacilityQuadTree(this.controler));
    log.info("Creating PersonQuadTree");
    Utils.setPersonQuadTree(Utils.createPersonQuadTree(this.controler));

    this.lrr = new LinksRetailerReader(this.controler, this.retailers);
    this.lrr.init();

    String rswOutputFile = this.controler.getConfig().findParam("Retailers", "rswOutputFile");
    if (rswOutputFile == null) {
      throw new RuntimeException("The file to which the Retailers Summary should be written has not been set");
    }

    this.rsw = new RetailersSummaryWriter(rswOutputFile);
  }

  public void notifyBeforeMobsim(BeforeMobsimEvent event)
  {
  }

  public void notifyIterationEnds(IterationEndsEvent event)
  {
    Retailer r;
    int modelIter = 0;
    String modelIterParam = this.controler.getConfig().findParam("Retailers", "modelIteration");
    if (modelIterParam == null) {
      log.warn("The iteration in which the model should be run has not been set, the model will be performed at the last iteration");
      modelIter = this.controler.getLastIteration();
    }
    else {
      modelIter = Integer.parseInt(modelIterParam);
    }

    int analysisFrequency = 0;
    String AnalysisFrequencyParam = this.controler.getConfig().findParam("Retailers", "analysisFrequency");
    if (AnalysisFrequencyParam == null) {
      log.warn("The frequency with which the analysis should be run has not been set, the analysis will be only performed when the model will run and at the last iteration");
      analysisFrequency = this.controler.getLastIteration();
    }
    else {
      analysisFrequency = Integer.parseInt(AnalysisFrequencyParam);
    }

    if (event.getIteration() == modelIter)
    {
      for (Iterator<Retailer> localIterator = this.retailers.getRetailers().values().iterator(); localIterator.hasNext(); ) { r = (Retailer)localIterator.next();
        this.rsw.write(r, event.getIteration(), this.cfc);
        r.runStrategy(this.lrr.getFreeLinks());
        this.lrr.updateFreeLinks();
        Map persons = this.controler.getPopulation().getPersons();
        new ReRoutePersons().run(r.getMovedFacilities(), this.controler.getNetwork(), persons, this.pcrl, this.controler.getFacilities());
      }
    }
    if ((this.controler.getIterationNumber().intValue() != 0) && (this.controler.getIterationNumber().intValue() % analysisFrequency == 0) && (this.controler.getIterationNumber().intValue() != modelIter) && (this.controler.getIterationNumber().intValue() != this.controler.getLastIteration()))
    {
    	log.info("Test1");
    	for (Iterator<Retailer> localIterator = this.retailers.getRetailers().values().iterator(); localIterator.hasNext(); ) { r = (Retailer)localIterator.next();

        this.rsw.write(r, this.controler.getIterationNumber().intValue(), this.cfc);
      }

    }

    if (this.controler.getIterationNumber().intValue() != this.controler.getLastIteration())
      return;
    for (Iterator<Retailer> localIterator = this.retailers.getRetailers().values().iterator(); localIterator.hasNext(); ) { r = (Retailer)localIterator.next();

      this.rsw.write(r, this.controler.getIterationNumber().intValue(), this.cfc);
      log.info("Test2");
    }

    this.rsw.close();
  }
}
