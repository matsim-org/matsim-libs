package playground.dhosse.prt.launch;

import java.io.PrintWriter;

import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.LegHistogramChart;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.LeastCostPathCalculatorWithCache;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathCalculatorImpl;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import pl.poznan.put.util.ChartUtils;
import playground.dhosse.prt.NPersonsActionCreator;
import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.dhosse.prt.request.NPersonsVehicleRequestPathFinder;
import playground.dhosse.prt.router.PrtTripRouterFactoryImpl;
import playground.dhosse.prt.scheduler.PrtScheduler;
import playground.michalm.taxi.TaxiActionCreator;
import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.optimizer.TaxiOptimizer;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.optimizer.filter.DefaultFilterFactory;
import playground.michalm.taxi.optimizer.filter.FilterFactory;
import playground.michalm.taxi.run.TaxiLauncherUtils;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.util.chart.TaxiScheduleChartUtils;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;

public class VrpLauncher {
	
	private PrtParameters params;
	
	private Scenario scenario;
	private TravelTimeSource ttimeSource;
	private TravelDisutilitySource tdisSource;
	private TravelTime travelTime;
	private TravelDisutility travelDisutility;
	private LeastCostPathCalculator router;
	private LeastCostPathCalculatorWithCache routerWithCache;
	private VrpPathCalculator calculator;
	
	private MatsimVrpContextImpl context;
	
	public VrpLauncher(PrtParameters params){
		this.params = params;
		this.scenario = VrpLauncherUtils.initScenario(this.params.pathToNetworkFile, this.params.pathToPlansFile);
		this.init(this.scenario, null);
	}
	
	public VrpLauncher(String netFile, String plansFile, String eventsFileName){
		
		scenario = VrpLauncherUtils.initScenario(netFile, plansFile);
		this.init(scenario, eventsFileName);
		
	}
	
	public void init(Scenario scenario, String eventsFileName){
		
		ttimeSource = TravelTimeSource.FREE_FLOW_SPEED;
        tdisSource = TravelDisutilitySource.TIME;

        this.travelTime = VrpLauncherUtils.initTravelTime(scenario, this.params.algorithmConfig.ttimeSource, eventsFileName);
        this.travelDisutility = VrpLauncherUtils.initTravelDisutility(this.params.algorithmConfig.tdisSource, this.travelTime);

        this.router = new Dijkstra(scenario.getNetwork(), this.travelDisutility, this.travelTime);

        this.routerWithCache = new LeastCostPathCalculatorWithCache(
                this.router, new TimeDiscretizer(31*4, 15 *60,false));
        
        this.calculator = new VrpPathCalculatorImpl(this.routerWithCache, this.travelTime,
                this.travelDisutility);
		
	}
	
	public void run(){
		
		this.context = new MatsimVrpContextImpl();
        this.context.setScenario(this.scenario);
        Config config = scenario.getConfig();
        config.qsim().setEndTime(30*3600);
        config.controler().setOutputDirectory("C:/Users/Daniel/Desktop/dvrp/");
        config.controler().setCreateGraphs(true);
        
        TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, "C:/Users/Daniel/Dropbox/dvrpTestRun/vehicles_mb.xml",
        		"C:/Users/Daniel/Dropbox/dvrpTestRun/ranks_mb.xml");
        context.setVrpData(taxiData);
        PrtData prtData = new PrtData(scenario.getNetwork(), taxiData);
        
        PrtTripRouterFactoryImpl tripRouterFactory = new PrtTripRouterFactoryImpl(this.context);
        final PersonAlgorithm router = new PlanRouter(tripRouterFactory.instantiateAndConfigureTripRouter(new RoutingContextImpl(this.travelDisutility, this.travelTime)));
        
        for(Person p : scenario.getPopulation().getPersons().values()){
        	router.run(p);
        }
        
        TaxiOptimizerConfiguration optimConfig = initOptimizerConfiguration();
        TaxiOptimizer optimizer = params.algorithmConfig.createTaxiOptimizer(optimConfig);
//        APSTaxiOptimizer optimizer = new APSTaxiOptimizer(optimConfig);
//        RESTaxiOptimizer optimizer = new RESTaxiOptimizer(optimConfig);
//        PrtNPersonsOptimizer optimizer = new PrtNPersonsOptimizer(optimConfig);
//        OTSTaxiOptimizer optimizer = new OTSTaxiOptimizer(optimizerConfig);
//        NOSTaxiOptimizer optimizer = new NOSTaxiOptimizer(optimConfig);
        
        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        context.setMobsimTimer(qSim.getSimTimer());
        
        qSim.addQueueSimulationListeners(optimizer);

        //passenger engine
        PassengerEngine passengerEngine = new PassengerEngine(PrtRequestCreator.MODE, new PrtRequestCreator(), optimizer,
                context);
        qSim.addMobsimEngine(passengerEngine);
        qSim.addDepartureHandler(passengerEngine);
        
        if(this.params.nPersons){
        	VrpLauncherUtils.initAgentSources(qSim, context, optimizer, new NPersonsActionCreator(
        			passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, params.pickupDuration));
        } else{
        	VrpLauncherUtils.initAgentSources(qSim, context, optimizer, new TaxiActionCreator(
        			passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, params.pickupDuration));
        }

        EventsManager events = qSim.getEventsManager();
        
        EventWriterXML writer = new EventWriterXML("C:/Users/Daniel/Desktop/dvrp/events.xml");
        events.addHandler(writer);
        
        PopulationWriter pWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        pWriter.write("C:/Users/Daniel/Desktop/dvrp/" + "person.xml");
        
        if (this.params.otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, true, ColoringScheme.taxicab);
        }
        
        LegHistogram legHistogram = new LegHistogram(300, 360);// LegHistogram(300);
        events.addHandler(legHistogram);

        qSim.run();
        
        events.finishProcessing();
        writer.closeFile();
        legHistogram.write("C:/Users/Daniel/Desktop/dvrp/hist.txt");
        LegHistogramChart.writeGraphic(legHistogram, "C:/Users/Daniel/Desktop/dvrp/hist.png", "prt");
        
//        generateOutput();
        
	}
	
	void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
//        pw.println(params.algorithmConfig.name());
        pw.println("m\t" + context.getVrpData().getVehicles().size());
        pw.println("n\t" + context.getVrpData().getRequests().size());
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator().calculateStats(context.getVrpData()
                .getVehicles());
        pw.println(stats);
        pw.flush();

//        if (params.vrpOutDir != null) {
//            new Schedules2GIS(context.getVrpData().getVehicles(),
//                    TransformationFactory.WGS84_UTM33N).write(params.vrpOutDir);
//        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(TaxiScheduleChartUtils.chartSchedule(context.getVrpData()
                .getVehicles()));

//        if (params.histogramOutDir != null) {
//            VrpLauncherUtils.writeHistograms(legHistogram, params.histogramOutDir);
//        }
    }
	
	private TaxiOptimizerConfiguration initOptimizerConfiguration(){
		
		if(this.params.nPersons){
			PrtScheduler scheduler = new PrtScheduler(this.context, this.calculator, this.params.taxiParams);
			NPersonsVehicleRequestPathFinder vrpFinder = new NPersonsVehicleRequestPathFinder(this.calculator, scheduler);
			FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
			
			return new TaxiOptimizerConfiguration(this.context, this.calculator, scheduler, vrpFinder, filterFactory,
					this.params.algorithmConfig.goal, this.params.workingDir);
		}
		
		TaxiScheduler scheduler = new TaxiScheduler(this.context, this.calculator, this.params.taxiParams);
		VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(this.calculator, scheduler);
		FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
		
		return new TaxiOptimizerConfiguration(this.context, this.calculator, scheduler, vrpFinder, filterFactory,
				this.params.algorithmConfig.goal, this.params.workingDir);
		
	}
	
	public Scenario getScenario() {
		return scenario;
	}

	public TravelTimeSource getTtimeSource() {
		return ttimeSource;
	}

	public TravelDisutilitySource getTdisSource() {
		return tdisSource;
	}

	public TravelTime getTravelTime() {
		return travelTime;
	}

	public TravelDisutility getTravelDisutility() {
		return travelDisutility;
	}

	public LeastCostPathCalculator getRouter() {
		return router;
	}

	public LeastCostPathCalculatorWithCache getRouterWithCache() {
		return routerWithCache;
	}

	public VrpPathCalculator getCalculator() {
		return calculator;
	}

}
