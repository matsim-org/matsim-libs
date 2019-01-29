/**
 * This package defines default <i>strategies</i> useful for joint decisions.
 * Though <i>modules</i> are defined in the feature-specific packages,
 * strategies are not, because they <i>need</i> to consider all enabled features.
 * To avoid multiplying strategies, those default strategies are made aware of
 * all provided features (joint trips, joint activities, shared vehicles...).
 * @author thibautd
 */
package org.matsim.contrib.socnetsim.usage.replanning.strategies;