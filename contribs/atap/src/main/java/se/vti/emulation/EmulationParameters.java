/**
 * se.vti.emulation
 * 
 * Copyright (C) 2023, 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
 * Partially based on Sebastian Hörl's IER.
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.emulation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import se.vti.emulation.emulators.ActivityEmulator;
import se.vti.emulation.emulators.LegEmulator;
import se.vti.emulation.emulators.NetworkLegEmulator;
import se.vti.emulation.emulators.OnlyDepartureArrivalLegEmulator;
import se.vti.emulation.emulators.OnlyStartEndActivityEmulator;
import se.vti.emulation.handlers.EmulationHandler;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class EmulationParameters {

	// -------------------- CONSTANTS --------------------

	private static final Logger logger = LogManager.getLogger(EmulationParameters.class);

	public static final String DEFAULT = "default";

	// -------------------- MEMBERS --------------------

	private final Map<String, Class<? extends ActivityEmulator>> actType2emulator = new LinkedHashMap<>();

	private final Map<String, Class<? extends LegEmulator>> mode2emulator = new LinkedHashMap<>();

	private final Set<Class<? extends EmulationHandler>> handlers = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public EmulationParameters() {
		this.setActivityEmulator(DEFAULT, OnlyStartEndActivityEmulator.class);
		this.setEmulator(TransportMode.car, NetworkLegEmulator.class);
		this.setEmulator(DEFAULT, OnlyDepartureArrivalLegEmulator.class);
	}

	// -------------------- SETTERS --------------------

	public void setActivityEmulator(String type, Class<? extends ActivityEmulator> clazz) {
		logger.info("Emulator for activity type " + type + " is of type " + clazz.getSimpleName());
		this.actType2emulator.put(type, clazz);
	}

	public void setEmulator(String mode, Class<? extends LegEmulator> clazz) {
		logger.info("Emulator for mode " + mode + " is of type " + clazz.getSimpleName());
		this.mode2emulator.put(mode, clazz);
	}

	public void addHandler(Class<? extends EmulationHandler> clazz) {
		logger.info("Added EmulationHandler of type " + clazz.getSimpleName());
		this.handlers.add(clazz);
	}

	// -------------------- GETTERS --------------------

	public Map<String, Class<? extends ActivityEmulator>> getActType2emulatorView() {
		return Collections.unmodifiableMap(this.actType2emulator);
	}

	public Map<String, Class<? extends LegEmulator>> getMode2emulatorView() {
		return Collections.unmodifiableMap(this.mode2emulator);
	}

	public Set<Class<? extends EmulationHandler>> getHandlerView() {
		return Collections.unmodifiableSet(this.handlers);
	}

}
