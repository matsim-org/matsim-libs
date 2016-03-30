package house;

import static floetteroed.utilities.math.MathHelpers.draw;
import static house.Summarizer.totalPersonCapacity;
import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.simulatedannealing.SolutionGenerator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MappingGenerator implements SolutionGenerator<List<Person>> {

	private final Random rnd;

	private final List<Person> allPersons;

	private final List<Room> allRooms;

	private Map<Room, Double> room2relativePersonCapacity;

	public MappingGenerator(final List<Person> allPersons,
			final List<Room> allRooms, final Random rnd) {
		this.allPersons = Collections.unmodifiableList(allPersons);
		this.allRooms = Collections.unmodifiableList(allRooms);
		this.rnd = rnd;

		final Map<Room, Double> room2relativePersonCapacity = new LinkedHashMap<>();
		final double totalPersonCapacity = totalPersonCapacity(allRooms);
		for (Room room : allRooms) {
			room2relativePersonCapacity.put(room,
					((double) room.personCapacity) / totalPersonCapacity);
		}
		this.room2relativePersonCapacity = unmodifiableMap(room2relativePersonCapacity);
	}

	@Override
	public List<Person> randomGeneration() {

		final LinkedList<Room> freeRooms = new LinkedList<Room>();
		for (Room room : this.allRooms) {
			for (int i = 0; i < room.personCapacity; i++) {
				freeRooms.add(room);
			}
		}
		Collections.shuffle(freeRooms);

		if (freeRooms.size() < this.allPersons.size()) {
			System.out.println("INFEASIBLE. There are "
					+ this.allPersons.size() + " persons but only "
					+ freeRooms.size() + " places.");
			System.exit(0);
		}

		final List<Person> result = new ArrayList<>(this.allPersons.size());
		for (Person person : this.allPersons) {
			final Person newPerson = person.newDeepCopy();
			newPerson.setRoom(freeRooms.removeFirst());
			result.add(newPerson);
		}

		return result;
	}

	private int numberOfSwitches(int personCnt) {
		int result = 1;
		for (int i = 1; i < personCnt; i++) {
			if (this.rnd.nextDouble() < 1.0 / personCnt) {
				result++;
			}
		}
		return result;
	}

	private Person drawPersonInRoom(final List<Person> persons, final Room room) {
		final List<Person> personsInRoom = new ArrayList<>(room.personCapacity);
		for (Person person : persons) {
			if (person.getRoom() == room) {
				personsInRoom.add(person);
			}
		}
		if (room.personCapacity * this.rnd.nextDouble() < personsInRoom.size()) {
			// return a person
			return personsInRoom.get(this.rnd.nextInt(personsInRoom.size()));
		} else {
			// return a free place
			return null;
		}
	}

	@Override
	public List<Person> variation(final List<Person> original) {
		final List<Person> result = this.copy(original);
		for (int i = 0; i < this.numberOfSwitches(result.size()); i++) {
			final Room room1 = draw(this.room2relativePersonCapacity, this.rnd);
			final Room room2 = draw(this.room2relativePersonCapacity, this.rnd);
			final Person person1 = this.drawPersonInRoom(result, room1);
			final Person person2 = this.drawPersonInRoom(result, room2);
			if (person1 != null) {
				person1.setRoom(room2);
			}
			if (person2 != null) {
				person2.setRoom(room1);
			}
		}
		return result;
	}

	@Override
	public List<Person> copy(final List<Person> original) {
		final List<Person> result = new ArrayList<>(original.size());
		for (Person person : original) {
			result.add(person.newDeepCopy());
		}
		return result;
	}
}
