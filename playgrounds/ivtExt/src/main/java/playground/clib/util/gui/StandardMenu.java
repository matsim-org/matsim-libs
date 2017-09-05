// code by jph
package playground.clib.util.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public abstract class StandardMenu {
    public static <Type extends StandardMenu> void bind(JButton myJButton, Supplier<Type> mySupplier) {
        myJButton.addActionListener(new ActionListener() {
            long tic = System.nanoTime();

            @Override
            public void actionPerformed(ActionEvent myActionEvent) {
                long toc = System.nanoTime();
                if (500_000_000L < toc - tic) {
                    StandardMenu myStandardMenu = mySupplier.get();
                    myStandardMenu.myJPopupMenu.addPopupMenuListener(new PopupMenuListener() {
                        @Override
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            // System.out.println("popupMenuWillBecomeVisible");
                        }

                        @Override
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                            // System.out.println("popupMenuWillBecomeInvisible");
                            tic = System.nanoTime();
                        }

                        @Override
                        public void popupMenuCanceled(PopupMenuEvent e) {
                            // System.out.println("popupMenuCanceled");
                            tic = System.nanoTime();
                        }
                    });
                    myStandardMenu.south(myJButton);
                }
            }
        });
    }

    // ---
    protected abstract void design(JPopupMenu myJPopupMenu);

    private JPopupMenu myJPopupMenu = new JPopupMenu();

    protected final JPopupMenu designShow() { // TODO misnomer & why return?
        design(myJPopupMenu);
        return myJPopupMenu;
    }

    /** non-blocking
     * 
     * @param myJComponent */
    public void south(JComponent myJComponent) {
        designShow().show(myJComponent, 0, myJComponent.getSize().height);
    }

    /** placement typically avoids that menu is created over mouse pointer
     * 
     * @param myJComponent */
    public void southEast(JComponent myJComponent) {
        designShow().show(myJComponent, myJComponent.getSize().width, myJComponent.getSize().height);
    }

    public void showRelative(JComponent myJComponent, Rectangle myRectangle) {
        designShow().show(myJComponent, myRectangle.x + myRectangle.width, myRectangle.y);
    }

    public void atMouse(JComponent myJComponent) {
        Point myMouse = DisplayHelper.getMouseLocation();
        Point myPoint = myJComponent.getLocationOnScreen();
        designShow().show(myJComponent, myMouse.x - myPoint.x, myMouse.y - myPoint.y);
    }

    // does not really work :-(
    @Deprecated
    protected static void quickDemo(StandardMenu myStandardMenu) {
        // myStandardMenu.atMouse();
    }
}
