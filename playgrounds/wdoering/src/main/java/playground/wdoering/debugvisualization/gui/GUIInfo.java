package playground.wdoering.debugvisualization.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Panel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import playground.wdoering.debugvisualization.controller.Controller;
import playground.wdoering.debugvisualization.gui.GUIToolbar.ActionPause;
import playground.wdoering.debugvisualization.gui.GUIToolbar.ActionPlay;
import playground.wdoering.debugvisualization.gui.GUIToolbar.ActionRewind;
import playground.wdoering.debugvisualization.model.Agent;
import playground.wdoering.debugvisualization.model.XYVxVyAgent;
import playground.wdoering.debugvisualization.model.XYVxVyDataPoint;

public class GUIInfo extends JPanel {

	private Controller controller;
	private JLabel labelTitle;
	private LinkedList<Double> timeSteps;
	private HashMap<String, Agent> agents;
	private JPanel agentList = new JPanel(new GridLayout());
	private JTable agentTable;
	private String[] columnNames = { "ID", "X", "Y", "Vx", "Vy" };

	private HashMap<String, Object[]> displayedAgents;
	private boolean updateData = true;
	
	DefaultTableModel tableModel;

	public GUIInfo(Controller controller) {
		this.controller = controller;

		this.setSize(384, 600);

		setLayout(new BorderLayout());
		// Container content = this.getContentPane();
		labelTitle = new JLabel("Agent Data");

		// buttonRewind = new JButton("rewind");
		// buttonPause = new JButton("pause");
		// buttonPlay = new JButton("play");

		JPanel panelButtons = new JPanel(new GridLayout(3, 0));

		// buttonPlay.addActionListener(new ActionPlay());
		// buttonPause.addActionListener(new ActionPause());
		// buttonRewind.addActionListener(new ActionRewind());

		// panelButtons.add(buttonRewind);
		// panelButtons.add(buttonPause);
		// panelButtons.add(buttonPlay);

		displayedAgents = new HashMap<String, Object[]>();

		Object[][] data = { { "0", "12", "32", "13", "23" },
				{ "1", "b12", "s32", "1x3", "323" },
				{ "2", "1a2", "3g2", "1j3", "23j" } };

		agentTable = new JTable(data, columnNames);
		agentTable.setSize(200, 200);

		tableModel = new DefaultTableModel(columnNames, 1);

		agentTable = new JTable(tableModel);

		add(new JScrollPane(agentTable), BorderLayout.CENTER);

		add(labelTitle, BorderLayout.NORTH);

	}

	public void updateView(LinkedList<Double> timeSteps,
			HashMap<String, Agent> agents) {
		
		if (updateData)
		{
	
			// update values
			this.timeSteps = timeSteps;
			this.agents = agents;
			Iterator agentsIterator = agents.entrySet().iterator();
			// While there are still agents in the agents array
	
			ArrayList<JPanel> agentPanels = new ArrayList<JPanel>();
	
			if ((agents != null) && (agents.size() > 0)) {
	
				String agentIDs = "";
	
				//tableModel = new DefaultTableModel(columnNames, 1);
	
				while (agentsIterator.hasNext()) {
	
					Map.Entry pairs = null;
					// Get current agent
					pairs = (Map.Entry) agentsIterator.next();
					Agent currentAgent = (XYVxVyAgent) pairs.getValue();
					String currentAgentID = (String) pairs.getKey();
					agentIDs = agentIDs + " | " + currentAgentID;
	
					XYVxVyDataPoint dataPoint = (XYVxVyDataPoint) currentAgent
							.getDataPoint(timeSteps.getLast());
	
					if (dataPoint != null) {
	
						Object rowData[] = { currentAgentID,
								dataPoint.getPosX().toString().substring(0, 7),
								dataPoint.getPosY().toString().substring(0, 7),
								dataPoint.getvX().toString().substring(0, 5),
								dataPoint.getvY().toString().substring(0, 5) };
	
						displayedAgents.put(currentAgentID, rowData);
	
					}
	
					/*
					 * 
					 * JPanel agentPanel = new JPanel(new BorderLayout()); JLabel
					 * label = new JLabel(currentAgentID); JLabel dataLabel = new
					 * JLabel("dp count:"+currentAgent.getDataPoints().size());
					 * 
					 * agentPanel.setBackground(new Color(255,0,0));
					 * 
					 * agentPanel.add(label, BorderLayout.CENTER);
					 * agentPanel.add(dataLabel, BorderLayout.SOUTH);
					 * 
					 * agentPanels.add(agentPanel);
					 */
	
				}
	
				Iterator displayedDataIterator = displayedAgents.entrySet()
						.iterator();
	
				Object[][] data = new Object[displayedAgents.size()][5];
				
				int i = 0;
				while (displayedDataIterator.hasNext())
				{
					Map.Entry pairs = (Map.Entry) displayedDataIterator.next();
					Object[] rowData = (Object[]) pairs.getValue();
					
					for (int j = 0; j < rowData.length; j++)
						data[i][j] = rowData[j];
					
					i++;
					
//					for (int i = 0; i < tableModel.getRowCount(); i++) 
//					{
//					
//						if (tableModel.getValueAt(i, 0).equals(rowData[0]))
//						{
//							
//						}
//						//System.out.println();
//						
//					}
	
					// System.out.println(rowData.toString());
	
//					tableModel.addRow(rowData);
				}
				
				//tableModel.setDataVector(data, columnNames);
				
				//agentTable.setModel(tableModel);
	
				/*
				 * agentList = new JPanel(new GridLayout(agentPanels.size(), 0));
				 * 
				 * for (JPanel agentPanel : agentPanels) agentList.add(agentPanel);
				 * 
				 * this.setBackground(new
				 * Color(40*agents.size(),40*agents.size(),40*agents.size()));
				 */
	
				labelTitle.setText(agentIDs);
			}
		}

		// this.update(this.getGraphics());

		// TODO Auto-generated method stub

	}

	public void disableUpdate(boolean b) {
		this.updateData = !b;

	}

}
