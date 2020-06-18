
# Signals

The MATSim contrib for traffic lights is able to simulate traffic signals microscopically. 
  
For fixed-time control a default implementation is provided. Because of the modular structure of MATSim and the signals extension, own signal control approaches (e.g. for traffic-responsive signal control methods) can be easily included. 
Some implementations of traffic-responsive signal control approaches are also provided by this contrib. 
It is possible to use different signal control methods for different intersections in MATSim (e.g. some fixed-time, some adaptive).

Necessary input data can either be provided by specifying the corresponding input files in the config or by adding the signal elements directly into the scenario via code. For both ways, examples are given in the code examples package in this contrib (see above). 

To help you to understand xml formats and code better, here some translations between MATSim terms and real world terms: 
- Signal: Traffic light, traffic signal, i.e. a physical box standing somewhere on the transport network indicating driving allowed/permited
- Signal Group: Logical group of traffic lights, all lights display same color at the same time
- Signal Control: Algorithm or control scheme that determines which colors are displayed by the different Signal Groups (e.g. fixed-time control)
- Signal System: Collection of Signal Groups that is controlled by the same Signal Control (e.g. an intersection)
 
As a starting point please have a look at the code examples in this contrib (see above: codeexamples) and the code examples in the matsim-code-examples project.
The best starting point is the class RunSignalSystemsExample in the package fixedTimeSignals. 

If you want to visualize the created scenario try to get and run the OTFVis contribution to MATSim, see OTFVis (this contribution is unsupported). An example is also given in the code examples package: VisualizeSignalScenario. 

For more information and documentation see the javadoc in the subpackage org.matsim.contrib.signals. 









  