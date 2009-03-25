/**
 * This package and its subpackages contain the MATSim basic classes and 
 * interfaces that are not ready for publishing in the org.matsim.interfaces
 * packages.
 * The interfaces and classes placed here are designed by the following principles:
 * <ul>
 *   <li>There is an interface for each class </li>
 *   <li>Interfaces are named BasicSomething, while the implementation has the name BasicSomethingImpl</li>
 *   <li>For each toplevel xml definition there is a container type, e.g. network_v1.dtd has the container 
 *   BasicNetwork</li> 
 *   <li>The types define only those attributes that are specified by the xml grammar, the resulting objects
 *   are purely used for data storage and have no state except the values of their attributes.</li>
 * </ul>
 * 
 * @see org.matism.basic
 * @see org.matsim.interfaces
 */
package org.matsim.core.basic;
