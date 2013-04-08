package playground.pieter.singapore.utils.events;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class EventsToPersonStages implements ActivityHandler, LegHandler {

	private final Map<Id, Plan> agentRecords = new TreeMap<Id, Plan>();

	private final Scenario scenario;

	public EventsToPersonStages(Scenario scenario) {
		this.scenario = scenario;
		for (Person person : scenario.getPopulation().getPersons().values())
			this.agentRecords.put(person.getId(), new PlanImpl());
	}

	@Override
	public void handleLeg(Id agentId, Leg leg) {
		if (agentRecords.get(agentId) != null)
			agentRecords.get(agentId).addLeg(leg);
	}

	@Override
	public void handleActivity(Id agentId, Activity activity) {
		if (agentRecords.get(agentId) != null)
			agentRecords.get(agentId).addActivity(activity);
	}
	

	public void writeExperiencedPlans(String tableName, DataBaseAdmin dba)
			throws SQLException, NoConnectionException {
		dba.executeStatement("DROP TABLE IF EXISTS matsim_stage_report");
		dba.executeUpdate("CREATE TABLE matsim_stage_report(  person_id VARCHAR(255)," +
				"plan_element_id INT, activity_id INT, journey_id  INT, " +
				"trip_id INT, substage_id INT, plan_element_type VARCHAR(255), line VARCHAR(255)"
							+ ", route VARCHAR(255), mode VARCHAR(255)" + ", distance double"
							+ ", duration double" + ", start_time double"
							+ ", end_time double,  id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (id))");
		for (Entry<Id, Plan> entry : agentRecords.entrySet()) {
			String paxid = entry.getKey().toString();
			List<PlanElement> planElements = entry.getValue().getPlanElements();
			int journey_id = 0;
			int trip_id = 0;
			int substage_id = 0;
			int activity_id = 0;
			boolean incrementTripCheck = false;
			String sqlString = "INSERT INTO matsim_stage_report (  " +
					"person_id, plan_element_id , activity_id , journey_id  , trip_id , substage_id , " +
					"plan_element_type , line, route, mode, " 
					+ " distance , duration , start_time , end_time) VALUES( ";
			for(int i=0; i<planElements.size();i++){
				PlanElement pe = planElements.get(i);
										
				if(i%2==0){
					//activity
					ActivityImpl act = (ActivityImpl) pe;
					if(act.getType().equals("pt interaction")){
						trip_id++;
						substage_id=0;
					}else{
						activity_id++;
						incrementTripCheck = true;
						trip_id = 0;
						substage_id = 0;
						sqlString += String.format("(\'%s\',%d,%d,NULL,NULL,NULL,\'%s\',NULL,NULL,NULL,NULL,%f,%f,%f),", 
								paxid,i,activity_id, "NULL","NULL","NULL",act.getType(),"NULL","NULL","NULL","NULL",
								act.getEndTime()-act.getStartTime(), act.getStartTime(),act.getEndTime());						
					}
				}else{
					//trip, or new trip stage
					
				}
			}

		}
	}

	/**
	 * @param args
	 *            0 - Network file 1 - Population input file 2 - Events input
	 *            file 3 - Plans output file
	 * @throws SQLException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException,
			SQLException, NoConnectionException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		new MatsimPopulationReader(scenario).readFile(args[1]);
		EventsToPersonStages eventsToPaxStages = new EventsToPersonStages(
				scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToActivities eventsToActivities = new EventsToActivities();
		eventsToActivities.setActivityHandler(eventsToPaxStages);
		EventsToLegs eventsToLegs = new EventsToLegs();
		eventsToLegs.setLegHandler(eventsToPaxStages);
		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);
		new EventsReaderXMLv1(eventsManager).parse(args[2]);
		DataBaseAdmin dba = new DataBaseAdmin(
				new File("calibration.properties"));
		eventsToPaxStages.writeExperiencedPlans(args[3], dba);
	}

}
