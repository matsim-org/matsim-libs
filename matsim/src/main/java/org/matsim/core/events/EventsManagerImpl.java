/* *********************************************************************** *
 * project: org.matsim.*
 * EventsManagerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EventHandling
 * <ol>
 * <li>Create a new class MyEventClass extends Event</li>
 * <li>Create a new interface MyEventHandlerI extends EventHandler</li>
 * <li>add method public void handleEvent(MyEvent event) to it</li>
 * <li>ready to go, just implement the interface somewhere and add a
 * HandlerObject with a call to <code>Events.addHandler(HandlerObject)</code></li>
 * <li>(optional) add an appropriate line in callHandlerFast() for speeding
 * up execution!</li>
 * </ol>
 *
 * @author dstrippgen
 * @author mrieser
 */
public final class EventsManagerImpl implements EventsManager {

	private static final Logger log = Logger.getLogger(EventsManagerImpl.class);

	static private class HandlerData {

		protected Class<?> eventklass;
		protected ArrayList<EventHandler> handlerList = new ArrayList<>(5);
		protected Method method;
		HandlerData(final Class<?> eventklass, final Method method) {
			this.eventklass = eventklass;
			this.method = method;
		}
		protected void removeHandler(final EventHandler handler) {
			this.handlerList.remove(handler);
		}
	}

	static private class HandlerInfo {
		final Class<?> eventClass;
		final EventHandler eventHandler;
		protected final Method method;

		HandlerInfo(final Class<?> eventClass, final EventHandler eventHandler, final Method method) {
			this.eventClass = eventClass;
			this.eventHandler = eventHandler;
			this.method = method;
		}
	}

	private final List<HandlerData> handlerData = new ArrayList<>();

	private final Map<Class<?>, HandlerInfo[]> cacheHandlers = new ConcurrentHashMap<>(15);

	private long counter = 0;
	private long nextCounterMsg = 1;
	private int iteration = 0;

	private HandlerData findHandler(final Class<?> evklass) {
		for (HandlerData handler : this.handlerData) {
			if (handler.eventklass == evklass) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public void processEvent(final Event event) {
		this.counter++;
		if (this.counter == this.nextCounterMsg) {
			this.nextCounterMsg *= 4;
			log.info(" event # " + this.counter);
		}
		computeEvent(event);
	}


	@Override
	public void addHandler (final EventHandler handler) {
		Set<Class<?>> addedHandlers = new HashSet<>();
		Class<?> test = handler.getClass();
		log.info("adding Event-Handler: " + test.getName());
		while (test != Object.class) {
			for (Class<?> theInterface: test.getInterfaces()) {
				if (!addedHandlers.contains(theInterface)) {
					log.info("  " + theInterface.getName());
					addHandlerInterfaces(handler, theInterface);
					addedHandlers.add(theInterface);
				}
			}
			test = test.getSuperclass();
		}
		this.cacheHandlers.clear();
		log.info("");
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		log.info("removing Event-Handler: " + handler.getClass().getName());
		for (HandlerData handlerList : this.handlerData) {
			handlerList.removeHandler(handler);
		}
		this.cacheHandlers.clear();
	}

	@Override
	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	@Override
	public void resetHandlers(final int iteration) {
		log.info("resetting Event-Handlers");
		this.counter = 0;
		this.nextCounterMsg = 1;
		Set<EventHandler> resetHandlers = new HashSet<>();
		for (HandlerData handlerdata : this.handlerData) {
			for (EventHandler handler : handlerdata.handlerList) {
				if (!resetHandlers.contains(handler)) {
					log.info("  " + handler.getClass().getName());
					handler.reset(iteration);
					resetHandlers.add(handler);
				}
			}
		}
	}

	@Override
	public void initProcessing() {
		resetHandlers(iteration);
	}

	@Override
	public void afterSimStep(double time) {
		// nothing to do in this implementation
	}

	@Override
	public void finishProcessing() {
	}

	private void addHandlerInterfaces(final EventHandler handler, final Class<?> handlerClass) {
		Method[] classmethods = handlerClass.getMethods();
		for (Method method : classmethods) {
			if (method.getName().equals("handleEvent")) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 1) {
					Class<?> eventClass = params[0];
					log.info("    > " + eventClass.getName());
					HandlerData dat = findHandler(eventClass);
					if (dat == null) {
						dat = new HandlerData(eventClass, method);
						this.handlerData.add(dat);
					}
					dat.handlerList.add(handler);
				}
			}
		}
	}

	private void computeEvent(final Event event) {
		for (HandlerInfo info : getHandlersForClass(event.getClass())) {
			synchronized(info.eventHandler) {
				if (callHandlerFast(info.eventClass, event, info.eventHandler)) {
					continue;
				}
				try {
					info.method.invoke(info.eventHandler, event);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e.getCause());
				}
			}
		}
	}

	private HandlerInfo[] getHandlersForClass(final Class<?> eventClass) {
		Class<?> klass = eventClass;
		HandlerInfo[] cache = this.cacheHandlers.get(eventClass);
		if (cache != null) {
			return cache;
		}

		ArrayList<HandlerInfo> info = new ArrayList<>();
		// first search in class-hierarchy
		while (klass != Object.class) {
			HandlerData dat = findHandler(klass);
			if (dat != null) {
				for(EventHandler handler: dat.handlerList) {
					info.add(new HandlerInfo(klass, handler, dat.method));
				}
			}
			klass = klass.getSuperclass();
		}
		// now search in implemented interfaces
		for (Class<?> intfc : getAllInterfaces(eventClass)) {
			HandlerData dat = findHandler(intfc);
			if (dat != null) {
				for(EventHandler handler: dat.handlerList) {
					info.add(new HandlerInfo(intfc, handler, dat.method));
				}
			}
		}

		cache = info.toArray(new HandlerInfo[info.size()]);
		this.cacheHandlers.put(eventClass, cache);
		return cache;
	}

	private Set<Class<?>> getAllInterfaces(final Class<?> klass) {
		Set<Class<?>> intfs = new HashSet<>();
		for (Class<?> intf : klass.getInterfaces()) {
			intfs.add(intf);
			intfs.addAll(getAllInterfaces(intf));
		}
		if (!klass.isInterface()) {
			Class<?> superclass = klass.getSuperclass();
			while (superclass != Object.class) {
				intfs.addAll(getAllInterfaces(superclass));
				superclass = superclass.getSuperclass();
			}
		}
		return intfs;
	}

	// this method is purely for performance reasons and need not be implemented
	private boolean callHandlerFast(final Class<?> klass, final Event ev, final EventHandler handler) {
		if (klass == LinkLeaveEvent.class) {
			((LinkLeaveEventHandler)handler).handleEvent((LinkLeaveEvent)ev);
			return true;
		} else if (klass == LinkEnterEvent.class) {
			((LinkEnterEventHandler)handler).handleEvent((LinkEnterEvent)ev);
			return true;
		} else if (klass == VehicleEntersTrafficEvent.class) {
			((VehicleEntersTrafficEventHandler)handler).handleEvent((VehicleEntersTrafficEvent)ev);
			return true;
		} else if (klass == PersonArrivalEvent.class) {
			((PersonArrivalEventHandler)handler).handleEvent((PersonArrivalEvent)ev);
			return true;
		} else if (klass == PersonDepartureEvent.class) {
			((PersonDepartureEventHandler)handler).handleEvent((PersonDepartureEvent)ev);
			return true;
		} else if (klass == ActivityEndEvent.class) {
			((ActivityEndEventHandler)handler).handleEvent((ActivityEndEvent)ev);
			return true;
		} else if (klass == ActivityStartEvent.class) {
			((ActivityStartEventHandler)handler).handleEvent((ActivityStartEvent)ev);
			return true;
		} else if (klass == TransitDriverStartsEvent.class) {
			((TransitDriverStartsEventHandler) handler).handleEvent((TransitDriverStartsEvent) ev);
			return true;
		} else if (klass == PersonStuckEvent.class) {
			((PersonStuckEventHandler)handler).handleEvent((PersonStuckEvent)ev);
			return true;
		} else if (klass == PersonMoneyEvent.class) {
			((PersonMoneyEventHandler)handler).handleEvent((PersonMoneyEvent)ev);
			return true;
		} else if (klass == AgentWaitingForPtEvent.class) {
			((AgentWaitingForPtEventHandler)handler).handleEvent((AgentWaitingForPtEvent)ev);
			return true;
		} else if (klass == PersonEntersVehicleEvent.class) {
			((PersonEntersVehicleEventHandler)handler).handleEvent((PersonEntersVehicleEvent)ev);
			return true;
		} else if (klass == PersonLeavesVehicleEvent.class) {
			((PersonLeavesVehicleEventHandler)handler).handleEvent((PersonLeavesVehicleEvent)ev);
			return true;
		} else if (klass == VehicleDepartsAtFacilityEvent.class) {
			((VehicleDepartsAtFacilityEventHandler) handler).handleEvent((VehicleDepartsAtFacilityEvent) ev);
			return true;
		} else if (klass == VehicleArrivesAtFacilityEvent.class) {
			((VehicleArrivesAtFacilityEventHandler) handler).handleEvent((VehicleArrivesAtFacilityEvent) ev);
			return true;
		} else if (klass == Event.class) {
			((BasicEventHandler)handler).handleEvent(ev);
			return true;
		}
		return false;
	}

	public void printEventHandlers() {
		log.info("currently registered event-handlers:");
		for (HandlerData handlerType : this.handlerData) {
			log.info("+ " + handlerType.eventklass.getName());
			for (EventHandler handler : handlerType.handlerList) {
				log.info("  - " + handler.getClass().getName());
			}
		}
	}

}
