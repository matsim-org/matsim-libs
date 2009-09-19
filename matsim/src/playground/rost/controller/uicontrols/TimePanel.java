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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import playground.rost.controller.gui.BasicMapGUI;

public class TimePanel extends JPanel implements TimeControl{
		
	protected JPanel btnPanel;
	protected JPanel sliderPanel;
	protected JPanel lblPanel;
	
	
	protected JButton stepBack;
	protected JButton start;
	protected JButton stop;
	protected JButton stepForward;
	protected JTextField fieldMsToSleep;
	protected JTextField fieldCurrentTime;	
	protected JTextField fieldStepSize;	
	protected JSlider slider;
	
	protected int currentTime;
	protected int startTime;
	protected int endTime;
	
	protected BasicMapGUI mapGUI;
	
	public class TimePlayback implements Runnable
	{
		public boolean abortPlayback = false;
		public boolean finished = false;
		
		
		public void run() {
			int sleepTime = Integer.parseInt(fieldMsToSleep.getText());
			changeEditableForStart();
			while(currentTime < endTime && !abortPlayback)
			{
				setTime(++currentTime);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			finished = true;
			changeEditableForEnd();
		}
		
		protected void changeEditableForStart()
		{
			start.setEnabled(false);
			stop.setEnabled(true);
			stepForward.setEnabled(false);
			stepBack.setEnabled(false);
			fieldMsToSleep.setEditable(false);
		}
		
		protected void changeEditableForEnd()
		{
			start.setEnabled(true);
			stop.setEnabled(false);
			stepForward.setEnabled(true);
			stepBack.setEnabled(true);
			fieldMsToSleep.setEditable(true);
		}
	}
	
	protected TimePlayback playback;
	
	public TimePanel(BasicMapGUI mapGUI, int startTime, int endTime)
	{
		super();
		this.mapGUI = mapGUI;

		this.startTime = startTime;
		this.endTime = endTime;
		this.currentTime = startTime;

		this.setLayout(new BorderLayout());
		btnPanel = createBtnPanel();
		this.add(btnPanel, BorderLayout.WEST);
		
		lblPanel = createLblPanel();
		this.add(lblPanel, BorderLayout.EAST);

		slider = createSlider();
		this.add(slider, BorderLayout.CENTER);
	}
	
	protected JPanel createBtnPanel()
	{
		JPanel result = new JPanel();
		ImageIcon imgStepBack = new ImageIcon("./src/playground/rost/res/images/rewind.png");
		stepBack = new JButton(imgStepBack);
		
		ImageIcon imgPlay = new ImageIcon("./src/playground/rost/res/images/play.png");
		start = new JButton(imgPlay);
		ImageIcon imgStop = new ImageIcon("./src/playground/rost/res/images/stop.png");
		stop = new JButton(imgStop);
		stop.setEnabled(false);
		
		ImageIcon imgStepForward = new ImageIcon("./src/playground/rost/res/images/forward.png");
		stepForward = new JButton(imgStepForward);
		result.add(stepBack);
		result.add(start);
		result.add(stop);
		result.add(stepForward);
		
		//add callbacks
		start.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				startPlayback();
			}
		});
		
		stop.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				stopPlayback();
			}
		});

		stepForward.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				stepForward();
			}
		});

		
		stepBack.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				stepBack();
			}
		});

		
		
		return result;
	}
	
	protected JSlider createSlider()
	{
		slider = new JSlider();
		slider.setMinimum(startTime);
		slider.setMaximum(endTime);
		int diff = endTime - startTime;
		slider.setMajorTickSpacing(diff / 20);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e) {
				if(!slider.getValueIsAdjusting())
					sliderChanged();
			}});
		slider.setValue(startTime);
		return slider;
	}
	
	protected JPanel createLblPanel()
	{
		JPanel result = new JPanel();
		
		result.setLayout(new GridLayout(0,2));
		JLabel lblCurrentTime = new JLabel("current time: ");
		result.add(lblCurrentTime);
		fieldCurrentTime = new JTextField(""+startTime);
		fieldCurrentTime.setEditable(false);
		result.add(fieldCurrentTime);
		
		
		fieldMsToSleep = new JTextField("100");
		JLabel lblMsToSleep = new JLabel("ms to sleep: ");
		result.add(lblMsToSleep);

		result.add(lblMsToSleep);
		result.add(fieldMsToSleep);
		
		JLabel lblStepSize = new JLabel("step size: ");
		fieldStepSize = new JTextField("1");
		result.add(lblStepSize);
		result.add(fieldStepSize);
		
		return result;

	}

	public int getTime() {
		return currentTime;
	}

	public void setTime(int time) {
		this.currentTime = time;
		if(currentTime < startTime)
			currentTime = startTime;
		if(currentTime > endTime)
			currentTime = endTime;
		timeHasChanged();
	}
	
	public void sliderChanged()
	{
		this.currentTime = slider.getValue();
		timeHasChanged();
	}
	
	public void timeHasChanged()
	{
		slider.setValue(currentTime);
		fieldCurrentTime.setText("" + currentTime);
		mapGUI.UIChange();
	}
	
	public void startPlayback()
	{
		if(this.playback != null && this.playback.finished == false)
			return;
		this.playback = new TimePlayback();
		Thread thread = new Thread(this.playback);
		thread.start();
	}
	
	public void stopPlayback()
	{
		if(this.playback != null)
			this.playback.abortPlayback = true;
	}
	
	public void stepForward()
	{
		int stepSize = Integer.parseInt(fieldStepSize.getText());
		setTime(currentTime + stepSize);
	}
	
	public void stepBack()
	{
		int stepSize = Integer.parseInt(fieldStepSize.getText());
		setTime(currentTime - stepSize);
	}
	
	
}
