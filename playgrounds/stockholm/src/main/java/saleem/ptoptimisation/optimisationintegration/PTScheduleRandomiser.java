package saleem.ptoptimisation.optimisationintegration;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import saleem.ptoptimisation.decisionvariables.TransitScheduleAdapter;
import saleem.ptoptimisation.utils.ScenarioHelper;
import floetteroed.opdyts.DecisionVariableRandomizer;
/**
 * A class to create variations of current transit schedule.
 * 
 * @author Mohammad Saleem
 *
 */
public class PTScheduleRandomiser implements DecisionVariableRandomizer<PTSchedule> {
	private final PTSchedule ptschedule;
	private final TransitScheduleAdapter adapter;
	private final Scenario scenario;
	String str = "";
	public PTScheduleRandomiser(final Scenario scenario, final PTSchedule decisionVariable) {
		this.ptschedule=decisionVariable;
		this.adapter = new TransitScheduleAdapter();
		this.scenario=scenario;
	}

	@Override
	public Collection<PTSchedule> newRandomVariations(PTSchedule decisionVariable) {
		TransitSchedule schedule = this.ptschedule.getSchedule();
		Vehicles vehicles = this.ptschedule.getVehicles();
		ScenarioHelper helper = new ScenarioHelper();
		
		final List<PTSchedule> result = new ArrayList<>();
		//Step size, (0.2*0.25=0.05) 5% change in transit schedule
		double factorline = 0.25, factorroute=0.2;
		//Create variations of transit schedule by adding and deleting lines to transit schedule
		result.add(adapter.updateScheduleDeleteLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), factorline*factorroute));
		result.add(adapter.updateScheduleAddLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), factorline*factorroute));
		//Create variations of transit schedule by adding and deleting routes to/from existing lines
		result.add(adapter.updateScheduleDeleteRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
		result.add(adapter.updateScheduleAddRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
		//Current best transit schedule is also included
		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
		//Create variations of transit schedule by making big vehicles serve peak hours and small vehicles serve off-peak hours
		result.add(adapter.updateScheduleChangeCapacity(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
		//Create variations of transit schedule by adding to or removing departures from existing routes
		result.add(adapter.updateScheduleAddDepartures(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));
		result.add(adapter.updateScheduleRemoveDepartures(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));

		return result;
	}
}
