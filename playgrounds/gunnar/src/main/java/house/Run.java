package house;

import static house.Summarizer.floor2groupsReport;
import static house.Summarizer.room2personClassReport;
import static house.Summarizer.room2personsReport;
import static house.Summarizer.totalPersonCapacity;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import floetteroed.utilities.simulatedannealing.SimulatedAnnealing;

public class Run {

	public Run() {
	}

	public static void main(String[] args) {

		final Random rnd = new Random();

		final String configFileName = args[0];
		final Config config = (new ConfigReader()).read(configFileName);
		final double groupMemberDistanceWeight = parseDouble(config.get(
				"allocation", "groupmemberdistanceweight"));
		final Config personsConfig = (new ConfigReader()).read(config.get(
				"allocation", "personsfile"));
		final Config roomsConfig = (new ConfigReader()).read(config.get(
				"allocation", "roomsfile"));

		/*
		 * Read in rooms.
		 */
		final List<Room> rooms = new ArrayList<>();
		{
			final List<String> labels = roomsConfig.getList("allocation",
					"room", "label");
			final List<String> numbers = roomsConfig.getList("allocation",
					"room", "number");
			final List<String> personCaps = roomsConfig.getList("allocation",
					"room", "personcapacity");
			final List<String> floors = roomsConfig.getList("allocation",
					"room", "floor");
			for (int i = 0; i < labels.size(); i++) {
				for (int cnt = 0; cnt < Integer.parseInt(numbers.get(i)); cnt++) {
					final Room room = new Room(labels.get(i),
							parseInt(personCaps.get(i)),
							parseInt(floors.get(i)));
					if (room.personCapacity > 0) {
						rooms.add(room);
					}
				}
			}
		}

		/*
		 * Read in distances.
		 */
		// final Map<Tuple<Room, Room>, Double> rooms2distance = new
		// LinkedHashMap<>();
		// {
		// final List<String> room1s = roomsConfig.getList("allocation",
		// "distance", "room1");
		// final List<String> room2s = roomsConfig.getList("allocation",
		// "distance", "room2");
		// final List<String> values = roomsConfig.getList("allocation",
		// "distance", "value");
		// for (int i = 0; i < room1s.size(); i++) {
		// final Room room1 = label2room.get(room1s.get(i));
		// final Room room2 = label2room.get(room2s.get(i));
		// final double dist = Double.parseDouble(values.get(i));
		// rooms2distance.put(new Tuple<Room, Room>(room1, room2), dist);
		// rooms2distance.put(new Tuple<Room, Room>(room2, room1), dist);
		// }
		// }

		/*
		 * Read in group memberships.
		 */
		final Map<String, LinkedList<String>> personLabel2groupInstances = new LinkedHashMap<>();
		{
			final List<String> persons = personsConfig.getList("allocation",
					"membership", "person");
			final List<String> groups = personsConfig.getList("allocation",
					"membership", "group");
			final List<String> numbers = personsConfig.getList("allocation",
					"membership", "number");
			for (int i = 0; i < persons.size(); i++) {
				LinkedList<String> groupInstances = personLabel2groupInstances
						.get(persons.get(i));
				if (groupInstances == null) {
					groupInstances = new LinkedList<String>();
					personLabel2groupInstances.put(persons.get(i),
							groupInstances);
				}
				for (int cnt = 0; cnt < parseInt(numbers.get(i)); cnt++) {
					groupInstances.add(groups.get(i));
				}
			}
		}

		/*
		 * Read in persons.
		 */
		final List<Person> persons = new ArrayList<>();
		{
			final List<String> labels = personsConfig.getList("allocation",
					"person", "label");
			final List<String> numbers = personsConfig.getList("allocation",
					"person", "number");
			final List<String> mingroups = personsConfig.getList("allocation",
					"person", "mingroup");
			final List<String> maxgroups = personsConfig.getList("allocation",
					"person", "maxgroup");
			final List<String> weights = personsConfig.getList("allocation",
					"person", "weight");
			for (int i = 0; i < labels.size(); i++) {
				for (int cnt = 0; cnt < parseInt(numbers.get(i)); cnt++) {
					final LinkedList<String> groupList = personLabel2groupInstances
							.get(labels.get(i));
					final String group;
					if ((groupList != null) && (groupList.size() > 0)) {
						group = groupList.removeFirst();
					} else {
						group = null;
					}
					final Person person = new Person(labels.get(i),
							parseInt(mingroups.get(i)),
							parseInt(maxgroups.get(i)),
							parseDouble(weights.get(i)), group);
					persons.add(person);
				}
			}
		}

		/*
		 * Solve the problem.
		 */
		final int maxFailures = 5;
		int failures = 0;
		List<Person> bestResult = null;
		Double bestObjFctVal = Double.POSITIVE_INFINITY;
		while (failures < maxFailures) {
			System.out.print(".");
			final MappingGenerator mg = new MappingGenerator(persons, rooms,
					rnd);
			final MappingEvaluator me = new MappingEvaluator(
					groupMemberDistanceWeight, rooms);
			final SimulatedAnnealing<List<Person>> sa = new SimulatedAnnealing<>(
					mg, me);
			sa.run(null, MAX_VALUE, POSITIVE_INFINITY,
					persons.size() * persons.size() * 10);
			if (sa.getOptimalEvaluation() < bestObjFctVal) {
				failures = 0;
				bestObjFctVal = sa.getOptimalEvaluation();
				bestResult = sa.getOptimalSolution();
			} else {
				failures++;
			}
		}
		System.out.println();
		System.out.println();

		/*
		 * Write out result.
		 */

		System.out
				.println("========== PARAMETERS =========================================");
		System.out.println();
		System.out.println("persons file: "
				+ config.get("allocation", "personsfile"));
		System.out.println("rooms file: "
				+ config.get("allocation", "roomsfile"));
		System.out.println("total number of persons: " + persons.size());
		System.out.println("total number of places: "
				+ totalPersonCapacity(rooms));
		System.out.println("weight for distance between group members: "
				+ groupMemberDistanceWeight);
		System.out.println();

		System.out
				.println("========== ROOM OCCUPANCY ALLOCATION ==========================");
		System.out.println();
		System.out.println(room2personClassReport(bestResult, rooms));

		System.out
				.println("========== EXAMPLE FLOOR ALLOCATION ===========================");
		System.out.println();
		System.out.println(floor2groupsReport(bestResult, rooms));

		System.out
				.println("========== EXAMPLE ALLOCATION =================================");
		System.out.println();
		System.out.println(room2personsReport(bestResult, rooms));
	}
}
