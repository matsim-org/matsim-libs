/* *********************************************************************** *
 * project: org.matsim.*
 * ControlToolbar.java
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

package org.matsim.utils.vis.netvis.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Timer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;
import org.matsim.utils.vis.netvis.visNet.DisplayNetStateReader;

/**
 * @author gunnar
 * 
 */
public class ControlToolbar extends JToolBar implements ActionListener, ItemListener, ChangeListener {

    private static final String TO_START = "to_start";

    private static final String STEP_BB = "step_bb";

    private static final String STEP_B = "step_b";

    private static final String PLAY = "play";

    private static final String STEP_F = "step_f";

    private static final String STEP_FF = "step_ff";

    private static final String TO_END = "to_end";

    private static final String ZOOM_IN = "zoom_in";

    private static final String ZOOM_OUT = "zoom_out";

    private static final String SET_TIME = "set_time";

    private static final String TOGGLE_AGENTS = "Agents";
    private static final String TOGGLE_NODE_LABELS ="Node Labels";
    private static final String TOGGLE_LINK_LABELS = "Link Labels";
    private static final String TOGGLE_ANTIALIAS = "AntiAlias";

    private static final int SKIP = 30;

    // -------------------- MEMBER VARIABLES --------------------

    private NetVis viz;

    private Timer movieTimer = null;

    private DisplayNetStateReader reader;

    private JButton playButton;

    private JFormattedTextField timeField;

    private float scale = 1;

    private VisConfig visConfig;

    // -------------------- CONSTRUCTION --------------------

    public ControlToolbar(NetVis viz, DisplayNet network,
            DisplayNetStateReader reader, VisConfig visConfig) {
        super();
        this.viz = viz;
        this.reader = reader;
        this.visConfig = visConfig;
        addButtons();
    }

    private void addButtons() {
        if (reader != null) {
            add(createButton("|<", TO_START));
            add(createButton("<<", STEP_BB));
            add(createButton("<", STEP_B));
            playButton = createButton("PLAY", PLAY);
            add(playButton);
            add(createButton(">", STEP_F));
            add(createButton(">>", STEP_FF));
            add(createButton(">|", TO_END));
        }

        if (reader != null) {
            timeField = new JFormattedTextField( new MessageFormat("{0,number,00}-{1,number,00}-{2,number,00}"));
            timeField.setMaximumSize(new Dimension(75,30));
            timeField.setActionCommand(SET_TIME);
            timeField.setHorizontalAlignment(JTextField.CENTER);
            add( timeField );
            timeField.addActionListener( this );
        }

        add(createButton("--", ZOOM_OUT));
        add(createButton("+", ZOOM_IN));

        createCheckBoxes();

        SpinnerNumberModel model = new SpinnerNumberModel(this.visConfig.getLinkWidthFactor(), 0, 2000, 1);
        JSpinner spin = addLabeledSpinner(this, "Lanewidth", model);
        spin.setMaximumSize(new Dimension(75,30));
        spin.addChangeListener(this);
    }

    private JButton createButton(String display, String actionCommand) {
        JButton button;

        button = new JButton();
        button.setActionCommand(actionCommand);
        button.addActionListener(this);
        button.setText(display);

        return button;
    }

    public void updateTimeLabel() {
        if (reader != null)
            timeField.setText(Time.strFromSec(reader.getCurrentTime_s()));
    }

    // ---------- IMPLEMENTATION OF ActionListener INTERFACE ----------

    private void stopMovie() {
        if (movieTimer != null) {
            movieTimer.cancel();
            movieTimer = null;
            playButton.setText("PLAY");
        }
    }

    private void pressed_TO_START() throws IOException {
        stopMovie();
        reader.toStart();
    }

    private void pressed_STEP_BB() throws IOException {
        stopMovie();
        reader.toTimeStep(reader.getCurrentTime_s() - SKIP
                * reader.timeStepLength_s());
    }

    private void pressed_STEP_B() throws IOException {
        stopMovie();
        reader.toPrevTimeStep();
    }

    private void pressed_PLAY() {
        if (movieTimer == null) {
            movieTimer = new Timer();
            MoviePlayer moviePlayer = new MoviePlayer(reader, viz);
            movieTimer.schedule(moviePlayer, 0, visConfig.getDelay_ms());
            playButton.setText("STOP");
        } else
            stopMovie();
    }

    private void pressed_STEP_F() throws IOException {
        stopMovie();
        reader.toNextTimeStep();
    }

    private void pressed_STEP_FF() throws IOException {
        stopMovie();
        reader.toTimeStep(reader.getCurrentTime_s() + SKIP
                * reader.timeStepLength_s());
    }

    private void pressed_TO_END() throws IOException {
        stopMovie();
        reader.toEnd();
    }

    private void pressed_ZOOM_OUT() {
        scale /= 1.42;
        viz.scaleNetwork(scale);
    }

    private void pressed_ZOOM_IN() {
        scale *= 1.42;
        viz.scaleNetwork(scale);
    }

    private void changed_SET_TIME(ActionEvent event) throws IOException {
    	String newTime = ((JFormattedTextField)event.getSource()).getText();
    	int newTime_s = Time.secFromStr(newTime);
        stopMovie();
        reader.toTimeStep(newTime_s);
    }

    @Override
		public void paint(Graphics g) {
        if (reader != null)
            updateTimeLabel();
        super.paint(g);
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();

        try {
            if (TO_START.equals(command))
                pressed_TO_START();
            else if (STEP_BB.equals(command))
                pressed_STEP_BB();
            else if (STEP_B.equals(command))
                pressed_STEP_B();
            else if (PLAY.equals(command))
                pressed_PLAY();
            else if (STEP_F.equals(command))
                pressed_STEP_F();
            else if (STEP_FF.equals(command))
                pressed_STEP_FF();
            else if (TO_END.equals(command))
                pressed_TO_END();
            else if (ZOOM_OUT.equals(command))
                pressed_ZOOM_OUT();
            else if (ZOOM_IN.equals(command))
                pressed_ZOOM_IN();
            else if (command.equals(SET_TIME))
            	changed_SET_TIME(event);
        } catch (IOException e) {
            System.err.println("ControlToolbar encountered problem: " + e);
        }

        updateTimeLabel();

        repaint();
 
        viz.paintNow();
    }

    /**
     * @return Returns the scale.
     */
    public float getScale() {
        return scale;
    }

    /**
     * @param scale
     *            The scale to set.
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

  static protected JSpinner addLabeledSpinner(Container c,   String label,  SpinnerModel model) 
  {
	  JLabel l = new JLabel(label);
	  c.add(l);
	  JSpinner spinner = new JSpinner(model);
	  l.setLabelFor(spinner);
	  c.add(spinner);
	  return spinner;
  }
  private void createCheckBoxes() {
        JCheckBox VehBox = new JCheckBox(TOGGLE_AGENTS);
        VehBox.setMnemonic(KeyEvent.VK_V);
        VehBox.setSelected(true);
        VehBox.addItemListener(this);
        add(VehBox);
        JCheckBox nodeLabelBox = new JCheckBox(TOGGLE_NODE_LABELS);
        nodeLabelBox.setMnemonic(KeyEvent.VK_N);
        nodeLabelBox.setSelected(visConfig.showNodeLabels());
        nodeLabelBox.addItemListener(this);
        add(nodeLabelBox);
        JCheckBox linkLabelBox = new JCheckBox(TOGGLE_LINK_LABELS);
        linkLabelBox.setMnemonic(KeyEvent.VK_L);
        linkLabelBox.setSelected(visConfig.showLinkLabels());
        linkLabelBox.addItemListener(this);
        add(linkLabelBox);
        JCheckBox AABox = new JCheckBox(TOGGLE_ANTIALIAS);
        AABox.setMnemonic(KeyEvent.VK_A);
        AABox.setSelected(false);
        AABox.addItemListener(this);
        add(AABox);
  }
	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals(TOGGLE_AGENTS)) {
			// toggle Labels to 
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				visConfig.set("ShowAgents", "false");
			} else {
				visConfig.set("ShowAgents", "true");
			}
        } else if (source.getText().equals(TOGGLE_NODE_LABELS)) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                visConfig.set(VisConfig.SHOW_NODE_LABELS, "false");
            } else {
                visConfig.set(VisConfig.SHOW_NODE_LABELS, "true");
            }
        } else if (source.getText().equals(TOGGLE_LINK_LABELS)) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				visConfig.set(VisConfig.SHOW_LINK_LABELS, "false");
			} else {
				visConfig.set(VisConfig.SHOW_LINK_LABELS, "true");
			}
		} else if (source.getText().equals(TOGGLE_ANTIALIAS)) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				visConfig.set(VisConfig.USE_ANTI_ALIASING, "false");
			} else {
				visConfig.set(VisConfig.USE_ANTI_ALIASING, "true");
			}
		}
		repaint();
		viz.paintNow();
	}

	public void stateChanged(ChangeEvent e) {
		JSpinner spinner = (JSpinner)e.getSource();
		int i = ((SpinnerNumberModel)spinner.getModel()).getNumber().intValue();
		visConfig.set(VisConfig.LINK_WIDTH_FACTOR, Integer.toString(i));
		repaint();
		viz.paintNow();
	}

}