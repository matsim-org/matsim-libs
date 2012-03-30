package playground.anhorni.surprice;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import playground.anhorni.surprice.preprocess.Zone;

public class UtilityAnalyzer {
	
	private List<Zone> zones = new Vector<Zone>();
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private UtilitiesBoxPlot boxPlot = new UtilitiesBoxPlot("Utilities");
	
	public static void main (final String[] args) {
		UtilityAnalyzer analyzer = new UtilityAnalyzer();
		Config config = ConfigUtils.loadConfig(args[0]);
		String outPath = args[1];
		
		Config configCreate = ConfigUtils.loadConfig("C:/l/studies/surprice/configCreate.xml");
		double sideLength = Double.parseDouble(configCreate.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
		
		analyzer.analyze(config, outPath, sideLength);
	}
		
	public void analyze(Config config, String outPath, double sideLength) {
		this.initZones(sideLength);
		
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());		
		new FacilitiesReaderMatsimV1(scenario).readFile(config.facilities().getInputFile());
		
		TreeMap<String, Utilities> utilitiesPerZone = new TreeMap<String, Utilities>();
		ArrayList<Double> utilities = new ArrayList<Double>();
		
		for (String day : Surprice.days) {
			String plansFilePath = outPath + "/" + day + "/" + day + ".output_plans.xml.gz";
			MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
			populationReader.readFile(plansFilePath);

			this.computeZoneUtilities(utilitiesPerZone, day);
			this.computeUtilities(utilities, day);
			this.boxPlot.addUtilities(utilities, day);
			
			this.scenario.getPopulation().getPersons().clear();
		}	
		this.write(outPath, utilitiesPerZone);
		this.boxPlot.createChart();
		this.boxPlot.saveAsPng(outPath + "/utilities.png", 400, 300);
	}
	
	private void computeUtilities(ArrayList<Double> utilities, String day) {		
		double avgUtility = 0.0;
		int n = 0;
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			avgUtility += person.getSelectedPlan().getScore();
			n++;
		}
		avgUtility /= n;
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			utilities.add(person.getSelectedPlan().getScore() / avgUtility);
		}		
	}
		
	private void computeZoneUtilities(TreeMap<String, Utilities> utilitiesPerZone, String day) {		
		for (Zone zone : this.zones) {
			if (utilitiesPerZone.get(zone.getName()) == null) {
				Utilities utilitiesPerDay = new Utilities();
				utilitiesPerZone.put(zone.getName(), utilitiesPerDay);
			}
			double avgUtility = 0.0;
			int n = 0;
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				Activity homeAct = (Activity)person.getSelectedPlan().getPlanElements().get(0);
				if (zone.inZone(homeAct.getCoord())) {
					avgUtility += person.getSelectedPlan().getScore();
					n++;
				}
			}
			avgUtility /= n;			
			utilitiesPerZone.get(zone.getName()).setUtilityPerDay(day, avgUtility);			
		}
	}
	
	private void write(String outPath, TreeMap<String, Utilities> utilitiesPerZone) {
		DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outPath + "/summary.txt")); 
			bufferedWriter.write("Zone\tMon\tTue\tWed\tThu\tFri\tSat\tSun\n");
			for (Zone zone : this.zones) {
				Utilities uPerZone = utilitiesPerZone.get(zone.getName());
				
				String line = zone.getName();
				
				for (String day : Surprice.days) {
					line += "\t" + formatter.format(uPerZone.getUtilityPerDay(day));
				}			
				bufferedWriter.append(line);
				bufferedWriter.newLine();
			}			
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initZones(double sideLength) {
		this.zones.add(new Zone("centerZone", (Coord) new CoordImpl(sideLength / 2.0 - 500.0, sideLength / 2.0 + 500.0), 1000.0, 1000.0));
		this.zones.add(new Zone("topLeftZone", (Coord) new CoordImpl(0.0, sideLength), 1000.0, 1000.0));
		this.zones.add(new Zone("bottomLeftZone", (Coord) new CoordImpl(0.0, 1000.0), 1000.0, 1000.0));
		this.zones.add(new Zone("bottomRightZone", (Coord) new CoordImpl(sideLength - 1000.0, 0.0), 1000.0, 1000.0));
	}
}
