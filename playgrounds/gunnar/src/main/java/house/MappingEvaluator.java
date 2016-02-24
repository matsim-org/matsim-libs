package house;

import static house.Summarizer.averageGroupMemberDistance;
import static house.Summarizer.newGroup2members;
import static house.Summarizer.newRoom2persons;
import static java.lang.Math.max;
import static java.util.Collections.unmodifiableList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.simulatedannealing.SolutionEvaluator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MappingEvaluator implements SolutionEvaluator<List<Person>> {

	private final double groupMemberDistanceWeight;

	private final List<Room> allRooms;

	public MappingEvaluator(final double groupMemberDistanceWeight,
			final List<Room> allRooms) {
		this.groupMemberDistanceWeight = groupMemberDistanceWeight;
		this.allRooms = unmodifiableList(allRooms);
	}

	@Override
	public boolean feasible(List<Person> solution) {

		// No room is allowed to be overcrowded.
		for (Map.Entry<Room, Set<Person>> entry : newRoom2persons(solution,
				this.allRooms).entrySet()) {
			if (entry.getKey().personCapacity < entry.getValue().size()) {
				return false;
			}
		}

		// All administrators must sit in the new house (levels 5 or 6).
		for (Person person : solution) {
			if ("Admin".equals(person.label)
					&& ((person.getRoom().floor != 5) && (person.getRoom().floor != 6))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public double evaluation(List<Person> solution) {
		double result = 0;
		final Map<Room, Set<Person>> room2persons = newRoom2persons(solution,
				this.allRooms);

		// Average dissatisfaction with room allocation.
		double dissatisfaction = 0;
		for (Person person : solution) {
			final int groupSize = room2persons.get(person.getRoom()).size();
			dissatisfaction += person.weight
					* max(0, groupSize - person.maxGroupSize);
			dissatisfaction += person.weight
					* max(0, person.minGroupSize - groupSize);
		}
		result += dissatisfaction / solution.size();

		// Average distance between group members.
		double distance = 0.0;
		final Map<String, Set<Person>> group2members = newGroup2members(solution);
		for (Set<Person> members : group2members.values()) {
			distance += averageGroupMemberDistance(members);
		}
		result += this.groupMemberDistanceWeight * distance
				/ group2members.size();

		return result;
	}
}
