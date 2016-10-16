/**<b>Definition:</b> <i>Withinday replanning</i> means that agents can replan while they are on their way, technically while the mobsim is running.
 * <p/>
 * At this point, there are at least two approaches:<ul>
 * <li> One approach, put forward by Christoph Dobler, which takes existing agents with plans and modifies future parts of their plans.
 * <li> Another approach, put forward by Kai Nagel and Michael Zilske, which re-programms the agent from the inside out.
 * </ul>
 * In the end, each approach will be able to emulate the other one, so it may come down to a matter of taste.
 * </p>
 * This package contains the <i>first</i> variant.  <i>The other one is described in another tutorial package.</i>
 * </p>
 * <b>This package contains (as of dec'10):</b><ul>
 * <li>{@link tutorial.programming.withinDayReplanningFromPlans.RunWithinDayReplanningFromPlansExample} (very short), which contains the main method.
 * <li>{@link tutorial.programming.withinDayReplanningFromPlans.MyMobsimFactory} (short), which defines that mobsim setup.
 * <li>{@link tutorial.programming.withinDayReplanningFromPlans.MyWithinDayMobsimListener}, which contains a concrete implementation for withinday replanning of vehicle drivers.  
 * </ul>
 * <p/>
 * </ul>
 * <p/>
 * Please address yourself to Christoph Dobler for support.
 */
package tutorial.programming.withinDayReplanningFromPlans;