package org.matsim.contrib.profiling.events;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Record Mobsim execution duration as a JFR profiling {@link Event}.
 */
@Label("Mobsim duration")
@Description("To record the duration of a mobsim iteration")
@Category("MATSim")
public class JFRMobsimEvent extends Event {}
