// License: GPL. For details, see Readme.txt file.
package playground.clruch.gfx;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.JMapViewerTree;
import playground.clruch.jmapviewer.interfaces.ICoordinate;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.OsmTileSource;
import playground.clruch.net.StorageSupplier;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * adapted from code by Jan Peter Stotz
 */
public class MatsimViewerFrame {
    Timer timer = new Timer();
    public final JFrame jFrame = new JFrame();
    private final JMapViewerTree treeMap;

    /** Constructs the {@code Demo}. */
    public MatsimViewerFrame(MatsimJMapViewer matsimJMapViewer) {
        treeMap = new JMapViewerTree(matsimJMapViewer, "Zones", false);
        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                timer.cancel();
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        JPanel panelTop = new JPanel();
        JPanel panelBottom = new JPanel();

        jFrame.add(panel, BorderLayout.NORTH);
        jFrame.add(matsimJMapViewer.jLabel, BorderLayout.SOUTH);
        panel.add(panelTop, BorderLayout.NORTH);
        panel.add(panelBottom, BorderLayout.SOUTH);

        panelTop.add(new MatsimToggleButton(matsimJMapViewer));

        JComboBox<TileSource> tileSourceSelector = new JComboBox<>(new TileSource[] { //
                new OsmTileSource.Mapnik(), //
                new OsmTileSource.CycleMap(), //
                new BingAerialTileSource(), });
        tileSourceSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                getJMapViewer().setTileSource((TileSource) e.getItem());
            }
        });
        panelTop.add(tileSourceSelector);
        {
            StorageSupplier storageSupplier = StorageSupplier.getDefault();
            final int size = storageSupplier.size();
            if (size == 0) {
                panelTop.add(new JLabel("no playback"));
            } else {
                System.out.println("points to playback = " + size);
                JSlider jSlider = new JSlider(0, size - 1, 0);
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
                panelTop.add(jSlider);

                JToggleButton jToggleButton = new JToggleButton("auto");
                panelTop.add(jToggleButton);

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (jToggleButton.isSelected())
                            jSlider.setValue(jSlider.getValue() + 1);
                    }
                }, 10, 333);

            }
        }
        // ---
        {
            final JCheckBox jCheckBox = new JCheckBox("links");
            jCheckBox.setSelected(matsimJMapViewer.linkLayer.getDraw());
            jCheckBox.addActionListener(e -> matsimJMapViewer.linkLayer.setDraw(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("req.dest");
            jCheckBox.setSelected(matsimJMapViewer.requestLayer.getDrawDestinations());
            jCheckBox.addActionListener(e -> matsimJMapViewer.requestLayer.setDrawDestinations(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("veh.dest");
            jCheckBox.setSelected(matsimJMapViewer.vehicleLayer.getDrawDestinations());
            jCheckBox.addActionListener(e -> matsimJMapViewer.vehicleLayer.setDrawDestinations(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("tree");
            jCheckBox.addActionListener(e -> treeMap.setTreeVisible(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("grid");
            jCheckBox.setSelected(getJMapViewer().isTileGridVisible());
            jCheckBox.addActionListener(e -> getJMapViewer().setTileGridVisible(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }

        jFrame.add(treeMap, BorderLayout.CENTER);

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
    }

    private JMapViewer getJMapViewer() {
        return treeMap.getViewer();
    }

    public void setDisplayPosition(double lat, double lon, int zoom) {
        getJMapViewer().setDisplayPosition(new Point(), new Coordinate(lat, lon), zoom);

    }

}
