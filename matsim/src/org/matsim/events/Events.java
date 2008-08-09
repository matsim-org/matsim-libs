/* *********************************************************************** *
 * project: org.matsim.*
 * Events.java
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

package org.matsim.events;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentUtilityEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.BasicEventHandler;
import org.matsim.events.handler.EventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;

/**
 * EventHandling
 * <ol>
 * <li>Create a new class MyEventClass extends BasicEvent</li>
 * <li>Create a new interface MyEventHandlerI extends EventHandler</li>
 * <li>add method public void handleEvent(MyEvent event) to it</li>
 * <li>ready to go, just implement the interface somewhere and add a
 * HandlerObject with a call to <code>Events.addHandler(HandlerObject)</code></li>
 * <li>(optional) add an appropriate line in callHandlerFast() for speeding
 * up execution!</li>
 * </ol>
 */
public class Events {

	private static final Logger log = Logger.getLogger(Events.class);

	static private class HandlerData {

		public Class<?> eventklass;
		public ArrayList<EventHandler> handlerList = new ArrayList<EventHandler>(5);
		public Method method;
		public HandlerData(final Class<?> eventklass, final Method method) {
			this.eventklass = eventklass;
			this.method = method;
		}
		public void removeHandler(final EventHandler handler) {
			this.handlerList.remove(handler);
		}
	}

	static private class HandlerInfo {
		public final Class<?> eventClass;
		public final EventHandler eventHandler;
		public final Method method;

		public HandlerInfo(final Class<?> eventClass, final EventHandler eventHandler, final Method method) {
			this.eventClass = eventClass;
			this.eventHandler = eventHandler;
			this.method = method;
		}
	}

	private final List<HandlerData> handlerData = new ArrayList<HandlerData>();

	private final Map<Class<?>, HandlerInfo[]> cacheHandlers = new HashMap<Class<?>, HandlerInfo[]>(15);

	private long counter = 0;
	private long nextCounterMsg = 1;

	/**
	 * creates new Events-data structure
	 */
	public Events() {
	}

	private HandlerData findHandler(final Class<?> evklass) {
		for (HandlerData handler : this.handlerData) {
			if (handler.eventklass == evklass) {
				return handler;
			}
		}
		return null;
	}

	public void processEvent(final BasicEvent event) {
		this.counter++;
		if (this.counter == this.nextCounterMsg) {
			this.nextCounterMsg *= 2;
			printEventsCount();
		}
		computeEvent(event);
	}

	public void printEventsCount() {
		log.info(" event # " + this.counter);
	}

	public void addHandler (final EventHandler handler) {
		Map<Class<?>, Object> addedHandlers = new HashMap<Class<?>, Object>();
		Class<?> test = handler.getClass();
		log.info("adding Event-Handler: " + test.getName());
		while (test != Object.class) {
			for (Class<?> theInterface: test.getInterfaces()) {
				if (!handler.equals(addedHandlers.get(theInterface))) {
					log.info("  " + theInterface.getName());
					addHandlerInterfaces(handler, theInterface);
					addedHandlers.put(theInterface, handler);
				}
			}
			test = test.getSuperclass();
		}
		this.cacheHandlers.clear();
	}

	public final void removeHandler(final EventHandler handler) {
		log.info("removing Event-Handler: " + handler.getClass().getName());
		for (HandlerData handlerList : this.handlerData) {
			handlerList.removeHandler(handler);
		}
		this.cacheHandlers.clear();
	}

	public final void clearHandlers() {
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

	private void addHandlerInterfaces(final EventHandler handler, final Class<?> handlerClass) {
		Method[] classmethods = handlerClass.getMethods();
		for (Method method : classmethods) {
			if (method.getName().equals("handleEvent")) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 1) {
					Class<?> eventClass = params[0];
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

	private void computeEvent(final BasicEvent event) {
		for (HandlerInfo info : getHandlersForClass(event.getClass())) {
			if (callHandlerFast(info.eventClass, event, info.eventHandler)) {
				continue;
			}
			try {
				info.method.invoke(info.eventHandler, event);
			} catch (Exception e) {
				e.printStackTrace();
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
		while (klass != Object.class) {
			HandlerData dat = findHandler(klass);
			if (dat != null) {
				for(EventHandler handler: dat.handlerList) {
					info.add(new HandlerInfo(klass, handler, dat.method));
				}
			}
			klass = klass.getSuperclass();
		}

		cache = info.toArray(new HandlerInfo[info.size()]);
		this.cacheHandlers.put(eventClass, cache);
		return cache;
	}

	// these method is purely for performance reasons and need not be implemented
	private boolean callHandlerFast(final Class<?> klass, final BasicEvent ev, final EventHandler handler) {

		if (klass == BasicEvent.class) {
			((BasicEventHandler)handler).handleEvent(ev);
			return true;
		} else if (klass == LinkLeaveEvent.class) {
			((LinkLeaveEventHandler)handler).handleEvent((LinkLeaveEvent)ev);
			return true;
		} else if (klass == LinkEnterEvent.class) {
			((LinkEnterEventHandler)handler).handleEvent((LinkEnterEvent)ev);
			return true;
		} else if (klass == ActEndEvent.class) {
			((ActEndEventHandler)handler).handleEvent((ActEndEvent)ev);
			return true;
		} else if (klass == ActStartEvent.class) {
			((ActStartEventHandler)handler).handleEvent((ActStartEvent)ev);
			return true;
		} else if (klass == AgentArrivalEvent.class) {
			((AgentArrivalEventHandler)handler).handleEvent((AgentArrivalEvent)ev);
			return true;
		} else if (klass == AgentDepartureEvent.class) {
			((AgentDepartureEventHandler)handler).handleEvent((AgentDepartureEvent)ev);
			return true;
		} else if (klass == AgentStuckEvent.class) {
			((AgentStuckEventHandler)handler).handleEvent((AgentStuckEvent)ev);
			return true;
		} else if (klass == AgentWait2LinkEvent.class) {
			((AgentWait2LinkEventHandler)handler).handleEvent((AgentWait2LinkEvent)ev);
			return true;
		} else if (klass == AgentUtilityEvent.class) {
			((AgentUtilityEventHandler)handler).handleEvent((AgentUtilityEvent)ev);
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
