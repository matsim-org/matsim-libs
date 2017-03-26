// License: GPL. For details, see Readme.txt file.
package playground.clruch.gfx;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.JMapViewerTree;
import playground.clruch.jmapviewer.OsmTileLoader;
import playground.clruch.jmapviewer.interfaces.ICoordinate;
import playground.clruch.jmapviewer.interfaces.TileLoader;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.OsmTileSource;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * adapted from code by Jan Peter Stotz
 */
public class MatsimViewerFrame {
    public final JFrame jFrame = new JFrame();
    private final JMapViewerTree treeMap;

    /** Constructs the {@code Demo}. */
    public MatsimViewerFrame(MatsimJMapViewer matsimJMapViewer) {

        treeMap = new JMapViewerTree(matsimJMapViewer, "Zones", false);

        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
//        JComboBox<TileLoader> tileLoaderSelector;
//        tileLoaderSelector = new JComboBox<>(new TileLoader[] { new OsmTileLoader(getJMapViewer()) });
//        tileLoaderSelector.addItemListener(new ItemListener() {
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                getJMapViewer().setTileLoader((TileLoader) e.getItem());
//            }
//        });
//        getJMapViewer().setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
        panelTop.add(tileSourceSelector);
//        panelTop.add(tileLoaderSelector);
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
