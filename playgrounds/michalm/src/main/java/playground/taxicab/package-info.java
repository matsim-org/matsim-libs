/**This package contains code that demonstrates some <i> ideas </i> of how to use matsim for withinday replanning.  
 * <p/>
 * <b>Definition:</b> Withinday replanning means that agents can replan while they are on their way, technically while the mobsim is running.
 * <p/>
 * <b>Why we are where we are:</b> Historically, matsim was (and still is) written with large-scale performance (while remaining 
 * microscopic) as its 
 * prime technological goal, which in practice
 * means the capability to simulate regions with 10 million inhabitants or more.  This has meant a couple of design decisions that
 * are, in fact, detrimental to within-day replanning.  Examples for this are the "horizontal" way in which strategy modules are 
 * run ("first run strategy module 1 for all agents, then strategy  module 2, etc.", rather than "for every agent, run the necessary
 * strategy modules"), but also the multiple layers and caches of agent structure in the mobsim which essentially assume that a 
 * plan does not change while the mobsim is running.
 * <p/>
 * <b>This package contains (as of dec'10):</b><ul>
 * <li>EquilTest (very short), which contains the main method.
 * <li>MyControlerListener (very short), which tells the Control(l)er at the right time in the initialization sequence to use a certain
 * setup of the mobsim.
 * <li>MyMobsimFactory (short), which defines that mobsim setup.
 * <li>WithinDayMobsimListener, which contains the "meat", i.e. the definitions how the within-day replanning should function.
 * This is a "stub" method, i.e. it attempts to define access to the agents for different states in which the agents could be, but 
 * it does not do anything else.  <i>It is also not tested</i> (i.e. it should be seen as a structural design suggestion, not a 
 * final solution).
 * <li>WithinDayMobsimListener2, which contains a concrete implementation for withinday replanning of vehicle drivers.  
 * example60... contains more material, but extending the Controler which I personally would avoid (kn).
 * </ul>
 * <p/>
 * Much of this is using material that is not in the public api, so there are only two recommended ways of using this:<ul>
 * <li> use a release and go from there, but be aware that your code will work <i>only</i> with that release
 * <li> find a way to collaborate with the matsim team.  This will, however, mean that there needs to be (possibly scientific)
 * funding for the matsim team
 * </ul>
 * <p/>
 * Again: This package contains <i>ideas</i>, not some version that is meant to be working.  Comments are welcome, please address
 * to nagel (or to the developers' email list).
 * 
 */
package playground.taxicab;

// attaching the above comment to a class would give us WYSIWYG in eclipse.  It means, however, that it does not display any
// more in the matsim javadoc browser.  kai, mar'11