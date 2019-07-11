
/* *********************************************************************** *
 * project: org.matsim.*
 * SaveFileSaver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.run.gui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * @author mrieser / Senozon AG
 */
/*package*/ class SaveFileSaver extends JFileChooser {
	private static final long serialVersionUID = 1L;

	@Override
  public void approveSelection() {
    File f = getSelectedFile();
    if (f.exists() && getDialogType() == SAVE_DIALOG) {
      String msg = String.format(
          "<html>“%s” already exists.<br>Do you want to replace it?",
          f.getName());
      int option = JOptionPane.showOptionDialog(this, msg, "Save As", JOptionPane.YES_NO_OPTION,
      		JOptionPane.WARNING_MESSAGE, null, new String[] {"Replace", "Cancel"}, "Cancel");
      if (option != 0) {
        return;
      }
    }
    super.approveSelection();
  }
}