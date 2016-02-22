package house;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Summarizer {

	private Summarizer() {
	}

	public static int totalPersonCapacity(final Iterable<Room> allRooms) {
		int result = 0;
		for (Room room : allRooms) {
			result += room.personCapacity;
		}
		return result;
	}

	public static Map<Room, Set<Person>> newRoom2persons(
			final List<Person> allPersons, final List<Room> allRooms) {
		final Map<Room, Set<Person>> result = new LinkedHashMap<>();
		for (Room room : allRooms) {
			result.put(room, new LinkedHashSet<Person>());
		}
		for (Person person : allPersons) {
			result.get(person.getRoom()).add(person);
		}
		return result;
	}

	public static Map<Integer, Set<Person>> newRoomOccupancy2persons(
			final List<Person> solution, final List<Room> allRooms) {
		final Map<Integer, Set<Person>> result = new TreeMap<>();
		final Map<Room, Set<Person>> room2persons = newRoom2persons(solution,
				allRooms);
		for (Set<Person> personsInRoom : room2persons.values()) {
			if (personsInRoom.size() > 0) {
				if (!result.containsKey(personsInRoom.size())) {
					result.put(personsInRoom.size(),
							new LinkedHashSet<Person>());
				}
				result.get(personsInRoom.size()).addAll(personsInRoom);
			}
		}
		return result;
	}

	// public static Map<Integer, Set<Person>> newRoomSize2persons(
	// final List<Person> allPersons, final List<Room> allRooms) {
	// final Map<Integer, Set<Person>> result = new TreeMap<>();
	// for (Room room : allRooms) {
	// if (!result.containsKey(room.personCapacity)) {
	// result.put(room.personCapacity, new LinkedHashSet<Person>());
	// }
	// }
	// for (Person person : allPersons) {
	// result.get(person.getRoom().personCapacity).add(person);
	// }
	// return result;
	// }

	public static Map<Integer, Set<Person>> newFloor2persons(
			final List<Person> allPersons, final List<Room> allRooms) {
		final Map<Integer, Set<Person>> result = new LinkedHashMap<>();
		for (Room room : allRooms) {
			if (!result.containsKey(room.floor)) {
				result.put(room.floor, new LinkedHashSet<Person>());
			}
		}
		for (Person person : allPersons) {
			result.get(person.getRoom().floor).add(person);
		}
		return result;
	}

	public static double averageGroupMemberDistance(final Set<Person> members) {
		double result = 0.0;
		for (Person p1 : members) {
			for (Person p2 : members) {
				result += Math.abs(p1.getRoom().floor - p2.getRoom().floor);
			}
		}
		result /= Math.pow(members.size() - 1.0, 2.0);
		return result;
	}

	public static Map<String, Set<Person>> newGroup2members(
			final Iterable<Person> persons) {
		final Map<String, Set<Person>> result = new LinkedHashMap<>();
		for (Person person : persons) {
			final String group = (person.group == null ? "None" : person.group);
			Set<Person> members = result.get(group);
			if (members == null) {
				members = new LinkedHashSet<>();
				result.put(group, members);
			}
			members.add(person);
		}
		return result;
	}

	// public static Map<Tuple<String, String>, Integer> newPersonGroupClasses(
	// final Iterable<Person> persons) {
	// final Map<Tuple<String, String>, Integer> personClasses = new
	// LinkedHashMap<>();
	// for (Person person : persons) {
	// final Tuple<String, String> key = new Tuple<>(person.label,
	// (person.group == null ? "no group" : person.group));
	// Integer cnt = personClasses.get(key);
	// if (cnt == null) {
	// cnt = 0;
	// }
	// personClasses.put(key, cnt + 1);
	// }
	// return personClasses;
	// }

	public static Map<String, Integer> newPersonClasses(
			final Iterable<Person> persons) {
		final Map<String, Integer> personClasses = new LinkedHashMap<>();
		for (Person person : persons) {
			Integer cnt = personClasses.get(person.label);
			if (cnt == null) {
				cnt = 0;
			}
			personClasses.put(person.label, cnt + 1);
		}
		return personClasses;
	}

	// public static final double averageRoomUsage(
	// final Map<Room, Set<Person>> room2persons) {
	// double result = 0;
	// for (Map.Entry<Room, Set<Person>> entry : room2persons.entrySet()) {
	// if (entry.getKey().personCapacity > 0) {
	// result += ((double) entry.getValue().size())
	// / ((double) entry.getKey().personCapacity);
	// }
	// }
	// result /= room2persons.size();
	// return result;
	// }

	// public static String summaryReport(final List<Person> allPersons,
	// List<Room> allRooms) {
	// final Map<Room, Set<Person>> room2persons = newRoom2persons(allPersons,
	// allRooms);
	// final StringBuffer result = new StringBuffer();
	// result.append("SUMMARY\n");
	// result.append("  average room usage: "
	// + MathHelpers.round(averageRoomUsage(room2persons) * 100)
	// + " percent\n");
	// return result.toString();
	// }

	public static String floor2groupsReport(final List<Person> solution,
			final List<Room> allRooms) {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Integer, Set<Person>> floor2personsEntry : newFloor2persons(
				solution, allRooms).entrySet()) {
			result.append("FLOOR " + floor2personsEntry.getKey() + "\n");
			for (Map.Entry<String, Set<Person>> group2membersEntry : newGroup2members(
					floor2personsEntry.getValue()).entrySet()) {
				result.append("  Group " + group2membersEntry.getKey() + ": "
						+ group2membersEntry.getValue().size() + " members\n");
			}
			result.append("\n");
		}
		return result.toString();
	}

	public static String room2personClassReport(final List<Person> solution,
			final List<Room> allRooms) {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Integer, Set<Person>> roomOccup2personsEntry : newRoomOccupancy2persons(
				solution, allRooms).entrySet()) {
			result.append("ROOM OCCUPANCY " + roomOccup2personsEntry.getKey()
					+ "\n");
			for (Map.Entry<String, Integer> personsClassesInRoomEntry : newPersonClasses(
					roomOccup2personsEntry.getValue()).entrySet()) {
				result.append("  " + personsClassesInRoomEntry.getValue() + " "
						+ personsClassesInRoomEntry.getKey() + "\n");
			}
			result.append("\n");
		}
		return result.toString();
	}

	public static String room2personsReport(final List<Person> allPersons,
			final List<Room> allRooms) {
		final StringBuffer result = new StringBuffer();
		final Map<Room, Set<Person>> room2persons = newRoom2persons(allPersons,
				allRooms);
		for (Room room : allRooms) {
			result.append(room.label.toUpperCase() + " (" + "usage: "
					+ room2persons.get(room).size() + "/" + room.personCapacity
					+ ")\n");
			final Map<String, Integer> personClassesInRoom = newPersonClasses(room2persons
					.get(room));
			for (Map.Entry<String, Integer> entry : personClassesInRoom
					.entrySet()) {
				result.append("  " + entry.getValue() + " " + entry.getKey()
						+ "\n");
			}
			result.append("\n");
		}
		return result.toString();
	}
}
