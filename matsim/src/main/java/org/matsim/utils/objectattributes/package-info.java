/**
 * Supports keeping track of arbitrary attributes for identifiable objects.
 *
 * An example usage can be seen in {@link org.matsim.utils.objectattributes.RunObjectAttributesExample}.
 * 
 * <h2>Design thoughts</h2>
 * I think of ObjectAttributes (OA) as a way to provide specific additional data
 * to algorithms and modules. Typically, specialized algorithms/modules might require
 * additional data than what is available by default in MATSim's data container
 * (like population, network). For example, modules could need information about
 * persons' income, a node's z-coordinate, or a link's curviness.
 * <p/>
 * Important to consider is that the algorithm always has to specify what additional
 * data it needs so it can access it. In the example above, an algorithm that needs
 * a person's income needs to know the name of the attribute containing the income.
 * Typically, it could do so by providing a setter <code>setPersonIncomeAttributeName(String)</code>
 * or even exposing it as a config-parameter.
 * <p/>
 * This implies that modules should always just access a few specific attributes, but
 * they should never have the need to just access all attributes and then maybe figure
 * out what they could do with the available attributes, probably even guessing some 
 * interpretation of the attributes based on their names. <strike>For this reason, there is
 * no method available directly in {@link ObjectAttributes} that returns all attributes
 * for a specific object. Only getters for single, named attributes are providers.
 * <!-- I don't think that this statement is true any more.  kai, nov'13 --></strike>
 */
package org.matsim.utils.objectattributes;
