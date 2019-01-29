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