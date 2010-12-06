/**
 * <p>
 * This package contains matsim tutorial files.  In theory, they should be described in "Getting started with MATSim".  In practice, the files here
 * have a tendency to be more modern than the descriptions in the tutorial.  Here is some help:
 * <ul>
 * <li> "config" contains files illustrating how to use matsim by modifying the (xml-) config files only. </li>
 * <li> "demandgeneration" contains examples on how to generate an initial demand (initial population file) for matsim.  There are many ways of doing this; the examples here 
 * only show a very small number of methods. </li>
 * <li> "programming" illustrates how one can extend matsim in useful ways, while remaining minimally invasive. </li>
 * <li> "old" contains the older tutorials.  They should probably not be used any more, since they are non minimally invasive.</li>
 * </ul>
 * 
 * <p>Regarding the "old" directory:</p>
<p>This package contains the classes referenced in the MATSim Tutorial "Getting started with MATSim".</p>
<p>{@link tutorial.old.example1.MyControler1} introduced reading of network and plans, writing of events and running the mobility simulation a single time.</p>
<p>{@link tutorial.old.example2.MyControler2} adds visualizer output.</p>
<p>{@link tutorial.old.example3.MyControler3} makes use of a configuration file instead of hard-coding all settings.</p>
<p>{@link tutorial.old.example4.MyControler4} calculates the score of the individual plans as well as the average score.</p>
<p>{@link tutorial.old.example5.MyControler5} adds an iteration loop and re-planning to create a real agent-based simulation with agent learning.</p>
*/
package playground.kai.usecases.withinday;
