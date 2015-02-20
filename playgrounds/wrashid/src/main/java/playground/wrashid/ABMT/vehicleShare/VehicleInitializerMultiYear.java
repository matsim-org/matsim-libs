package playground.wrashid.ABMT.vehicleShare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

/**
 * 
 * @author wrashid
 *
 */
public class VehicleInitializerMultiYear implements IterationStartsListener, StartupListener {

	protected static final Logger log = Logger.getLogger(VehicleInitializerMultiYear.class);

	public static IntegerValueHashMap<Id> vehicleExpiryYear = new IntegerValueHashMap<Id>();

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		
		
		
		
		if (isLastIteration(event)){
			//falls person momentan 
		}
	}

	private boolean isLastIteration(IterationStartsEvent event) {
		return event.getIteration()==Integer.parseInt(event.getControler().getConfig().getParam("controler", "lastIteration"));
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		if (GlobalTESFParameters.currentYear == 0) {
			Random random = new Random(GlobalTESFParameters.tesfSeed);
			for (Person person : event.getControler().getScenario().getPopulation().getPersons().values()) {
				if (VehicleInitializer.hasCarLeg(person.getSelectedPlan())) {
					vehicleExpiryYear.set(person.getId(), random.nextInt(8));
				}
			}
		}

	}
}
