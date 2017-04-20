// code by jph
package playground.clruch.utils.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * selector in gui for easy scrolling through a list with mouse-wheel but no pull-down menu
 * 
 * @param <Type>
 */
public class SpinnerLabel<Type> {
    public static final Color background1 = new Color(248, 248, 248, 128);
    public static final Color background0 = new Color(248, 248, 248, 64);
    private static final int border_width_min = 9;
    private static final int border_width_max = 16;
    // ---
    private boolean mouseInside = false;
    private Point myLastMouse = new Point();
    private int border_width = 0;
    final List<SpinnerListener<Type>> mySpinnerListeners = new LinkedList<>();
    private final JLabel myJLabel = new JLabel("", SwingConstants.RIGHT) {
        @Override
        protected void paintComponent(Graphics graphics) {
            final boolean enabled = isEnabled();
            final boolean insideActive = mouseInside && enabled;
            Graphics2D myGraphics = (Graphics2D) graphics;
            Dimension myDimension = getSize(); // myJLabel.
            border_width = Math.min(Math.max(border_width_min, border_width_min - 2 + myDimension.width / 10), border_width_max);
            // ---
            if (insideActive) {
                myGraphics.setColor(background1);
                setForeground(Colors.label);
            } else {
                myGraphics.setColor(background0);
                setForeground(new Color(51 + 32, 51 + 32, 51 + 32));
            }
            myGraphics.fillRect(0, 0, myDimension.width, myDimension.height);
            // ---
            if (isOverArrows(myLastMouse) && enabled) {
                myGraphics.setColor(Color.white);
                myGraphics.fillRect(myDimension.width - border_width, 0, border_width, myDimension.height);
            } else {
                myGraphics.setColor(Colors.alpha128(Color.gray));
                final int b = myDimension.width - 1;
                myGraphics.drawLine(b, 0, b, myDimension.height - 1);
            }
            // ---
            final int piy;
            if (numel() < 2)
                piy = myDimension.height / 2;
            else {
                double num = numel() - 1;
                piy = (int) Math.round((myDimension.height - 1) * index / num);
            }
            myGraphics.setColor(Color.white);
            myGraphics.drawLine(0, piy, myDimension.width, piy);
            if (insideActive) {
                myGraphics.setColor(Colors.withAlpha(Color.lightGray, 96));
                myGraphics.drawLine(0, piy + 1, myDimension.width, piy + 1);
            }
            // Point2D start = new Point2D.Float(0, 0);
            // Point2D end = new Point2D.Float(0, myDimension.height - 1);
            // float[] dist = { 0f, (numel() - index) / (float) (numel() + 1), 1f };
            // Color[] colors = { Colors.alpha064(Color.lightGray), Colors.alpha128(Color.white), Colors.alpha064(Color.lightGray) };
            // LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
            // myGraphics.setPaint(p);
            // ---
            myGraphics.setColor(insideActive ? Colors.selection : Colors.alpha064(Color.lightGray));
            final int w = 3;
            final int r = myDimension.width - 2 * w - 1;
            final int h = myDimension.height - w - 1;
            {
                Path2D myPath2D = new Path2D.Double();
                myPath2D.moveTo(r, 1 + w);
                myPath2D.lineTo(r + 2 * w - 1, 1 + w);
                myPath2D.lineTo(r + w, 1);
                myPath2D.closePath();
                myGraphics.fill(myPath2D);
            }
            {
                Path2D myPath2D = new Path2D.Double();
                myPath2D.moveTo(r, h);
                myPath2D.lineTo(r + w, h + w);
                myPath2D.lineTo(r + 2 * w - 1, h);
                myPath2D.closePath();
                myGraphics.fill(myPath2D);
            }
            // ---
            super.paintComponent(myGraphics);
        }
    };
    private boolean isMenuEnabled = true;
    private boolean isMenuHover = false;
    LazyMouseListener myLazyMouseListener = myMouseEvent -> {
        if (myMouseEvent.getButton() == MouseEvent.BUTTON1) {
            if (myJLabel.isEnabled()) {
                Dimension myDimension = myJLabel.getSize();
                Point myPoint = myMouseEvent.getPoint();
                if (isOverArrows(myPoint))
                    increment(myPoint.y < myDimension.height / 2 ? -1 : 1); // sign of difference
                else {
                    if (isMenuEnabled)
                        new SpinnerMenu<>(this, isMenuHover).showRight(myJLabel);
                }
            }
        }
    };

    public void setMenuEnabled(boolean isMenuEnabled) {
        this.isMenuEnabled = isMenuEnabled;
    }

    public void setMenuHover(boolean hover) {
        this.isMenuHover = hover;
    }

    int value = 0;
    boolean cyclic = false;
    JSpinner myJSpinner = new JSpinner(new SpinnerNumberModel(value, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
    int index = -1;
    List<Type> myList;

    // protected boolean isUsingArrows = true;
    // public void setUsingArrows(boolean myBoolean) {
    // isUsingArrows = myBoolean;
    // }
    public boolean isOverArrows(Point myPoint) {
        Dimension myDimension = myJLabel.getSize();
        return mouseInside && myDimension.width - border_width < myPoint.x;
    }

    public SpinnerLabel() {
        // myJLabel.setOpaque(true);
        // myJLabel.setBackground(background);
        myJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        myJLabel.setOpaque(false);
        myJLabel.addMouseWheelListener(myMouseWheelEvent -> {
            if (myJLabel.isEnabled())
                increment(myMouseWheelEvent.getWheelRotation());
        });
        MouseAdapter myMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent myMouseEvent) {
                mouseInside = true;
                myLastMouse = myMouseEvent.getPoint();
                myJLabel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent myMouseEvent) {
                mouseInside = false;
                myJLabel.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent myMouseEvent) {
                myLastMouse = myMouseEvent.getPoint();
                // if (isUsingArrows)
                myJLabel.repaint(); // not very efficient
            }
        };
        myJLabel.addMouseListener(myMouseAdapter);
        myJLabel.addMouseMotionListener(myMouseAdapter);
        new LazyMouse(myLazyMouseListener).addListenersTo(myJLabel);
        // myJSpinner.setFocusable(false); // does not have effect
        myJSpinner.setPreferredSize(new Dimension(16, 28));
        myJSpinner.addChangeListener(myChangeEvent -> {
            int delta = (Integer) myJSpinner.getValue() - value;
            increment(delta);
            value = (Integer) myJSpinner.getValue();
        });
    }

    public SpinnerLabel(SpinnerListener<Type> mySpinnerListener) {
        this();
        addSpinnerListener(mySpinnerListener);
    }

    public void addSpinnerListener(SpinnerListener<Type> mySpinnerListener) {
        mySpinnerListeners.add(mySpinnerListener);
    }

    public void setCyclic(boolean myBoolean) {
        cyclic = myBoolean;
    }

    public boolean getCyclic() {
        return cyclic;
    }

    public void setEnabled(boolean myBoolean) {
        myJLabel.setEnabled(myBoolean);
        myJSpinner.setEnabled(myBoolean);
    }

    private void increment(int delta) {
        int prev = index;
        if (cyclic)
            index = IntegerMath.mod(index + delta, numel());
        else
            index = Math.min(Math.max(0, index + delta), numel() - 1);
        if (index != prev) {
            updateLabel();
            reportToAll();
        }
    }

    public void reportToAll() {
        Type myType = getValue();
        mySpinnerListeners.forEach(mySpinnerListener -> mySpinnerListener.actionPerformed(myType));
    }

    /**
     * @param myList
     *            is used by reference.
     *            Any modification to myList is discouraged and (eventually) reflected in the {@link SpinnerLabel}.
     */
    public void setList(List<Type> myList) {
        this.myList = myList;
    }

    public void setStream(Stream<Type> myStream) {
        setList(myStream.collect(Collectors.toList()));
    }

    // public void setIterable(Iterable<Type> myIterable) {
    // setList(ListUtils.copyIterator(myIterable));
    // }

    public void setArray(@SuppressWarnings("unchecked") Type... values) {
        setList(Arrays.asList(values));
    }

    public Type getValue() {
        if (0 <= index && index < numel())
            return myList.get(index);
        return null;
    }

    public int getIndex() {
        return index;
    }

    public int numel() {
        return myList == null ? 0 : myList.size();
    }

    /**
     * does not invoke call backs
     * 
     * @param myType
     */
    public void setValue(Type myType) {
        index = myList.indexOf(myType);
        updateLabel();
    }

    public void setValueSafe(Type myType) {
        try {
            setValue(myType);
        } catch (Exception myException) {
            myException.printStackTrace();
            if (!myList.isEmpty())
                setValue(myList.get(0));
        }
    }

    public void setIndex(int index) {
        this.index = index;
        updateLabel();
    }

    public void setToolTipText(String myString) {
        myJLabel.setToolTipText(myString);
        myJSpinner.setToolTipText(myString);
    }

    public String stringFormat(Type myType) {
        return myType == null ? "" : myType.toString();
    }

    private void updateLabel() {
        myJLabel.setText(stringFormat(getValue()));
        myJSpinner.setEnabled(1 < myList.size()); // added recently to indicate that there is nothing to scroll
    }
    
    public JComponent getLabelComponent() {
        return myJLabel;
    }

    public JComponent getSpinnerComponent() {
        return myJSpinner;
    }

    public void addToComponent(JComponent myJComponent, Dimension myDimension, String myToolTip) {
        addToComponentReduced(myJComponent, myDimension, myToolTip);
        myJComponent.add(getSpinnerComponent());
    }

    public void addToComponentReduced(JComponent myJComponent, Dimension myDimension, String myToolTip) {
        myJLabel.setToolTipText(myToolTip == null || myToolTip.isEmpty() ? null : myToolTip);
        myJLabel.setPreferredSize(myDimension);
        myJComponent.add(myJLabel);
    }

    public void setVisible(boolean myBoolean) {
        myJLabel.setVisible(myBoolean);
        myJSpinner.setVisible(myBoolean);
    }
}
