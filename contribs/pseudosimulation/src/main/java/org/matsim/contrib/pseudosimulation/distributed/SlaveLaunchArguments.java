package org.matsim.contrib.pseudosimulation.distributed;

/** Raw command-line values used to start a distributed PSim slave. */
record SlaveLaunchArguments(String configFile, String hostname, String port, String threads) {
}
