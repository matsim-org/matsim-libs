// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.checkBoxTree;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;

/**
 * Editor for checkBox Tree
 *
 * @author galo
 */
public class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
    /** SerialVersionUID */
    private static final long serialVersionUID = -8921320784224636657L;

    private final CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();

    private final CheckBoxTree theTree;

    public CheckBoxNodeEditor(final CheckBoxTree tree) {
        theTree = tree;
    }

    @Override
    public Object getCellEditorValue() {
        final CheckBoxNodePanel panel = renderer.getPanel();
        /*final CheckBoxNodeData checkBoxNode =
            new CheckBoxNodeData(panel.label.getText(), panel.check.isSelected());
        return checkBoxNode;
        CheckBoxNodeData data = search(theTree.rootNode(), panel.label.getText());
        data.setSelected(panel.check.isSelected());*/
        return panel.getData();
    }
    /*public CheckBoxNodeData search(DefaultMutableTreeNode node, String name){
        CheckBoxNodeData data = CheckBoxTree.data(node);
        if(data.getText().equals(name)) return data;
        else{
            data = null;
            for(int i=0; i<node.getChildCount() && data==null; i++){
                data = search((DefaultMutableTreeNode)node.getChildAt(i), name);
            }
            return data;
        }
    }*/

    public void addNodeListener(MouseAdapter listener) {
        renderer.addNodeListener(listener);
    }

    @Override
    public boolean isCellEditable(final EventObject event) {
        if (!(event instanceof MouseEvent)) return false;
        final MouseEvent mouseEvent = (MouseEvent) event;

        final TreePath path =
            theTree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
        if (path == null) return false;

        final Object node = path.getLastPathComponent();
        if (!(node instanceof DefaultMutableTreeNode)) return false;
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;

        final Object userObject = treeNode.getUserObject();
        return userObject instanceof CheckBoxNodeData;
    }

    @Override
    public Component getTreeCellEditorComponent(final JTree tree,
        final Object value, final boolean selected, final boolean expanded,
        final boolean leaf, final int row) {

        final Component editor =
            renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf,
                row, true);

        // editor always selected / focused
        final ItemListener itemListener = new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent itemEvent) {
                if (stopCellEditing()) {
                    fireEditingStopped();
                }
            }
        };
        if (editor instanceof CheckBoxNodePanel) {
            final CheckBoxNodePanel panel = (CheckBoxNodePanel) editor;
            panel.check.addItemListener(itemListener);
        }

        return editor;
    }
}
