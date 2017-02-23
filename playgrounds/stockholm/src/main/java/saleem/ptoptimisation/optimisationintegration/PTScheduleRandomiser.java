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

public class PTScheduleRandomiser implements DecisionVariableRandomizer<PTSchedule> {
	private final PTSchedule ptschedule;
	private final TransitScheduleAdapter adapter;
	private final Scenario scenario;
	String str = "";
	public PTScheduleRandomiser(final Scenario scenario, final PTSchedule decisionVariable) {
		this.ptschedule=decisionVariable;
		this.adapter = new TransitScheduleAdapter();
		this.scenario=scenario;
		// TODO Auto-generated method stub
	}

	@Override
	public Collection<PTSchedule> newRandomVariations(
			PTSchedule decisionVariable) {
		TransitSchedule schedule = this.ptschedule.schedule;
		Vehicles vehicles = this.ptschedule.vehicles;
		ScenarioHelper helper = new ScenarioHelper();
		final List<PTSchedule> result = new ArrayList<>(2);
		
		double factorline = 0.25, factorroute=0.2;
		
			result.add(adapter.updateScheduleDeleteLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), factorline*factorroute));
			result.add(adapter.updateScheduleAddLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), factorline*factorroute));
			result.add(adapter.updateScheduleDeleteRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
			result.add(adapter.updateScheduleAddRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
			result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
			result.add(adapter.updateScheduleChangeCapacity(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
			result.add(adapter.updateScheduleAdd(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));
			result.add(adapter.updateScheduleRemove(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));

		return result;
	}
}
