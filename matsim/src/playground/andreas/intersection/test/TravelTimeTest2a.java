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
	

	final int timeToWaitBeforeMeasure = 498; // Make sure measurement starts with second 0 in signalsystemplan 
	
	public void testTrafficLightIntersection2arms_w_TrafficLight_0_60(){
  		
  		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/data/plans_2a_5000.xml.gz";
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
				
				this.beginningOfLink2 = null;

				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(signalSystems)));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));

				boolean moveOn = true;

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
			new QSim(events, data.getPopulation(), data.getNetwork(), tempFile, groupDefinitions, false).run();
			results.put(Integer.valueOf(i), this.beginningOfLink2);
			
			File delFile = new File(tempFile);
			delFile.delete();
		}
		
		int j = 2;
		
		for (MeasurePoint resMeasurePoint : results.values()) {
			System.out.println(this.beginningOfLink2.timeToStartMeasurement + ", " + j + ", " + resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_ + ", " + resMeasurePoint.numberOfVehPassed_ + ", " + resMeasurePoint.firstVehPassTime_s + ", " + resMeasurePoint.lastVehPassTime_s);
			j++;
			assertEquals(5000.0, resMeasurePoint.numberOfVehPassed_, EPSILON);
		}
		

		
	}	
	
	public void testTrafficLightIntersection2arms_w_TrafficLight(){
  		  		
		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/data/plans_2a_5000.xml.gz";
		String netFileName = "src/playground/andreas/intersection/test/data/net_2a.xml.gz";
		
		String signalSystems = "./src/playground/andreas/intersection/test/data/signalSystemConfig_2a.xml";
		String groupDefinitions = "./src/playground/andreas/intersection/test/data/signalGroupDefinition_2a.xml";

		conf.plans().setInputFile(popFileName);
		conf.network().setInputFile(netFileName);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		new QSim(events, data.getPopulation(), data.getNetwork(), signalSystems, groupDefinitions, false).run();
		System.out.println("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + this.beginningOfLink2.numberOfVehPassed_ + ", " + this.beginningOfLink2.firstVehPassTime_s + ", " + this.beginningOfLink2.lastVehPassTime_s);
		
		MeasurePoint qSim = this.beginningOfLink2;		
		this.beginningOfLink2 = null;
		
		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
		System.out.println("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + this.beginningOfLink2.numberOfVehPassed_ + ", " + this.beginningOfLink2.firstVehPassTime_s + ", " + this.beginningOfLink2.lastVehPassTime_s);
		MeasurePoint queueSimulation = this.beginningOfLink2;
				
		// circle time is 60s, green 60s
		assertEquals(5000.0, qSim.numberOfVehPassed_, EPSILON);
		
		// QSim is 4s slower than QueueSim
		assertEquals(qSim.firstVehPassTime_s - 3, queueSimulation.firstVehPassTime_s, EPSILON);
		assertEquals(qSim.numberOfVehPassed_, queueSimulation.numberOfVehPassed_, EPSILON);
		assertEquals(qSim.numberOfVehPassedDuringTimeToMeasure_, queueSimulation.numberOfVehPassedDuringTimeToMeasure_, EPSILON);
		
  	}  	

	public void handleEvent(EventLinkEnter event) {
		
		if (event.linkId.equalsIgnoreCase("2")) {
			
			if (this.beginningOfLink2 == null){				
				this.beginningOfLink2 = new MeasurePoint(event.time + this.timeToWaitBeforeMeasure);
			}
			
			this.beginningOfLink2.numberOfVehPassed_++;
			
			if( this.beginningOfLink2.timeToStartMeasurement <= event.time){				

				if (this.beginningOfLink2.firstVehPassTime_s == -1){
					this.beginningOfLink2.firstVehPassTime_s = event.time;
				}
				
				if (event.time < this.beginningOfLink2.timeToStartMeasurement + this.beginningOfLink2.timeToMeasure_s){
					this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_++;
					this.beginningOfLink2.lastVehPassTime_s = event.time;
				}		
			}
		}		
	}	
	
	public void handleEvent(EventLinkLeave event) {
	}

	public void reset(int iteration) {
	}
	
	private class MeasurePoint{
		
		private final int timeToMeasure_s = 60 * 60;
		double timeToStartMeasurement;
		double firstVehPassTime_s = -1;
		double lastVehPassTime_s;
	  	int numberOfVehPassed_ = 0;
	  	int numberOfVehPassedDuringTimeToMeasure_ = 0;
		
		public MeasurePoint(double time) {
			this.timeToStartMeasurement = time;
		}		
	}

}