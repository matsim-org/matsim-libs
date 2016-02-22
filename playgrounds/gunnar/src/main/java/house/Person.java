package house;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Person extends Labeled {

	public final int minGroupSize;

	public final int maxGroupSize;

	public final double weight;

	public final String group;

	private Room room = null;

	public Person(final String label, final int minGroupSize,
			final int maxGroupSize, final double weight, final String group) {
		super(label);
		this.minGroupSize = minGroupSize;
		this.maxGroupSize = maxGroupSize;
		this.weight = weight;
		this.group = group;
	}

	public Room getRoom() {
		return room;
	}

	public void setRoom(Room room) {
		this.room = room;
	}

	public Person newDeepCopy() {
		final Person result = new Person(this.label, this.minGroupSize,
				this.maxGroupSize, this.weight, this.group);
		result.room = this.room;
		return result;
	}

	@Override
	public String toString() {
		return this.label + " in group " + this.group + " sitting in room "
				+ this.room;
	}
}
