/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigPanel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.oldenburg;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

public class ConfigPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	// Select lambda for departure time distribution
	private JLabel jLabelDelta;
	private JSlider jSliderDelta;
	private final int deltaMin = 0;
	private final int deltaMax = 120;
	private final int deltaInit = 0;
	
	// Select lambda for departure time distribution
	private JLabel jLabelLambda;
	private JSlider jSliderLambda;
	private final int lambdaMin = 1;
	private final int lambdaMax = 10;
	private final int lambdaInit = 5;
	
	// ButtonGroups for link activation order
	private RadioButtonStateChecker radioButtonStateChecker = new RadioButtonStateChecker();
	private ButtonGroup buttonGroup0 = createJButtonGroup(radioButtonStateChecker.radioButtonItemListener0);
	private ButtonGroup buttonGroup1 = createJButtonGroup(radioButtonStateChecker.radioButtonItemListener1);
	private ButtonGroup buttonGroup2 = createJButtonGroup(radioButtonStateChecker.radioButtonItemListener2);
	private ButtonGroup buttonGroup3 = createJButtonGroup(radioButtonStateChecker.radioButtonItemListener3);
	
	private JLabel jLabel1;
	private JLabel jLabel2;
	private JLabel jLabel3;
	private JLabel jLabel4;
	
	// Run Simulation
	private DemoController demoController;
	
	// Results
	private JLabel jLabelLongestEvacuationTime;
	private JLabel jlabelMeanEvacuationTime;
	
	
	private JTextField jTextField0;

	private final Font defaultFont = new Font("monospaced", Font.PLAIN, 12);
	private final Font labelFont = new Font("Serif", Font.BOLD, 14);
	private final Font borderFont = new Font("monospaced", Font.BOLD, 12);
	private JButton jButtonStartSimulation;
	
	public ConfigPanel() {
		initSimulation();
		initComponents();
	}

	private void initSimulation() {
		demoController = new DemoController(new String[]{});

		// do not dump plans, network and facilities and the end
//		demoController(false);
		
		// overwrite old files
		demoController.setOverwriteFiles(true);
	}
	
	private void initComponents() {
		setLayout(new GridBagLayout());
		GridBagConstraints gridBagConstraints;
		
		/*
		 * delta for departure time
		 */
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabelDelta(), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJSliderDelta(), gridBagConstraints);
		
		/*
		 * lambda for departure time distribution
		 */
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabelLambda(), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJSliderLambda(), gridBagConstraints);
        
		/*
		 * link activation order
		 */
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabel1(), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(createRadioButtonJPanel(buttonGroup0), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabel2(), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(createRadioButtonJPanel(buttonGroup1), gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabel3(), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(createRadioButtonJPanel(buttonGroup2), gridBagConstraints);
	
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabel4(), gridBagConstraints);
        
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(createRadioButtonJPanel(buttonGroup3), gridBagConstraints);
		
		/*
		 * Run the simulation
		 */
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.weightx = 0.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.CENTER;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJButtonStartSimulation(), gridBagConstraints);
		
		/*
		 * Results
		 */
		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabelLongestEvacuationTime(), gridBagConstraints);

		gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new Insets(5, 8, 5, 5);
		this.add(getJLabelMeanEvacuationTime(), gridBagConstraints);
	}

	private JButton getJButtonStartSimulation() {
		if (jButtonStartSimulation == null) {
			jButtonStartSimulation = new JButton();
			jButtonStartSimulation.setPreferredSize(new Dimension(250, 25));
			jButtonStartSimulation.setText("start simulation");
		}
		return jButtonStartSimulation;
	}

	private JTextField getJTextField0() {
		if (jTextField0 == null) {
			jTextField0 = new JTextField();
			//jTextField0.setMinimumSize(new Dimension(250, 25));
			//jTextField0.setMaximumSize(new Dimension(250, 25));
			jTextField0.setPreferredSize(new Dimension(250, 30));
			jTextField0.setFont(labelFont);
			jTextField0.setText("");
		}
		return jTextField0;
	}
		
//	private JLabel getJLabel3() {
//		if (jLabel3 == null) {
//			jLabel3 = new JLabel(new ImageIcon(getClass().getResource("eth-ivt-logo.png")));
//		}
//		return jLabel3;
//		return getJLabel2();
//	}

	private JLabel getJLabelDelta() {
		if (jLabelDelta == null) {
			jLabelDelta = new JLabel();
			jLabelDelta.setFont(labelFont);
			jLabelDelta.setText("Select the \u0394 for the depature time:");
		}
		return jLabelDelta;
	}

	private JSlider getJSliderDelta() {
		if (jSliderDelta == null) {
			jSliderDelta = new JSlider(JSlider.HORIZONTAL, deltaMin, deltaMax, deltaInit);
			
			//Turn on labels at major tick marks.
			jSliderDelta.setMajorTickSpacing(10);
			jSliderDelta.setMinorTickSpacing(5);
			jSliderDelta.setSnapToTicks(true);
			jSliderDelta.setPaintTicks(true);
			jSliderDelta.setPaintLabels(true);
			jSliderDelta.setPreferredSize(new Dimension(500, jSliderDelta.getPreferredSize().height));
			jSliderDelta.setMinimumSize(jSliderDelta.getPreferredSize());
			jSliderDelta.setMaximumSize(jSliderDelta.getPreferredSize());
		}
		
		return jSliderDelta;
	}
	
	private JLabel getJLabelLambda() {
		if (jLabelLambda == null) {
			jLabelLambda = new JLabel();
			jLabelLambda.setFont(labelFont);
			jLabelLambda.setText("Select the \u03BB for the depature time distribution:");
		}
		return jLabelLambda;
	}

	private JSlider getJSliderLambda() {
		if (jSliderLambda == null) {
			jSliderLambda = new JSlider(JSlider.HORIZONTAL, lambdaMin, lambdaMax, lambdaInit);
			
			//Turn on labels at major tick marks.
			jSliderLambda.setMajorTickSpacing(1);
			jSliderLambda.setMinorTickSpacing(1);
			jSliderLambda.setSnapToTicks(true);
			jSliderLambda.setPaintTicks(true);
			jSliderLambda.setPaintLabels(true);	
			jSliderLambda.setPreferredSize(new Dimension(250, jSliderLambda.getPreferredSize().height));
			jSliderLambda.setMinimumSize(jSliderLambda.getPreferredSize());
			jSliderLambda.setMaximumSize(jSliderLambda.getPreferredSize());
		}
		
		return jSliderLambda;
	}

	private JLabel getJLabel1() {
		if (jLabel1 == null) {
			jLabel1 = new JLabel();
			jLabel1.setFont(labelFont);
			jLabel1.setText("Link \"evac1\" activation time:");
		}
		return jLabel1;
	}
	private JLabel getJLabel2() {
		if (jLabel2 == null) {
			jLabel2 = new JLabel();
			jLabel2.setFont(labelFont);
			jLabel2.setText("Link \"evac2\" activation time:");
		}
		return jLabel2;
	}
	private JLabel getJLabel3() {
		if (jLabel3 == null) {
			jLabel3 = new JLabel();
			jLabel3.setFont(labelFont);
			jLabel3.setText("Link \"evac3\" activation time:");
		}
		return jLabel3;
	}
	private JLabel getJLabel4() {
		if (jLabel4 == null) {
			jLabel4 = new JLabel();
			jLabel4.setFont(labelFont);
			jLabel4.setText("Link \"evac4\" activation time:");
		}
		return jLabel4;
	}
	
//	private JPanel getActivateLinksJPanel() {
//		if (activateLinksJPanel == null) {
//			activateLinksJPanel = new JPanel();
//			activateLinksJPanel.setLayout(new GridLayout(4,1));
//			activateLinksJPanel.add(getJLabel1());
//			activateLinksJPanel.add(getJLabel2());
//			activateLinksJPanel.add(getJLabel3());
//			activateLinksJPanel.add(getJLabel4());
//		}
//		return activateLinksJPanel;
//	}
	
	private ButtonGroup createJButtonGroup(ItemListener radioButtonItemListener) {
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton radioButton;
		
		radioButton = new JRadioButton("1");
		radioButton.addItemListener(radioButtonItemListener);
		buttonGroup.add(radioButton);
		radioButtonStateChecker.ones.add(radioButton);

		radioButton = new JRadioButton("2");
		radioButton.addItemListener(radioButtonItemListener);
		buttonGroup.add(radioButton);
		radioButtonStateChecker.twos.add(radioButton);
		
		radioButton = new JRadioButton("3");
		radioButton.addItemListener(radioButtonItemListener);
		buttonGroup.add(radioButton);
		radioButtonStateChecker.threes.add(radioButton);

		radioButton = new JRadioButton("4", true);
		radioButton.addItemListener(radioButtonItemListener);
		buttonGroup.add(radioButton);
		radioButtonStateChecker.fours.add(radioButton);
		
		return buttonGroup;
	}
	
	private JPanel createRadioButtonJPanel(ButtonGroup buttonGroup) {
		JPanel radioButtonJPanel = new JPanel();
		radioButtonJPanel.setLayout(new GridLayout(1,4));
			
		Enumeration<AbstractButton> e;

		e = buttonGroup.getElements();
		while (e.hasMoreElements()) radioButtonJPanel.add(e.nextElement());

		return radioButtonJPanel;
	}

	private JLabel getJLabelLongestEvacuationTime() {
		if (jLabelLongestEvacuationTime == null) {
			jLabelLongestEvacuationTime = new JLabel();
			jLabelLongestEvacuationTime.setFont(labelFont);
			jLabelLongestEvacuationTime.setText("Longest evacuation time:");
		}
		return jLabelLongestEvacuationTime;
	}
	
	private JLabel getJLabelMeanEvacuationTime() {
		if (jlabelMeanEvacuationTime == null) {
			jlabelMeanEvacuationTime = new JLabel();
			jlabelMeanEvacuationTime.setFont(labelFont);
			jlabelMeanEvacuationTime.setText("Mean evacuation time:");
		}
		return jlabelMeanEvacuationTime;
	}
	
//	2011-08-18 17:58:49,223  INFO DemoController:149 Longest evacuation time: 03:08:16
//	2011-08-18 17:58:49,223  INFO DemoController:150 Mean evacuation time: 01:58:43
	
	private static class RadioButtonStateChecker {
		
		/*package*/ RadioButtonItemListener radioButtonItemListener0 = new RadioButtonItemListener(this);
		/*package*/ RadioButtonItemListener radioButtonItemListener1 = new RadioButtonItemListener(this);
		/*package*/ RadioButtonItemListener radioButtonItemListener2 = new RadioButtonItemListener(this);
		/*package*/ RadioButtonItemListener radioButtonItemListener3 = new RadioButtonItemListener(this);
		
		/*package*/ List<JRadioButton> ones = new ArrayList<JRadioButton>();
		/*package*/ List<JRadioButton> twos = new ArrayList<JRadioButton>();
		/*package*/ List<JRadioButton> threes = new ArrayList<JRadioButton>();
		/*package*/ List<JRadioButton> fours = new ArrayList<JRadioButton>();
		
		/*package*/ void checkRadioButtonState(RadioButtonItemListener radioButtonItemListener) {

			/*
			 * get current selection
			 */
			int one = radioButtonItemListener0.selected;
			int two = radioButtonItemListener1.selected;
			int three = radioButtonItemListener2.selected;
			int four = radioButtonItemListener3.selected;
			
			/*
			 * check for all possible combinations whether they are valid or not
			 */
			this.ones.get(0).setEnabled(checkCombination(1, two, three, four));
			this.twos.get(0).setEnabled(checkCombination(2, two, three, four));
			this.threes.get(0).setEnabled(checkCombination(3, two, three, four));
			this.fours.get(0).setEnabled(checkCombination(4, two, three, four));
			
			this.ones.get(1).setEnabled(checkCombination(one, 1, three, four));
			this.twos.get(1).setEnabled(checkCombination(one, 2, three, four));
			this.threes.get(1).setEnabled(checkCombination(one, 3, three, four));
			this.fours.get(1).setEnabled(checkCombination(one, 4, three, four));
			
			this.ones.get(2).setEnabled(checkCombination(one, two, 1, four));
			this.twos.get(2).setEnabled(checkCombination(one, two, 2, four));
			this.threes.get(2).setEnabled(checkCombination(one, two, 3, four));
			this.fours.get(2).setEnabled(checkCombination(one, two, 4, four));
			
			this.ones.get(3).setEnabled(checkCombination(one, two, three, 1));
			this.twos.get(3).setEnabled(checkCombination(one, two, three, 2));
			this.threes.get(3).setEnabled(checkCombination(one, two, three, 3));
			this.fours.get(3).setEnabled(checkCombination(one, two, three, 4));
		}
		
		private boolean checkCombination(int one, int two, int three, int four) {
			
			/*
			 * get distribution
			 */
			int ones = 0;
			int twos = 0;
			int threes = 0;
			int fours = 0;
			
			if (one == 1) ones++;
			else if (one == 2) twos++;
			else if (one == 3) threes++;
			else fours++;
			
			if (two == 1) ones++;
			else if (two == 2) twos++;
			else if (two == 3) threes++;
			else fours++;
			
			if (three == 1) ones++;
			else if (three == 2) twos++;
			else if (three == 3) threes++;
			else fours++;
			
			if (four == 1) ones++;
			else if (four == 2) twos++;
			else if (four == 3) threes++;
			else fours++;
			
			if (ones > 1) return false;
			if (twos > 2) return false; 
			if (threes > 3) return false;
			
			if (ones + twos > 2) return false;
			if (ones + twos + threes > 3) return false;
			
			return true;
		}
	}
	
	private static class RadioButtonItemListener implements ItemListener {

		/*package*/ int selected = 4;
		/*package*/ RadioButtonStateChecker radioButtonChecker;
		
		public RadioButtonItemListener(RadioButtonStateChecker radioButtonChecker) {
			this.radioButtonChecker = radioButtonChecker;
		}
		
		@Override
		public void itemStateChanged(ItemEvent e) {
			JRadioButton radioButton = (JRadioButton) e.getSource();
			if (radioButton.isSelected()) {
				selected = Integer.valueOf(radioButton.getText());
				radioButtonChecker.checkRadioButtonState(this);
			}
		}
	}
	
}
