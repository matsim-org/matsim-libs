package org.matsim.core.api.population;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Link;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

public interface Plan extends BasicPlan<PlanElement> {

	/**
	 * @deprecated use Leg.Mode instead
	 */
	@Deprecated
	public enum Type { CAR, PT, RIDE, BIKE, WALK, UNDEFINED}
	
	/**
	 * Constant describing the score of an unscored plan. <b>Do not use this constant in
	 * comparisons</b>, but use <code>getScore() == null</code>
	 * instead to test if a plan has an undefined score.
	 */
	@Deprecated
	public static final double UNDEF_SCORE = Double.NaN;

	public ActivityImpl createActivity(final String type, final Coord coord);

	public ActivityImpl createActivity(final String type, final ActivityFacility fac);

	public ActivityImpl createActivity(final String type, final Link link);

	public LegImpl createLeg(final TransportMode mode);

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
	public void removeActivity(final int index);

	/**
	 * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
	 * the following leg will be emptied to keep consistency (i.e. for the route)
	 *
	 * @param index
	 */
	public void removeLeg(final int index);

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
	public void insertLegAct(final int pos, final LegImpl leg, final ActivityImpl act) throws IllegalArgumentException;

	public LegImpl getPreviousLeg(final ActivityImpl act);

	public ActivityImpl getPreviousActivity(final LegImpl leg);

	/**
	 * Returns the leg following the specified act. <b>Important Note: </b> This method (together with
	 * {@link #getNextActivity(LegImpl)}) has a very bad performance if it is used to iterate over all Acts and
	 * Legs of a plan. In that case, it is advised to use a regular iterator over {@link #getPlanElements()}
	 * together with <code>instanceof</code>.
	 *
	 * @param act
	 * @return The Leg following <tt>act</tt> in the plan, null if <tt>act</tt> is the last Act in the plan.
	 */
	public LegImpl getNextLeg(final ActivityImpl act);

	/**
	 * Returns the activity following the specified leg. <b>Important Note: </b> This method (together with
	 * {@link #getNextLeg(ActivityImpl)}) has a very bad performance if it is used to iterate over all Acts and Legs of
	 * a plan. In that case, it is advised to use a regular iterator over {@link #getPlanElements()} 
	 * together with <code>instanceof</code>.
	 *
	 * @param leg
	 * @return The Act following <tt>leg</tt> in the plan.
	 */
	public ActivityImpl getNextActivity(final LegImpl leg);

	public ActivityImpl getFirstActivity();

	public ActivityImpl getLastActivity();

	@Deprecated
	public void setType(Plan.Type type);
	@Deprecated
	public Plan.Type getType();

}