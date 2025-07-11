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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;

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

	private static final Logger log = LogManager.getLogger(EventsManagerImpl.class);

	static private class HandlerData {

		protected Class<? extends Event> eventClass;
		protected ArrayList<EventHandler> handlerList = new ArrayList<EventHandler>(5);
		protected Method method;

		protected HandlerData(final Class<? extends Event> eventClass, final Method method) {
			this.eventClass = eventClass;
			this.method = method;
		}

		protected void removeHandler(final EventHandler handler) {
			this.handlerList.remove(handler);
		}
	}

	static private class HandlerInfo {
		protected final Class<? extends Event> eventClass;
		protected final EventHandler eventHandler;
		protected final Method method;

		protected HandlerInfo(final Class<? extends Event> eventClass, final EventHandler eventHandler,
				final Method method) {
			this.eventClass = eventClass;
			this.eventHandler = eventHandler;
			this.method = method;
		}
	}

	private final List<HandlerData> handlerData = new ArrayList<>();

	private final Map<Class<? extends Event>, HandlerInfo[]> cacheHandlers = new ConcurrentHashMap<>(15);

	private long counter = 0;
	private long nextCounterMsg = 1;

	private HandlerData findHandler(final Class<? extends Event> evklass) {
		for (HandlerData handler : this.handlerData) {
			if (handler.eventClass == evklass) {
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
		for (HandlerInfo info : getHandlersForClass( event.getClass() )) {
			synchronized(info.eventHandler) {
				if (callHandlerFast(info.eventClass, event, info.eventHandler )) {
					continue;
				}
				try {
					info.method.invoke(info.eventHandler, event );
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException("problem invoking EventHandler " + info.eventHandler.getClass().getCanonicalName() + " for event-class " + info.eventClass.getCanonicalName(), e.getCause());
				}
			}
		}
	}

	@Override
	public void addHandler (final EventHandler handler) {
		Set<Class<?>> addedHandlers = new HashSet<>();
		Class<?> test = handler.getClass();
		if (log.getLevel().isMoreSpecificThan(Level.DEBUG)) {
			log.info("=== Logging of event-handlers skipped ===");
			log.info("To enable debug output, set an environment variable i.e. export LOG_LEVEL='debug', "
				+ "or set log.setLogLevel(Level.DEBUG) in your run class.");
		}
		log.debug("adding Event-Handler: " + test.getName());
		do {
			for (Class<?> theInterface : test.getInterfaces()) {
				if (EventHandler.class.isAssignableFrom(theInterface)) {
					Class<? extends EventHandler> eventHandlerInterface = (Class<? extends EventHandler>)theInterface;
					if (!addedHandlers.contains(theInterface)) {
						log.debug("  " + theInterface.getName());
						addHandlerInterfaces(handler, eventHandlerInterface);
						addedHandlers.add(theInterface);
					}
				}
			}
			test = test.getSuperclass();
		} while ((EventHandler.class.isAssignableFrom(test)));

		this.cacheHandlers.clear();
		log.debug("");
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

	@Override
	public void initProcessing() {
		// nothing to do in this implementation
	}

	@Override
	public void afterSimStep(double time) {
		// nothing to do in this implementation
	}

	@Override
	public void finishProcessing() {
		// nothing to do in this implementation
	}

	private void addHandlerInterfaces(final EventHandler handler, final Class<? extends EventHandler> handlerClass) {
		Method[] classmethods = handlerClass.getMethods();
		for (Method method : classmethods) {
			if (method.getName().equals("handleEvent")) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 1) {
					Class<? extends Event> eventClass = params[0].asSubclass(Event.class);
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

	private HandlerInfo[] getHandlersForClass(final Class<? extends Event> eventClass) {
		HandlerInfo[] cache = this.cacheHandlers.get(eventClass);
		if (cache != null) {
			return cache;
		}

		ArrayList<HandlerInfo> info = new ArrayList<>();
		// search in class hierarchy
		Class<?> klass = eventClass;
		do {
			Class<? extends Event> eventKlass = (Class<? extends Event>)klass;
			HandlerData dat = findHandler(eventKlass);
			if (dat != null) {
				for (EventHandler handler : dat.handlerList) {
					info.add(new HandlerInfo(eventKlass, handler, dat.method));
				}
			}
			klass = klass.getSuperclass();
		} while (Event.class.isAssignableFrom(klass));

		cache = info.toArray(new HandlerInfo[0]);
		this.cacheHandlers.put(eventClass, cache);
		return cache;
	}

	// this method is purely for performance reasons and need not be implemented
	private static boolean callHandlerFast(final Class<? extends Event> klass, final Event ev,
			final EventHandler handler) {
		if (klass == LinkLeaveEvent.class) {
			((LinkLeaveEventHandler)handler).handleEvent((LinkLeaveEvent)ev);
			return true;
		} else if (klass == LinkEnterEvent.class) {
			((LinkEnterEventHandler)handler).handleEvent((LinkEnterEvent)ev);
			return true;
		} else if (klass == VehicleEntersTrafficEvent.class) {
			((VehicleEntersTrafficEventHandler) handler).handleEvent((VehicleEntersTrafficEvent) ev);
			return true;
		} else if (klass == VehicleLeavesTrafficEvent.class) {
			((VehicleLeavesTrafficEventHandler) handler).handleEvent((VehicleLeavesTrafficEvent) ev);
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
			log.info("+ " + handlerType.eventClass.getName());
			for (EventHandler handler : handlerType.handlerList) {
				log.info("  - " + handler.getClass().getName());
			}
		}
	}

}
