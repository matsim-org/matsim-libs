/** 
 */
/**
 * @author balac
 *
 * <h2>Usage</h2>
 * An example input files can be found in the input folder of the test folder for carsharing. 
 * <p>
 * Main method of the RunCarsharing class takes only the config file as an input in order to run the carsharing module.
 * <p>
 * 
 * <h2>Input files</h2>
 * 
 *  Necessary input files include:
 *  <ul>
 *  <li>Location of stations (or vehicles), per company. Example can be seen in CarsharingStations.xml </li>
 *  <li>Membership information per person, per company per carsharing type. Example can be seen in CSMembership.xml </li>
 *  <li>Config file where we set up the paths to the above mentioned input files and whether or not specific carsharing options are used.
 *  <p> Additionally in the config file we have to specify carsharing specific replanning strategies. An example can be seen in config.xml. </li>
 *  </ul>
 *<p>
 * Models infrastcture along with examples are located in models package of the carsharing contrib.
 * <p>
 * CostsCalculator which is later used in the scoring is located in the manager.supply.costs package along with an example.
 * <p>
 * In order to implement your own cost structure one needs to implement CostCalculation interface and to define cost structures for each operator(company) and each carsharing option.
 *  <p>
 *  In CarsharingUtils class in the runExample package one can see an example of defining cost structures for different companies and carsharing types.
 *  
 *  <h2>Output</h2>
 *  Output files are located in each iteration folder and contain all the necessary information about all the carsharing rentals during the iteration.
 *  
 *  
 */
package org.matsim.contrib.carsharing.runExample;