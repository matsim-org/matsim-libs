package org.matsim.core.router;

/**
 * Interface that exists so that implementations of this interface can be replaced via injection.
 */
public interface FallbackRoutingModule extends RoutingModule {
	// (The main reason for addressing this via injection is that even the default implementation of FallbackRoutingModule needs access to material that is
	// not available in TripRouter.  kai, aug'19)
}
