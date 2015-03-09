package playground.singapore.calibration;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import playground.singapore.calibration.charts.AddSecondChart;
import playground.singapore.calibration.charts.StackedBarChart;

import java.io.IOException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * CalibrationGraphGenerator generates charts which can be useful for the calibration of the simulation
 * In particular it produces same charts for the simulation output and the provided benchmark data (e.g. travel diary/survey), which can be then conveniently compared 
 *
 * So far following charts are produced:
 * - Trip distance distribution per mode for benchmark/survey and simulation data
 * - Trip travel time distribution per mode for benchmark/survey and simulation data
 * - Comparison graph for differences of mode shares between simulation and benchmark data for each distance class
 * - Comparison graph for differences of mode shares between simulation and benchmark data for each travel time class
 * - Cumulative trip distance distribution per mode for benchmark/survey data and simulation data
 * - Cumulative trip travel time distribution per mode for benchmark/survey data and simulation data
 * - Comparison graph for differences of cumulative mode shares between simulation and benchmark data for each distance class
 * - Comparison graph for differences of cumulative mode shares between simulation and benchmark data for each travel time class
 * - Evolution of simulation mode share per iteration, compared to target value from benchmark/survey
 *
 * @author artemc
 * 
 */

public class CalibrationGraphGenerator  {

	static private final Logger log = Logger.getLogger(CalibrationGraphGenerator.class);

	BenchmarkDataReader surveyDistDataset;
	BenchmarkDataReader simulationDistDataset;
	BenchmarkDataReader surveyTTDataset;
	BenchmarkDataReader simulationTTDataset;
	BenchmarkDataReader modeShareHistoryDataset;
	BenchmarkDataReader modeshareWithinPTDataset;

	/**Output file paths*/
	final String fileTripDistanceByModeBenchmark = "tripDistanceByModeBenchmark.png";
	final String fileTripDistanceByModeSimulation = "tripDistanceByModeSimulation.png";
	final String fileTripDistanceByModeComparison = "tripDistanceByModeComparison.png";
	final String fileCumulativeTripDistanceByModeBenchmark = "cumTripDistanceByModeBenchmark.png";
	final String fileCumulativeTripDistanceByModeSimulation = "cumTripDistanceByModeSimulation.png";
	final String fileCumulativeTripDistanceByModeComparison = "cumTripDistanceByModeComparison.png";

	final String fileModeshareHistory = "modeshareHistory.png";
	final String filePTBreakdownEZLink = "ptBreakdownEZLink.png";

	final String fileTripTTByModeBenchmark = "tripTTByModeBenchmark.png";
	final String fileTripTTByModeSimulation = "tripTTByModeSimulation.png";
	final String fileTripTTByModeComparison = "tripTTByModeComparison.png";
	final String fileCumulativeTripTTByModeBenchmark = "cumTripTTByModeBenchmark.png";
	final String fileCumModeshareTTSimulation = "cumTripTTByModeSimulation.png";
	final String fileCumModeshareTTComparison = "cumTripTTByModeComparison.png";
	private String commonColorScheme;
	
	private String outputDistanceUnit;
	private String outputTimeUnit;


	public CalibrationGraphGenerator(String outputDistanceUnit, String outputTimeUnit){
		this.outputDistanceUnit = outputDistanceUnit;
		this.outputTimeUnit = outputTimeUnit;
	}
	
	
	/**
	 * Creates calibration charts for the simulation and benchmark/survey dataset
	 *
	 * param color scheme (Red_Scheme, Autumn, Muted Rainbow, Lollapalooza, French Girl, M8_Colors)
	 * param map of number of trips per distances class, split by mode
	 * param map of number of trips per travel time class, split by mode
	 * param map of number of trips per iteration, split my mode
	 * param path of the iteration folder
	 * param survey name
	 */

	public void createCalibrationCharts(String colorScheme, SortedMap<Integer, Integer[]> distanceTripMap, SortedMap<Integer, Integer[]> travelTimeTripMap, SortedMap<Integer, Integer[]> numberTripsPerMode, String path, String surveyName) throws IOException{

		path = path.substring(0,path.lastIndexOf("/"))+"/calibration"+path.substring(path.lastIndexOf("/"),path.length());
		getSimulationData(distanceTripMap, travelTimeTripMap, numberTripsPerMode);

		simulationDistDataset.calculateSharesAndTotals();
		simulationTTDataset.calculateSharesAndTotals();

		commonColorScheme = colorScheme;

		createModeShareStackedBarChart(surveyName+" Mode Share per Distance Class", path+fileTripDistanceByModeBenchmark, surveyDistDataset, "Distance [km]", "Mode share [%]");
		createModeShareStackedBarChart("Simulation Mode Share per Distance Class", path+fileTripDistanceByModeSimulation, simulationDistDataset, "Distance [km]", "Mode share [%]");
		createModeShareComparisonChart("Modeshare per Distance Class - "+surveyName+" vs. Simulation", path+fileTripDistanceByModeComparison, simulationDistDataset, surveyDistDataset, "Distance [km]", "Bias mode share [%]");
		createCummulativeModeShareChart(surveyName+" Cumulative Mode Share per Distance Class",path+fileCumulativeTripDistanceByModeBenchmark,surveyDistDataset, "Distance [km]", "Cumulative mode share [%]");
		createCummulativeModeShareChart("Simulation Cumulative Mode Share per Distance Class",path+fileCumulativeTripDistanceByModeSimulation,simulationDistDataset, "Distance [km]", "Cumulative mode share [%]");
		createCummulativeModeShareComparisonChart("Cummulative Modeshare per Distance Class - "+surveyName+" vs. Simulation", path+fileCumulativeTripDistanceByModeComparison, simulationDistDataset, surveyDistDataset, "Distance [km]", "Bias cumulative mode share (%)");

		createModeShareStackedBarChart(surveyName+" Mode Share per Travel Time Class", path+fileTripTTByModeBenchmark, surveyTTDataset, "Travel Time [min]", "Mode share [%]");
		createModeShareStackedBarChart("Simulation Mode Share per Travel Time Class", path+fileTripTTByModeSimulation, simulationTTDataset, "Travel Time [min]", "Mode share [%]");
		createModeShareComparisonChart("Modeshare per Travel Time - "+surveyName+" vs. Simulation", path+fileTripTTByModeComparison, simulationTTDataset, surveyTTDataset, "Travel Time [min]", "Bias mode share [%]");
		createCummulativeModeShareChart(surveyName+" Cumulative Mode Share per Travel Time Class",path+fileCumulativeTripTTByModeBenchmark,surveyTTDataset, "Travel Time [min]", "Cumulative mode share [%]");
		createCummulativeModeShareChart("Simulation Cumulative Mode Share per Travel Time Class",path+fileCumModeshareTTSimulation,simulationTTDataset, "Travel Time [min]", "Cumulative mode share [%]");
		createCummulativeModeShareComparisonChart("Cummulative Modeshare per Travel Time Class - "+surveyName+" vs. Simulation", path+fileCumModeshareTTComparison, simulationTTDataset, surveyTTDataset, "Travel Time [min]", "Bias cumulative mode share (%)");

		createModeShareHistoryChart("Simulation Modeshare", path.substring(0,path.indexOf("ITERS"))+fileModeshareHistory, modeShareHistoryDataset, "Iteration", "Mode share [%]", surveyTTDataset);		
	}


	public void getSurveyData(String modeshareDistanceBenchmarkFile, String modeshareTTBenchmarkFile) throws IOException{
		surveyDistDataset = new BenchmarkDataReader(modeshareDistanceBenchmarkFile, outputDistanceUnit);
		surveyTTDataset = new BenchmarkDataReader(modeshareTTBenchmarkFile, outputTimeUnit);
	}



	private void getSimulationData(SortedMap<Integer, Integer[]> distanceJourneyMap, SortedMap<Integer, Integer[]> travelTimeJourneyMap, SortedMap<Integer, Integer[]> numberJourneysPerMode) throws IOException {

		/**Read distance datasets from .csv files*/

		simulationDistDataset = new BenchmarkDataReader(distanceJourneyMap, surveyDistDataset.getModes(), surveyDistDataset.getCategories(), outputDistanceUnit);

		if(surveyDistDataset.getDataMap().keySet().size()!=simulationDistDataset.getDataMap().keySet().size() ||
				surveyDistDataset.getModes().length != simulationDistDataset.getModes().length){
			log.error("Calibration: Datasets ModeshareDist are inconisstent!");
		}

		/**Read travel time datasets from .csv files*/

		simulationTTDataset = new BenchmarkDataReader(travelTimeJourneyMap, surveyTTDataset.getModes(), surveyTTDataset.getCategories(), outputTimeUnit);
		if(surveyTTDataset.getDataMap().keySet().size()!=simulationTTDataset.getDataMap().keySet().size() ||
				surveyTTDataset.getModes().length != simulationTTDataset.getModes().length){
			log.error("Calibration: Datasets ModeshareTT are inconisstent!");
		}
		String[] iterations = new String[numberJourneysPerMode.keySet().size()];
		int i=0;
		for(Integer iteration:numberJourneysPerMode.keySet()) {
			iterations[i] = iteration.toString();
			i++;
		}
		modeShareHistoryDataset = new BenchmarkDataReader(numberJourneysPerMode, surveyDistDataset.getModes(), iterations, "iteration");
		modeShareHistoryDataset.calculateSharesAndTotals();
	}


	/**Create series for each mode as percentage of total trips for each distance class*/
	private void createModeShareStackedBarChart(String title, String filePath, BenchmarkDataReader data, String xLabel, String yLabel){

		StackedBarChart stackedBarChart = new StackedBarChart(title, xLabel, yLabel, data.getCategories());
		double[] modeShareArray = new double[data.getCategories().length];

		int modeCount = 0;
		for(String mode:data.getModes()){
			int i=0;
			for(Integer key:data.getDataMap().keySet()){			
				Integer[] shares = data.getDataMap().get(key);
				modeShareArray[i]= shares[modeCount] / data.getTotalTripsMap().get(key) *100;
				i++;
			}				
			stackedBarChart.addSeries(mode, modeShareArray);
			modeCount++;
		}	

		AddSecondChart secondAxis = new AddSecondChart(stackedBarChart.getChart(), "Total share of observations [%]", data.getCategories(), 0,50);
		secondAxis.addChartAndAxis();        		
		secondAxis.addSeries("Total trip share", data.getTotalTripShareArray());

		GraphEditor stackedBarChartEdit = new GraphEditor(stackedBarChart, 10 ,0.0, 100.0, commonColorScheme);
		stackedBarChartEdit.stackedBarRenderer();
		stackedBarChart.saveAsPng(filePath, 1024, 768);	
	}

	private void createModeShareComparisonChart(String title, String filePath, BenchmarkDataReader simulationData,
			BenchmarkDataReader surveyData, String xLabel, String yLabel) {

		SortedMap<Integer, Double[]> comparisonMap = new TreeMap<Integer, Double[]>();
		comparisonMap = MapComparison.createDiffMap(simulationData.getDataMap(), surveyData.getDataMap());

		BarChart comparisonChart = new BarChart(title, xLabel, yLabel, surveyData.getCategories());

		GraphEditor barChartComparisonEdit = new GraphEditor(comparisonChart, 10, commonColorScheme);
		barChartComparisonEdit.barRenderer();

		double[] modeComparisonArray =  new double[surveyData.getCategories().length] ;
		for(int modeNumber=0; modeNumber<surveyData.getModes().length; modeNumber++){

			int i=0;
			for(Integer key:surveyData.getDataMap().keySet()){
				modeComparisonArray[i] = comparisonMap.get(key)[modeNumber];
				i++;
			}
			comparisonChart.addSeries(surveyData.getModes()[modeNumber],modeComparisonArray);
		}
		comparisonChart.saveAsPng(filePath, 1024, 768);

	}

	private void createCummulativeModeShareChart(String title, String filenameCum,
			BenchmarkDataReader surveyData, String xLabel, String yLabel) {

		SortedMap<Integer, Double[]> cumulativeValuesMap = new TreeMap<Integer, Double[]>();
		cumulativeValuesMap = MapComparison.createCumulativeValuesMap(surveyData);
		StackedBarChart cumulativeStackedBarChart = new StackedBarChart(title, xLabel, yLabel, surveyData.getCategories());
		double[] cumulativeModeShareArray = new double[surveyData.getCategories().length];

		/**Create series for each mode as percentage of total trips for each distance class*/
		int modeCount=0;
		for(String mode:surveyData.getModes()){
			int i=0;
			for(Integer key:cumulativeValuesMap.keySet()){			
				cumulativeModeShareArray[i]= cumulativeValuesMap.get(key)[modeCount]*100;				
				i++;
			}	
			modeCount++;
			cumulativeStackedBarChart.addSeries(mode, cumulativeModeShareArray);
		}					
		GraphEditor cumulativeStackedBarChartEdit = new GraphEditor(cumulativeStackedBarChart, 10, 0.0, 100.0, commonColorScheme);
		cumulativeStackedBarChartEdit.stackedBarRenderer();
		cumulativeStackedBarChart.saveAsPng(filenameCum, 1024, 768);		
	}

	private void createCummulativeModeShareComparisonChart(String title, String filePath,
			BenchmarkDataReader simulationData, BenchmarkDataReader surveyData, String xLabel, String yLabel) {

		SortedMap<Integer, Double[]> cumulativeComparisonMap = new TreeMap<Integer, Double[]>();
		cumulativeComparisonMap = MapComparison.createCumulativeDiffMap(simulationData.getDataMap(), surveyData.getDataMap());

		BarChart comparisonCumulativeChart = new BarChart(title, xLabel, yLabel, surveyData.getCategories());
		GraphEditor barChartComparisonEdit = new GraphEditor(comparisonCumulativeChart, 5, commonColorScheme);
		barChartComparisonEdit.barRenderer();

		double[] cumulativeComparisonArray =  new double[surveyData.getCategories().length] ;

		for(int modeNumber=0; modeNumber<surveyData.getModes().length; modeNumber++){	
			int i=0;
			for(Integer key:surveyData.getDataMap().keySet()){
				cumulativeComparisonArray[i] = cumulativeComparisonMap.get(key)[modeNumber];
				i++;
			}
			comparisonCumulativeChart.addSeries(surveyData.getModes()[modeNumber],cumulativeComparisonArray);
		}

		int lb = (int) Math.round(comparisonCumulativeChart.getChart().getCategoryPlot().getRangeAxis().getLowerBound());
		int ub = (int) Math.round(comparisonCumulativeChart.getChart().getCategoryPlot().getRangeAxis().getUpperBound());

		AddSecondChart cumulativeTotalChart = new AddSecondChart(comparisonCumulativeChart.getChart(), "Total share [%]", surveyData.getCategories(), lb,ub);
		cumulativeTotalChart.addChart();       		

		Double totalCumulativeDifference[] = new Double[cumulativeComparisonMap.keySet().size()];
		Arrays.fill(totalCumulativeDifference, 0.0);
		int r=0;
		for(Integer key:cumulativeComparisonMap.keySet()){
			for(int i=0;i<cumulativeComparisonMap.get(key).length;i++){
				totalCumulativeDifference[r] = (totalCumulativeDifference[r] +  (double) cumulativeComparisonMap.get(key)[i]);
			}
			r++;
		}

		cumulativeTotalChart.addSeries("Total cumulative bias", totalCumulativeDifference);
		comparisonCumulativeChart.saveAsPng(filePath, 1024, 768);		
	}

	/**Create series for each mode as percentage of total trips for each distance class*/
	private void createModeShareHistoryChart(String title, String filePath, BenchmarkDataReader data, String xLabel, String yLabel, BenchmarkDataReader surveyData){

		String[] newCategories = new String[data.getCategories().length+2];
		for(int p=0;p<data.getCategories().length;p++)
			newCategories[p]=data.getCategories()[p];
		newCategories[newCategories.length-2]="";
		newCategories[newCategories.length-1]="Survey";

		StackedBarChart stackedBarChart = new StackedBarChart(title, xLabel, yLabel, newCategories);
		double[] modeShareArray = new double[newCategories.length];	
		int modeCount = 0;
		for(String mode:data.getModes()){
			int i=0;
			for(Integer key:data.getDataMap().keySet()){			
				Integer[] shares = data.getDataMap().get(key);
				modeShareArray[i]= shares[modeCount] / data.getTotalTripsMap().get(key) *100;
				i++;
			}	
			System.out.println(mode+","+surveyData.getTripsPerModeMap().get(mode)+","+surveyData.getTotalTrips());
			modeShareArray[modeShareArray.length-2]=0.0;
			modeShareArray[modeShareArray.length-1]= surveyData.getTripsPerModeMap().get(mode) / (double) surveyData.getTotalTrips() *100;
			stackedBarChart.addSeries(mode, modeShareArray);
			modeCount++;
		}				

		GraphEditor stackedBarChartEdit = new GraphEditor(stackedBarChart, 10 ,0.0, 100.0, commonColorScheme);
		stackedBarChartEdit.stackedBarRenderer();
		stackedBarChart.saveAsPng(filePath, 1024, 768);	
	}


	public BenchmarkDataReader getSurveyDistDataset() {
		return surveyDistDataset;
	}


	public BenchmarkDataReader getSurveyTTDataset() {
		return surveyTTDataset;
	}

}

