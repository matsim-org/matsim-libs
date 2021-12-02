package org.matsim.contrib.drt.extension.alonso_mora.algorithm.graphs;

import java.util.Collection;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;

/**
 * Represents a shareability graph between requests
 * 
 * @author sebhoerl
 */
public interface RequestGraph {

	void addRequest(AlonsoMoraRequest request, double now);

	Collection<AlonsoMoraRequest> getShareableRequests(AlonsoMoraRequest request);

	int getSize();

}