package playground.andreas.intersection.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.io.IOUtils;

import playground.andreas.intersection.sim.QSim;

/**
 * @author aneumann
 *
 */
public class TravelTimeTestFourWay extends MatsimTestCase implements	LinkLeaveEventHandler, LinkEnterEventHandler, ActEndEventHandler, ActStartEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler, AgentWait2LinkEventHandler {

	BufferedWriter writer = null;
  
	public void testTrafficLightIntersection4arms() {
		
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
		Config conf = loadConfig("src/playground/andreas/intersection/test/data/fourways/config.xml");
		
		String newLSADef = "src/playground/andreas/intersection/test/data/fourways/lsa.xml";
		String newLSADefCfg = "src/playground/andreas/intersection/test/data/fourways/lsa_config.xml";
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		
		try {		
			this.writer = IOUtils.getBufferedWriter("temp.txt.gz", true);
			new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, newLSADefCfg).run();

			this.writer.flush();
			this.writer.close();
			
			assertEquals(CRCChecksum.getCRCFromFile("temp.txt.gz"),	CRCChecksum.getCRCFromFile("src/playground/andreas/intersection/test/data/fourways/reference.txt.gz"));
			
			new File("temp.txt.gz").delete();

		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}
	
	public void testTrafficLightIntersection4armsWithUTurn() {
		
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
		Config conf = loadConfig("src/playground/andreas/intersection/test/data/fourways/config.xml");
		conf.plans().setInputFile("src/playground/andreas/intersection/test/data/fourways/plans_uturn.xml.gz");
		
		String newLSADef = "src/playground/andreas/intersection/test/data/fourways/lsa.xml";
		String newLSADefCfg = "src/playground/andreas/intersection/test/data/fourways/lsa_config.xml";
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		
		try {		
			this.writer = IOUtils.getBufferedWriter("temp.txt.gz", true);
			new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, newLSADefCfg).run();

			this.writer.flush();
			this.writer.close();
			
			assertEquals(CRCChecksum.getCRCFromFile("temp.txt.gz"),	CRCChecksum.getCRCFromFile("src/playground/andreas/intersection/test/data/fourways/reference_uturn.txt.gz"));
			
			new File("temp.txt.gz").delete();

		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}

	public void handleEvent(LinkEnterEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public void handleEvent(LinkLeaveEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void reset(@SuppressWarnings("unused")int iteration) {
		// Not used in that TestCase
	}
	
	public void handleEvent(ActEndEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(ActStartEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentDepartureEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
