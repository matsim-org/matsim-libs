// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Locale;

/**
 * Default map controller which implements map moving by pressing the right
 * mouse button and zooming by double click or by mouse wheel.
 *
 * @author Jan Peter Stotz
 *
 */
public class DefaultMapController extends JMapController implements MouseListener, MouseMotionListener,
MouseWheelListener {

    private static final int MOUSE_BUTTONS_MASK = MouseEvent.BUTTON3_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK
    | MouseEvent.BUTTON2_DOWN_MASK;

    private static final int MAC_MOUSE_BUTTON3_MASK = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;

    private Point lastDragPoint;

    private boolean isMoving;

    private boolean movementEnabled = true;

    private int movementMouseButton = MouseEvent.BUTTON3;
    private int movementMouseButtonMask = MouseEvent.BUTTON3_DOWN_MASK;

    private boolean wheelZoomEnabled = true;
    private boolean doubleClickZoomEnabled = true;

    /**
     * Constructs a new {@code DefaultMapController}.
     * @param map map panel
     */
    public DefaultMapController(MapComponent map) {
        super(map);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!movementEnabled || !isMoving)
            return;
        // Is only the selected mouse button pressed?
        if ((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == movementMouseButtonMask
                || (isPlatformOsx() && e.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK)) {
            Point p = e.getPoint();
            if (lastDragPoint != null) {
                int diffx = lastDragPoint.x - p.x;
                int diffy = lastDragPoint.y - p.y;
                map.moveMap(diffx, diffy);
            }
            lastDragPoint = p;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (doubleClickZoomEnabled && e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            map.zoomIn(e.getPoint());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == movementMouseButton || (isPlatformOsx() && e.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK)) {
            lastDragPoint = null;
            isMoving = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == movementMouseButton || (isPlatformOsx() && e.getButton() == MouseEvent.BUTTON1)) {
            lastDragPoint = null;
            isMoving = false;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (wheelZoomEnabled) {
            int rotation = MapComponent.zoomReverseWheel ? -e.getWheelRotation() : e.getWheelRotation();
            map.setZoom(map.getZoom() - rotation, e.getPoint());
        }
    }

    /**
     * Determines if the map pane is allowed to be moved using the mouse
     * @return {@code true} to allow the map pane to be moved using the mouse
     */
    public boolean isMovementEnabled() {
        return movementEnabled;
    }

    /**
     * Enables or disables that the map pane can be moved using the mouse.
     *
     * @param movementEnabled {@code true} to allow the map pane to be moved using the mouse
     */
    public void setMovementEnabled(boolean movementEnabled) {
        this.movementEnabled = movementEnabled;
    }

    public int getMovementMouseButton() {
        return movementMouseButton;
    }

    /**
     * Sets the mouse button that is used for moving the map. Possible values are:
     * <ul>
     * <li>{@link MouseEvent#BUTTON1} (left mouse button)</li>
     * <li>{@link MouseEvent#BUTTON2} (middle mouse button)</li>
     * <li>{@link MouseEvent#BUTTON3} (right mouse button)</li>
     * </ul>
     *
     * @param movementMouseButton the mouse button that is used for moving the map
     */
    public void setMovementMouseButton(int movementMouseButton) {
        this.movementMouseButton = movementMouseButton;
        switch (movementMouseButton) {
            case MouseEvent.BUTTON1:
                movementMouseButtonMask = MouseEvent.BUTTON1_DOWN_MASK;
                break;
            case MouseEvent.BUTTON2:
                movementMouseButtonMask = MouseEvent.BUTTON2_DOWN_MASK;
                break;
            case MouseEvent.BUTTON3:
                movementMouseButtonMask = MouseEvent.BUTTON3_DOWN_MASK;
                break;
            default:
                throw new RuntimeException("Unsupported button");
        }
    }

    public boolean isWheelZoomEnabled() {
        return wheelZoomEnabled;
    }

    public void setWheelZoomEnabled(boolean wheelZoomEnabled) {
        this.wheelZoomEnabled = wheelZoomEnabled;
    }

    public boolean isDoubleClickZoomEnabled() {
        return doubleClickZoomEnabled;
    }

    public void setDoubleClickZoomEnabled(boolean doubleClickZoomEnabled) {
        this.doubleClickZoomEnabled = doubleClickZoomEnabled;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Mac OSX simulates with  ctrl + mouse 1  the second mouse button hence no dragging events get fired.
        //
        if (isPlatformOsx()) {
            if (!movementEnabled || !isMoving)
                return;
            // Is only the selected mouse button pressed?
            if (e.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK) {
                Point p = e.getPoint();
                if (lastDragPoint != null) {
                    int diffx = lastDragPoint.x - p.x;
                    int diffy = lastDragPoint.y - p.y;
                    map.moveMap(diffx, diffy);
                }
                lastDragPoint = p;
            }
        }
    }

    /**
     * Replies true if we are currently running on OSX
     *
     * @return true if we are currently running on OSX
     */
    public static boolean isPlatformOsx() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase(Locale.ENGLISH).startsWith("mac os x");
    }
}
