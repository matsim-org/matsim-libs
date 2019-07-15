
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 /**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 * 
 * <h2>Package Maintainer:</h2>
 * <ul>
 *   <li>Marcel Rieser</li>
 * </ul>
 * 
  * <h2>Parameters:<a name="controler_parameters"></a></h2>
 * <ul>
 * 	<li><strong><font color="blue">outputDirectory</font></strong> <br>
 * 		Type and range: ... <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">firstIteration</font></strong> <br>
 * 		Type and range: integer >= 0 <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">lastIteration</font></strong> <br>
 * 		Type and range: integer >=1000 <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">routingAlgorithmType</font></strong> <br>
 * 		Type and range: ... <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">runId</font></strong> <br>
 * 		Type and range: String <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">enableLinkToLinkRouting</font></strong> <br>
 * 		Type and range: ... <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">eventsFileFormat</font></strong> <br>
 * 		Type and range: ... <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * <li><strong><font color="blue">writeEventsInterval</font></strong> <br>
 * 		Type and range: ... <br>
 * 		Default: ... <br>
 * 		Description: ...
 * 	</li>
 * </ul>
 * 
 * <h2>Details</h2>
 *
 * <h3>Conceptual Structure</h3>
 * <p>The Controler has three main parts:<ul>
 * <li>Execution &ndash; Mobility simulation</li>
 * <li>Scoring</li>
 * <li>Replanning</li>
 * </ul>
 * These three parts are repeated in a loop and build the iterations. By default, <em>Scoring</em> and <em>Replanning</em>
 * refers to {@link org.matsim.population plans}, but it could as well be done for facilities, traffic lights, etc.<br>
 *
 * The Controler offers several <em>extension points</em>, where additional functionality can be plugged in.
 * These extension points are realized with <em>Events</em> and <em>Listeners</em>:
 * Classes can implement one or more {@link org.matsim.core.controler.listener Listener Interfaces} and can be registered
 * with the Controler with {@link org.matsim.core.controler.Controler#addControlerListener(org.matsim.core.controler.listener.ControlerListener) addControlerListener()}.
 * The Controler sends {@link org.matsim.core.controler.events Controler Events} at the corresponding points during the run
 * to the registered Listeners, at which point the Listeners can execute their own code.<br>
 *
 * Currently, the following Events (and corresponding Listeners) are available:
 * <pre>
 * [The iteration loop of the Controler]
 *
 * +-----------+                +----------------+         +-----------+            +------------+
 * |Startup (1)|-->(2)--->(3)-->|Execution/MobSim|-->(4)-->|Scoring (5)|-->(6)--+-->|Shutdown (8)|
 * +-----------+       ^        +----------------+         +-----------+        |   +------------+
 *                     |                                                        v
 *                      \                    +--------------+                  /
 *                        ---------------<---|Replanning (7)|<----------(2)<---
 *                                           +--------------+
 * </pre>
 * <ul>
 * <li>(1) Startup</li>
 * <li>(2) Iteration Starts</li>
 * <li>(3) Before Mobsim</li>
 * <li>(4) After Mobsim</li>
 * <li>(5) Scoring</li>
 * <li>(6) Iteration Ends</li>
 * <li>(7) Replanning</li>
 * <li>(8) Shutdown</li>
 * </ul>
 *
 * All Events are issued when {@link org.matsim.core.controler.Controler#run()} is called.
 * When the Startup-Event is issued, the configuration as well as other data (plans, network, ...) are already
 * loaded and initialized.
 *
 * <h3>Best Practices</h3>
 * <h4>Using custom functionality</h4>
 * If you plan to write your own ControlerListener to provide additional functionality to MATSim, use the following
 * class as a starting point for integrating your ControlerListener into the Controler:
 * <pre>
 * import org.matsim.controler.Controler;
 * import org.matsim.myfunctionality.MyFunctionality;
 *
 * class MyClass {
 *   public static void main(final String[] args) {
 *     Controler controler = new Controler(args);
 *     controler.addControlerListener(new MyFunctionality());
 *     controler.run();
 *   }
 * }
 * </pre>
 *
 * <h4>Additional Configuration Parameters</h4>
 * If your additional functionality requires additional parameters in the configuration file, you can provide
 * a custom {@link org.matsim.core.config.groups Config-Group} and load it in the constructor of your ControlerListener.
 * When the configuration file will be parsed later, your config-group gets loaded with the settings from the file,
 * and you can later on access the values.
 * <pre>
 * class MyFunctionality implements StartupListener {
 *   final MyConfigGroup settings = new MyConfigGroup();
 *
 *   public MyFunctionality(final Controler controler) {
 *     controler.getConfig().addModule("my_functionality", this.settings);
 *   }
 *
 *   public void notifyStartup(final StartupEvent event) {
 *     int value = this.settings.getMyValue();
 *   }
 * }
 * </pre>
 */
package org.matsim.core.controler;
