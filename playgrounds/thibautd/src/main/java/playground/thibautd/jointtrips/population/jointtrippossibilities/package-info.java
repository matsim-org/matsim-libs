/**
 * Defines interfaces and default implementation to identify all possible
 * joint trips for a plan, even the ones which are not planned.
 *
 * Implementations of the interfaces should be immutable: the same instance
 * is referenced can be referenced by several plans. Modifications could thus
 * have strange side-effects.
 *
 * @author thibautd
 */
package playground.thibautd.jointtrips.population.jointtrippossibilities;
