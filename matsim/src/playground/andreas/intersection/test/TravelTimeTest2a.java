package playground.andreas.intersection.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.TreeMap;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;

import playground.andreas.intersection.sim.QSim;

/**
 * @author aneumann
 *
 */
public class TravelTimeTest2a extends MatsimTestCase implements	EventHandlerLinkLeaveI, EventHandlerLinkEnterI {

	MeasurePoint beginningOfLink2 = null;
	
	public void testTrafficLightIntersection2arms_w_TrafficLight_0_60(){
  		
  		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/data/plans_2a_2500.xml.gz";
		String netFileName = "src/playground/andreas/intersection/test/data/net_2a.xml.gz";
				
		String groupDefinitions = "./src/playground/andreas/intersection/test/data/signalGroupDefinition_2a.xml";
		String signalSystems = "./src/playground/andreas/intersection/test/data/signalSystemConfig_2a.xml";

		conf.plans().setInputFile(popFileName);
		conf.network().setInputFile(netFileName);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		String tempFile = "./src/playground/andreas/intersection/test/__tempFile__.xml";
		
		TreeMap<Integer, MeasurePoint> results = new TreeMap<Integer, MeasurePoint>();		
		
		for (int i = 2; i <= 60; i++) {

			try {
				
				beginningOfLink2 = null;

				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(signalSystems)));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));

				Boolean moveOn = true;

				while(moveOn){

					String line = reader.readLine();

					if (line != null){

						if(line.contains("<dropping sec=")){
							writer.write("<dropping sec=\"" + i + "\" />" + "\n");		
						} else {
							writer.write(line + "\n");
						}				

					} else {
						moveOn = false;
					}					
				}

				reader.close();
				writer.flush();
				writer.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

//			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			new QSim(events, data.getPopulation(), data.getNetwork(), tempFile, groupDefinitions).run();
			results.put(Integer.valueOf(i), beginningOfLink2);
			
			File delFile = new File(tempFile);
			delFile.delete();
		}
		
		int j = 2;
		
		for (MeasurePoint resMeasurePoint : results.values()) {
			System.out.println(j + ", " + resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_ + ", " + resMeasurePoint.numberOfVehPassed_ + ", " + resMeasurePoint.firstVehPassTime_s + ", " + resMeasurePoint.lastVehPassTime_s);
			j++;
			assertEquals(2500.0, resMeasurePoint.numberOfVehPassed_, EPSILON);
		}
		

		
	}	
	
	public void testTrafficLightIntersection2arms_w_TrafficLight(){
  		  		
		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/data/plans_2a_2500.xml.gz";
		String netFileName = "src/playground/andreas/intersection/test/data/net_2a.xml.gz";
		
		String signalSystems = "./src/playground/andreas/intersection/test/data/signalSystemConfig_2a.xml";
		String groupDefinitions = "./src/playground/andreas/intersection/test/data/signalGroupDefinition_2a.xml";

		conf.plans().setInputFile(popFileName);
		conf.network().setInputFile(netFileName);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		new QSim(events, data.getPopulation(), data.getNetwork(), signalSystems, groupDefinitions).run();
		System.out.println("tF = 60s, " + beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + beginningOfLink2.numberOfVehPassed_ + ", " + beginningOfLink2.firstVehPassTime_s + ", " + beginningOfLink2.lastVehPassTime_s);
		MeasurePoint QSim = beginningOfLink2;
		
		beginningOfLink2 = null;
		
		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
		System.out.println("tF = 60s, " + beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + beginningOfLink2.numberOfVehPassed_ + ", " + beginningOfLink2.firstVehPassTime_s + ", " + beginningOfLink2.lastVehPassTime_s);
		MeasurePoint QueueSimulation = beginningOfLink2;
				
		// circle time is 60s, green 60s
		assertEquals(2500.0, QSim.numberOfVehPassed_, EPSILON);
		
		assertEquals(QSim.firstVehPassTime_s - 4, QueueSimulation.firstVehPassTime_s, EPSILON);
		assertEquals(QSim.numberOfVehPassed_, QueueSimulation.numberOfVehPassed_, EPSILON);
		assertEquals(QSim.numberOfVehPassedDuringTimeToMeasure_, QueueSimulation.numberOfVehPassedDuringTimeToMeasure_, EPSILON);
		
  	}  	

	public void handleEvent(EventLinkEnter event) {
		
		if (event.linkId.equalsIgnoreCase("2")) {

			if (beginningOfLink2 == null){				
				beginningOfLink2 = new MeasurePoint(event.time);
			}
			
			beginningOfLink2.numberOfVehPassed_++;
			beginningOfLink2.lastVehPassTime_s = event.time;
			
			if (beginningOfLink2.firstVehPassTime_s + beginningOfLink2.timeToMeasure_s > event.time){
				beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_++;
			}			
		}		
	}	
	
	public void handleEvent(EventLinkLeave event) {
	}

	public void reset(int iteration) {
	}
	
	private class MeasurePoint{
		
		private final int timeToMeasure_s = 60 * 60;
		private final double firstVehPassTime_s;
	  	private double lastVehPassTime_s;
	  	private int numberOfVehPassed_ = 0;
	  	private int numberOfVehPassedDuringTimeToMeasure_ ;
		
		public MeasurePoint(double time) {
			this.firstVehPassTime_s = time;
		}		
	}

}