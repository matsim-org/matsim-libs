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
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.Events;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;

import playground.andreas.intersection.sim.QSim;

/**
 * @author aneumann
 *
 */
public class TravelTimeTestOneWay extends MatsimTestCase implements	LinkLeaveEventHandler, LinkEnterEventHandler {

	MeasurePoint beginningOfLink2 = null;	

	final static int timeToWaitBeforeMeasure = 498; // Make sure measurement starts with second 0 in signalsystemplan 
	
	public void testTrafficLightIntersection2arms_w_TrafficLight_0_60(){
  		
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
  		Config conf = loadConfig("src/playground/andreas/intersection/test/data/oneways/config.xml");
				
		String newLSADef = "./src/playground/andreas/intersection/test/data/oneways/lsa.xml";
		String newLSADefCfg = "./src/playground/andreas/intersection/test/data/oneways/lsa_config.xml";
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		String tempFile = "./src/playground/andreas/intersection/test/__tempFile__.xml";
		
		TreeMap<Integer, MeasurePoint> results = new TreeMap<Integer, MeasurePoint>();		
		
		int umlaufzeit = 60;
		
		for (int i = 1; i <= umlaufzeit; i++) {

			try {
				
				this.beginningOfLink2 = null;
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(newLSADefCfg)));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
				boolean moveOn = true;

				while(moveOn){

					String line = reader.readLine();

					if (line != null){

						if(line.contains("<dropping sec=")){
							writer.write("<dropping sec=\"" + i + "\" />" + "\n");		
						} else if (line.contains("<circulationTime seconds=")){
							writer.write("<circulationTime seconds=\"" + umlaufzeit + "\" />");
						}
						
						else {
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
			new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, tempFile).run();
			results.put(Integer.valueOf(i), this.beginningOfLink2);
			
			File delFile = new File(tempFile);
			delFile.delete();
		}
		
		int j = 1;
		
		for (MeasurePoint resMeasurePoint : results.values()) {
			System.out.println(j + ", " + resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_ + ", " + resMeasurePoint.numberOfVehPassed_ + ", " + this.beginningOfLink2.timeToStartMeasurement + ", " + resMeasurePoint.firstVehPassTime_s + ", " + resMeasurePoint.lastVehPassTime_s + ", " + (resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_ - j * 2000 / umlaufzeit));
			assertEquals((j * 2000 / umlaufzeit), resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_, 1);
			j++;
			assertEquals(5000.0, resMeasurePoint.numberOfVehPassed_, EPSILON);
		}
		

		
	}	
	
	public void testTrafficLightIntersection2arms_w_TrafficLight(){
  		  	
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
		Config conf = loadConfig("src/playground/andreas/intersection/test/data/oneways/config.xml");

		String newLSADef = "./src/playground/andreas/intersection/test/data/oneways/lsa.xml";
		String newLSADefCfg = "./src/playground/andreas/intersection/test/data/oneways/lsa_config.xml";
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, newLSADefCfg).run();
		System.out.println("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + this.beginningOfLink2.numberOfVehPassed_ + ", " + this.beginningOfLink2.firstVehPassTime_s + ", " + this.beginningOfLink2.lastVehPassTime_s);
		
		MeasurePoint qSim = this.beginningOfLink2;		
		this.beginningOfLink2 = null;
		
		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
		System.out.println("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + this.beginningOfLink2.numberOfVehPassed_ + ", " + this.beginningOfLink2.firstVehPassTime_s + ", " + this.beginningOfLink2.lastVehPassTime_s);
		MeasurePoint queueSimulation = this.beginningOfLink2;
				
		// circle time is 60s, green 60s
		assertEquals(5000.0, qSim.numberOfVehPassed_, EPSILON);

		assertEquals(qSim.firstVehPassTime_s, queueSimulation.firstVehPassTime_s, EPSILON);
		assertEquals(qSim.numberOfVehPassed_, queueSimulation.numberOfVehPassed_, EPSILON);
		assertEquals(qSim.numberOfVehPassedDuringTimeToMeasure_, queueSimulation.numberOfVehPassedDuringTimeToMeasure_, EPSILON);
		
  	}  	

	public void handleEvent(LinkEnterEvent event) {
		
		if (event.linkId.equalsIgnoreCase("2")) {
			
			if (this.beginningOfLink2 == null){				
				this.beginningOfLink2 = new MeasurePoint(event.time + TravelTimeTestOneWay.timeToWaitBeforeMeasure);
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
	
	public void handleEvent(LinkLeaveEvent event) {
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