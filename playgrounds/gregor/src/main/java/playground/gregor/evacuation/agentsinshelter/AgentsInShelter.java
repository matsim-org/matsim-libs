package playground.gregor.evacuation.agentsinshelter;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;

public class AgentsInShelter implements AgentArrivalEventHandler{
	
	private int ais = 0;
	private String events;

	public AgentsInShelter(String events) {
		this.events = events;
	}

	public int run() {
		
		EventsManagerImpl e = new EventsManagerImpl();
		e.addHandler(this);
		new EventsReaderTXTv1(e).readFile(this.events);
		
		return this.ais;
	}
	
	
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (!event.getLinkId().toString().equals("el1")) {
			this.ais++;
		}
		
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	
	public static void main(String [] args) {
		
		String iters = args[0];
		int last = Integer.parseInt(args[1]);
		String out = args[2];
		for (int i =0 ; i < last; i++) {
			String events = iters + "/it." + i + "/" + i + ".events.txt.gz";
			AgentsInShelter ais = new AgentsInShelter(events);
			System.out.println("AGENT in Sehlter: "+ i + " " + ais.run());
		}
		
	}


}
