package playground.balmermi.datapuls;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balmermi.datapuls.modules.FrequencyAnalyser;

public class LinkFrequencyCalculator {
	
	private static Set<Id<Link>> readLinkIds(File linkIdFile) throws IOException {
		FileReader fr = new FileReader(linkIdFile);
		BufferedReader br = new BufferedReader(fr);
		String currLine = null;
		Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
		while ((currLine = br.readLine()) != null) {
			Id<Link> id = Id.create(currLine.trim(), Link.class);
			linkIds.add(id);
		}
		br.close();
		return linkIds;
	}

	public static void main(String[] args) throws IOException {
		File net = new File(args[0]);
		if (!net.isFile() || !net.canRead()) { throw new RuntimeException("net="+net+" is not a File or cannot be read"); }
		System.out.println("network file: "+net);

		File inDir = new File(args[1]);
		if (!inDir.isDirectory()) { throw new RuntimeException("inDir="+inDir+" is not a directory"); }
		System.out.println("events input directory: "+inDir);

		File outDir = new File(args[2]);
		if (!outDir.isDirectory()) { throw new RuntimeException("outDir="+outDir+" is not a directory"); }
		System.out.println("analysis output directory: "+outDir);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(net.getPath());
		
		Set<Id<Link>> linkIds = null;
		if (args.length == 4) {
			File linkIdFile = new File(args[3]);
			if (!linkIdFile.isFile() || !linkIdFile.canRead()) { throw new RuntimeException("linkIdFile="+linkIdFile+" is not a File or cannot be read"); }
			System.out.println("link id file: "+linkIdFile);
			linkIds = readLinkIds(linkIdFile);
		}
		else {
			linkIds = scenario.getNetwork().getLinks().keySet();
		}

		FrequencyAnalyser fa = new FrequencyAnalyser(scenario.getNetwork(),linkIds);
		EventsManager eManager = EventsUtils.createEventsManager();
		eManager.addHandler(fa);
		MatsimEventsReader reader = new MatsimEventsReader(eManager);
		
		Map<Id<Link>, List<Integer>> freqs = new TreeMap<>();
		for (Id<Link> lid : linkIds) { freqs.put(lid,new ArrayList<Integer>()); }

		Map<Integer,File> eventFiles = new TreeMap<Integer, File>();
		for (File eventfile : inDir.listFiles()) {
			if (eventfile.getName().contains("events")) {
				System.out.println("gathering "+eventfile+"...");
				String [] entries = eventfile.getName().split("[//\\.]");
				for (int i=0; i<entries.length; i++) { System.out.print("["+entries[i]+"]"); }
				Integer it = Integer.parseInt(entries[entries.length-4]);
				eventFiles.put(it,eventfile);
				System.out.print("\n");
				System.out.println("done. (gathering)");
			}
		}
		
		for (Integer it : eventFiles.keySet()) {
			File eventfile = eventFiles.get(it);
			System.out.println("processing "+eventfile+"...");
			reader.readFile(eventfile.toString());
			for (Id<Link> lid : fa.getFrequencies().keySet()) {
				freqs.get(lid).add(fa.getFrequencies().get(lid).size());
			}
			fa.resetLog();
			System.out.println("done. (processing)");
		}
		
		System.out.println("writing output...");
		FileWriter fw = new FileWriter(outDir+"/linkFrequencies.txt");
		BufferedWriter out = new BufferedWriter(fw);
		out.write("linkId");
		for (int i=0; i<freqs.values().iterator().next().size(); i++) { out.write("\tstep"+i); }
		out.write("\n");
		for (Id<Link> lid : freqs.keySet()) {
			out.write(lid.toString());
			for (Integer i : freqs.get(lid)) { out.write("\t"+i); }
			out.write("\n");
		}
		out.close();
		fw.close();
		System.out.println("done. (writing)");
	}
}
