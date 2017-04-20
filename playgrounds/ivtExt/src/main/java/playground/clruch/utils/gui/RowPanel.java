// code by jph
package playground.clruch.utils.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

public final class RowPanel {
    private GridBagLayout gridBagLayout = new GridBagLayout();
    public JPanel jPanel = new JPanel(gridBagLayout);
    private GridBagConstraints gridBagConstraints = new GridBagConstraints();

    public RowPanel() {
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1;
        jPanel.setOpaque(false);
    }

    public void add(JComponent myJComponent) {
        ++gridBagConstraints.gridy; // initially -1
        gridBagLayout.setConstraints(myJComponent, gridBagConstraints);
        jPanel.add(myJComponent);
        jPanel.repaint();
    }
}
