package playground.andreas.bln.net.simplex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class ScaleFactorToChangeEvent {
	
	private NetworkImpl network;
	private BufferedWriter writer;
	
	private TreeMap<String, Double> timeScaleFactorMap = new TreeMap<String, Double>();
	
	public ScaleFactorToChangeEvent(String networkFile, String outFile){
		this.network = readNetwork(networkFile);
		try {
			this.writer = new BufferedWriter(new FileWriter(new File(outFile)));			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addTimeScaleFactorSet(String time, double scaleFactor){
		this.timeScaleFactorMap.put(time, Double.valueOf(scaleFactor));
	}
	
	public void writeEvents() throws IOException{
		
		this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); this.writer.newLine();
		this.writer.write("<networkChangeEvents xmlns=\"http://www.matsim.org/files/dtd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/networkChangeEvents.xsd\">"); this.writer.newLine();
		this.writer.newLine();
		
		double lastScaleFactor = 1.0;
		
		for (Entry<String, Double> entry : this.timeScaleFactorMap.entrySet()) {
			this.writer.write("<networkChangeEvent startTime=\"" + entry.getKey() + "\">"); this.writer.newLine();
			for (Link link : this.network.getLinks().values()) {
				if(link.getId().toString().contains("miv_")){
					// ignore miv link
				} else {
					this.writer.write("<link refId=\"" + link.getId().toString() + "\"/>"); this.writer.newLine();
				}
			}
			this.writer.write("<freespeed type=\"scaleFactor\" value=\"" + (entry.getValue().doubleValue() / lastScaleFactor) + "\"/>"); this.writer.newLine();
			this.writer.write("</networkChangeEvent>"); this.writer.newLine();
			
			this.writer.newLine();
			
			lastScaleFactor = entry.getValue().doubleValue();
		}		
		
		this.writer.write("</networkChangeEvents>");
		this.writer.flush();
		this.writer.close();		
	}
	
	private NetworkImpl readNetwork(String networkFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		return scenario.getNetwork();
	}

}
