package playground.andreas.bln.counts;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Generates population from counts data for specified lines. Additional group of agents can be added as an option.
 *
 * @author aneumann
 *
 */

public class Counts2Plans {

	private static final Logger log = Logger.getLogger(Counts2Plans.class);
	private static final Random rnd = new Random(4711);

	private Counts access = new Counts();
	private Counts egress = new Counts();
	private TransitSchedule transitSchedule;

	private HashMap<String, LinkedList<Id<TransitStopFacility>>> lines = new HashMap<>();

	private int runningID = 1;
	private int numberOfPersonsWithValidPlan = 0;
	private int numberOfPersonsLeftInBusAtEndOfLine = 0;
	private int numberOfPersonsCouldNotLeaveTheBusWhenSupposedTo = 0;

	private LinkedList<Person> completedAgents = new LinkedList<Person>();


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Counts2Plans counts2plans = new Counts2Plans();
		counts2plans.access = Counts2Plans.readCountsFile("d:/Berlin/BVG/berlin-bvg09/counts/pt/counts_boarding_M44_344.xml");
		counts2plans.egress = Counts2Plans.readCountsFile("d:/Berlin/BVG/berlin-bvg09/counts/pt/counts_alighting_M44_344.xml");

		counts2plans.transitSchedule = Counts2Plans.readTransitSchedule("d:/Berlin/intervalltakt/simulation/transitSchedule.xml", "d:/Berlin/intervalltakt/simulation/network.xml");

//		counts2plans.addLine("344  ");

		counts2plans.addM44_H();
		counts2plans.addM44_R();
//		counts2plans.add344_H();
//		counts2plans.add344_R();

		counts2plans.createPlans();
		counts2plans.printLog();
		counts2plans.addTouristGroup(100, Time.parseTime("08:30:00"), Id.create("812030.1", TransitStopFacility.class), Id.create("806520.1", TransitStopFacility.class));
		counts2plans.createPopulation("d:/Berlin/intervalltakt/simulation/plans_neu.xml.gz");

		Counts2Plans.log.info("Finished");

	}

	private void addTouristGroup(int number, double time, Id<TransitStopFacility> from, Id<TransitStopFacility> to) {

		for (int i = 1; i <= number; i++) {

			Person person = createPerson();
			ActivityImpl a = ((PlanImpl) person.getSelectedPlan()).createAndAddActivity("start", this.transitSchedule.getFacilities().get(from).getLinkId());
			a.setCoord(this.transitSchedule.getFacilities().get(from).getCoord());

			((PlanImpl) person.getSelectedPlan()).createAndAddLeg(TransportMode.pt);
			((PlanImpl) person.getSelectedPlan()).getFirstActivity().setEndTime(time);

			a = ((PlanImpl) person.getSelectedPlan()).createAndAddActivity("finish", this.transitSchedule.getFacilities().get(to).getLinkId());
			a.setCoord(this.transitSchedule.getFacilities().get(to).getCoord());

			this.completedAgents.add(person);
			this.numberOfPersonsWithValidPlan++;
		}

		log.info(this.numberOfPersonsWithValidPlan + " persons after creating additional 'tourists'");

	}

	private void printLog() {
		log.info(this.numberOfPersonsWithValidPlan + " persons were created according to counts files");
		log.info(this.numberOfPersonsCouldNotLeaveTheBusWhenSupposedTo + " persons were supposed to leave the bus, but the vehicle didn't contain any");
		log.info(this.numberOfPersonsLeftInBusAtEndOfLine + " persons where sitting in the bus at the end of line, because they never got the possibility to leave the vehicle");
	}

	private void createPlans() {

		for (int hour = 1; hour <= 24; hour++) {

			log.info("Hour: " + hour);
			LinkedList<Person> passengersInVehicle = new LinkedList<Person>();

			for (Entry<String, LinkedList<Id<TransitStopFacility>>> entry : this.lines.entrySet()) {

				for (Id<TransitStopFacility> stopID : entry.getValue()) {
					Id<Link> stopIdAsLink = Id.create(stopID, Link.class);
					if (this.egress.getCount(stopIdAsLink) != null) {
						if (this.egress.getCount(stopIdAsLink).getVolume(hour) != null) {

							for (int i = 0; i < this.egress.getCount(stopIdAsLink).getVolume(hour).getValue(); i++) {

								Person person = passengersInVehicle.pollFirst();

								if (person == null) {
									log.warn("StopID: " + stopID + ", Passenger should leave the vehicle, but none is there");
									this.numberOfPersonsCouldNotLeaveTheBusWhenSupposedTo++;
								} else {
									ActivityImpl a = ((PlanImpl) person.getSelectedPlan()).createAndAddActivity("finish", this.transitSchedule.getFacilities().get(stopID).getLinkId());
									a.setCoord(this.transitSchedule.getFacilities().get(stopID).getCoord());
									//									((PlanImpl) person.getSelectedPlan()).createAndAddActivity("finish", this.egress.getCount(stopID).getCoord());
									this.completedAgents.add(person);
									this.numberOfPersonsWithValidPlan++;
								}
							}
						}
					}

					if (this.access.getCount(stopIdAsLink) != null) {
						if (this.access.getCount(stopIdAsLink).getVolume(hour) != null) {

							for (int i = 0; i < this.access.getCount(stopIdAsLink).getVolume(hour).getValue(); i++) {
								Person person = createPerson();
								ActivityImpl a = ((PlanImpl) person.getSelectedPlan()).createAndAddActivity("start", this.transitSchedule.getFacilities().get(stopID).getLinkId());
								a.setCoord(this.transitSchedule.getFacilities().get(stopID).getCoord());
								//								((PlanImpl) person.getSelectedPlan()).createAndAddActivity("start", this.access.getCount(stopID).getCoord());
								((PlanImpl) person.getSelectedPlan()).createAndAddLeg(TransportMode.pt);

								// Verlegen der Nachfrage auf die Zeit des OEV-Angebots. Dieses geht von 3:30 bis 27:30 Uhr.
								if(hour < 4){
									((PlanImpl) person.getSelectedPlan()).getFirstActivity().setEndTime((hour + 24 - 1 + rnd.nextDouble()) * 3600);
								} else {
									((PlanImpl) person.getSelectedPlan()).getFirstActivity().setEndTime((hour - 1 + rnd.nextDouble()) * 3600);
								}
								passengersInVehicle.add(person);
							}
						}
					}

				}

				if(passengersInVehicle.size() != 0){
					log.warn(hour + " hour, " + entry.getKey() + " line, " + passengersInVehicle.size() + " passengers still in vehicle after last stop");
					this.numberOfPersonsLeftInBusAtEndOfLine += passengersInVehicle.size();
				}

			}

		}

	}

	private Person createPerson(){
		Person person = PersonImpl.createPerson(Id.create(this.runningID, Person.class));
		PersonUtils.createAndAddPlan(person, true);
		this.runningID++;
		return person;
	}

	private void createPopulation(String filename) {

        ScenarioImpl sc = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        Population pop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());

		for (Person person : this.completedAgents) {
			pop.addPerson(person);
		}

		PopulationWriter popWriter = new PopulationWriter(pop, null);
		popWriter.writeStartPlans(filename);
		popWriter.writePersons();
		popWriter.writeEndPlans();

	}

	private static Counts readCountsFile(String filename) {
		Counts counts = new Counts();
		MatsimCountsReader matsimCountsReader = new MatsimCountsReader(counts);
		matsimCountsReader.readFile(filename);
		return counts;
	}

	private static TransitSchedule readTransitSchedule(String transitScheduleFile, String networkFile) {

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);

		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, scenario.getNetwork());

		transitScheduleReaderV1.readFile(transitScheduleFile);

		return transitSchedule;
	}

	private void add344_H() {
		LinkedList<Id<TransitStopFacility>> b344_H = new LinkedList<>();
		this.lines.put("344_H", b344_H);

		b344_H.add(Id.create("792040.1", TransitStopFacility.class));
		b344_H.add(Id.create("792200.1", TransitStopFacility.class));
		b344_H.add(Id.create("792013.1", TransitStopFacility.class));
		b344_H.add(Id.create("792030.1", TransitStopFacility.class));
		b344_H.add(Id.create("792023.1", TransitStopFacility.class));
		b344_H.add(Id.create("792910.1", TransitStopFacility.class));
		b344_H.add(Id.create("781060.1", TransitStopFacility.class));
		b344_H.add(Id.create("781040.1", TransitStopFacility.class));
	}

	private void add344_R() {
		LinkedList<Id<TransitStopFacility>> b344_R = new LinkedList<>();
		this.lines.put("344_R", b344_R);

		b344_R.add(Id.create("781015.2", TransitStopFacility.class));
		b344_R.add(Id.create("792910.2", TransitStopFacility.class));
		b344_R.add(Id.create("792023.2", TransitStopFacility.class));
		b344_R.add(Id.create("792030.2", TransitStopFacility.class));
		b344_R.add(Id.create("792013.2", TransitStopFacility.class));
		b344_R.add(Id.create("792200.2", TransitStopFacility.class));
		b344_R.add(Id.create("792040.2", TransitStopFacility.class));
	}

	private void addM44_H() {
		LinkedList<Id<TransitStopFacility>> m44_H = new LinkedList<>();
		this.lines.put("m44_H", m44_H);

		m44_H.add(Id.create("812020.1", TransitStopFacility.class));
		m44_H.add(Id.create("812550.1", TransitStopFacility.class));
		m44_H.add(Id.create("812030.1", TransitStopFacility.class));
		m44_H.add(Id.create("812560.1", TransitStopFacility.class));
		m44_H.add(Id.create("812570.1", TransitStopFacility.class));
		m44_H.add(Id.create("812013.1", TransitStopFacility.class));
		m44_H.add(Id.create("806520.1", TransitStopFacility.class));
		m44_H.add(Id.create("806030.1", TransitStopFacility.class));
		m44_H.add(Id.create("806010.1", TransitStopFacility.class));
		m44_H.add(Id.create("806540.1", TransitStopFacility.class));
		m44_H.add(Id.create("804070.1", TransitStopFacility.class));
		m44_H.add(Id.create("804060.1", TransitStopFacility.class));
		m44_H.add(Id.create("801020.1", TransitStopFacility.class));
		m44_H.add(Id.create("801030.1", TransitStopFacility.class));
		m44_H.add(Id.create("801530.1", TransitStopFacility.class));
		m44_H.add(Id.create("801040.1", TransitStopFacility.class));
		m44_H.add(Id.create("792050.1", TransitStopFacility.class));
		m44_H.add(Id.create("792200.3", TransitStopFacility.class));
	}

	private void addM44_R() {
		LinkedList<Id<TransitStopFacility>> m44_R = new LinkedList<>();
		this.lines.put("m44_R", m44_R);

		m44_R.add(Id.create("792200.4", TransitStopFacility.class));
		m44_R.add(Id.create("792050.2", TransitStopFacility.class));
		m44_R.add(Id.create("801040.2", TransitStopFacility.class));
		m44_R.add(Id.create("801530.2", TransitStopFacility.class));
		m44_R.add(Id.create("801030.2", TransitStopFacility.class));
		m44_R.add(Id.create("801020.2", TransitStopFacility.class));
		m44_R.add(Id.create("804060.2", TransitStopFacility.class));
		m44_R.add(Id.create("804070.2", TransitStopFacility.class));
		m44_R.add(Id.create("806540.2", TransitStopFacility.class));
		m44_R.add(Id.create("806010.2", TransitStopFacility.class));
		m44_R.add(Id.create("806030.2", TransitStopFacility.class));
		m44_R.add(Id.create("806520.2", TransitStopFacility.class));
		m44_R.add(Id.create("812013.2", TransitStopFacility.class));
		m44_R.add(Id.create("812570.2", TransitStopFacility.class));
		m44_R.add(Id.create("812560.2", TransitStopFacility.class));
		m44_R.add(Id.create("812030.2", TransitStopFacility.class));
		m44_R.add(Id.create("812550.2", TransitStopFacility.class));
		m44_R.add(Id.create("812020.2", TransitStopFacility.class));
	}

	private void addLine(String lineID) {

		TransitLine transitLine = this.transitSchedule.getTransitLines().get(Id.create(lineID, TransitLine.class));
		LinkedList<Id<TransitStopFacility>> routeStops = new LinkedList<>();

		for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
			if(transitRoute.getStops().size() > routeStops.size()){
				routeStops = new LinkedList<>();
				for (TransitRouteStop stop : transitRoute.getStops()) {
					routeStops.add(stop.getStopFacility().getId());
				}
			}
		}

		this.lines.put("344_H", routeStops);

	}

}
