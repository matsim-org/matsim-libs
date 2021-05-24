package org.matsim.run.gui;

import java.awt.HeadlessException;
import java.io.File;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.utils.TransitScheduleValidator;

/**
 * @author mrieser / Simunto GmbH
 */
public class ScheduleValidatorWindow extends JDialog {

	private File lastUsedDirectory;
	private final JTextField txtScheduleFilename;
	private final JTextField txtNetworkFilename;
	private final DefaultTableModel resultTableModel;
	private final JTable resultTable;
	private final JButton btnValidate;

	public ScheduleValidatorWindow(JFrame parent) throws HeadlessException {
		super(parent);
		setTitle("Transit Schedule Validator");

		// UI elements
		JLabel lblSchedule = new JLabel("Transit Schedule:");
		JLabel lblNetwork = new JLabel("Network (optional):");
		JLabel lblOutput = new JLabel("Output");

		this.txtScheduleFilename = new JTextField("", 20);
		this.txtNetworkFilename = new JTextField("", 20);

		JButton btnChooseSchedule = new JButton("Choose");
		JButton btnChooseNetwork = new JButton("Choose");

		this.btnValidate = new JButton("Validate");

		this.resultTableModel = new DefaultTableModel(0, 2) {
			@Override
			public String getColumnName(int column) {
				return new String[] { "Type", "Message" }[column];
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		this.resultTable = new JTable(this.resultTableModel);
		this.resultTable.getColumnModel().getColumn(0).setWidth(150);
		this.resultTable.getColumnModel().getColumn(0).setMaxWidth(250);
		JScrollPane outputPane = new JScrollPane(this.resultTable);

		// behavior

		this.lastUsedDirectory = new File(".");

		btnChooseSchedule.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(this.lastUsedDirectory);
			int result = chooser.showOpenDialog(null);
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				this.lastUsedDirectory = f.getParentFile();
				this.txtScheduleFilename.setText(f.getAbsolutePath());
			}
		});

		btnChooseNetwork.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(this.lastUsedDirectory);
			int result = chooser.showOpenDialog(null);
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				this.lastUsedDirectory = f.getParentFile();
				this.txtNetworkFilename.setText(f.getAbsolutePath());
			}
		});

		this.btnValidate.addActionListener(e -> run());

		// layout

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup()
						.addGroup(groupLayout.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup()
										.addComponent(lblSchedule)
										.addComponent(lblNetwork)
										.addComponent(this.btnValidate)
										.addComponent(lblOutput))
								.addGroup(groupLayout.createParallelGroup()
										.addComponent(this.txtScheduleFilename)
										.addComponent(this.txtNetworkFilename))
								.addGroup(groupLayout.createParallelGroup()
										.addComponent(btnChooseSchedule)
										.addComponent(btnChooseNetwork)))
						.addComponent(outputPane, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE, Short.MAX_VALUE))
				.addContainerGap());

		groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblSchedule)
						.addComponent(this.txtScheduleFilename)
						.addComponent(btnChooseSchedule))
				.addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblNetwork)
						.addComponent(this.txtNetworkFilename)
						.addComponent(btnChooseNetwork))
				.addGroup(
						groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(this.btnValidate))
				.addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblOutput))
				.addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(outputPane, 200, Short.MAX_VALUE, Short.MAX_VALUE))
				.addContainerGap());

		getContentPane().setLayout(groupLayout);

		this.pack();
		this.setSize(800, 600);
	}

	public void loadFromConfig(Config config, File configDirectory) {
		String scheduleFilename = config.transit().getTransitScheduleFile();
		if (scheduleFilename != null) {
			this.txtScheduleFilename.setText(new File(configDirectory, scheduleFilename).getAbsolutePath());
		}
		String networkFilename = config.network().getInputFile();
		if (networkFilename != null) {
			this.txtNetworkFilename.setText(new File(configDirectory, networkFilename).getAbsolutePath());
		}
	}

	public void run() {
		this.btnValidate.setEnabled(false);
		this.resultTableModel.setRowCount(0);
		this.resultTableModel.addRow(new Object[] { "", "Validating transit schedule..." });

		new Thread(() -> {
			String scheduleFilename = this.txtScheduleFilename.getText();
			String networkFilename = this.txtNetworkFilename.getText();

			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			TransitSchedule schedule = null;
			Network network = null;

			if (networkFilename != null) {
				network = scenario.getNetwork();
				new MatsimNetworkReader(network).readFile(networkFilename);
			}
			if (scheduleFilename != null) {
				schedule = scenario.getTransitSchedule();
				new TransitScheduleReader(scenario).readFile(scheduleFilename);
			}

			TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateAll(schedule, network);
			SwingUtilities.invokeLater(() -> {
				this.resultTableModel.setRowCount(0);
				if (result.isValid()) {
					this.resultTableModel.addRow(new Object[] { "SUCCESS", "The schedule appears valid" });
				}
				for (String message : result.getWarnings()) {
					this.resultTableModel.addRow(new Object[] { "WARNING", message });
				}
				for (String message : result.getErrors()) {
					this.resultTableModel.addRow(new Object[] { "ERROR", message });
				}
				this.btnValidate.setEnabled(true);
			});
		}).start();

	}

	public static void main(String[] args) {
		new ScheduleValidatorWindow(null).setVisible(true);
	}
}
