package playground.wrashid.test.root.deqsim;

import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.testcases.MatsimTestCase;

import org.matsim.gbl.Gbl;
import playground.wrashid.PDES2.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.deqsim.PDESController2;
import playground.wrashid.deqsim.PDESStarter2;

public class PDESStarter2Test extends MatsimTestCase {

	static class TestHandler implements ActEndEventHandler,
			AgentDepartureEventHandler, AgentWait2LinkEventHandler,
			LinkLeaveEventHandler, LinkEnterEventHandler,
			AgentArrivalEventHandler, ActStartEventHandler,
			AgentStuckEventHandler {

		public int eventCounter = 0;
		
		public int linkEnterEventCounter = 0;
		public int linkLeaveEventCounter = 0;
		public int departureEventCounter = 0;
		public int arrivalEventCounter = 0;
		
		public boolean printEvent=true;

		public void reset(final int iteration) {
			this.eventCounter = 0;
		}

		public void handleEvent(final ActEndEvent event) {
			this.eventCounter++;
		}

		public void handleEvent(final AgentDepartureEvent event) {
			this.departureEventCounter++;
			if (printEvent){
				System.out.println(event.toString());
			}
		}

		public void handleEvent(final AgentWait2LinkEvent event) {
			this.eventCounter++;
		}

		public void handleEvent(final LinkLeaveEvent event) {
			this.linkLeaveEventCounter++;
			if (printEvent){
				System.out.println(event.toString());
			}
		}

		public void handleEvent(final LinkEnterEvent event) {
			this.linkEnterEventCounter++;
			if (printEvent){
				System.out.println(event.toString());
			}
		}

		public void handleEvent(final AgentArrivalEvent event) {
			this.arrivalEventCounter++;
			if (printEvent){
				System.out.println(event.toString());
			}
		}

		public void handleEvent(final ActStartEvent event) {
			this.eventCounter++;
		}

		public void handleEvent(final AgentStuckEvent event) {
			this.eventCounter++;
		}

	};

	public void testScenarios() {
		t_equilEventCounts();
	}
  //TODO: continue test cases here...
	private void t_equilEventCounts() {
		// nochmals kontrollieren, ob diese zahlen stimmen
		// 201 departure
		// 201 actend
		// 201 arrival
		// 200 wait2link
		// 201 actstart
		// 700 left link
		// 700 entered link
		// => thats all
		String[] args = new String[1];
		args[0] = "test/scenarios/equil/config.xml";
		TestHandler testHandler = new TestHandler();
		SimulationParameters.testEventHandler = testHandler;
		
		PDESStarter2.main(args);
		//assertEquals(700,testHandler.linkEnterEventCounter); // nicht sicher ob stimmt
		//assertEquals(700,testHandler.linkLeaveEventCounter); // nicht sicher ob stimmt
		assertEquals(201,testHandler.departureEventCounter);
		assertEquals(201,testHandler.arrivalEventCounter); 
	}
	
	// noch testen, ob departure und arrival in richtiger reihenfolge passieren
	// vielleicht noch detailliertere kontrolle für einen bestimmten agent
	// eventuell eine single agent simulation machen: da kann man sagen, es soll einfach
	// mit dem plan exact vergleichen, weil ja dieser alle strassen einfach abfahren kann
	

}
