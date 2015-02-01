/**<b>Definition:</b> <i>Withinday replanning</i> means that agents can replan while they are on their way, technically while the mobsim is running.
 * <p/>
 * At this point, there are at least two approaches:<ul>
 * <li> One approach, put forward by Christoph Dobler, which takes existing agents with plans and modifies future parts of their plans.
 * <li> Another approach, put forward by Kai Nagel and Michael Zilske, which re-programms the agent from the inside out.
 * </ul>
 * In the end, each approach will be able to emulate the other one, so it may come down to a matter of taste.
 * </p>
 * This package contains the <i>second</i> variant.  <i>The other one is described in another tutorial package.</i>
 * </p>
 * At this point, all code is contained in a single file.  The Main class is necessary to plug everything together and run it.  The MyAgent class is an
 * example for an agent that randomly moves around in the system.  It should be enough to explain the general functionality.
 * See {@link tutorial.programming.ownMobsimAgentWithPerception} and {@link tutorial.programming.ownMobsimAgentUsingRouter} for
 * more complete examples.
 * </p>
 * Please address yourself to Kai Nagel or Michael Zilske for support.
 */
package tutorial.programming.withinDayReplanningAgents;

// attaching the above comment to a class would give us WYSIWYG in eclipse.  It means, however, that it does not display any
// more in the matsim javadoc browser.  kai, mar'11