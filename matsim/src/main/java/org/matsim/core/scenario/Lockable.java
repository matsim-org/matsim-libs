package org.matsim.core.scenario;

public interface Lockable {

	/**
	 * This is to set certain properties of data objects locked when the simulations start, but allow setters for them
	 * upstream of the simulation run.  Some of them are (or should 
	 * be):<ul>
	 * <li> IDs (since the IDs are used in the Maps as keys, so changing them without removing and re-inserting
	 * the object makes the map invalid)
	 * <li> Some of the coordinates.  This is a bit tricky, for example, activities should be able to change their coordinates
	 * because of location choice.  However, nodes and facilities should not.  (And location choice, in consequence, is 
	 * rather changing the facility reference than changing the coordinate.)
	 * </ul>
	 */
	void setLocked();

}
