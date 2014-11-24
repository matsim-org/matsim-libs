/**
 * 
 * Package that takes demand and infrastructure (roads, ...) as input and runs an adaptive "minibus" model to serve that demand.
 * The resulting minibus lines can for example be used as follows:<ul>
 * <li> As a starting point to construct a schedule for formal public transit.
 * <li> As paratransit supply for scenarios where no information about paratransit is available.
 * </ul>
 * 
 * Some publications (list not regularly updated):<ul>
 * 
 * <li> Ph.D. dissertation of Andreas Neumann, pdf-version available at <a href="http://nbn-resolving.de/urn/resolver.pl?urn:nbn:de:kobv:83-opus4-53866"> UB, TU Berlin </a>
 * <li> Towards a simulation of minibuses in South Africa, preprint available from <a href="https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2014/14-03/"> VSP, TU Berlin </a>
 * </ul>
 * 
 * Look into {@link org.matsim.contrib.minibus.PMain} to get started.
 * <p/>
 * 
 * Quickstart from command line (without eclipse):<ul>
 * <li> Create an empty directory and cd into it.
 * <li> Download MATSim-rXXXXX.jar from http://matsim.org/files/builds/ into that directory.
 * <li> Download MATSim-libs.zip from ... into that directory and unzip.
 * <li> Download minibus...zip from ... into that directory and unzip.
 * <li> Get test scenario from ... .
 * <li> Type
 * <code> java -Xmx2000m -cp MATSim-rXXXXX.jar:minibus-0.X.0-SNAPSHOT-rXXXXX.jar playground.andreas.P2.PMain config.xml
 * </code>
 * </ul>
 *
 * @author aneumann
 */
package org.matsim.contrib.minibus;
