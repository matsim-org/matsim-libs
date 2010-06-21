/* *********************************************************************** *
 * project: org.matsim.*
 * SynchronizedEventsManagerImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.ptproject.qsim;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;


/**
 * @author dgrether
 *
 */
public class SynchronizedEventsManagerImpl implements EventsManager {
	// yy I assume this is needed in order to make "processEvent" synchronized.  But could someone please explain to me
	// (preferably in javadoc to this class) why it makes sense to maintain both classes, one without the
	// "synchronized" and one with it?  I assume there is a deeper reason, but without guidance I would assume
	// that people will just randomly select one or othe other.  Thanks.  kai, jun'10

  private final EventsManager delegate;
  
  public SynchronizedEventsManagerImpl(EventsManager eventsManager){
    this.delegate = eventsManager;
  }
  
  @Override
  public void addHandler(EventHandler handler) {
    this.delegate.addHandler(handler);
  }

  @Override
  public synchronized EventsFactory getFactory() {
    return this.delegate.getFactory();
  }

  @Override
  public synchronized void processEvent(Event event) {
    this.delegate.processEvent(event);
  }
  @Override
  public void removeHandler(EventHandler handler) {
    this.delegate.removeHandler(handler);
  }

}
