/* *********************************************************************** *
 * project: org.matsim.*
 * Events.java
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.BasicPersonEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityEndEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentMoneyEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentWait2LinkEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicPersonEventHandler;
import org.matsim.core.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.AgentMoneyEventHandler;
import org.matsim.core.events.handler.AgentReplanEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;

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
 * 
 * @author dstrippgen
 * @author mrieser
 */
public class Events {

	private static final Logger log = Logger.getLogger(Events.class);

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

	private BasicEventsBuilder builder;
	
	public Events() {
		this.builder = new BasicEventsBuilderImpl();
	}
	
	public Events(BasicEventsBuilder builder) {
		this.builder = builder;
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
	 * {@link #processEvent(BasicEvent)}). Can be used to clean up internal data structures used
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

	private void computeEvent(final BasicEvent event) {
		for (HandlerInfo info : getHandlersForClass(event.getClass())) {
			synchronized(info.eventHandler) {
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
	private boolean callHandlerFast(final Class<?> klass, final BasicEvent ev, final EventHandler handler) {

		if (klass == LinkLeaveEvent.class) {
			((LinkLeaveEventHandler)handler).handleEvent((LinkLeaveEvent)ev);
			return true;
		} else if (klass == BasicLinkLeaveEvent.class) {
				((BasicLinkLeaveEventHandler)handler).handleEvent((BasicLinkLeaveEvent)ev);
				return true;
		} else if (klass == LinkEnterEvent.class) {
			((LinkEnterEventHandler)handler).handleEvent((LinkEnterEvent)ev);
			return true;
		} else if (klass == BasicLinkEnterEvent.class) {
			((BasicLinkEnterEventHandler)handler).handleEvent((BasicLinkEnterEvent)ev);
			return true;
		} else if (klass == AgentWait2LinkEvent.class) {
			((AgentWait2LinkEventHandler)handler).handleEvent((AgentWait2LinkEvent)ev);
			return true;
		} else if (klass == BasicAgentWait2LinkEvent.class) {
			((BasicAgentWait2LinkEventHandler)handler).handleEvent((BasicAgentWait2LinkEvent)ev);
			return true;
		} else if (klass == AgentArrivalEvent.class) {
			((AgentArrivalEventHandler)handler).handleEvent((AgentArrivalEvent)ev);
			return true;
		} else if (klass == BasicAgentArrivalEvent.class) {
			((BasicAgentArrivalEventHandler)handler).handleEvent((BasicAgentArrivalEvent)ev);
			return true;
		} else if (klass == AgentDepartureEvent.class) {
			((AgentDepartureEventHandler)handler).handleEvent((AgentDepartureEvent)ev);
			return true;
		} else if (klass == BasicAgentDepartureEvent.class) {
			((BasicAgentDepartureEventHandler)handler).handleEvent((BasicAgentDepartureEvent)ev);
			return true;
		} else if (klass == ActivityEndEvent.class) {
			((ActivityEndEventHandler)handler).handleEvent((ActivityEndEvent)ev);
			return true;
		} else if (klass == BasicActivityEndEvent.class) {
			((BasicActivityEndEventHandler)handler).handleEvent((BasicActivityEndEvent)ev);
			return true;
		} else if (klass == ActivityStartEvent.class) {
			((ActivityStartEventHandler)handler).handleEvent((ActivityStartEvent)ev);
			return true;
		} else if (klass == BasicActivityStartEvent.class) {
			((BasicActivityStartEventHandler)handler).handleEvent((BasicActivityStartEvent)ev);
			return true;
		} else if (klass == AgentStuckEvent.class) {
			((AgentStuckEventHandler)handler).handleEvent((AgentStuckEvent)ev);
			return true;
		} else if (klass == BasicAgentStuckEvent.class) {
			((BasicAgentStuckEventHandler)handler).handleEvent((BasicAgentStuckEvent)ev);
			return true;
		} else if (klass == AgentMoneyEvent.class) {
			((AgentMoneyEventHandler)handler).handleEvent((AgentMoneyEvent)ev);
			return true;
		} else if (klass == BasicAgentMoneyEvent.class) {
			((BasicAgentMoneyEventHandler)handler).handleEvent((BasicAgentMoneyEvent)ev);
			return true;
		} else if (klass == AgentReplanEvent.class) {
			((AgentReplanEventHandler)handler).handleEvent((AgentReplanEvent)ev);
			return true;
		} else if (klass == BasicEvent.class) {
			((BasicEventHandler)handler).handleEvent(ev);
			return true;
		} else if (klass == BasicPersonEvent.class) {
			((BasicPersonEventHandler)handler).handleEvent((BasicPersonEvent)ev);
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
	
	public BasicEventsBuilder getBuilder(){
		return this.builder;
	}

}
