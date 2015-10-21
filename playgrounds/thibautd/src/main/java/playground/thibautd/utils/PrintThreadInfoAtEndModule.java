/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;

/**
 * @author thibautd
 */
public class PrintThreadInfoAtEndModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(PrintThreadInfoAtEndModule.class);

	@Override
	public void install() {
		addControlerListenerBinding().toInstance( new Listenner() );
	}

	private static class Listenner implements ShutdownListener {
		@Override
		public void notifyShutdown(ShutdownEvent event) {
			log.debug("##################################################################");
			log.debug("info on active threads:");

			int i = 0;
			for ( Map.Entry<Thread,StackTraceElement[]> e : Thread.getAllStackTraces().entrySet() ) {
				i++;
				log.debug( "########### Thread "+i+" ##########################3");
				log.debug("Thread is " + e.getKey());
				log.debug("Thread name is " + e.getKey().getName());
				log.debug( "Thread class is "+e.getKey().getClass().getCanonicalName() );
				log.debug("Thread state is " + e.getKey().getState());
				if ( e.getKey().getState() == Thread.State.WAITING ) {
					final ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo( e.getKey().getId() );
					log.debug("Thread lock is " + info.getLockName() );
					log.debug("Thread lock is owned by " + info.getLockOwnerName() );
				}
				log.debug( "Thread is daemon "+e.getKey().isDaemon() );
				for ( StackTraceElement ste : e.getValue() ) {
					log.debug( "       at "+ste.getClassName()+"."+ste.getMethodName()+"( "+ste.getFileName()+":"+ste.getLineNumber()+" )" );
				}
			}
			log.debug( "");
		}
	}
}
