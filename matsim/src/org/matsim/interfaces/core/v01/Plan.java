package org.matsim.interfaces.core.v01;

import java.util.ArrayList;

import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.stats.algorithms.PlanStats;

public interface Plan extends BasicPlan {
	
	public Act createAct(final String type, final Coord coord) throws IllegalStateException;

	public Act createAct(final String type, final Facility fac) throws IllegalStateException;

	public Act createAct(final String type, final Link link) throws IllegalStateException;

	public Leg createLeg(final BasicLeg.Mode mode) throws IllegalStateException;

	/**
	 * Removes the specified act from the plan as well as a leg according to the following rule:
	 * <ul>
	 * <li>first act: removes the act and the following leg</li>
	 * <li>last act: removes the act and the previous leg</li>
	 * <li>in-between act: removes the act, removes the previous leg's route, and removes the following leg.
	 * </ul>
	 *
	 * @param index
	 */
	public void removeAct(final int index);

	/**
	 * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
	 * the following leg will be emptied to keep consistency (i.e. for the route)
	 *
	 * @param index
	 */
	public void removeLeg(final int index);

	public ArrayList<Object> getActsLegs();

	public Person getPerson();

	public void setPerson(final Person person);

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////
	public boolean isSelected();

	public void setSelected(final boolean selected);

	public String toString();

	/** loads a copy of an existing plan
	 * @param in a plan who's data will be loaded into this plan
	 **/
	public void copyPlan(final Plan in);

	/**
	 * Inserts a leg and a following act at position <code>pos</code> into the plan.
	 *
	 * @param pos the position where to insert the leg-act-combo. acts and legs are both counted from the beginning starting at 0.
	 * @param leg the leg to insert
	 * @param act the act to insert, following the leg
	 * @throws IllegalArgumentException If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
	 */
	public void insertLegAct(final int pos, final Leg leg, final Act act) throws IllegalArgumentException;

	public Leg getPreviousLeg(final Act act);

	public Act getPreviousActivity(final Leg leg);

	/**
	 * Returns the leg following the specified act. <b>Important Note: </b> This method (together with
	 * {@link #getNextActivity(Leg)}) has a very bad performance if it is used to iterate over all Acts and
	 * Legs of a plan. In that case, it is advised to use one of the special iterators.
	 *
	 * @param act
	 * @return The Leg following <tt>act</tt> in the plan, null if <tt>act</tt> is the last Act in the plan.
	 *
	 * @see #getIterator()
	 * @see #getIteratorAct()
	 * @see #getIteratorLeg()
	 */
	public Leg getNextLeg(final Act act);

	/**
	 * Returns the activity following the specified leg. <b>Important Note: </b> This method (together with
	 * {@link #getNextLeg(Act)}) has a very bad performance if it is used to iterate over all Acts and Legs of
	 * a plan. In that case, it is advised to use one of the special iterators.
	 *
	 * @param leg
	 * @return The Act following <tt>leg</tt> in the plan.
	 *
	 * @see #getIterator()
	 * @see #getIteratorAct()
	 * @see #getIteratorLeg()
	 */
	public Act getNextActivity(final Leg leg);

	public Act getFirstActivity();

	public Act getLastActivity();

	public void setFirstPlanStatsAlgorithm(PlanStats firstPlanStatsAlgorithm);

	public PlanStats getFirstPlanStatsAlgorithm();

}