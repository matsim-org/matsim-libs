// code by jph
package playground.clruch.utils.gui;

import java.awt.event.MouseEvent;

public interface LazyMouseListener {
  void lazyClicked(MouseEvent myMouseEvent);

  default void lazyDragged(MouseEvent myMouseEvent) {
    // empty by default
  }
}
