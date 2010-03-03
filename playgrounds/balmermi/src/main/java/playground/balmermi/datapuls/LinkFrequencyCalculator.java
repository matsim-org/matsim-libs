package playground.balmermi.datapuls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;

import playground.balmermi.datapuls.modules.FrequencyAnalyser;

public class LinkFrequencyCalculator {

	public static void main(String[] args) throws IOException {
		File net = new File(args[0]);
		if (!net.isFile() || !net.canRead()) { throw new RuntimeException("net="+net+" is not a File or cannot be read"); }

		File inDir = new File(args[1]);
		if (!inDir.isDirectory()) { throw new RuntimeException("inDir="+inDir+" is not a directory"); }

		File outDir = new File(args[2]);
		if (!outDir.isDirectory()) { throw new RuntimeException("outDir="+outDir+" is not a directory"); }

		System.out.println("network file: "+net);
		System.out.println("events input directory: "+inDir);
		System.out.println("analysis output directory: "+outDir);
		
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(net.getPath());
		
		FrequencyAnalyser fa = new FrequencyAnalyser(scenario.getNetwork());
		EventsManager eManager = new EventsManagerImpl();
		eManager.addHandler(fa);
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(eManager);
		
		Map<Id,List<Integer>> freqs = new TreeMap<Id, List<Integer>>();
		for (Id lid : scenario.getNetwork().getLinks().keySet()) {
			freqs.put(lid,new ArrayList<Integer>());
		}

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
			for (Id lid : fa.getFrequencies().keySet()) {
				freqs.get(lid).add(fa.getFrequencies().get(lid).size());
			}
			System.out.println("done. (processing)");
		}
		
		System.out.println("writing output...");
		FileWriter fw = new FileWriter(outDir+"/linkFrequencies.txt");
		BufferedWriter out = new BufferedWriter(fw);
		out.write("linkId");
		for (int i=0; i<freqs.values().iterator().next().size(); i++) { out.write("\tstep"+i); }
		out.write("\n");
		for (Id lid : freqs.keySet()) {
			out.write(lid.toString());
			for (Integer i : freqs.get(lid)) { out.write("\t"+i); }
			out.write("\n");
		}
		out.close();
		fw.close();
		System.out.println("done. (writing)");
	}
}
