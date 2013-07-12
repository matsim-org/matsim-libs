package org.matsim.core.router;

import org.matsim.core.api.internal.MatsimFactory;

/**
 * Creates configured {@link TripRouter} instances.
 * This interface must be implemented to implement a custom routing behaviour.
 * @author thibautd
 */
public interface TripRouterFactory extends MatsimFactory {

	/**
	 * Creates a new {@link TripRouter} instance.
	 * <p/>
	 * This method is not the usual createXxx(...) method to draw attention to the fact that it does not return an interface but a class.  The syntax is roughly
	 * <pre>
	 *   public TripRouter instantiateAndConfigureTripRouter() {
	 *      TripRouter tr = new TripRouter(...) ;
	 *      tr.setRoutingModule( modeString, routingModule ) ;
	 *      tr....(...) ;
	 *      return tr ;
	 *   }
	 * </pre>
	 * The actual router is set by routingModule of type {@link RoutingModule}; it is responsible for the leg mode described by modeString.
	 * <p/>
	 * Also see <code> tutorial.programming.example12PluggableTripRouter </code> 
	 * and <code> tutorial.programming.example13MultiStateTripRouting </code>.
	 * 
	 * @param routingContext Will contain current travel times and disutilities, to be
	 * used for routing.
	 * @return a fully initialised {@link TripRouter}.
	 */
	TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext);

}
