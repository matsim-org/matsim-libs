package playground.artemc.scoring;

import org.apache.log4j.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.charts.XYLineChart;
import playground.artemc.analysis.TripAnalysisHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class DisaggregatedHeterogeneousScoreAnalyzer implements IterationEndsListener{
	private static final Logger log = Logger.getLogger(DisaggregatedHeterogeneousScoreAnalyzer.class);

	private ScenarioImpl scenario;
	private Map<Id, DisaggregatedScore> disaggregatedScores = new HashMap<Id, DisaggregatedScore>();
	private Map<Integer, Double> activityUtility2it = new TreeMap<Integer, Double>();
	private Map<Integer, Double> legUtilityTotal2it = new TreeMap<Integer, Double>();
	private Map<String, Map<Integer, Double>> legUtility2it = new TreeMap<String, Map<Integer, Double>>();
	private Map<Integer, Double> moneyUtility2it = new TreeMap<Integer, Double>();
	private Map<Integer, Double> stuckUtility2it = new TreeMap<Integer, Double>();
	private Map<Integer, Double> sumUtility2it = new TreeMap<Integer, Double>();
	private TripAnalysisHandler tripAnalysisHandler;

	public DisaggregatedHeterogeneousScoreAnalyzer(ScenarioImpl scenario, TripAnalysisHandler tripAnalysisHandler) {
		super();
		this.scenario = scenario;
		this.tripAnalysisHandler = tripAnalysisHandler;

		for(String mode:scenario.getConfig().planCalcScore().getModes().keySet()){
			legUtility2it.put(mode, new TreeMap<Integer, Double>());
		};	
		legUtility2it.put("transit_walk", new TreeMap<Integer, Double>());

	}


	public Map<Id, DisaggregatedScore> getDisaggregatedScores() {
		return disaggregatedScores;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		this.activityUtility2it.put(event.getIteration(),0.0);
		this.legUtilityTotal2it.put(event.getIteration(),0.0);
		for(String mode:legUtility2it.keySet()){
			legUtility2it.get(mode).put(event.getIteration(), 0.0);
		};	
		this.moneyUtility2it.put(event.getIteration(),0.0);
		this.stuckUtility2it.put(event.getIteration(),0.0);
		this.sumUtility2it.put(event.getIteration(),0.0);

        for(Person person: event.getControler().getScenario().getPopulation().getPersons().values()) {
			//DisaggregatedSumScoringFunction sf = (DisaggregatedSumScoringFunction) event.getControler().getPlansScoring().getScoringFunctionForAgent(person.getId());
			HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory disScoringFactory = (HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory) event.getControler().getScoringFunctionFactory();
			DisaggregatedSumScoringFunction sf = (DisaggregatedSumScoringFunction) disScoringFactory.getPersonScoringFunctions().get(person.getId());

			disaggregatedScores.put(person.getId(), new DisaggregatedScore(sf.getActivityTotalScore(), sf.getLegScores(), sf.getMoneyTotalScore(), sf.getStuckScore()));
			this.activityUtility2it.put(event.getIteration(),activityUtility2it.get(event.getIteration())+sf.getActivityTotalScore());
			this.legUtilityTotal2it.put(event.getIteration(),legUtilityTotal2it.get(event.getIteration())+sf.getLegTotalScore( ));
			this.moneyUtility2it.put(event.getIteration(),moneyUtility2it.get(event.getIteration())+sf.getMoneyTotalScore());
			this.stuckUtility2it.put(event.getIteration(),stuckUtility2it.get(event.getIteration())+sf.getStuckScore());
			for(String mode:legUtility2it.keySet()){
				legUtility2it.get(mode).put(event.getIteration(), legUtility2it.get(mode).get(event.getIteration())+sf.getLegScores().get(mode));
			};	
			this.sumUtility2it.put(event.getIteration(), sumUtility2it.get(event.getIteration())+sf.getScore());
		}


		writeAnalysis(event);
	}

	private void writeAnalysis(IterationEndsEvent event) {

		String fileName = this.scenario.getConfig().controler().getOutputDirectory() + "/disaggregatedScore.csv";
		File file = new File(fileName);
        Integer persons = event.getControler().getScenario().getPopulation().getPersons().size();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			String header = "Iteration;Activity utility;Leg Utility";
			ArrayList<String> modeList = new ArrayList<String>();
			for(String mode:scenario.getConfig().planCalcScore().getModes().keySet()){
				header = header + ";"+mode+" utility";
				modeList.add(mode);
			};	
			header = header + ";Transit walk;Money utility;Stuck Utility;Total utility";
			bw.write(header);
			bw.newLine();

			for (Integer it : this.activityUtility2it.keySet()){
				String legsScores = "";
				for(String mode:modeList){
					if(tripAnalysisHandler.getModeLegCount().get(mode)>0){
						legsScores = legsScores + (double)legUtility2it.get(mode).get(it)/tripAnalysisHandler.getModeLegCount().get(mode)+";";
					}
				};	
				if(tripAnalysisHandler.getModeLegCount().get("transit_walk")>0){
					legsScores = legsScores + (double)legUtility2it.get("transit_walk").get(it)/tripAnalysisHandler.getModeLegCount().get("transit_walk")+";";
				}
				
				bw.write(it + ";" + (double)this.activityUtility2it.get(it)/persons + ";" + (double)this.legUtilityTotal2it.get(it)/persons + ";" + legsScores 
						+  + (double)this.moneyUtility2it.get(it)/persons + ";" + (double)this.stuckUtility2it.get(it)/persons + ";" + this.sumUtility2it.get(it)/persons
						);
				bw.newLine();
			}

			bw.close();
			log.info("Output written to " + fileName);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// ##################################################

		ArrayList<Map> data = new ArrayList<Map>();

		data.add(activityUtility2it);
		data.add(legUtilityTotal2it);
		data.add(moneyUtility2it);
		data.add(stuckUtility2it);
		//	data.add(sumUtility2it);

		String[] names = {"Activity Utility", "Leg Utility", "Money Utility", "Stuck Utility","Total Utility"};
		writeGraphs("scoreBreakdown", names, "Utility", data, persons);



	}

	private void writeGraphs(String chartName, String[] names, String yLabel, ArrayList<Map> data, Integer persons) {

		int i=0;
		XYLineChart chart = new XYLineChart(chartName, "Iteration", yLabel);
		for(Map<Integer, Double> series:data){
			double[] xValues = new double[series.size()];
			double[] yValues = new double[series.size()];
			int counter = 0;
			for (Integer iteration : series.keySet()){
				xValues[counter] = iteration.doubleValue();
				yValues[counter] = (double)series.get(iteration)/persons;
				counter++;
			}

			chart.addSeries(names[i], xValues, yValues);
			i++;
		}

		//XYPlot plot = chart.getChart().getXYPlot(); 
		//NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		//axis.setAutoRange(true);
		//axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + chartName + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}


	private void writeGraphSum(String name, String yLabel, Map<Integer, Double> it2Double1, Map<Integer, Double> it2Double2) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double1.size()];
		double[] yValues = new double[it2Double1.size()];
		int counter = 0;
		for (Integer iteration : it2Double1.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = (it2Double1.get(iteration)) + (it2Double2.get(iteration));
			counter++;
		}

		chart.addSeries(name, xValues, yValues);

		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

	private void writeGraphDiv(String name, String yLabel, Map<Integer, Double> it2Double1, Map<Integer, Double> it2Double2) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double1.size()];
		double[] yValues = new double[it2Double1.size()];
		int counter = 0;
		for (Integer iteration : it2Double1.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = (it2Double1.get(iteration)) / (it2Double2.get(iteration));
			counter++;
		}

		chart.addSeries(name, xValues, yValues);

		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

	private void writeGraph(String name, String yLabel, Map<Integer, Double> it2Double) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double.size()];
		double[] yValues = new double[it2Double.size()];
		int counter = 0;
		for (Integer iteration : it2Double.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = it2Double.get(iteration);
			counter++;
		}

		chart.addSeries(name, xValues, yValues);

		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);

		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}



	public class DisaggregatedScore {
		private double activityScore;
		private Map<String, Double> legScores = new HashMap<String, Double>();
		private double moneyScore;
		private double stuckScore;

		public DisaggregatedScore(double activityScore, Map<String, Double> legScores, double moneyScore, double stuckScore) {
			super();
			this.activityScore = activityScore;
			this.legScores = legScores;
			this.moneyScore = moneyScore;
			this.stuckScore = stuckScore;
		}

	}

}
