/******************************************************************************
 *project: org.matsim.*
 * TimePanel.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.uicontrols;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JPanel;

import playground.rost.controller.TimeHandler;

public class TimePanel extends JPanel {
	JButton start = new JButton("start");
	JButton stop= new JButton("stop");
	JButton pause= new JButton("pause");
	
	class PlayBack implements Runnable
	{
		final int stepSize = 1;
		int steps;
		int msToSleep;
		TimePanel tPanel;
		public PlayBack(TimePanel tPanel, int steps, int msToSleep)
		{
			this.tPanel = tPanel;
			this.steps = steps;
			this.msToSleep = msToSleep;
		}
	
		
		
		public void run()
		{
			for(int i = 0; i < steps;++i)
			{
				tPanel.time = tPanel.time + 1;
				tPanel.callCallbacks();
				try {
					Thread.sleep(msToSleep);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	protected int time;
	
	protected Set<TimeHandler> callbacks = new HashSet<TimeHandler>();
	
	public void forwardTime()
	{
		time += 1;
		callCallbacks();
	}
	
	public void backwardTime()
	{
		time -= 1;
		callCallbacks();
	}
	
	public void subscribeTimeEvents(TimeHandler tHandler)
	{
		callbacks.add(tHandler);
	}
	
	public void unsubscribeTimeEvents(TimeHandler tHandler)
	{
		callbacks.remove(tHandler);
	}
	
	protected void play()
	{
		(new Thread(new PlayBack(this, 1000, 100))).start();
	}

	protected void callCallbacks()
	{
		for(TimeHandler tHandler : callbacks)
		{
			tHandler.timeChanged(time);
		}
	}
	
	
	
	
	public TimePanel(int time)
	{
		super();
		this.time = time;
		this.add(start);
		start.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				forwardTime();
			}
		});
		
		stop.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				backwardTime();
			}
		});
		
		pause.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				play();
			}
		});
		
		this.add(stop);
		this.add(pause);
	}
	
	
}
