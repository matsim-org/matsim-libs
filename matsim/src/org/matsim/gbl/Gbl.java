/* *********************************************************************** *
 * project: org.matsim.*
 * Gbl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.gbl;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.config.ConfigReaderMatsimV1;
import org.matsim.counts.Counts;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;

/* some ideas about this class... [MR, dec06]
 * Currently, there are the following methods:
 * - createConfig(String[] args);
 * - getConfig();
 * - createWorld();
 * - getWorld();
 * - createFacilities();
 * - createMatrices();
 * - createCounts();
 * - reset();
 *
 * The problem: It is not clear, who is responsible to create the globals.
 * e.g.: Before, with World.getSingleton(), the world was created if needed (it
 * was a singleton after all, so there should not have been a special create-
 * Singleton() imho). Anyway, a clearer thing would be:
 * - init(String[] args);
 * - reset();
 * - getConfig();
 * - getWorld();
 * - getFacilities();
 * - getMatrices();
 * In that case, init() has to be called exactly once, and that init-method
 * takes care of creating config, world, facilities etc.
 */
/* The usage of Gbl.getConfig() all throughout the code makes it very hard
 * to debug. We would thus prefer if Gbl.getConfig() could be removed in
 * the longer term and the config-object, or single params from the config,
 * could be handed as parameters where needed. // VSP/mrieser, 11sep2007
 */

public abstract class Gbl {

	private static final long DEFAULT_RANDOM_SEED = 4711;

	private static Config config = null;
	private static World world = null;

	//////////////////////////////////////////////////////////////////////
	// project global random number generator
	//////////////////////////////////////////////////////////////////////

	// the global random number generator
	// the seed is set by the config package (see matsim.gbl.Config)
	public static final Random random = new Random(DEFAULT_RANDOM_SEED);

	private static final Logger log = Logger.getLogger(Gbl.class);

	//////////////////////////////////////////////////////////////////////
	// config creation
	//////////////////////////////////////////////////////////////////////

	public static final Config createConfig(final String[] args) {
		if (Gbl.config != null) {
			Gbl.errorMsg("config exists already! Cannot create a 2nd global config.");
		}

		Gbl.config = new Config();
		Gbl.config.addCoreModules();

		if ((args != null) && (args.length == 1)) {
			log.info("Input config file: " + args[0]);
			ConfigReaderMatsimV1 reader = new ConfigReaderMatsimV1(Gbl.config);
			reader.readFile(args[0]);
		} else if ((args != null) && (args.length >= 1)) {
			log.info("Input config file: " + args[0]);
			log.info("Input local config dtd: " + args[1]);
			ConfigReaderMatsimV1 reader = new ConfigReaderMatsimV1(Gbl.config);
			reader.readFile(args[0], args[1]);
		}

		Gbl.random.setSeed(Gbl.config.global().getRandomSeed());
		Time.setDefaultTimeFormat(Gbl.config.global().getOutputTimeFormat());

		return Gbl.config;
	}

	public static final Config getConfig() {
		return Gbl.config;
	}

	public static final void setConfig(final Config config) {
		Gbl.config = config;
	}

	//////////////////////////////////////////////////////////////////////
	// world creation
	//////////////////////////////////////////////////////////////////////

	public static final World createWorld() {
		if (Gbl.world != null) {
			Gbl.errorMsg("world exists already! Cannot create a 2nd global world.");
		}

		Gbl.world = new World();
		return Gbl.world;
	}

	public static final World getWorld() {
		if (Gbl.world == null) {
			Gbl.createWorld();
		}
		return Gbl.world;
	}

	//////////////////////////////////////////////////////////////////////
	// reset scenario
	//////////////////////////////////////////////////////////////////////

	public static final void reset() {
		log.info("Gbl.reset() -- reset config, world");
		Gbl.config = null;
		Gbl.world = null;
		Gbl.random.setSeed(DEFAULT_RANDOM_SEED);
		SimulationTimer.reset();
		Counts.reset();
		CharyparNagelScoringFunction.initialized = false; // TODO [MR] see todo-comment in BasicScoringFunction.java
	}

	// TODO [MR] this really shouldn't be here, but it had to be done quick :-(
	public static final boolean useRoadPricing() {
		return Gbl.config.global().useRoadPricing();
	}

	public static final void printMemoryUsage() {
		long totalMem = Runtime.getRuntime().totalMemory();
		long freeMem = Runtime.getRuntime().freeMemory();
		long usedMem = totalMem - freeMem;
		log.info("used RAM: " + usedMem + "B = " + (usedMem/1024) + "kB = " + (usedMem/1024/1024) + "MB" +
				"  free: " + freeMem + "B = " + (freeMem/1024/1024) + "MB  total: " + totalMem + "B = " + (totalMem/1024/1024) + "MB");
	}

	/** This method will soon be deprecated. Please start using Logger.info() for informational output.
	 * @param c
	 * @param method
	 * @param msg */
	public static final void noteMsg(final Class<?> c, final String method, final String msg) {
		System.out.println("NOTE: In " + c.getName() + "." + method + ": " + msg);
		System.out.flush();
	}

	/** This method will soon be deprecated. Please start using Logger.warn() for informational output.
	 * @param c
	 * @param method
	 * @param msg */
	public static final void warningMsg(final Class<?> c, final String method, final String msg) {
		System.err.println("WARNING: In " + c.getName() + "." + method + ": " + msg);
		System.err.flush();
	}

	public static final void errorMsg(final Exception e) {
		e.printStackTrace();
		System.exit(-1);
	}

	public static final void errorMsg(final String msg) {
		// the following is only so we get a useful stacktrace and know where the error happened.
		try {
			throw new Exception(msg);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(-1);
	}

	//////////////////////////////////////////////////////////////////////
	// time measurement
	//////////////////////////////////////////////////////////////////////

	private static long measurementStartTime = Long.MAX_VALUE;

	private static final String printTime() {
		if (Gbl.measurementStartTime == Long.MAX_VALUE) {
			log.error("Did not start measurements.");
			return "";
		}
		return printTimeDiff(System.currentTimeMillis(), Gbl.measurementStartTime);
	}

	public static final String printTimeDiff(final long later, final long earlier) {
		long elapsedTimeMillis = later - earlier;
		float elapsedTimeSec = elapsedTimeMillis/1000F;
		float elapsedTimeMin = elapsedTimeMillis/(60*1000F);
		float elapsedTimeHour = elapsedTimeMillis/(60*60*1000F);
		float elapsedTimeDay = elapsedTimeMillis/(24*60*60*1000F);

		return elapsedTimeMillis + " msecs; " +
				elapsedTimeSec    + " secs; " +
				elapsedTimeMin    + " mins; " +
				elapsedTimeHour   + " hours; " +
				elapsedTimeDay    + " days ###";
	}

	public static final void startMeasurement() {
		Gbl.measurementStartTime = System.currentTimeMillis();
	}

	public static final void printElapsedTime() {
		log.info("### elapsed time: " + Gbl.printTime());
	}

	public static final void printRoundTime() {
		log.info("### round time: " + Gbl.printTime());
		Gbl.startMeasurement();
	}
}
