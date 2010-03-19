package playground.mzilske.postgres;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ArgumentParser;

public class ExportNetworkToSQL {

	public void run(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			printUsage();
			System.exit(0);
		} else {
			String inputFile = arg;
			if (!argIter.hasNext()) {
				System.out.println("Too few arguments.");
				printUsage();
				System.exit(1);
			}
			String outputFile = argIter.next();
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				printUsage();
				System.exit(1);
			}
			run(inputFile, outputFile);
		}
	}
	
	public void run(final String inputNetworkFile, final String outputSQLFile) {
		final Scenario scenario = new ScenarioImpl();
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);

		

		try {
			int id = 0;
			PrintWriter out = new PrintWriter(IOUtils.getBufferedWriter(outputSQLFile, false));
			for (Link link : network.getLinks().values()) {
				out.println(
						id++ + "," 
						+ link.getId().toString() + ","
						+ link.getFromNode().getId() + "," 
						+ link.getToNode().getId() + "," 
						+ link.getFreespeed(0.0) * link.getLength() + ","
						+ link.getFromNode().getCoord().getX() + "," 
						+ link.getFromNode().getCoord().getY() + "," 
						+ link.getToNode().getCoord().getX() + "," 
						+ link.getToNode().getCoord().getY());
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void printUsage() {
		System.out.println("wurst");
	}

	public static void main(String[] args) {
		new ExportNetworkToSQL().run(args);
	}
	
}
