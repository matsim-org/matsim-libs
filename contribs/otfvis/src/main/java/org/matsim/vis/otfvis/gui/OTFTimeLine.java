/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.vis.otfvis.gui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.core.utils.misc.Time;

// TODO should not be an OTFDrawer, need to handle invalidate better
/**
 * OTFTimeLine is the time line toolbar.
 * It is only used in case of playing a mvi file.
 *
 * @author dstrippgen
 *
 */
public class OTFTimeLine extends JToolBar implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 1L;

	private static class TimestepSlider extends JSlider {

		private static final long serialVersionUID = 1L;

		public TimestepSlider(final BoundedRangeModel model) {
			super(model);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Rectangle bounds = g.getClipBounds();
			bounds.grow(-32, 0);
		}
	}

	private final OTFHostControl hostControl;

	private JSlider timestepSlider;

	Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();

	private int loopStart;

	private int loopEnd;

	public OTFTimeLine(String string, OTFHostControl hostControl) {
		super(string);
		this.hostControl = hostControl;
		addSlider();
		addLoopButtons();
		this.setVisible(true);
	}

	private void addSlider() {
		//Create the slider.
		Collection<Double> steps = hostControl.getTimeStepsdrawer();
		if ((steps == null) || (steps.size() == 0)) {
			timestepSlider = new TimestepSlider(hostControl.getSimTimeModel());
			return; // nothing to display
		}

		int min = hostControl.getSimTimeModel().getMinimum();
		int max = hostControl.getSimTimeModel().getMaximum();
		timestepSlider = new TimestepSlider(hostControl.getSimTimeModel());

		timestepSlider.getModel().addChangeListener(this);
		timestepSlider.setMajorTickSpacing((min-max)/10);
		timestepSlider.setPaintTicks(true);

		//Create the label table.
		//PENDING: could use images, but we don't have any good ones.
		Double[] dsteps = steps.toArray(new Double[steps.size()]);
		labelTable.put(Integer.valueOf( min ),
				new JLabel(Time.writeTime(dsteps[0], Time.TIMEFORMAT_HHMM)));
		//new JLabel(createImageIcon("images/stop.gif")) );
		labelTable.put(Integer.valueOf( max ),
				new JLabel(Time.writeTime(dsteps[dsteps.length-1], Time.TIMEFORMAT_HHMM)) );
		//new JLabel(createImageIcon("images/fast.gif")) );

		int n = dsteps.length/10 + 1;

		for(int i= n; i< dsteps.length-1; i+=n) {
			labelTable.put(Integer.valueOf( dsteps[i].intValue() ),
					new JLabel(Time.writeTime(dsteps[i], Time.TIMEFORMAT_HHMM)) );
		}
		timestepSlider.setLabelTable(labelTable);

		timestepSlider.setPaintLabels(true);
		timestepSlider.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
		add(timestepSlider);
	}

	private void addLoopButtons() {
		JButton button = new JButton();
		button.setText("[");
		button.setActionCommand("setLoopStart");
		button.addActionListener(this);
		button.setToolTipText("Sets the loop start time");
		add(button);

		button = new JButton();
		button.setText("]");
		button.setActionCommand("setLoopEnd");
		button.addActionListener(this);
		button.setToolTipText("Sets the loop end time");
		add(button);
	}

	void replaceLabel(String label, int newEnd) {
		for (Integer i : labelTable.keySet() ) {
			JLabel value = labelTable.get(i);
			if(value.getText().equals(label)) {
				labelTable.remove(i);
				break;
			}
		}
		labelTable.put(Integer.valueOf(newEnd), new JLabel(label));
		timestepSlider.setLabelTable(labelTable);
		timestepSlider.repaint();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int time = timestepSlider.getValue();
		if (e.getActionCommand().equals("setLoopStart")){
			loopStart = time;
			replaceLabel("[", time);
		} else if (e.getActionCommand().equals("setLoopEnd")){
			loopEnd = time;
			replaceLabel("]", time);
		}
		hostControl.setLoopBounds(loopStart, loopEnd);
	}

	/** Listen to the slider. */
	@Override
	public void stateChanged(ChangeEvent e) {
		int gotoTime = hostControl.getSimTime();
		hostControl.requestTimeStep(gotoTime);
	}

}
