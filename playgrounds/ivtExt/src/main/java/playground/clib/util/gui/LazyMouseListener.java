// code by jph
package playground.clib.util.gui;

import java.awt.event.MouseEvent;

public interface LazyMouseListener {
    void lazyClicked(MouseEvent myMouseEvent);

    default void lazyDragged(MouseEvent myMouseEvent) {
        // empty by default
    }
}
