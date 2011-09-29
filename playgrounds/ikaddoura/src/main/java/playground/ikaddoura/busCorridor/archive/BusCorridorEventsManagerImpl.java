/* *********************************************************************** *
 * project: org.matsim.*
 * MyEventsManagerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.busCorridor.archive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

public class BusCorridorEventsManagerImpl implements EventsManager {

	private static final Logger log = Logger.getLogger(BusCorridorEventsManagerImpl.class);

	static private class HandlerData {

		protected Class<?> eventklass;
		protected ArrayList<EventHandler> handlerList = new ArrayList<EventHandler>(5);
		protected Method method;
		protected HandlerData(final Class<?> eventklass, final Method method) {
			this.eventklass = eventklass;
			this.method = method;
		}
		protected void removeHandler(final EventHandler handler) {
			this.handlerList.remove(handler);
		}
	}

	static private class HandlerInfo {
		protected final Class<?> eventClass;
		protected final EventHandler eventHandler;
		protected final Method method;

		protected HandlerInfo(final Class<?> eventClass, final EventHandler eventHandler, final Method method) {
			this.eventClass = eventClass;
			this.eventHandler = eventHandler;
			this.method = method;
		}
	}

	private final List<HandlerData> handlerData = new ArrayList<HandlerData>();

	private final Map<Class<?>, HandlerInfo[]> cacheHandlers = new ConcurrentHashMap<Class<?>, HandlerInfo[]>(15);

	private long counter = 0;
	private long nextCounterMsg = 1;

	private EventsFactory builder;

	protected BusCorridorEventsManagerImpl() {
		this.builder = new EventsFactoryImpl();
	}

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
			this.nextCounterMsg *= 2;
			printEventsCount();
		}
		try {
			computeEvent(event);
		} catch (NullPointerException e) {
			e.printStackTrace(); // print the stack trace here, as it seems to be lost in the later process until it gets catched.
			throw e;
		}
	}

	public void printEventsCount() {
		log.info(" event # " + this.counter);
	}

	@Override
	public void addHandler (final EventHandler handler) {
		Set<Class<?>> addedHandlers = new HashSet<Class<?>>();
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

	public void clearHandlers() {
		log.info("clearing Event-Handlers");
		for (HandlerData handler : this.handlerData) {
			handler.handlerList.clear();
		}
		this.cacheHandlers.clear();
	}

	public void resetHandlers(final int iteration) {
		log.info("resetting Event-Handlers");
		this.counter = 0;
		this.nextCounterMsg = 1;
		Set<EventHandler> resetHandlers = new HashSet<EventHandler>();
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

	/**
	 * Resets the event counter to zero.
	 */
	public void resetCounter() {
		this.counter = 0;
		this.nextCounterMsg = 1;
	}

	/**
	 * Called before the first event is sent for processing. Allows to initialize internal
	 * data structures used to process events.
	 */
	public void initProcessing() {
		// nothing to do in this implementation
	}

	/**
	 * Called after the last event is sent for processing. The method must only return when all
	 * events are completely processing (in case they are not directly processed in
	 * {@link #processEvent(Event)}). Can be used to clean up internal data structures used
	 * to process events.
	 */
	public void finishProcessing() {
		// nothing to do in this implementation
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
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e);
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

		ArrayList<HandlerInfo> info = new ArrayList<HandlerInfo>();
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
		Set<Class<?>> intfs = new HashSet<Class<?>>();
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
		} else if (klass == AgentWait2LinkEvent.class) {
			((AgentWait2LinkEventHandler)handler).handleEvent((AgentWait2LinkEvent)ev);
			return true;
		} else if (klass == AgentArrivalEvent.class) {
			((AgentArrivalEventHandler)handler).handleEvent((AgentArrivalEvent)ev);
			return true;
		} else if (klass == AgentDepartureEvent.class) {
			((AgentDepartureEventHandler)handler).handleEvent((AgentDepartureEvent)ev);
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
		} else if (klass == AgentStuckEvent.class) {
			((AgentStuckEventHandler)handler).handleEvent((AgentStuckEvent)ev);
			return true;
		} else if (klass == AgentMoneyEvent.class) {
			((AgentMoneyEventHandler)handler).handleEvent((AgentMoneyEvent)ev);
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
		} else if (klass == PersonEvent.class) {
			((PersonEventHandler)handler).handleEvent((PersonEvent)ev);
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

	@Override
	public EventsFactory getFactory(){
		return this.builder;
	}

}