package playground.sergioo.mixedtraffic2016;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class MainMixedTraffic {
	
	/**
	 * 
	 * @param args
	 * 0 - config file
	 * 1 - link id
	 * 2 - events file
	 * 3 - interval (s)
	 * 4 - output file
	 * [5] - end time (s)
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		File configFile = new File(args[0]);
		Scenario scenario =ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile.getAbsolutePath()));
		File networkFile = new File(configFile.getParentFile(),scenario.getConfig().network().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile.getAbsolutePath());
		EventsManager events = EventsUtils.createEventsManager();
		double endTime = scenario.getConfig().qsim().getEndTime();
		if(endTime==0)
			endTime = Double.parseDouble(args[5]);
		double interval = Double.parseDouble(args[3]);
		Collection<String> modes = scenario.getConfig().plansCalcRoute().getNetworkModes();
		LinkAnalyzer analyzer = new LinkAnalyzer(Id.createLinkId(args[1]), interval, modes, scenario.getNetwork(), endTime);
		events.addHandler(analyzer);
		new MatsimEventsReader(events).readFile(args[2]);
		System.out.println(analyzer.maxNumVehicles);
		PrintWriter writer = new PrintWriter(args[4]);
		writer.println("Time,Mode,Density,Flow,Speed");
		for(int i=0; i<endTime/interval; i++) {
			writer.println(interval*i+",all,"+analyzer.getDensity(i)+","+analyzer.getFlow(i)+","+analyzer.getSpeed(i));
			for(String mode:modes)
				writer.println(interval*i+","+mode+","+analyzer.getDensity(mode,i)+","+analyzer.getFlow(mode,i)+","+analyzer.getSpeed(mode,i));
		}
		writer.close();
	}

}
