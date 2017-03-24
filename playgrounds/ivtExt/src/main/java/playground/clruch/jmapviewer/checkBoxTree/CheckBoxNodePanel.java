// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.checkBoxTree;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Node Panel for checkBox Tree
 *
 * @author galo
 */
public class CheckBoxNodePanel extends JPanel {
    /** Serial Version UID */
    private static final long serialVersionUID = -7236481597785619029L;
    private final JLabel label = new JLabel();
    private CheckBoxNodeData data;
    public final JCheckBox check = new JCheckBox();

    public CheckBoxNodePanel() {
        this.check.setMargin(new Insets(0, 0, 0, 0));
        setLayout(new BorderLayout());
        add(check, BorderLayout.WEST);
        add(label, BorderLayout.CENTER);
    }

    public void setSelected(Boolean bool) {
        if (bool == null) {
            check.getModel().setPressed(true);
            check.getModel().setArmed(true);
        } else {
            check.setSelected(bool.booleanValue());
            check.getModel().setArmed(false);
        }
    }

    public CheckBoxNodeData getData() {
        data.setSelected(check.isSelected());
        return data;
    }

    public void setData(CheckBoxNodeData data) {
        this.data = data;
        label.setText(data.getText());
    }

    public JLabel getLabel() {
        return label;
    }
}
