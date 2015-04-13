/**
 * <h2>Model building</h2>
 * 
 * The classes of this package can be used to build a signal system model. The creation of 
 * a model is task of a SignalSystemsModelBuilder. The default builder, the FromDataBuilder, creates the model
 * based on the data of the <code>org.matsim.contrib.signals.data</code> package and its public methods can be used if certain
 * parts of a custom model shall be used based on a partially data based implementation.
 * <p>
 * The default builder uses a SignalModelFactory that can be exchanged if certain
 * model components should be created by the use of the data classes but with an
 * extended behaviour.
 * </p>
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Dominik Grether</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 * @author dgrether
 */
package org.matsim.contrib.signals.builder;