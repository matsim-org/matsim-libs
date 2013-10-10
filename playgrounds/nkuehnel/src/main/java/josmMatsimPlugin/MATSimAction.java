package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Adds the MATSim button to the tools bar.
 * @author nkuehnel
 * 
 */
public class MATSimAction extends JosmAction
{

	public JDialog dlg;

	public MATSimAction() {
        super(tr("MATSim"), "images/dialogs/logo.png",
        tr("Import or Export MATSim network file"),
        Shortcut.registerShortcut("menu:matsimexport", tr("Menu: {0}", tr("MATSim Export")),
        KeyEvent.VK_G, Shortcut.ALT_CTRL), false);
    }

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {
		dlg = new JDialog();
		MATSimDialog dialog = new MATSimDialog(dlg);
       
        dlg.setTitle("MATSim");
        dlg.add(dialog);
        dlg.pack();
        dlg.setLocationRelativeTo(Main.parent);
        dlg.setVisible(true);
	}
	
}
