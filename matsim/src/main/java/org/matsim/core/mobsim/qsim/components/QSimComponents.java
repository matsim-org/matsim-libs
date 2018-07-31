package org.matsim.core.mobsim.qsim.components;

import java.util.LinkedList;
import java.util.List;

/**
 * Contains information about which QSim components should be used in the
 * simulation and in which order they are registered with the QSim.
 */
final public class QSimComponents {
	final public List<String> activeMobsimEngines = new LinkedList<>();
	final public List<String> activeDepartureHandlers = new LinkedList<>();
	final public List<String> activeActivityHandlers = new LinkedList<>();
	final public List<String> activeAgentSources = new LinkedList<>();
}
