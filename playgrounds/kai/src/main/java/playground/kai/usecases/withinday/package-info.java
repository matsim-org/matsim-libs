
/**This package contains code that demonstrates some <i> ideas </i> of how to use matsim for withinday replanning.  
 * <p/>
 * <b>Definition:</b> Withinday replanning means that agents can replan while they are on their way, technically while the mobsim is running.
 * <p/>
 * <b>Why we are where we are:</b>Historically, matsim was (and still is) written with large-scale performance as its 
 * prime technological goal, which in practice
 * means the capability to run for regions with 10 million inhabitants or more.  This has meant a couple of design decisions that
 * are, in fact, detrimental to within-day replanning.  Examples for this are the "horizontal" way in which strategy modules are 
 * run ("first run strategy module 1 for all agents, then strategy  module 2, etc.", rather than "for every agent, run the necessary
 * strategy modules), but also the multiple layers and caches of agent structure in the mobsim which essentially assume that a 
 * plan does not change while the mobsim is running.
 * 
 * <b>This package contains (as of dec'10):</b><ul>
 * <li>EquilTest (very short), which contains the main method.
 * <li>MyControlerListener (very short), which tells the Controler at the right time in the initaliziation sequence to use a certain
 * setup of the mobsim.
 * <li>MyMobsimFactory (still very short), which defines that mobsim setup.
 * <li>WithinDayMobsimListener, which contains the "meat", i.e. the definitions how the within-day replanning should function.
 * This is a "stub" method, i.e. it attempts to define access to the agents for different states in which the agents could be, but 
 * it does not do anything else.  <i>It is also not tested</i> (i.e. it should be seen as a structural design suggestion, not a 
 * final solution).
 * <li>WithinDayMobsimListener2, which contains a concrete implementation for withinday replanning of vehicle drivers.  
 * This is a bare boned version of Christoph's code, still in the playground, but soon moving to be a withinday replanning package
 * for matsim.  Christoph's code will contain more elaborate examples and configuration options.
 * </ul>
 * 
 */

package playground.kai.usecases.withinday;
