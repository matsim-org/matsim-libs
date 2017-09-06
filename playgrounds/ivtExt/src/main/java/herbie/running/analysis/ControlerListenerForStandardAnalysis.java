package herbie.running.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;

import utils.Bins;


/**
 * 
 * @author bvitins, anhorni
 *
 */
public class ControlerListenerForStandardAnalysis implements StartupListener, IterationEndsListener, ShutdownListener {

	private StandardAnalysisEventHandler eventHandler; 
	private Map<Integer, Double> timePerIterationMap = new HashMap<Integer, Double>();
	private String currentOutputPath;
	private Network network;
	private TreeMap<String, Bins> travelDistanceDistributionByMode;
	private Counts counts;
	private double countsScaleFactor;
	private Double distanceFilter;
	private String distanceFilterCenterNode;
	private String coordinateSystem;
	private String inputCountsFile;
	private String outputFormat;
	
	private double distanceInterval = 1000.0; // default value = 1000, in m
	private double timeInterval = 10; // default value = 10, in minutes
	
	/**
	 * @param currentDir for the output plots
	 */
	public ControlerListenerForStandardAnalysis() {
	}


	@Override
	public void notifyStartup(StartupEvent event) {
		// after the services is started create and add the event handler for events of the mobility simulation
		this.eventHandler = new StandardAnalysisEventHandler();
		event.getServices().getEvents().addHandler(this.eventHandler);
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		// for averageTravelTime
		this.timePerIterationMap.put(event.getIteration(), this.eventHandler.getAverageOverallTripDuration());
		
		if(event.getIteration() % 10 == 0 && event.getIteration() > 0) {
			this.currentOutputPath = event.getServices().getControlerIO().getIterationPath(event.getIteration());
			generateStandardAnalysis(event.getServices());
		}
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.currentOutputPath = event.getServices().getControlerIO().getOutputPath()+"/";
		generateStandardAnalysis(event.getServices());
	}

	private void generateStandardAnalysis(MatsimServices controler) {
		
		compareCounts(controler);
		
		createChartForAverageTravelTime();
		
		createBinsCharts(this.eventHandler.getTravelTimeDistributionByMode(timeInterval), "travel time", "min");
		
		calculateTravelDistanceDistributionByMode(controler);
		createBinsCharts(travelDistanceDistributionByMode, "travel distance", "km");
	}
	
	private void compareCounts(MatsimServices controler) {
//		
//		readCountsParameters(services);
//		
//		// reading counts
//		this.counts = new Counts();
//		MatsimCountsReader countsParser = new MatsimCountsReader(this.counts);
//		countsParser.readFile(this.inputCountsFile);
//		
//		this.network = services.getNetwork();
//		VolumesAnalyzer analyzer = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network);
//		
//		services.getEvents().addHandler(analyzer);
//		
//		CountsComparisonAlgorithm comparator = new CountsComparisonAlgorithm(analyzer, this.counts, this.network, countsScaleFactor);
//		
//		if ((this.distanceFilter != null) && (this.distanceFilterCenterNode != null)) {
//			comparator.setDistanceFilter(this.distanceFilter, this.distanceFilterCenterNode);
//		}
//		comparator.run();
//		
//		int iterationNumber = services.getIterationNumber();
//		if (this.outputFormat.contains("html") || this.outputFormat.contains("all")) {
//				CountsHtmlAndGraphsWriter cgw = new CountsHtmlAndGraphsWriter(this.currentOutputPath, comparator.getComparison(), iterationNumber);
//				cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
//				cgw.addGraphsCreator(new CountsErrorGraphCreator("errors"));
//				cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
//				cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
//				cgw.createHtmlAndGraphs();
//		}
//		if (this.outputFormat.contains("kml")|| this.outputFormat.contains("all")) {
//			String filename = this.currentOutputPath + "/countscompare.kmz";
//			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
//					comparator.getComparison(), this.network, 
//					TransformationFactory.getCoordinateTransformation(this.coordinateSystem, TransformationFactory.WGS84 ));
//			kmlWriter.setIterationNumber(iterationNumber);
//			kmlWriter.writeFile(filename);
//		}
//		if (this.outputFormat.contains("txt")||	this.outputFormat.contains("all")) {
//			String filename = this.currentOutputPath +  "/countscompare.txt";
//			CountSimComparisonTableWriter ctw=new CountSimComparisonTableWriter(comparator.getComparison(),Locale.ENGLISH);
//			ctw.writeFile(filename);
//		}
	}
	
	
	private void readCountsParameters(MatsimServices controler) {
		this.countsScaleFactor = controler.getConfig().counts().getCountsScaleFactor();
		this.distanceFilter = controler.getConfig().counts().getDistanceFilter();
		this.distanceFilterCenterNode = controler.getConfig().counts().getDistanceFilterCenterNode();
		this.inputCountsFile = controler.getConfig().counts().getCountsFileName();
		this.outputFormat = controler.getConfig().counts().getOutputFormat();
		this.coordinateSystem = controler.getConfig().global().getCoordinateSystem();
	}


	private void createBinsCharts(TreeMap<String, Bins> treeMap, String xLabel, String xUnit) {
		
		for (String mode : treeMap.keySet()) {
			Bins bin = treeMap.get(mode);
			bin.plotBinnedDistribution(currentOutputPath, xLabel, xUnit, "#");
		}
	}


	private void createChartForAverageTravelTime() {
		//create the chart with the appropriate helper class of org.matsim.utils.charts
		XYLineChart chart = new XYLineChart("Average travel times per iteration",
				"Iterations", "ttimes");
		chart.addMatsimLogo();
		//create the arrays needed for chart creation
		double[] iters = new double[this.timePerIterationMap.size()];
		double[] times = new double[this.timePerIterationMap.size()];
		int decrement = 1;
		if (this.timePerIterationMap.containsKey(Integer.valueOf(0))) {
			decrement = 0;
		}
		//unfortunately we have to do this as...
		for (Integer k : this.timePerIterationMap.keySet()) {
			iters[k - decrement] = k;
			times[k - decrement] = this.timePerIterationMap.get(k);
		}
		//write to file doing this not in the loop above is cause we
		//would like to have a sorted output.
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < iters.length; i++) {
			buffer.append(iters[i]);
			buffer.append("\t");
			buffer.append(times[i]);
			buffer.append("\n");
		}
		//write the chart
		chart.addSeries("tt", iters, times);
		chart.saveAsPng(currentOutputPath+"travelTimes.png", 800, 600);
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(currentOutputPath+"/travelTimes.txt");
			writer.write(buffer.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * The trip length is the sum of the length of all links, including the route's
	 * end link, but<em>not</em> including the route's start link.
	 *  
	 * @param controler
	 */
	private void calculateTravelDistanceDistributionByMode(MatsimServices controler) {
        this.network = controler.getScenario().getNetwork();
		
		travelDistanceDistributionByMode = new TreeMap<String, Bins>();

        Map<Id<Person>, ? extends Person> persons = controler.getScenario().getPopulation().getPersons();
		
		for (Id<Person> id : persons.keySet()) {
			run(persons.get(id).getSelectedPlan());
		}
	}
	
	public void run(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				Route route = leg.getRoute();
				if (route != null) {
					double dist = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, this.network);
					if (route.getEndLinkId() != null && route.getStartLinkId() != route.getEndLinkId()) {
						dist += this.network.getLinks().get(route.getEndLinkId()).getLength();
					}
					
					String mode = leg.getMode();
					if(!travelDistanceDistributionByMode.containsKey(mode)) {
						createNewDistanceDistributionBin(mode);
					}
					travelDistanceDistributionByMode.get(mode).addVal(dist, 1.0);
				}
			}
		}
	}

	private void createNewDistanceDistributionBin(String mode) {
		Bins travelDistanceDistribution = new Bins(distanceInterval, 100000.0, "Travel Distance Distribution "+mode);
		travelDistanceDistributionByMode.put(mode, travelDistanceDistribution);
	}
}
