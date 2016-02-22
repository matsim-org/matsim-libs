package house;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Room extends Labeled {

	public final int personCapacity;

	public final int floor;

	public Room(final String label, final int personCapacity, final int floor) {
		super(label);
		this.personCapacity = personCapacity;
		this.floor = floor;
	}

	@Override
	public String toString() {
		return this.label + " (personCapacity=" + this.personCapacity
				+ ", floor=" + this.floor + ")";
	}
}
