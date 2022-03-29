/* *********************************************************************** *
 * project: org.matsim.*
 * SingleHandlerEventsManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.misc.ClassUtils;

/**
 * Implementation of an EventsManager that serves exactly one EventHandler.
 * Events that are not handled by that handler are ignored.
 * 
 * @author cdobler
 */
public final class SingleHandlerEventsManager implements EventsManager {

	private static final Logger log = Logger.getLogger(SingleHandlerEventsManager.class);
	
	/*
	 * This cannot be just a map<Class, Method> since we need to differentiate between
	 * a) Class is handled the first time, therefore we have to check whether the Handler can handle it (no HandlerInfo object)
	 * b) Class cannot be handled (HandlerInfo with empty Method field)
	 */
	private final Map<Class<?>, HandlerInfo> methodToHandle = new HashMap<Class<?>, HandlerInfo>();

	private final EventHandler eventHandler;
	
	private final boolean isLeaveLinkHandler;
	private final boolean isLinkEnterHandler; 
	private final boolean isWait2LinkHandler;
	private final boolean isPersonArrivalHandler;
	private final boolean isPersonDepatureHandler;
	private final boolean isActivityEndHandler;
	private final boolean isActivityStartHandler;
	private final boolean isTeleportationArrivalHandler;
	private final boolean isTransitDriverStartsHandler;
	private final boolean isPersonStuckHandler;
	private final boolean isPersonMoneyHandler;
	private final boolean isPersonScoreHandler;
	private final boolean isAgentWaitingForPtHandler;
	private final boolean isPersonEntersVehicleHandler;
	private final boolean isPersonLeavesVehicleHandler;
	private final boolean isVehicleDepartsAtFacilityHandler;
	private final boolean isVehicleArrivesAtFacilityHandler;
	private final boolean isVehicleLeavesTrafficHandler;
	private final boolean isVehicleAbortsHandler;
	private final boolean isBasicEventHandler;
	
	private long counter = 0;
	private long nextCounterMsg = 1;

	private boolean isActive = true;
	
	public SingleHandlerEventsManager(EventHandler eventHandler) {
		this.eventHandler = eventHandler;

		this.isLeaveLinkHandler = this.eventHandler instanceof LinkLeaveEventHandler;
		this.isLinkEnterHandler = this.eventHandler instanceof LinkEnterEventHandler;
		this.isWait2LinkHandler = this.eventHandler instanceof VehicleEntersTrafficEventHandler;
		this.isPersonArrivalHandler = this.eventHandler instanceof PersonArrivalEventHandler;
		this.isPersonDepatureHandler = this.eventHandler instanceof PersonDepartureEventHandler;
		this.isActivityEndHandler = this.eventHandler instanceof ActivityEndEventHandler;
		this.isActivityStartHandler = this.eventHandler instanceof ActivityStartEventHandler;
		this.isTeleportationArrivalHandler = this.eventHandler instanceof TeleportationArrivalEventHandler;
		this.isTransitDriverStartsHandler = this.eventHandler instanceof TransitDriverStartsEventHandler;
		this.isPersonStuckHandler = this.eventHandler instanceof PersonStuckEventHandler;
		this.isPersonMoneyHandler = this.eventHandler instanceof PersonMoneyEventHandler;
		this.isPersonScoreHandler = this.eventHandler instanceof PersonScoreEventHandler;
		this.isAgentWaitingForPtHandler = this.eventHandler instanceof AgentWaitingForPtEventHandler;
		this.isPersonEntersVehicleHandler = this.eventHandler instanceof PersonEntersVehicleEventHandler;
		this.isPersonLeavesVehicleHandler = this.eventHandler instanceof PersonLeavesVehicleEventHandler;
		this.isVehicleDepartsAtFacilityHandler = this.eventHandler instanceof VehicleDepartsAtFacilityEventHandler;
		this.isVehicleArrivesAtFacilityHandler = this.eventHandler instanceof VehicleArrivesAtFacilityEventHandler;
		this.isVehicleLeavesTrafficHandler = this.eventHandler instanceof VehicleLeavesTrafficEventHandler;
		this.isVehicleAbortsHandler = this.eventHandler instanceof VehicleAbortsEventHandler;
		this.isBasicEventHandler = this.eventHandler instanceof BasicEventHandler;

		// identify the implemented Handler Interfaces
		Set<Class<?>> addedHandlers = new HashSet<Class<?>>();
		Class<?> test = eventHandler.getClass();
		log.info("adding Event-Handler: " + test.getName());
		while (test != Object.class) {
			for (Class<?> theInterface: test.getInterfaces()) {
				if (!addedHandlers.contains(theInterface)) {
					log.info("  " + theInterface.getName());
					addHandlerInterfaces(theInterface);
					addedHandlers.add(theInterface);
				}
			}
			test = test.getSuperclass();
		}
		log.info("");
	}
	
	static private class HandlerInfo {
		protected final Method method;
		protected HandlerInfo(final Method method) {
			this.method = method;
		}
	}

	public void deactivate() {
		this.isActive = false;
	}
	
	@Override
	public void processEvent(final Event event) {
		
		if (!this.isActive) return;
		
		this.counter++;
		if (this.counter == this.nextCounterMsg) {
			this.nextCounterMsg *= 4;
			log.info(" event # " + this.counter);
		}
		computeEvent(event);
	}

	@Override
	public void addHandler(final EventHandler handler) {
		throw new UnsupportedOperationException("This implementation supports only a single EventHandler which "
				+ "has to be provided upon creation. Aborting!");
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		throw new UnsupportedOperationException("This implementation supports only a single EventHandler which "
				+ "has to be provided upon creation. Aborting!");
	}

	@Override
	public void resetHandlers(final int iteration) {
		log.info("resetting Event-Handler");
		this.counter = 0;
		this.nextCounterMsg = 1;
		this.eventHandler.reset(iteration);
	}

	@Override
	public void initProcessing() {
		// nothing to do in this implementation
	}

	@Override
	public void afterSimStep(double time) {
		// Maybe we can move stuff from the ParallelEventsManager here?
	}

	@Override
	public void finishProcessing() {
		// nothing to do in this implementation
	}

	public EventHandler getEventHandler() {
		return this.eventHandler;
	}
	
	public String getEventHandlerClassName() {
		return this.eventHandler.getClass().toString();
	}
	
	private void computeEvent(final Event event) {
		if (callHandlerFast(event)) return;
		try {
			Method method = this.getHandlersForClass(event.getClass());
			if (method != null) method.invoke(this.eventHandler, event);
		} catch(InvocationTargetException e) {
			throw new RuntimeException("problem invoking EventHandler " + this.eventHandler.getClass().getCanonicalName() + " for event-class " + event.getClass().getCanonicalName(), e.getTargetException());
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("problem invoking EventHandler " + this.eventHandler.getClass().getCanonicalName() + " for event-class " + event.getClass().getCanonicalName(), e);
		}
	}
	
	private Method getHandlersForClass(final Class<?> eventClass) {
		Class<?> klass = eventClass;
		
		HandlerInfo info = this.methodToHandle.get(eventClass);
		if (info != null) return info.method;

		Method method = null;
		
		// first search in class-hierarchy
		while (klass != Object.class) {
			info = this.methodToHandle.get(klass);
			if (info != null) {
				method = info.method;
				break;
			}
			klass = klass.getSuperclass();
		}
		
		// second search in implemented interfaces if no method was found yet
		if (method == null) {
			for (Class<?> intfc : ClassUtils.getAllInterfaces(eventClass )) {
				info = this.methodToHandle.get(intfc);
				if (info != null) {
					method = info.method;
					break;
				}
			}			
		}

		info = new HandlerInfo(method);
		this.methodToHandle.put(eventClass, new HandlerInfo(info.method));
		
		return method;
	}
	
	private void addHandlerInterfaces(final Class<?> handlerClass) {
		Method[] classmethods = handlerClass.getMethods();
		for (Method method : classmethods) {
			if (method.getName().equals("handleEvent")) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 1) {
					Class<?> eventClass = params[0];
					log.info("    > " + eventClass.getName());
					if (!this.methodToHandle.containsKey(eventClass)) {
						HandlerInfo info = new HandlerInfo(method);
						this.methodToHandle.put(eventClass, info);
					}
				}
			}
		}
	}

	// this method is purely for performance reasons and need not be implemented
	private boolean callHandlerFast(final Event ev) {
		boolean ret = false;
		Class<?> klass = ev.getClass(); 
		if (this.isBasicEventHandler) {
			((BasicEventHandler) this.eventHandler).handleEvent(ev);
			ret = true;
		}
		if (this.isLeaveLinkHandler && klass == LinkLeaveEvent.class) {
			((LinkLeaveEventHandler) this.eventHandler).handleEvent((LinkLeaveEvent)ev);
			return true;
		}
		if (this.isLinkEnterHandler && klass == LinkEnterEvent.class) {
			((LinkEnterEventHandler) this.eventHandler).handleEvent((LinkEnterEvent)ev);
			return true;
		}
		if (this.isWait2LinkHandler && klass == VehicleEntersTrafficEvent.class) {
			((VehicleEntersTrafficEventHandler) this.eventHandler).handleEvent((VehicleEntersTrafficEvent)ev);
			return true;
		}
		if (this.isPersonArrivalHandler && klass == PersonArrivalEvent.class) {
			((PersonArrivalEventHandler) this.eventHandler).handleEvent((PersonArrivalEvent)ev);
			return true;
		}
		if (this.isPersonDepatureHandler && klass == PersonDepartureEvent.class) {
			((PersonDepartureEventHandler) this.eventHandler).handleEvent((PersonDepartureEvent)ev);
			return true;
		}
		if (this.isActivityEndHandler && klass == ActivityEndEvent.class) {
			((ActivityEndEventHandler) this.eventHandler).handleEvent((ActivityEndEvent)ev);
			return true;
		}
		if (this.isActivityStartHandler && klass == ActivityStartEvent.class) {
			((ActivityStartEventHandler) this.eventHandler).handleEvent((ActivityStartEvent)ev);
			return true;
		}
		if (this.isTeleportationArrivalHandler && klass == TeleportationArrivalEvent.class) {
			((TeleportationArrivalEventHandler) this.eventHandler).handleEvent((TeleportationArrivalEvent)ev);
			return true;
		}
		if (this.isTransitDriverStartsHandler && klass == TransitDriverStartsEvent.class) {
			((TransitDriverStartsEventHandler) this.eventHandler).handleEvent((TransitDriverStartsEvent) ev);
			return true;
		}
		if (this.isPersonStuckHandler && klass == PersonStuckEvent.class) {
			((PersonStuckEventHandler) this.eventHandler).handleEvent((PersonStuckEvent)ev);
			return true;
		}
		if (this.isPersonMoneyHandler && klass == PersonMoneyEvent.class) {
			((PersonMoneyEventHandler) this.eventHandler).handleEvent((PersonMoneyEvent)ev);
			return true;
		}
		if (this.isPersonScoreHandler && klass == PersonScoreEvent.class) {
			((PersonScoreEventHandler) this.eventHandler).handleEvent((PersonScoreEvent)ev);
			return true;
		}
		if (this.isAgentWaitingForPtHandler && klass == AgentWaitingForPtEvent.class) {
			((AgentWaitingForPtEventHandler) this.eventHandler).handleEvent((AgentWaitingForPtEvent)ev);
			return true;
		}
		if (this.isPersonEntersVehicleHandler && klass == PersonEntersVehicleEvent.class) {
			((PersonEntersVehicleEventHandler) this.eventHandler).handleEvent((PersonEntersVehicleEvent)ev);
			return true;
		}
		if (this.isPersonLeavesVehicleHandler && klass == PersonLeavesVehicleEvent.class) {
			((PersonLeavesVehicleEventHandler) this.eventHandler).handleEvent((PersonLeavesVehicleEvent)ev);
			return true;
		}
		if (this.isVehicleDepartsAtFacilityHandler && klass == VehicleDepartsAtFacilityEvent.class) {
			((VehicleDepartsAtFacilityEventHandler) this.eventHandler).handleEvent((VehicleDepartsAtFacilityEvent) ev);
			return true;
		}
		if (this.isVehicleArrivesAtFacilityHandler && klass == VehicleArrivesAtFacilityEvent.class) {
			((VehicleArrivesAtFacilityEventHandler) this.eventHandler).handleEvent((VehicleArrivesAtFacilityEvent) ev);
			return true;
		}
		if (this.isVehicleLeavesTrafficHandler && klass == VehicleLeavesTrafficEvent.class) {
			((VehicleLeavesTrafficEventHandler) this.eventHandler).handleEvent((VehicleLeavesTrafficEvent) ev);
			return true;
		}
		if (this.isVehicleAbortsHandler && klass == VehicleAbortsEvent.class) {
			((VehicleAbortsEventHandler) this.eventHandler).handleEvent((VehicleAbortsEvent) ev);
			return true;
		}
		return ret;
	}
}
