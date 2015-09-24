package playground.artemc.crowding;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

import playground.artemc.crowding.newScoringFunctions.PersonScore;
import playground.artemc.crowding.newScoringFunctions.ScoreListener;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.newScoringFunctions.VehicleScore;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.analysis.postgresql.PostgresType;
import playground.artemc.analysis.postgresql.PostgresqlCSVWriter;
import playground.artemc.analysis.postgresql.PostgresqlColumnDefinition;
import playground.artemc.analysis.postgresql.TableWriter;

/**
 * This class implement the creation of two SQL tables, one describing the bus states 
 * and the second one saving the score of each agent
 * 
 *  @author achakirov, grerat
 * 
 */

public class DatabaseSQLwriter {

	private String schemaName;
	private String postgresProperties;
	
	private Set<Id> ptVehicles;

	public DatabaseSQLwriter(String schemaName, String postgresProperties){
		this.schemaName = schemaName;
		this.postgresProperties = postgresProperties;
		this.ptVehicles = new HashSet<Id>();
	}

	public void writeSQLCrowdednessObserver(String tableName, String simulationType, HashMap<Vehicle, VehicleStateAdministrator> vehicleStates) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException,SQLException
	{
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_MM");
		String formattedDate = df.format(new Date());
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("VehicleId", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("StationId", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("StationIdSpec", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("Time", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TravelTime", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("LoadFactorAtArrival", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("LoadFactorAtDeparture", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("Boarding", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("Alighting", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("SimulationType", PostgresType.TEXT));


		TableWriter busWriter = null;

		String tabname = schemaName + "." + tableName;
		File file = new File(postgresProperties);
		DataBaseAdmin dba = new DataBaseAdmin(file);
		busWriter = new PostgresqlCSVWriter("CROWDEDNESSWRITER", tabname,
				dba, 100, columns);
		busWriter.addComment(String.format("created on %s for eventsfile ",formattedDate));

		for (Vehicle vehicle : vehicleStates.keySet()) {
			for (Id facilityId : vehicleStates.get(vehicle).getFacilityStates().keySet()) {
				Object[] args = new Object[columns.size()];
				args[0] = vehicle.getId().toString();
				BusFacilityInteractionEvent state = vehicleStates.get(vehicle).getFacilityStates().get(facilityId);
				args[1] = new String(state.getStationId().toString());
				args[2] = new Double(state.getStationId().toString() + ".1");
				args[3] = new Double(state.getBusArrivalTime());
				args[4] = new Double(state.getBusArrivalTime()-vehicleStates.get(vehicle).getRouteDepartureTime());
				args[5] = new Double(state.getArrivalLoadFactor());
				args[6] = new Double(state.getDepartureLoadFactor());
				args[7] = new Double(state.getPersonsBoarding());
				args[8] = new Double(state.getPersonsAlighting());
				args[9] = new String(simulationType);
				busWriter.addLine(args);
				
				
				Object[] args2 = new Object[columns.size()];
				args2[0] = vehicle.getId().toString();
				BusFacilityInteractionEvent state2 = vehicleStates.get(vehicle)
						.getFacilityStates().get(facilityId);
				args2[1] = new String(state2.getStationId().toString());
				args2[2] = new String(state2.getStationId().toString() + ".2");
				args2[3] = new Double(state2.getBusDepartureTime());
				args2[4] = new Double(state2.getBusDepartureTime()-vehicleStates.get(vehicle).getRouteDepartureTime());
				args2[5] = new Double(state2.getArrivalLoadFactor());
				args2[6] = new Double(state2.getDepartureLoadFactor());
				args2[7] = new Double(state2.getPersonsBoarding());
				args2[8] = new Double(state2.getPersonsAlighting());
				args2[9] = new String(simulationType);
				busWriter.addLine(args2);

			}
		} 
		busWriter.finish();
	}
	
	public void writeSQLPersonScore(String tableName, String simulationType, ScoreTracker scoreTracker, Scenario scenario) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException,SQLException
	{

		DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_MM");
		String formattedDate = df.format(new Date());
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("PersonId", PostgresType.TEXT));
		//columns.add(new PostgresqlColumnDefinition("VehicleId", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("ScoringTime", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TripDuration", PostgresType.FLOAT8));
//		columns.add(new PostgresqlColumnDefinition("FacilityOfAlighting", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("Score", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("CrowdednessPenalty", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("CrowdednessExternalityCharge", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("InVehTimeDelayExternalityCharge", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("CapacityConstraintsExternalityCharge", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("MoneyPaid", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("SimulationType", PostgresType.TEXT));


		TableWriter scoreWriter = null;

		String tabname = schemaName + "." + tableName;
		File file = new File(postgresProperties);
		DataBaseAdmin dba = new DataBaseAdmin(file);
		scoreWriter = new PostgresqlCSVWriter("PERSONSCOREWRITER", tabname,
				dba, 100, columns);
		scoreWriter.addComment(String.format("created on %s for eventsfile ",formattedDate));

		for (Id agentId : scenario.getPopulation().getPersons().keySet()) {

			double score = scenario.getPopulation().getPersons().get(agentId).getSelectedPlan().getScore();
			
			if(scoreTracker.getPersonScores().containsKey(agentId)){
				scoreTracker.getPersonScores().get(agentId).setTotalUtility(score);
				
				Object[] args = new Object[columns.size()];
				args[0] = agentId.toString();
				PersonScore person = scoreTracker.getPersonScores().get(agentId);
				args[1] = new Double(person.getScoringTime());
				args[2] = new Double(person.getTripDuration());
//				args[3] = new String(person.getFacilityOfAlighting().toString());
				args[3] = new Double(score);
				args[4] = new Double(person.getCrowdingUtility());
				args[5] = new Double(person.getCrowdednessExternalityCharge());
				args[6] = new Double(person.getInVehicleTimeDelayExternalityCharge());
				args[7] = new Double(person.getCapacityConstraintsExternalityCharge());
				args[8] = new Double(person.getMoneyPaid());
				args[9] = new String(simulationType);
				scoreWriter.addLine(args);
			}
		} 
		scoreWriter.finish();
	}

	
	public void writeSQLVehicleScore(String tableName, String simulationType, ScoreTracker scoreTracker, Scenario scenario) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException,SQLException
	{

		DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_MM");
		String formattedDate = df.format(new Date());
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("VehicleId", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("StationId", PostgresType.TEXT));
		columns.add(new PostgresqlColumnDefinition("Time", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("CrowdednessPenalty", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("CrowdednessExternalityCharge", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("InVehTimeDelayExternalityCharge", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("CapacityConstraintsExternalityCharge", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("MoneyPaid", PostgresType.FLOAT8));
//		columns.add(new PostgresqlColumnDefinition("DwellTime", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("SimulationType", PostgresType.TEXT));
		
		TableWriter scoreWriter = null;

		String tabname = schemaName + "." + tableName;
		File file = new File(postgresProperties);
		DataBaseAdmin dba = new DataBaseAdmin(file);
		scoreWriter = new PostgresqlCSVWriter("VEHICLESCOREWRITER", tabname,
				dba, 100, columns);
		scoreWriter.addComment(String.format("created on %s for eventsfile ",formattedDate));

		ptVehicles.clear();
		for (TransitLine tsl : scenario.getTransitSchedule().getTransitLines().values())
		{
			for (TransitRoute tr : tsl.getRoutes().values())
			{
				for (Departure dep : tr.getDepartures().values())
				{
					ptVehicles.add(dep.getVehicleId());
				}
			}
		}
		
		for (Id vehicleId : ptVehicles) {
			VehicleScore vehicle = scoreTracker.getVehicleExternalities().get(vehicleId);
			if(scoreTracker.getVehicleExternalities().containsKey(vehicleId)){
			for(Id facilityId : vehicle.getFacilityId().get(vehicleId)){
				Object[] args = new Object[columns.size()];
				args[0] = vehicleId.toString();
				args[1] = facilityId.toString();
				args[2] = new Double(vehicle.getFacilityTime().get(facilityId));
				
				if(vehicle.getVehicleCrowdingCost().containsKey(facilityId)){
					args[3] = new Double(vehicle.getVehicleCrowdingCost().get(facilityId));
				}
				else {
					args[3] = 0.0;
				}

				if(vehicle.getVehicleCrowdednessExternalityCharge().containsKey(facilityId)){
					args[4] = new Double(vehicle.getVehicleCrowdednessExternalityCharge().get(facilityId));
				}
				else {
					args[4] = 0.0;
				}

				if(vehicle.getVehicleInVehicleTimeDelayExternalityCharge().containsKey(facilityId)){
					args[5] = new Double(vehicle.getVehicleInVehicleTimeDelayExternalityCharge().get(facilityId));
				}
				else {
					args[5] = 0.0;
				}

				if(vehicle.getVehicleCapacityConstraintsExternalityCharge().containsKey(facilityId)){
					args[6] = new Double(vehicle.getVehicleCapacityConstraintsExternalityCharge().get(facilityId));
				}
				else {
					args[6] = 0.0;
				}
				args[7] = new Double(vehicle.getVehicleMoneyPaid().get(facilityId));
				
//				if(vehicle.getDwellTime().containsKey(facilityId)){
//					args[8] = new Double(vehicle.getDwellTime().get(facilityId));
//				}
//				
//				else {
//					args[8] = 0.0;
//				}
				args[8] = new String(simulationType);
				scoreWriter.addLine(args);
			}
		}
		}
		scoreWriter.finish();
	}

	public void writeSQLScoreProIteration(String tableName, String simulationType, ScoreListener scoreListener) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException,SQLException {

		DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_MM");
		String formattedDate = df.format(new Date());
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("IterationNr", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TotalCrowdednessUtility", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TotalCrowdednessExternality", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TotalInVehTimeDelayExternality", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TotalCapacityConstraintsExternality", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("TotalMoneyPaid", PostgresType.FLOAT8));
		columns.add(new PostgresqlColumnDefinition("SimulationType", PostgresType.TEXT));

		TableWriter scoreWriter = null;

		String tabname = schemaName + "." + tableName;
		File file = new File(postgresProperties);
		DataBaseAdmin dba = new DataBaseAdmin(file);
		scoreWriter = new PostgresqlCSVWriter("SCOREPROITERATION", tabname,
				dba, 100, columns);
		scoreWriter.addComment(String.format("created on %s for eventsfile ",formattedDate));

		for(int iteration : scoreListener.getIterations()) {
			Object[] args = new Object[columns.size()];
			args[0] = iteration;
			args[1] = scoreListener.getCrowdednessProIteration().get(iteration);
			args[2] = scoreListener.getCrowdednessExternalitiesProIteration().get(iteration);
			args[3] = scoreListener.getInVehicleTimeDelayExternalitiesProIteration().get(iteration);
			args[4] = scoreListener.getCapacityConstraintsExternalitiesProIteration().get(iteration);
			args[5] = scoreListener.getMoneyPaidProIteration().get(iteration);
			args[6] = new String(simulationType);
			scoreWriter.addLine(args);
		}
		scoreWriter.finish();
	}
}