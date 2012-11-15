package playground.anhorni.surprice.analysis;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.anhorni.surprice.Surprice;

public class TwoRunAnalyzer {
	
	private final static Logger log = Logger.getLogger(TwoRunAnalyzer.class);
	private String run0path;
	private String run1path;
	private String outPath;
	
	private ScenarioImpl scenario = null; 
	
	
	public static void main(String[] args) {
		if (args.length != 3) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		TwoRunAnalyzer analyzer = new TwoRunAnalyzer();
		analyzer.init(args[0], args[1], args[2]);
		analyzer.run();
	}
	
	public void init(String run0path, String run1path, String outPath) {
		this.run0path = run0path;
		this.run1path = run1path;
		this.outPath = outPath;
				
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		String plansFilePath = run0path + "/mon/mon.output_plans.xml.gz";
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);			
	}
	
	public void run() {
		for (String day : Surprice.days) {	
			ObjectAttributes run0prefs = new ObjectAttributes();
			this.readPrefs(day, run0prefs, this.run0path);
			
			ObjectAttributes run1prefs = new ObjectAttributes();
			this.readPrefs(day, run1prefs, this.run1path);
					
			ObjectAttributes run0td = new ObjectAttributes();
			this.readTollDistances(day, run0td, this.run0path);
			
			ObjectAttributes run1td = new ObjectAttributes();
			this.readTollDistances(day, run1td, this.run1path);
			
			this.writePlots(day, run0prefs, run1prefs, run0td, run1td, "alpha", "tolltd", "tolltd_alpha_Diff", 10.0 * 1000.0);
					
			ObjectAttributes run0tt = new ObjectAttributes();
			this.readTT(day, run0tt, this.run0path);
			
			ObjectAttributes run1tt = new ObjectAttributes();
			this.readTT(day, run1tt, this.run1path);
			
			this.writePlots(day, run0prefs, run1prefs, run0td, run1td, "alpha", "tt", "tt_alpha_dDiff", 3600.0);		
			
		}	
		log.info("=================== Finished analyses ====================");
	}
	
	/*
	 * y can be limited!
	 */
	private void writePlots(String day, ObjectAttributes run0x, ObjectAttributes run1x,
		ObjectAttributes run0y, ObjectAttributes run1y, String xName, String yName, String fileName, double range) {
				
		ArrayList<Double> xDiffA = new ArrayList<Double>();
		ArrayList<Double> yDiffA = new ArrayList<Double>();
		
		TreeMap<Id, Double> yDiffsPerAgent = new TreeMap<Id, Double>();
		TreeMap<Id, Double> xDiffsPerAgent = new TreeMap<Id, Double>();
		
		boolean cont = true;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			double yDiff = 0.0;
			if (run1y.getAttribute(p.getId().toString(), yName) != null) {
				yDiff = (Double) run1y.getAttribute(p.getId().toString(), yName);
				cont = false;
			}
			if (run0y.getAttribute(p.getId().toString(), yName) != null) {
				yDiff -= (Double) run0y.getAttribute(p.getId().toString(), yName);
				cont = false;
			}
			if (cont) continue;	
			yDiffA.add(yDiff);
			
			double xDiff = (Double) run1x.getAttribute(p.getId().toString(), xName) -
					(Double) run0x.getAttribute(p.getId().toString(), xName);
			xDiffA.add(xDiff);	
			yDiffsPerAgent.put(p.getId(), yDiff);
			xDiffsPerAgent.put(p.getId(), xDiff);
		}
		this.write(day, xDiffA, yDiffA, fileName, range, xName, yName);
		this.writeDiffsPerAgent(day, xDiffsPerAgent, yDiffsPerAgent, fileName + "PerAgent", xName, yName);
	}
		
	private void write(String day, ArrayList<Double> xDiffA, ArrayList<Double> yDiffA, String fileName, double range,
			String xName, String yName) {		
		XYScatterChart chart = new XYScatterChart("", "diff " + xName, "diff " + yName);
		
		double x[] = new double[xDiffA.size()];				
		double y[] = new double[yDiffA.size()];
		for (int i = 0; i < yDiffA.size(); i++) {
			if (Math.abs(yDiffA.get(i)) < range) {
				y[i] = yDiffA.get(i);
				x[i] = xDiffA.get(i);
			}
		}	
		chart.addSeries("", x, y);
		chart.saveAsPng(this.outPath + "/" + day + "." + fileName + ".png", 800, 600);			
	}
	
	private void writeDiffsPerAgent(String day, TreeMap<Id, Double> xDiffsPerAgent, TreeMap<Id, Double> yDiffsPerAgent, String fileName,
			String xName, String yName) {
		ObjectAttributes diffsPerAgentOA = new ObjectAttributes();		
		for (Id id : yDiffsPerAgent.keySet()) {
			diffsPerAgentOA.putAttribute(id.toString(), "diff_" + xName, xDiffsPerAgent.get(id));
			diffsPerAgentOA.putAttribute(id.toString(), "diff_" + yName, yDiffsPerAgent.get(id));		
		}		
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(diffsPerAgentOA);
		attributesWriter.writeFile(this.outPath + "/" + day + "." + fileName + ".txt");
	}
	
	private void readTollDistances(String day, ObjectAttributes oa, String path) {	
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(oa);
		preferencesReader.parse(path + "/" + day + "/" + day + ".tolltdPerAgent.txt");
	}
	
	private void readTT(String day, ObjectAttributes oa, String path) {	
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(oa);
		preferencesReader.parse(path + "/" + day + "/" + day + ".ttPerAgent.txt");
	}
	
	private void readPrefs(String day, ObjectAttributes oa, String path) {	
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(oa);
		preferencesReader.parse(path + "/" + day + "/" + day + ".prefsPerAgent.txt");
	}
}
