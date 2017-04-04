// License: GPL. For details, see Readme.txt file.
package playground.clruch.gfx;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.JMapViewerTree;
import playground.clruch.jmapviewer.interfaces.ICoordinate;
import playground.clruch.net.StorageSupplier;
import playground.clruch.utils.gui.SpinnerLabel;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * adapted from code by Jan Peter Stotz
 */
public class MatsimViewerFrame implements Runnable {
    boolean isLaunched = true;
    final JToggleButton jToggleButton = new JToggleButton("auto");
    JSlider jSlider = null;
    Scalar playbackSpeed = RealScalar.of(4);
    final Thread thread;

    public final JFrame jFrame = new JFrame();
    private final JMapViewerTree treeMap;

    public static FlowLayout createFlowLayout() {
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        return flowLayout;
    }

    /** Constructs the {@code Demo}. */
    public MatsimViewerFrame(MatsimJMapViewer matsimJMapViewer) {
        treeMap = new JMapViewerTree(matsimJMapViewer, "Zones", false);
        // ---
        jFrame.setTitle("ETH Z\u00fcrich MATSim Viewer");
        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isLaunched = false;
                thread.interrupt();
            }
        });
        JPanel panelNorth = new JPanel(new BorderLayout());
        JPanel panelControls = new JPanel(createFlowLayout());
        JPanel panelSettings = new JPanel(createFlowLayout());
        panelNorth.add(panelControls, BorderLayout.NORTH);
        panelNorth.add(panelSettings, BorderLayout.CENTER);

        jFrame.add(panelNorth, BorderLayout.NORTH);
        jFrame.add(treeMap, BorderLayout.CENTER);
        jFrame.add(matsimJMapViewer.jLabel, BorderLayout.SOUTH);

        panelControls.add(new MatsimToggleButton(matsimJMapViewer));
        JMapTileSelector.install(panelControls, matsimJMapViewer);
        {
            SpinnerLabel<Integer> spinnerLabel = new SpinnerLabel<>();
            spinnerLabel.setArray(0, 32, 64, 96, 128, 160, 192, 255);
            spinnerLabel.setValueSafe(matsimJMapViewer.mapAlphaCover);
            spinnerLabel.addSpinnerListener(i -> matsimJMapViewer.setMapAlphaCover(i));
            spinnerLabel.addToComponentReduced(panelControls, new Dimension(50, 28), "alpha cover");
        }
        {
            StorageSupplier storageSupplier = StorageSupplier.getDefault();
            final int size = storageSupplier.size();
            if (size == 0) {
                panelControls.add(new JLabel("no playback"));
            } else {
                System.out.println("points to playback = " + size);
                {
                    jSlider = new JSlider(0, size - 1, 0);
                    jSlider.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            int index = jSlider.getValue();
                            try {
                                matsimJMapViewer.simulationObject = storageSupplier.getSimulationObject(index);
                                matsimJMapViewer.repaint();
                            } catch (Exception exception) {
                                System.out.println("cannot load: " + index);
                            }
                        }
                    });
                    jSlider.setPreferredSize(new Dimension(400, 30));
                    panelNorth.add(jSlider, BorderLayout.SOUTH);
                }
                {
                    JButton jButton = new JButton("<");
                    jButton.addActionListener(e -> jSlider.setValue(jSlider.getValue() - 1));
                    panelControls.add(jButton);
                }
                {
                    JButton jButton = new JButton(">");
                    jButton.addActionListener(e -> jSlider.setValue(jSlider.getValue() + 1));
                    panelControls.add(jButton);
                }
                panelControls.add(jToggleButton);
                {
                    SpinnerLabel<Scalar> spinnerLabel = new SpinnerLabel<>();
                    spinnerLabel.setArray( //
                            RationalScalar.of(1, 2), //
                            RealScalar.of(1), //
                            RealScalar.of(2), //
                            RealScalar.of(4), //
                            RealScalar.of(8), //
                            RealScalar.of(16) //
                    );
                    spinnerLabel.setValueSafe(playbackSpeed);
                    spinnerLabel.addSpinnerListener(i -> playbackSpeed = i);

                    spinnerLabel.addToComponentReduced(panelControls, new Dimension(50, 28), "playback factor");
                }
            }
        }
        // ---
        {
            final JCheckBox jCheckBox = new JCheckBox("links");
            jCheckBox.setSelected(matsimJMapViewer.linkLayer.getDraw());
            jCheckBox.addActionListener(e -> matsimJMapViewer.linkLayer.setDraw(jCheckBox.isSelected()));
            panelSettings.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("req.dest");
            jCheckBox.setSelected(matsimJMapViewer.requestLayer.getDrawDestinations());
            jCheckBox.addActionListener(e -> matsimJMapViewer.requestLayer.setDrawDestinations(jCheckBox.isSelected()));
            panelSettings.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("veh.dest");
            jCheckBox.setSelected(matsimJMapViewer.vehicleLayer.getDrawDestinations());
            jCheckBox.addActionListener(e -> matsimJMapViewer.vehicleLayer.setDrawDestinations(jCheckBox.isSelected()));
            panelSettings.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("cells");
            jCheckBox.setSelected(matsimJMapViewer.virtualNetworkLayer.getDrawCells());
            jCheckBox.addActionListener(e -> matsimJMapViewer.virtualNetworkLayer.setDrawCells(jCheckBox.isSelected()));
            panelSettings.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("tree");
            jCheckBox.addActionListener(e -> treeMap.setTreeVisible(jCheckBox.isSelected()));
            panelSettings.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("grid");
            jCheckBox.setSelected(getJMapViewer().isTileGridVisible());
            jCheckBox.addActionListener(e -> getJMapViewer().setTileGridVisible(jCheckBox.isSelected()));
            panelSettings.add(jCheckBox);
        }

        getJMapViewer().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ICoordinate asd = getJMapViewer().getPosition(e.getPoint());
                System.out.println(asd.getLat() + "  " + asd);

                Point p = getJMapViewer().getMapPosition(asd.getLat(), asd.getLon());
                System.out.println(p);
            }
        });
        getJMapViewer().setZoomContolsVisible(false);

        getJMapViewer().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean cursorHand = getJMapViewer().getAttribution().handleAttributionCursor(p);
                if (cursorHand) {
                    getJMapViewer().setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    getJMapViewer().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                // if (showToolTip.isSelected())
                // getJMapViewer().setToolTipText(getJMapViewer().getPosition(p).toString());
            }
        });
        thread = new Thread(this);
        thread.start();
    }

    private JMapViewer getJMapViewer() {
        return treeMap.getViewer();
    }

    public void setDisplayPosition(Coord coord, int zoom) {
        // double[] bb = NetworkUtils.getBoundingBox(network.getNodes().values());
        // System.out.println(bb[0] + " " + bb[1] + " " + bb[2] + " " + bb[3]);
        setDisplayPosition(coord.getY(), coord.getX(), zoom);
    }

    public void setDisplayPosition(double lat, double lon, int zoom) {
        getJMapViewer().setDisplayPosition(new Point(), new Coordinate(lat, lon), zoom);
    }

    @Override
    public void run() {
        while (isLaunched) {
            int millis = 500;
            if (jSlider != null && jToggleButton.isSelected()) {
                jSlider.setValue(jSlider.getValue() + 1);
                millis = RealScalar.of(1000).divide(playbackSpeed).number().intValue();
            }
            try {
                Thread.sleep(millis);
            } catch (Exception e) {
            }
        }

    }

}
