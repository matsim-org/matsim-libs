/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

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
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import floetteroed.utilities.Time;


/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
class ControlToolbar extends JToolBar implements ActionListener, ItemListener,
		ChangeListener {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private static final String TO_START = "to_start";
	// private static final String STEP_BB = "step_bb";
	private static final String STEP_B = "step_b";
	private static final String PLAY = "play";
	private static final String STEP_F = "step_f";
	// private static final String STEP_FF = "step_ff";
	private static final String TO_END = "to_end";
	private static final String ZOOM_IN = "zoom_in";
	private static final String ZOOM_OUT = "zoom_out";
	private static final String SET_TIME = "set_time";

	private static final String TOGGLE_NODE_LABELS = "node labels";
	private static final String TOGGLE_LINK_LABELS = "link labels";
	private static final String TOGGLE_ANTIALIAS = "anti-alias";

	private static final String PRINT = "print";

	private final NetVis vis;

	private final RenderableDynamicData<VisLink> data;

	private final VisConfig visConfig;

	// -------------------- MEMBERS --------------------

	private Timer movieTimer = null;

	private JButton playButton = null;

	private JFormattedTextField timeField = null;

	private double scale = 1;

	// -------------------- CONSTRUCTION --------------------

	ControlToolbar(final NetVis viz, final RenderableDynamicData<VisLink> data,
			final VisConfig visConfig) {

		super();
		this.vis = viz;
		this.data = data;
		this.visConfig = visConfig;

		if (this.data != null) {
			this.add(this.createButton("|<", TO_START));
			// add(createButton("<<", STEP_BB));
			this.add(this.createButton("<", STEP_B));
			this.playButton = this.createButton("PLAY", PLAY);
			this.add(this.playButton);
			this.add(createButton(">", STEP_F));
			// add(createButton(">>", STEP_FF));
			this.add(createButton(">|", TO_END));

			this.timeField = new JFormattedTextField(new MessageFormat(
					"{0,number,00}-{1,number,00}-{2,number,00}"));
			this.timeField.setMaximumSize(new Dimension(75, 30));
			this.timeField.setActionCommand(SET_TIME);
			this.timeField.setHorizontalAlignment(JTextField.CENTER);
			add(timeField);
			this.timeField.addActionListener(this);
		}

		this.add(this.createButton("--", ZOOM_OUT));
		this.add(this.createButton("+", ZOOM_IN));
		this.add(this.createButton("PRN", PRINT));

		final JCheckBox nodeLabelBox = new JCheckBox(TOGGLE_NODE_LABELS);
		nodeLabelBox.setMnemonic(KeyEvent.VK_N);
		nodeLabelBox.setSelected(visConfig.getShowNodeLabels());
		nodeLabelBox.addItemListener(this);
		this.add(nodeLabelBox);

		final JCheckBox linkLabelBox = new JCheckBox(TOGGLE_LINK_LABELS);
		linkLabelBox.setMnemonic(KeyEvent.VK_L);
		linkLabelBox.setSelected(visConfig.getShowLinkLabels());
		linkLabelBox.addItemListener(this);
		this.add(linkLabelBox);

		final JCheckBox AABox = new JCheckBox(TOGGLE_ANTIALIAS);
		AABox.setMnemonic(KeyEvent.VK_A);
		AABox.setSelected(false);
		AABox.addItemListener(this);
		this.add(AABox);

		final SpinnerNumberModel model = new SpinnerNumberModel(new Integer(
				this.visConfig.getLinkWidthFactor()), new Integer(0),
				new Integer(2000), new Integer(1));
		JLabel l = new JLabel("link width");
		this.add(l);
		JSpinner spin = new JSpinner(model);
		l.setLabelFor(spin);
		this.add(spin);
		spin.setMaximumSize(new Dimension(75, 30));
		spin.addChangeListener(this);
	}

	private JButton createButton(final String display,
			final String actionCommand) {
		final JButton button;
		button = new JButton();
		button.setActionCommand(actionCommand);
		button.addActionListener(this);
		button.setText(display);
		return button;
	}

	// -------------------- SETTERS & GETTERS --------------------

	void setScale(double scale) {
		this.scale = scale;
	}

	double getScale() {
		return scale;
	}

	// -------------------- IMPLEMENTATION --------------------

	private void updateTimeLabel() {
		if (data != null) {
			final int time_s = data.getStartTime_s() + data.currentBin()
					* data.getBinSize_s();
			timeField.setText(Time.strFromSec(time_s));
		}
	}

	void repaintForMovie() {
		this.updateTimeLabel();
	}

	// -------------------- OVERRIDING OF JComponent --------------------

	@Override
	public void paint(Graphics g) {
		if (data != null)
			updateTimeLabel();
		super.paint(g);
	}

	// ---------- IMPLEMENTATION OF ActionListener INTERFACE ----------

	private void stopMovie() {
		if (this.movieTimer != null) {
			this.movieTimer.cancel();
			this.movieTimer = null;
			this.playButton.setText("PLAY");
		}
	}

	private void pressed_TO_START() throws IOException {
		stopMovie();
		this.data.toStart();
	}

	// private void pressed_STEP_BB() throws IOException {
	// stopMovie();
	// reader.toTimeStep(reader.getCurrentTime_s() - SKIP
	// * reader.timeStepLength_s());
	// }

	private void pressed_STEP_B() throws IOException {
		stopMovie();
		this.data.bwd();
	}

	private void pressed_PLAY() {
		if (this.movieTimer == null) {
			this.movieTimer = new Timer();
			MoviePlayer moviePlayer = new MoviePlayer(this.data, this.vis);
			this.movieTimer.schedule(moviePlayer, 0,
					this.visConfig.getDelay_ms());
			this.playButton.setText("STOP");
		} else
			stopMovie();
	}

	private void pressed_STEP_F() throws IOException {
		stopMovie();
		this.data.fwd();
	}

	// private void pressed_STEP_FF() throws IOException {
	// stopMovie();
	// reader.toTimeStep(reader.getCurrentTime_s() + SKIP
	// * reader.timeStepLength_s());
	// }

	private void pressed_TO_END() throws IOException {
		stopMovie();
		this.data.toEnd();
	}

	private void pressed_ZOOM_OUT() {
		this.scale /= 1.42;
		this.vis.scaleNetwork(this.scale);
	}

	private void pressed_ZOOM_IN() {
		this.scale *= 1.42;
		this.vis.scaleNetwork(this.scale);
	}

	private void pressed_PRINT() {
		(new Printer(this.vis.networkComponent(), this.vis.networkScrollPane()
				.getViewport())).run();
	}

	private void changed_SET_TIME(final ActionEvent event) throws IOException {
		final String newTime = ((JFormattedTextField) event.getSource())
				.getText();
		stopMovie();
		this.data.toTime(Time.secFromStr(newTime));
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		final String command = event.getActionCommand();
		try {
			if (TO_START.equals(command)) {
				this.pressed_TO_START();
				// } else if (STEP_BB.equals(command)) {
				// pressed_STEP_BB();
			} else if (STEP_B.equals(command)) {
				this.pressed_STEP_B();
			} else if (PLAY.equals(command)) {
				this.pressed_PLAY();
			} else if (STEP_F.equals(command)) {
				this.pressed_STEP_F();
				// } else if (STEP_FF.equals(command)) {
				// pressed_STEP_FF();
			} else if (TO_END.equals(command)) {
				this.pressed_TO_END();
			} else if (ZOOM_OUT.equals(command)) {
				this.pressed_ZOOM_OUT();
			} else if (ZOOM_IN.equals(command)) {
				this.pressed_ZOOM_IN();
			} else if (PRINT.equals(command)) {
				this.pressed_PRINT();
			} else if (command.equals(SET_TIME)) {
				this.changed_SET_TIME(event);
			}
		} catch (IOException e) {
			System.err.println("ControlToolbar encountered problem: " + e);
		}
		updateTimeLabel();
		repaint();
		this.vis.paintNow();
	}

	// -------------------- IMPLEMENTATION OF ItemListener --------------------

	@Override
	public void itemStateChanged(final ItemEvent e) {
		final JCheckBox source = (JCheckBox) e.getItemSelectable();
		if (source.getText().equals(TOGGLE_NODE_LABELS)) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				this.visConfig.setShowNodeLabels(false);
			} else {
				this.visConfig.setShowNodeLabels(true);
			}
		} else if (source.getText().equals(TOGGLE_LINK_LABELS)) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				this.visConfig.setShowLinkLabels(false);
			} else {
				this.visConfig.setShowLinkLabels(true);
			}
		} else if (source.getText().equals(TOGGLE_ANTIALIAS)) {
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				this.visConfig.setUseAntiAliasing(false);
			} else {
				this.visConfig.setUseAntiAliasing(true);
			}
		}
		repaint();
		this.vis.paintNow();
	}

	// -------------------- IMPLEMENTATION OF ItemListener --------------------

	@Override
	public void stateChanged(final ChangeEvent e) {
		final JSpinner spinner = (JSpinner) e.getSource();
		int i = ((SpinnerNumberModel) spinner.getModel()).getNumber()
				.intValue();
		this.visConfig.setLinkWidthFactor(i);
		repaint();
		this.vis.paintNow();
	}

}
