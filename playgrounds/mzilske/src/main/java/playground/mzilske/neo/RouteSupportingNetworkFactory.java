package playground.mzilske.neo;

import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Route;

public interface RouteSupportingNetworkFactory extends NetworkFactory {

	Route createRoute();

}
