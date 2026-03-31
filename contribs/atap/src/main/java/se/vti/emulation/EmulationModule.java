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

import java.util.Map;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import se.vti.emulation.emulators.ActivityEmulator;
import se.vti.emulation.emulators.LegEmulator;
import se.vti.emulation.emulators.PlanEmulator;
import se.vti.emulation.handlers.EmulationHandler;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class EmulationModule extends AbstractModule {

	private final EmulationParameters emulationParameters;

	public EmulationModule(final EmulationParameters emulationParameters) {
		this.emulationParameters = emulationParameters;
	}

	@Override
	public void install() {

		bind(EmulationEngine.class);
		bind(PlanEmulator.class);

		{
			final MapBinder<String, ActivityEmulator> mapBinder = MapBinder.newMapBinder(super.binder(),
					new TypeLiteral<String>() {
					}, new TypeLiteral<ActivityEmulator>() {
					});
			for (Map.Entry<String, Class<? extends ActivityEmulator>> entry : this.emulationParameters
					.getActType2emulatorView().entrySet()) {
				mapBinder.addBinding(entry.getKey()).to(entry.getValue());
			}
		}
		{
			final MapBinder<String, LegEmulator> mapBinder = MapBinder.newMapBinder(super.binder(),
					new TypeLiteral<String>() {
					}, new TypeLiteral<LegEmulator>() {
					});
			for (Map.Entry<String, Class<? extends LegEmulator>> entry : this.emulationParameters.getMode2emulatorView()
					.entrySet()) {
				mapBinder.addBinding(entry.getKey()).to(entry.getValue());
			}
		}
		{
			final Multibinder<EmulationHandler> setBinder = Multibinder.newSetBinder(super.binder(),
					EmulationHandler.class);
			for (Class<? extends EmulationHandler> handlerClass : this.emulationParameters.getHandlerView()) {
				setBinder.addBinding().to(handlerClass);
			}
		}
	}
}
