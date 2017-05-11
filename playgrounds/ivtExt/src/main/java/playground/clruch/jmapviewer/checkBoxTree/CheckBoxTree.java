// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.checkBoxTree;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import playground.clruch.jmapviewer.AbstractLayer;
import playground.clruch.jmapviewer.Layer;
import playground.clruch.jmapviewer.LayerGroup;

/**
 * JTree for checkBox Tree Layers
 *
 * @author galo
 */
public class CheckBoxTree extends JTree {
    /** Serial Version UID */
    private static final long serialVersionUID = 6943401106938034256L;

    private final CheckBoxNodeEditor editor;

    // public CheckBoxTree(AbstractLayer layer) {
    // this(new CheckBoxNodeData(layer));
    // }

    public CheckBoxTree(String rootName) {
        this(new CheckBoxNodeData(rootName));
    }

    public CheckBoxTree(CheckBoxNodeData root) {
        this(new DefaultMutableTreeNode(root));
    }

    public CheckBoxTree(DefaultMutableTreeNode node) {
        super(new DefaultTreeModel(node));

        final CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
        setCellRenderer(renderer);

        editor = new CheckBoxNodeEditor(this);
        setCellEditor(editor);
        setEditable(true);

        // listen for changes in the model (including check box toggles)
        getModel().addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(final TreeModelEvent e) {
                DefaultTreeModel model = (DefaultTreeModel) e.getSource();
                Object[] nodes = e.getChildren();
                DefaultMutableTreeNode node;
                if (nodes == null || nodes.length == 0) {
                    node = node(model.getRoot());
                } else {
                    node = node(nodes[0]);
                }
                nodeChanged(node);
                repaint();
            }

            @Override
            public void treeNodesInserted(final TreeModelEvent e) {
                // System.out.println("nodes inserted");
            }

            @Override
            public void treeNodesRemoved(final TreeModelEvent e) {
                // System.out.println("nodes removed");
            }

            @Override
            public void treeStructureChanged(final TreeModelEvent e) {
                // System.out.println("structure changed");
            }
        });
    }

    public void addNodeListener(MouseAdapter listener) {
        editor.addNodeListener(listener);
    }

    public static void main(final String[] args) {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new CheckBoxNodeData("Root", Boolean.TRUE));

        final DefaultMutableTreeNode accessibility = add(root, "Accessibility", true);
        add(accessibility, "Move system caret with focus/selection changes", false);
        add(accessibility, "Always expand alt text for images", true);
        root.add(accessibility);

        final DefaultMutableTreeNode browsing = new DefaultMutableTreeNode(new CheckBoxNodeData("Browsing", null));
        add(browsing, "Notify when downloads complete", true);
        add(browsing, "Disable script debugging", true);
        add(browsing, "Use AutoComplete", true);
        add(browsing, "Browse in a new process", false);
        root.add(browsing);

        final CheckBoxTree tree = new CheckBoxTree(root);
        ((DefaultMutableTreeNode) tree.getModel().getRoot()).add(new DefaultMutableTreeNode(new CheckBoxNodeData("gggg", null)));
        ((DefaultTreeModel) tree.getModel()).reload();
        // listen for changes in the selection
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(final TreeSelectionEvent e) {
                // System.out.println("selection changed");
            }
        });
        // show the tree on screen
        final JFrame frame = new JFrame("CheckBox Tree");
        final JScrollPane scrollPane = new JScrollPane(tree);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setVisible(true);
    }

    private static Boolean childStatus(DefaultMutableTreeNode node) {
        Boolean status = data(node.getChildAt(0)).isSelected();
        for (int i = 1; i < node.getChildCount() && status != null; i++) {
            if (!status.equals(data(node.getChildAt(i)).isSelected()))
                return null;
        }
        return status;
    }

    private static void changeParents(DefaultMutableTreeNode node) {
        if (node != null) {
            DefaultMutableTreeNode parent = node(node.getParent());
            if (parent != null) {
                CheckBoxNodeData dataParent = data(parent);
                Boolean childStatus = childStatus(parent);
                if (childStatus != null && !childStatus.equals(dataParent.isSelected())) {
                    dataParent.setSelected(childStatus);
                    changeParents(parent);
                }
            }
        }
    }

    private static void nodeChanged(DefaultMutableTreeNode node) {
        if (node != null) {
            changeParents(node);
            setChildrens(node, data(node).isSelected());
        }
    }

    private static void setChildrens(DefaultMutableTreeNode node, Boolean value) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = node(node.getChildAt(i));
            if (!data(childNode).isSelected().equals(data(node).isSelected())) {
                data(childNode).setSelected(data(node).isSelected());
                setChildrens(childNode, value);
            }
        }
    }

    public DefaultMutableTreeNode rootNode() {
        return node(getModel().getRoot());
    }

    public LayerGroup rootLayer() {
        return (LayerGroup) rootData().getAbstractLayer();
    }

    public CheckBoxNodeData rootData() {
        return data(rootNode());
    }

    private static DefaultMutableTreeNode node(Object node) {
        return (DefaultMutableTreeNode) node;
    }

    public static CheckBoxNodeData data(DefaultMutableTreeNode node) {
        return node == null ? null : (CheckBoxNodeData) node.getUserObject();
    }

    private static CheckBoxNodeData data(Object node) {
        return data(node(node));
    }

    private static DefaultMutableTreeNode add(final DefaultMutableTreeNode parent, final String text, final boolean checked) {
        final CheckBoxNodeData data = new CheckBoxNodeData(text, checked);
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
        parent.add(node);
        return node;
    }

    public static CheckBoxNodeData createNodeData(AbstractLayer layer) {
        return new CheckBoxNodeData(layer);
    }

    public static DefaultMutableTreeNode createNode(AbstractLayer layer) {
        return new DefaultMutableTreeNode(createNodeData(layer));
    }

    /*
     * public DefaultMutableTreeNode addLayerGroup(LayerGroup group) {
     * if (group != null){
     * if (group.getParent() == null){
     * return add(rootNode(), group);
     * } else {
     * DefaultMutableTreeNode parentGroup = searchNode(group.getParent());
     * if(parentGroup==null) parentGroup = addLayerGroup(group.getParent());
     * DefaultMutableTreeNode node = add(parentGroup, group);
     * return node;
     * }
     * }else return null;
     * }
     */

    public Layer addLayer(String name) {
        Layer layer = new Layer(name);
        addLayer(layer);
        return layer;
    }

    public DefaultMutableTreeNode addLayer(AbstractLayer layer) {
        if (layer != null) {
            DefaultMutableTreeNode parent;
            if (layer.getParent() == null) {
                rootLayer().add(layer);
                parent = rootNode();
            } else {
                parent = searchNode(layer.getParent());
                if (parent == null)
                    parent = addLayer(layer.getParent());
            }
            return add(parent, layer);
        } else
            return null;
    }

    public DefaultMutableTreeNode add(DefaultMutableTreeNode parent, final AbstractLayer layer) {
        layer.setVisible(data(parent).isSelected());
        DefaultMutableTreeNode node = createNode(layer);
        parent.add(node);
        ((DefaultTreeModel) getModel()).reload();
        // System.out.println("Created node "+layer+" upper of "+data(parent));
        return node;
    }

    public DefaultMutableTreeNode searchNode(AbstractLayer layer) {
        return searchNode(rootNode(), layer);
    }

    public DefaultMutableTreeNode searchNode(DefaultMutableTreeNode node, AbstractLayer layer) {
        CheckBoxNodeData data = CheckBoxTree.data(node);
        if (data.getAbstractLayer() == layer)
            return node;
        else {
            DefaultMutableTreeNode found = null;
            for (int i = 0; i < node.getChildCount() && found == null; i++) {
                found = searchNode((DefaultMutableTreeNode) node.getChildAt(i), layer);
            }
            return found;
        }
    }
}
