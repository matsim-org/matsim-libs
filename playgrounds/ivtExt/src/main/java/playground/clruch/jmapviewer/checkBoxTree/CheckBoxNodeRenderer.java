// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.checkBoxTree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 * Renderer for checkBox Tree
 *
 * @author galo
 */
public class CheckBoxNodeRenderer implements TreeCellRenderer {

    private final CheckBoxNodePanel panel = new CheckBoxNodePanel();
    private final DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();
    private final Color selectionForeground;
    private final Color selectionBackground;
    private final Color textForeground;
    private final Color textBackground;

    /**
     * Constructs a new {@code CheckBoxNodeRenderer}.
     */
    public CheckBoxNodeRenderer() {
        final Font fontValue = UIManager.getFont("Tree.font");
        if (fontValue != null) panel.getLabel().setFont(fontValue);

        final Boolean focusPainted =
            (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
        panel.check.setFocusPainted(focusPainted != null && focusPainted);

        selectionForeground = UIManager.getColor("Tree.selectionForeground");
        selectionBackground = UIManager.getColor("Tree.selectionBackground");
        textForeground = UIManager.getColor("Tree.textForeground");
        textBackground = UIManager.getColor("Tree.textBackground");
    }

    protected CheckBoxNodePanel getPanel() {
        return panel;
    }

    public void addNodeListener(MouseAdapter listener) {
        panel.addMouseListener(listener);
    }

    // -- TreeCellRenderer methods --

    @Override
    public Component getTreeCellRendererComponent(final JTree tree,
        final Object value, final boolean selected, final boolean expanded,
        final boolean leaf, final int row, final boolean hasFocus) {
        CheckBoxNodeData data = null;
        if (value instanceof DefaultMutableTreeNode) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            final Object userObject = node.getUserObject();
            if (userObject instanceof CheckBoxNodeData) {
                data = (CheckBoxNodeData) userObject;
            }
        }

        //final String stringValue =
        //    tree.convertValueToText(value, selected, expanded, leaf, row, false);
        //panel.label.setText(stringValue);

        panel.setSelected(Boolean.FALSE);

        panel.setEnabled(tree.isEnabled());

        if (selected) {
            panel.setForeground(selectionForeground);
            panel.setBackground(selectionBackground);
            panel.getLabel().setForeground(selectionForeground);
            panel.getLabel().setBackground(selectionBackground);
        } else {
            panel.setForeground(textForeground);
            panel.setBackground(textBackground);
            panel.getLabel().setForeground(textForeground);
            panel.getLabel().setBackground(textBackground);
        }

        if (data == null) {
            // not a check box node; return default cell renderer
            return defaultRenderer.getTreeCellRendererComponent(tree, value,
                selected, expanded, leaf, row, hasFocus);
        }

        //panel.label.setText(data.getText());
        panel.setData(data);
        panel.setSelected(data.isSelected());

        return panel;
    }
/*
    private JPopupMenu createPopupMenu(final AbstractLayer layer) {
        JMenuItem menuItem;

        //Create the popup menu.
        if (layer.isVisibleTexts()) menuItem = new JMenuItem("hide texts");
        else menuItem = new JMenuItem("show texts");
        JPopupMenu popup = new JPopupMenu();
        popup.add(menuItem);
        menuItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setVisibleTexts(layer, !layer.isVisibleTexts());
            }
        });

        return popup;
    }

    private void setVisibleTexts(AbstractLayer layer, boolean visible) {
        layer.setVisibleTexts(visible);
        if (layer instanceof LayerGroup) {
            LayerGroup group = (LayerGroup) layer;
            if (group.getLayers() != null)
                for (AbstractLayer al : group.getLayers()) {
                    setVisibleTexts(al, visible);
                }
        }
    }
*/
}
