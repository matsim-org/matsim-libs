// code by jph
package playground.clib.util.gui;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class SpinnerMenu<Type> extends StandardMenu {
    Map<Type, JMenuItem> myMap = new LinkedHashMap<>();
    final SpinnerLabel<Type> mySpinnerLabel;
    final boolean hover;

    SpinnerMenu(SpinnerLabel<Type> mySpinnerLabel, boolean hover) {
        this.mySpinnerLabel = mySpinnerLabel;
        this.hover = hover;
    }

    @Override
    protected void design(JPopupMenu myJPopupMenu) {
        for (Type myType : mySpinnerLabel.myList) {
            JMenuItem myJMenuItem = new JMenuItem(myType.toString());
            if (hover)
                myJMenuItem.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent myMouseEvent) {
                        setValue(myType);
                    }
                });
            myJMenuItem.addActionListener(myActionEvent -> {
                if (!myType.equals(mySpinnerLabel.getValue())) // invoke only when different
                    setValue(myType);
            });
            myMap.put(myType, myJMenuItem);
            myJPopupMenu.add(myJMenuItem);
        }
    }

    private void setValue(Type myType) {
        mySpinnerLabel.setValueSafe(myType);
        mySpinnerLabel.reportToAll();
    }

    public void showRight(JLabel myJLabel) {
        JPopupMenu myJPopupMenu = designShow();
        // ---
        Type myType = mySpinnerLabel.getValue();
        if (myType != null) {
            int delta = 2;
            myMap.get(myType).setBackground(Colors.activeItem); // Colors.gold
            for (Entry<Type, JMenuItem> myEntry : myMap.entrySet()) {
                delta += myEntry.getValue().getPreferredSize().height;
                if (myEntry.getKey().equals(myType)) {
                    delta -= myEntry.getValue().getPreferredSize().height / 2;
                    break;
                }
            }
            Dimension myDimension = myJLabel.getSize();
            myJPopupMenu.show(myJLabel, myDimension.width, myDimension.height / 2 - delta);
        }
    }
}
