package scenarios.braessWoSignals;

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.analysis.AnalyzeBraessSimulation;

/**
 * Class to run a simulation of the braess scenario without signals.
 * It also analyzes the simulation with help of AnalyseBraessSimulation.java.
 * 
 * @author tthunig
 *
 */
public class RunBraessWoSignals {

	public static void main(String[] args) {
		String date = "2015-04-14";
		boolean middleLink = true;
		boolean reduceCap = true;
		String inputDir = DgPaths.SHAREDSVN + "studies/tthunig/scenarios/BraessWoSignals/";
		
		String info;
		if (middleLink)
			info = "Braess";
		else
			info = "BraessWoMiddleLink";
		if (reduceCap)
			info += "_capReduced";
		String outputDir = inputDir + "matsim-output/" + date + "_" + info + "/";
		String configFile = inputDir + "config.xml";
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFile);
		config.controler().setOutputDirectory(outputDir);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		if (!middleLink) {
			// delete middle link by setting a travel time of 200s (instead of 0s)
			scenario.getNetwork().getLinks().get(Id.create(4, Link.class)).setFreespeed(1); //instead of 20
			scenario.getNetwork().getLinks().get(Id.create(4, Link.class)).setLength(200); //instead of 0
		}
		if (reduceCap){
			// reduce capacity on link 2 and 6 from 2100 veh/h to 1800 veh/h
			scenario.getNetwork().getLinks().get(Id.create(2, Link.class)).setCapacity(1800);
			scenario.getNetwork().getLinks().get(Id.create(6, Link.class)).setCapacity(1800);
		}
		
		Controler controler = new Controler(scenario);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
//		controler.setOverwriteFiles(true);
		controler.run();
		
		//Analyze
		String analyzeDir = outputDir + "analysis/";
		new File(analyzeDir).mkdir();
		int lastIteration = 100;		
		new AnalyzeBraessSimulation(outputDir, lastIteration, analyzeDir).analyze();
	}

}
