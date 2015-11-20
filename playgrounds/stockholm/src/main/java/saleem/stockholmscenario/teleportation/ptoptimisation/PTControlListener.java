package saleem.stockholmscenario.teleportation.ptoptimisation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
public class PTControlListener implements StartupListener, IterationStartsListener{
	Scenario scenario;
	VehicleRemover rveh;
	TransitScheduleAdaptation adapter;
	public PTControlListener(Scenario scenario){
			this.scenario = scenario;
	}
	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
			rveh = new VehicleRemover(scenario);
			adapter = new TransitScheduleAdaptation(scenario);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if(event.getIteration()==1){
			rveh.removeVehicles(0.5);
			adapter.removeDeletedVehicleDepartures();
			
		}
	}

}

