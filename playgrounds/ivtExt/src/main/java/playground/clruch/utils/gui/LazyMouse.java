// code by jph
package playground.clruch.utils.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class LazyMouse implements MouseListener, MouseMotionListener {
  public static final float default_tolerance = 3;
  // ---
  private Point myPressedC = new Point(); // component
  private Point myPressedS = new Point(); // screen
  private boolean isClick;
  private float tolerance = default_tolerance;
  private final LazyMouseListener myLazyMouseListener;

  public LazyMouse(LazyMouseListener myLazyMouseListener) {
    this.myLazyMouseListener = myLazyMouseListener;
  }

  public void setTolerance(float tolerance) {
    if (tolerance < 0)
      throw new RuntimeException("tolerance is negative");
    this.tolerance = tolerance;
  }

  @Override
  public final void mousePressed(MouseEvent myMouseEvent) {
    myPressedC = myMouseEvent.getPoint();
    myPressedS = myMouseEvent.getLocationOnScreen();
    isClick = true;
  }

  private boolean shortFromPressed(MouseEvent myMouseEvent) {
    Point myPointC = myMouseEvent.getPoint();
    Point myPointS = myMouseEvent.getLocationOnScreen();
    return Math.hypot(myPointC.x - myPressedC.x, myPointC.y - myPressedC.y) <= tolerance && //
        Math.hypot(myPointS.x - myPressedS.x, myPointS.y - myPressedS.y) <= tolerance;
  }

  @Override
  public final void mouseReleased(MouseEvent myMouseEvent) {
    isClick &= shortFromPressed(myMouseEvent);
    if (isClick)
      myLazyMouseListener.lazyClicked(myMouseEvent);
  }

  @Override
  public final void mouseClicked(MouseEvent myMouseEvent) {
    // this has to be empty!
  }

  @Override
  public final void mouseDragged(MouseEvent myMouseEvent) {
    isClick &= shortFromPressed(myMouseEvent);
    if (!isClick)
      myLazyMouseListener.lazyDragged(myMouseEvent);
  }

  @Override
  public void mouseMoved(MouseEvent myMouseEvent) {
    // empty by default
  }

  @Override
  public void mouseEntered(MouseEvent myMouseEvent) {
    // empty by default
  }

  @Override
  public void mouseExited(MouseEvent myMouseEvent) {
    // empty by default
  }

  public void addListenersTo(Component myComponent) {
    myComponent.addMouseListener(this);
    myComponent.addMouseMotionListener(this);
  }
}
