/**
 * THis package contains the classes corresponding to the "old"
 * routing approach (ie where a router only handled individual legs).
 * They are still provided for backward compatibility reasons,
 * but should disappear sooner or later.
 * Use the new "trip-based" class if you implement new stuff.
 * @author thibautd
 *
 *
 * @author pieterfourie: copied here because of ultimate dependency on several casts to @link PopulationFactoryImpl,
 * as well as all the classes' constructors now being non-public.
 * currently the plan is to keep distribted simulation compliant with matsim as far as possible, so
 * not using the trip-based classes for now, until matsim does.
 */
package playground.pieter.distributed.plans.router.old;