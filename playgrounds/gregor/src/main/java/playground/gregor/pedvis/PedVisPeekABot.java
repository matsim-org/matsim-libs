/* *********************************************************************** *
 * project: org.matsim.*
 * PedVisPeekABot.java
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
package playground.gregor.pedvis;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d.events.XYZAzimuthEvent;
import playground.gregor.sim2d.events.XYZEventsFileReader;
import playground.gregor.sim2d.events.XYZEventsHandler;
import playground.gregor.sim2d.peekabot.PeekABotClient;

/**
 * @author laemmel
 * 
 */
public class PedVisPeekABot implements XYZEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler {

	private final PeekABotClient pc;
	private String file;
	private final double speedUp;

	private long lastTime = 0;
	double lastEventsTime = 0;

	public PedVisPeekABot(double speedUp) {
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
		this.pc.init();
	}

	public PedVisPeekABot(String eventsFile, boolean loop, double speedUp) {
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
		this.pc.init();
		this.file = eventsFile;
		if (!loop) {
			run();
		} else {
			while (true) {
				run();
			}
		}
	}

	/**
	 * 
	 */
	private void run() {
		EventsManagerImpl ev = new EventsManagerImpl();
		ev.addHandler(this);
		XYZEventsFileReader reader = new XYZEventsFileReader(ev);
		try {
			reader.parse(this.file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
	}

	public void handleEvent(XYZAzimuthEvent e) {
		testWait(e.getTime());
		this.pc.setBotPosition(Integer.parseInt(e.getPersonId().toString()), (float) e.getX(), (float) e.getY(), (float) e.getZ(), (float) e.getAzimuth());

	}

	/**
	 * @param d
	 * 
	 */
	private void testWait(double d) {
		if (d == this.lastEventsTime) {
			return;
		}
		double step = ((d - this.lastEventsTime) * 1000) / this.speedUp;
		long currentTime = System.currentTimeMillis();
		long diff = (long) (step - (currentTime - this.lastTime));
		if (diff > 0) {
			try {
				Thread.sleep(diff);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.lastEventsTime = d;
		this.lastTime = currentTime;
	}

	public static void main(String[] args) {
		PedVisPeekABot vis = new PedVisPeekABot("/home/laemmel/devel/dfg/output/ITERS/it.0/0.xyzAzimuthEvents.xml.gz", true, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentDepartureEvent)
	 */
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		this.pc.addBot(Integer.parseInt(e.getPersonId().toString()), 10, 10, 10);
		float r = 0;
		float g = 0;
		float b = 0;

		// experimental id dependent colorization
		MatsimRandom.reset(Integer.parseInt(e.getPersonId().toString()));
		MatsimRandom.getRandom().nextDouble();
		MatsimRandom.getRandom().nextDouble();
		b = MatsimRandom.getRandom().nextFloat();
		if (Integer.parseInt(e.getLinkId().toString()) % 2 == 0) {
			r = 1.f;
		} else {
			g = 1.f;
		}
		this.pc.setBotColor(Integer.parseInt(e.getPersonId().toString()), r, g, b);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentArrivalEvent)
	 */
	@Override
	public void handleEvent(AgentArrivalEvent e) {
		this.pc.setBotPosition(Integer.parseInt(e.getPersonId().toString()), -10, -10, -10, -10);

	}

}
