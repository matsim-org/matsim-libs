/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQueryControl
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

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;


/**
 * @author dgrether
 *
 */
public class OTFQueryControlToolBar extends JToolBar implements ActionListener, ItemListener {

	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(OTFQueryControlToolBar.class);

	private JTextField textField;
    private JTabbedPane pane;

	private final OTFVisConfigGroup config;

	private final OTFQueryControl queryControl;


	public OTFQueryControlToolBar(OTFQueryControl queryControl, final OTFVisConfigGroup config) {
		super();
        this.setFloatable(false);
		this.config = config;
		this.queryControl = queryControl;
        this.pane = new JTabbedPane();
        this.add(pane);
        JPanel panel = new JPanel();
        panel.setSize(500, 60);
        {
            JLabel jLabel3 = new JLabel();
            panel.add(jLabel3);
            jLabel3.setText("Query:");
            jLabel3.setBounds(344, 45, 36, 31);
        }
        {
            Vector<QueryEntry> queries = this.queryControl.getQueries();
            JComboBox queryTypeComboBox = new JComboBox(queries);
            queryTypeComboBox.setActionCommand("type_changed");
            queryTypeComboBox.setBounds(57, 76, 92, 27);
            queryTypeComboBox.setMaximumSize(new Dimension(250, 60));
            queryTypeComboBox.addActionListener(this);
            queryTypeComboBox.setToolTipText(queries.get(0).toolTip);
            panel.add(queryTypeComboBox);
            this.config.setQueryType(queries.get(0).clazz.getCanonicalName());
        }
        {
            JLabel jLabel3 = new JLabel();
            panel.add(jLabel3);
            jLabel3.setText(" Id:");
            textField = new JTextField();
            panel.add(textField);
            textField.setActionCommand("id_changed");
            textField.setPreferredSize(new Dimension(350, 30));
            textField.setMaximumSize(new Dimension(350, 40));
            textField.addActionListener(this);
        }
        {
            JLabel jLabel3 = new JLabel();
            panel.add(jLabel3);
            jLabel3.setText("  ");

            JButton button = new JButton();
            button.setText("Clear!");
            button.setActionCommand("clear");
            button.addActionListener(this);
            button.setToolTipText("Clears all queries!");
            panel.add(button);

            JCheckBox SynchBox = new JCheckBox("multiple select");
            SynchBox.setMnemonic(KeyEvent.VK_M);
            SynchBox.setSelected(this.config.isMultipleSelect());
            SynchBox.addItemListener(this);
            panel.add(SynchBox);
        }
        panel.doLayout();
        panel.setBounds(250, 130, 220, 160);
        pane.addTab("Query", panel);
    }

    @Override
	public void actionPerformed(ActionEvent e) {
		OTFVisConfigGroup cfg = this.config;
		String command = e.getActionCommand();
		if("id_changed".equals(command)) {
			String id = ((JTextField)e.getSource()).getText();
			log.debug("action performed on textfield with text " + id);
			if (!cfg.isMultipleSelect()){
				this.queryControl.removeQueries();
			}
			this.queryControl.handleIdQuery(id.trim(), cfg.getQueryType());
		} 
		else if ("type_changed".equals(command)) {
			JComboBox cb = (JComboBox)e.getSource();
			QueryEntry queryType = (QueryEntry)cb.getSelectedItem();
			cfg.setQueryType(queryType.clazz.getCanonicalName());
			this.queryControl.removeQueries();
			OTFQuery test = this.queryControl.createQuery(queryType.clazz.getCanonicalName());

			if(pane.getTabCount() > 1){
				pane.remove(1);
			}

			if(test instanceof OTFQueryOptions) {
				JComponent settings = ((OTFQueryOptions)test).getOptionsGUI(pane);
				pane.addTab("Options", settings);
			}

			cb.setToolTipText(queryType.toolTip);
		} else if ("clear".equals(command)) {
			this.queryControl.removeQueries();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBox source = (JCheckBox)e.getItemSelectable();
		if (source.getText().equals("multiple select")) {
			this.config.setMultipleSelect(e.getStateChange() != ItemEvent.DESELECTED);
		}
	}


	public synchronized JTextField getTextField() {
		return textField;
	} 
}
