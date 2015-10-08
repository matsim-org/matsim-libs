package playground.benjamin.scenarios.santiago.run;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;

public class PTFlatFareHandler implements PersonArrivalEventHandler, ActivityStartEventHandler {

	private Set<Id<Person>> ptUsers = new HashSet<>();
	private final Controler controler;
	
	private final double amount = -600;
	
	public PTFlatFareHandler(final Controler controler){
		
		this.controler = controler;
		
	}
	
	@Override
	public void reset(int iteration) {
		
		this.ptUsers.clear();

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if(this.isPTMode(event.getLegMode())){
			
			this.ptUsers.add(event.getPersonId());
			
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		
		if(this.ptUsers.contains(event.getPersonId())){
				
			if(!event.getActType().equals("pt interaction")){
				
				this.controler.getEvents().processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), this.amount));
				this.ptUsers.remove(event.getPersonId());
				
			}
			
		}
	
	}
	
	private boolean isPTMode(String legMode){
		
		return legMode.equals("feeder bus") || legMode.equals("main bus") || legMode.equals("subway") || legMode.equals("institutional bus")
				|| legMode.equals("rural bus") || legMode.equals("urban bus") || legMode.equals("train");
		
	}

}
