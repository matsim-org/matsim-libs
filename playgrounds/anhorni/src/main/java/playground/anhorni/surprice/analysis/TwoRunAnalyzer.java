package playground.anhorni.surprice.analysis;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

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
			ObjectAttributes run0td = new ObjectAttributes();
			this.readTollDistances(day, run0td, this.run0path);
			
			ObjectAttributes run1td = new ObjectAttributes();
			this.readTollDistances(day, run1td, this.run1path);
			
			ObjectAttributes run0prefs = new ObjectAttributes();
			this.readPrefs(day, run0prefs, this.run0path);
			
			ObjectAttributes run1prefs = new ObjectAttributes();
			this.readPrefs(day, run1prefs, this.run1path);	
			
			ArrayList<Double> alphaDiffA = new ArrayList<Double>();
			ArrayList<Double> tollDistA = new ArrayList<Double>();
			
			for (Person p : this.scenario.getPopulation().getPersons().values()) {
				double tollDistDiff = 0.0;
				boolean cont = true;
				
				if (run1td.getAttribute(p.getId().toString(), "tolltd") != null) {
					tollDistDiff = (Double) run1td.getAttribute(p.getId().toString(), "tolltd");
					cont = false;
				}
				if (run0td.getAttribute(p.getId().toString(), "tolltd") != null) {
					tollDistDiff -= (Double) run0td.getAttribute(p.getId().toString(), "tolltd");
					cont = false;
				}
				if (cont) continue;				
				tollDistA.add(tollDistDiff);
				
				double alphaDiff = (Double) run1prefs.getAttribute(p.getId().toString(), "alpha") -
						(Double) run0prefs.getAttribute(p.getId().toString(), "alpha");
				alphaDiffA.add(alphaDiff);				
			}
			this.write(day, alphaDiffA, tollDistA);
		}	
	}
	
	private void write(String day, ArrayList<Double> alphaDiffA, ArrayList<Double> tollDistA) {
		XYScatterChart chart = new XYScatterChart("DistPrefDiffs", "Delta alpha", "Delta dist");
		
		double alpha[] = new double[alphaDiffA.size()];
		double dist[] = new double[tollDistA.size()];
		
		chart.addSeries("alpha_dist", alpha, dist);
		chart.saveAsPng(this.outPath + day + ".DiffAlphaDist", 800, 600);
	}
	
	private void readTollDistances(String day, ObjectAttributes oa, String path) {	
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(oa);
		preferencesReader.parse(path + "/" + day + ".tollDistancesAgent.txt");
	}
	
	private void readPrefs(String day, ObjectAttributes oa, String path) {	
		ObjectAttributesXmlReader preferencesReader = new ObjectAttributesXmlReader(oa);
		preferencesReader.parse(path + "/" + day + ".prefsAgent.txt");
	}
}
