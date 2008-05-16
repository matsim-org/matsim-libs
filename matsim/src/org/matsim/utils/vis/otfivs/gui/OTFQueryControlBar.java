package org.matsim.utils.vis.otfivs.gui;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfivs.interfaces.OTFQueryHandler;


public class OTFQueryControlBar extends JToolBar implements ActionListener, ItemListener, ChangeListener {

	private final OTFQueryHandler handler;
	private String queryType = "Agent";
	private final OTFVisConfig cfg;
	
	public OTFQueryControlBar(String name, OTFQueryHandler handler) {
		super(name);
		this.handler = handler;
		this.cfg = (OTFVisConfig)Gbl.getConfig().getModule("otfvis");
		{
			JLabel jLabel3 = new JLabel();
			add(jLabel3);
			jLabel3.setText("Query:");
			jLabel3.setBounds(344, 45, 36, 31);
		}
		{
			ComboBoxModel leftMFuncModel = 
				new DefaultComboBoxModel(
						new String[] { "Agent", "Spinne", "None" });
			leftMFuncModel.setSelectedItem(queryType);
			JComboBox queryType = new JComboBox();
			add(queryType);
			queryType.setActionCommand("type_changed");
			queryType.setModel(leftMFuncModel);
			queryType.setBounds(57, 76, 92, 27);
			queryType.setMaximumSize(new Dimension(250,60));
			queryType.addActionListener(this);
		}
		{
			JLabel jLabel3 = new JLabel();
			add(jLabel3);
			jLabel3.setText(" Id:");
			JTextField text = new JTextField();
			add(text);
			text.setActionCommand("id_changed");
			text.setMaximumSize(new Dimension(350,40));
			text.addActionListener(this);
		}
		{
			JLabel jLabel3 = new JLabel();
			add(jLabel3);
			jLabel3.setText("  ");

			JButton button = new JButton();
			button.setText("Clear!");
			button.setActionCommand("clear");
			button.addActionListener(this);
		    button.setToolTipText("Clears all queries!");
		    add(button);
		    
			JCheckBox SynchBox = new JCheckBox("multiple select");
			SynchBox.setMnemonic(KeyEvent.VK_M);
			SynchBox.setSelected(cfg.isMultipleSelect());
			SynchBox.addItemListener(this);
			add(SynchBox);

		}
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if("id_changed".equals(command)) {
			String id = ((JTextField)e.getSource()).getText();
			
			if (!cfg.isMultipleSelect())handler.removeQueries();
			
			handler.handleIdQuery(id, cfg.getQueryType());
		} else if ("type_changed".equals(command)) {
			JComboBox cb = (JComboBox)e.getSource();
	        queryType = (String)cb.getSelectedItem();
	        cfg.setQueryType(queryType);
	        handler.removeQueries();
		} else if ("clear".equals(command)) {
			handler.removeQueries();
		}
        
	}

	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals("multiple select")) {
			cfg.setMultipleSelect(e.getStateChange() != ItemEvent.DESELECTED);
		}
	}

	public void stateChanged(ChangeEvent e) {

	}
	
}
