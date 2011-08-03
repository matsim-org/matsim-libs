package playground.mzilske.osm;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.mapviewer.bmng.CylindricalProjectionTileFactory;
import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.jdesktop.swingx.mapviewer.wms.WMSTileFactory;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;

public class JXMapViewerTest extends JXPanel {
	
	private boolean miniMapVisible = true;
    private boolean zoomSliderVisible = true;
    private boolean zoomButtonsVisible = true;
    private final boolean sliderReversed = false;
    
    
    public enum DefaultProviders { SwingLabsBlueMarble, OpenStreetMaps, Custom };
    
    private DefaultProviders defaultProvider = DefaultProviders.SwingLabsBlueMarble;
    
    private boolean addressLocationShown = true;
    
    private boolean dataProviderCreditShown = true;
    
    /**
     * Creates a new JXMapKit
     */
    public JXMapViewerTest() {
        initComponents();
        setDataProviderCreditShown(false);
        
        zoomSlider.setOpaque(false);
        try {
        Icon minusIcon = new ImageIcon(getClass().getResource(
                "/org/jdesktop/swingx/mapviewer/resources/minus.png"));
        this.zoomOutButton.setIcon(minusIcon);
        this.zoomOutButton.setText("");
        Icon plusIcon = new ImageIcon(getClass().getResource(
                "/org/jdesktop/swingx/mapviewer/resources/plus.png"));
        this.zoomInButton.setIcon(plusIcon);
        this.zoomInButton.setText("");
        } catch (Throwable thr) {
            System.out.println("error: " + thr.getMessage());
            thr.printStackTrace();
        }
        
        setTileFactory(new CylindricalProjectionTileFactory());
        
        mainMap.setCenterPosition(new GeoPosition(0,0));
        miniMap.setCenterPosition(new GeoPosition(0,0));
        mainMap.setRestrictOutsidePanning(true);
        miniMap.setRestrictOutsidePanning(true);
        
        rebuildMainMapOverlay();
        
        
        /*
        // adapter to move the minimap after the main map has moved
        MouseInputAdapter ma = new MouseInputAdapter() {
            public void mouseReleased(MouseEvent e) {
                miniMap.setCenterPosition(mapCenterPosition);
            }
        };
        mainMap.addMouseMotionListener(ma);
        mainMap.addMouseListener(ma);*/
        
        
        
        
        mainMap.addPropertyChangeListener("center", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Point2D mapCenter = (Point2D)evt.getNewValue();
                TileFactory tf = mainMap.getTileFactory();
                GeoPosition mapPos = tf.pixelToGeo(mapCenter,mainMap.getZoom());
                miniMap.setCenterPosition(mapPos);
            }
        });
        
        
        mainMap.addPropertyChangeListener("centerPosition", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                mapCenterPosition = (GeoPosition)evt.getNewValue();
                miniMap.setCenterPosition(mapCenterPosition);
                Point2D pt = miniMap.getTileFactory().geoToPixel(mapCenterPosition,miniMap.getZoom());
                miniMap.setCenter(pt);
                miniMap.repaint();
            }
        });
        
        mainMap.addPropertyChangeListener("zoom",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                zoomSlider.setValue(mainMap.getZoom());
                miniMap.setZoom(mainMap.getZoom()+4);
            }
        });
        
        
        // an overlay for the mini-map which shows a rectangle representing the main map
        miniMap.setOverlayPainter(new Painter<JXMapViewer>() {
            public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
                // get the viewport rect of the main map
                Rectangle mainMapBounds = mainMap.getViewportBounds();
                
                // convert to Point2Ds
                Point2D upperLeft2D = mainMapBounds.getLocation();
                Point2D lowerRight2D = new Point2D.Double(upperLeft2D.getX() + mainMapBounds.getWidth(),
                        upperLeft2D.getY() + mainMapBounds.getHeight());
                
                // convert to GeoPostions
                GeoPosition upperLeft = mainMap.getTileFactory().pixelToGeo(upperLeft2D,mainMap.getZoom());
                GeoPosition lowerRight = mainMap.getTileFactory().pixelToGeo(lowerRight2D,mainMap.getZoom());
                
                // convert to Point2Ds on the mini-map
                upperLeft2D = map.getTileFactory().geoToPixel(upperLeft,map.getZoom());
                lowerRight2D = map.getTileFactory().geoToPixel(lowerRight,map.getZoom());
                
                
                g = (Graphics2D) g.create();
                Rectangle rect = map.getViewportBounds();
                //p("rect = " + rect);
                g.translate(-rect.x, -rect.y);
                Point2D centerpos = map.getTileFactory().geoToPixel(mapCenterPosition, map.getZoom());
                //p("center pos = " + centerpos);
                g.setPaint(Color.RED);
                //g.drawRect((int)centerpos.getX()-30,(int)centerpos.getY()-30,60,60);
                g.drawRect((int)upperLeft2D.getX(),(int)upperLeft2D.getY(),
                        (int)(lowerRight2D.getX()-upperLeft2D.getX()),
                        (int)(lowerRight2D.getY()-upperLeft2D.getY()));
                g.setPaint(new Color(255,0,0,50));
                g.fillRect((int)upperLeft2D.getX(),(int)upperLeft2D.getY(),
                        (int)(lowerRight2D.getX()-upperLeft2D.getX()),
                        (int)(lowerRight2D.getY()-upperLeft2D.getY()));
                //g.drawOval((int)lowerRight2D.getX(),(int)lowerRight2D.getY(),1,1);
                g.dispose();
            }
        });
        
        if(getDefaultProvider() == DefaultProviders.OpenStreetMaps) {
            setZoom(10);
        } else {
            setZoom(3);// joshy: hack, i shouldn't need this here
        }
        this.setCenterPosition(new GeoPosition(0,0));
    }
    
    //private Point2D mapCenter = new Point2D.Double(0,0);
    private GeoPosition mapCenterPosition = new GeoPosition(0,0);
    private boolean zoomChanging = false;
    
    /**
     * Set the current zoomlevel for the main map. The minimap will
     * be updated accordingly
     * @param zoom the new zoom level
     */
    public void setZoom(int zoom) {
        zoomChanging = true;
        mainMap.setZoom(zoom);
        miniMap.setZoom(mainMap.getZoom()+4);
        if(sliderReversed) {
            zoomSlider.setValue(zoomSlider.getMaximum()-zoom);
        } else {
            zoomSlider.setValue(zoom);
        }
        zoomChanging = false;
    }
    
    /**
     * Returns an action which can be attached to buttons or menu
     * items to make the map zoom out
     * @return a preconfigured Zoom Out action
     */
    public Action getZoomOutAction() {
        Action act = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setZoom(mainMap.getZoom()-1);
            }
        };
        act.putValue(Action.NAME,"-");
        return act;
    }
    
    /**
     * Returns an action which can be attached to buttons or menu
     * items to make the map zoom in
     * @return a preconfigured Zoom In action
     */
    public Action getZoomInAction() {
        Action act = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setZoom(mainMap.getZoom()+1);
            }
        };
        act.putValue(Action.NAME,"+");
        return act;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mainMap = new org.jdesktop.swingx.JXMapViewer();
        miniMap = new org.jdesktop.swingx.JXMapViewer();
        jPanel1 = new javax.swing.JPanel();
        zoomInButton = new javax.swing.JButton();
        zoomOutButton = new javax.swing.JButton();
        zoomSlider = new javax.swing.JSlider();

        setLayout(new java.awt.GridBagLayout());

        mainMap.setLayout(new java.awt.GridBagLayout());

        miniMap.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        miniMap.setMinimumSize(new java.awt.Dimension(100, 100));
        miniMap.setPreferredSize(new java.awt.Dimension(100, 100));
        miniMap.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainMap.add(miniMap, gridBagConstraints);

        jPanel1.setOpaque(false);
        jPanel1.setLayout(new java.awt.GridBagLayout());

        zoomInButton.setAction(getZoomOutAction());
        zoomInButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/mapviewer/resources/plus.png")));
        zoomInButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        zoomInButton.setMaximumSize(new java.awt.Dimension(20, 20));
        zoomInButton.setMinimumSize(new java.awt.Dimension(20, 20));
        zoomInButton.setOpaque(false);
        zoomInButton.setPreferredSize(new java.awt.Dimension(20, 20));
        zoomInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(zoomInButton, gridBagConstraints);

        zoomOutButton.setAction(getZoomInAction());
        zoomOutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/jdesktop/swingx/mapviewer/resources/minus.png")));
        zoomOutButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        zoomOutButton.setMaximumSize(new java.awt.Dimension(20, 20));
        zoomOutButton.setMinimumSize(new java.awt.Dimension(20, 20));
        zoomOutButton.setOpaque(false);
        zoomOutButton.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(zoomOutButton, gridBagConstraints);

        zoomSlider.setMajorTickSpacing(1);
        zoomSlider.setMaximum(15);
        zoomSlider.setMinimum(10);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setMinimumSize(new java.awt.Dimension(35, 100));
        zoomSlider.setPreferredSize(new java.awt.Dimension(35, 190));
        zoomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zoomSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel1.add(zoomSlider, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        mainMap.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(mainMap, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    
    private void zoomInButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_zoomInButtonActionPerformed
    
    private void zoomSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomSliderStateChanged
        if(!zoomChanging){
            setZoom(zoomSlider.getValue());
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_zoomSliderStateChanged
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private org.jdesktop.swingx.JXMapViewer mainMap;
    private org.jdesktop.swingx.JXMapViewer miniMap;
    private javax.swing.JButton zoomInButton;
    private javax.swing.JButton zoomOutButton;
    private javax.swing.JSlider zoomSlider;
    // End of variables declaration//GEN-END:variables
    
    private static void p(String str) {
        System.out.println(str);
    }
    
    /**
     * Indicates if the mini-map is currently visible
     * @return the current value of the mini-map property
     */
    public boolean isMiniMapVisible() {
        return miniMapVisible;
    }
    
    /**
     * Sets if the mini-map should be visible
     * @param miniMapVisible a new value for the miniMap property
     */
    public void setMiniMapVisible(boolean miniMapVisible) {
        boolean old = this.isMiniMapVisible();
        this.miniMapVisible = miniMapVisible;
        miniMap.setVisible(miniMapVisible);
        firePropertyChange("miniMapVisible",old,this.isMiniMapVisible());
    }
    
    /**
     * Indicates if the zoom slider is currently visible
     * @return the current value of the zoomSliderVisible property
     */
    public boolean isZoomSliderVisible() {
        return zoomSliderVisible;
    }
    
    /**
     * Sets if the zoom slider should be visible
     * @param zoomSliderVisible the new value of the zoomSliderVisible property
     */
    public void setZoomSliderVisible(boolean zoomSliderVisible) {
        boolean old = this.isZoomSliderVisible();
        this.zoomSliderVisible = zoomSliderVisible;
        zoomSlider.setVisible(zoomSliderVisible);
        firePropertyChange("zoomSliderVisible",old,this.isZoomSliderVisible());
    }
    
    /**
     * Indicates if the zoom buttons are visible. This is a bound property
     * and can be listed for using a PropertyChangeListener
     * @return current value of the zoomButtonsVisible property
     */
    public boolean isZoomButtonsVisible() {
        return zoomButtonsVisible;
    }
    
    /**
     * Sets if the zoom buttons should be visible. This ia bound property.
     * Changes can be listened for using a PropertyChaneListener
     * @param zoomButtonsVisible new value of the zoomButtonsVisible property
     */
    public void setZoomButtonsVisible(boolean zoomButtonsVisible) {
        boolean old = this.isZoomButtonsVisible();
        this.zoomButtonsVisible = zoomButtonsVisible;
        zoomInButton.setVisible(zoomButtonsVisible);
        zoomOutButton.setVisible(zoomButtonsVisible);
        firePropertyChange("zoomButtonsVisible",old,this.isZoomButtonsVisible());
    }
    
    /**
     * Sets the tile factory for both embedded JXMapViewer components.
     * Calling this method will also reset the center and zoom levels
     * of both maps, as well as the bounds of the zoom slider.
     * @param fact the new TileFactory
     */
    public void setTileFactory(TileFactory fact) {
        mainMap.setTileFactory(fact);
        mainMap.setZoom(fact.getInfo().getDefaultZoomLevel());
        mainMap.setCenterPosition(new GeoPosition(0,0));
        miniMap.setTileFactory(fact);
        miniMap.setZoom(fact.getInfo().getDefaultZoomLevel()+3);
        miniMap.setCenterPosition(new GeoPosition(0,0));
        zoomSlider.setMinimum(fact.getInfo().getMinimumZoomLevel());
        zoomSlider.setMaximum(fact.getInfo().getMaximumZoomLevel());
    }
    
    public void setCenterPosition(GeoPosition pos) {
        mainMap.setCenterPosition(pos);
        miniMap.setCenterPosition(pos); 
    }
    
    public GeoPosition getCenterPosition() {
        return mainMap.getCenterPosition();
    }
    
    public GeoPosition getAddressLocation() {
        return mainMap.getAddressLocation();
    }
    
    public void setAddressLocation(GeoPosition pos) {
        mainMap.setAddressLocation(pos);
    }
    
    /**
     * Returns a reference to the main embedded JXMapViewer component
     * @return the main map
     */
    public JXMapViewer getMainMap() {
        return this.mainMap;
    }
    
    /**
     * Returns a reference to the mini embedded JXMapViewer component
     * @return the minimap JXMapViewer component
     */
    public JXMapViewer getMiniMap() {
        return this.miniMap;
    }
    
    /**
     * returns a reference to the zoom in button
     * @return a jbutton
     */
    public JButton getZoomInButton() {
        return this.zoomInButton;
    }
    /**
     * returns a reference to the zoom out button
     * @return a jbutton
     */
    public JButton getZoomOutButton() {
        return this.zoomOutButton;
    }
    /**
     * returns a reference to the zoom slider
     * @return a jslider
     */
    public JSlider getZoomSlider() {
        return this.zoomSlider;
    }
    
    public void setAddressLocationShown(boolean b) {
        boolean old = isAddressLocationShown();
        this.addressLocationShown = b;
        addressLocationPainter.setVisible(b);
        firePropertyChange("addressLocationShown",old,b);
        repaint();
    }
    
    public boolean isAddressLocationShown() {
        return addressLocationShown;
    }
    
    public void setDataProviderCreditShown(boolean b) {
        boolean old = isDataProviderCreditShown();
        this.dataProviderCreditShown = b;
        dataProviderCreditPainter.setVisible(b);
        repaint();
        firePropertyChange("dataProviderCreditShown",old,b);
    }
    
    public boolean isDataProviderCreditShown() {
        return dataProviderCreditShown;
    }
    
    private void rebuildMainMapOverlay() {
        CompoundPainter cp = new CompoundPainter();
        cp.setCacheable(false);
        /*
        List<Painter> ptrs = new ArrayList<Painter>();
        if(isDataProviderCreditShown()) {
            ptrs.add(dataProviderCreditPainter);
        }
        if(isAddressLocationShown()) {
            ptrs.add(addressLocationPainter);
        }*/
        cp.setPainters(dataProviderCreditPainter, addressLocationPainter);
        mainMap.setOverlayPainter(cp);
    }
    
    public void setDefaultProvider(DefaultProviders prov) {
        DefaultProviders old = this.defaultProvider;
        this.defaultProvider = prov;
        if(prov == DefaultProviders.SwingLabsBlueMarble) {
            setTileFactory(new CylindricalProjectionTileFactory());
            setZoom(3);
        }
        if(prov == DefaultProviders.OpenStreetMaps) {
            final int max = 17;
            TileFactoryInfo info = new TileFactoryInfo(1,max-2,max,
                    256, true, true, // tile size is 256 and x/y orientation is normal
                    "http://tile.openstreetmap.org",//5/15/10.png",
                    "x","y","z") {
                public String getTileUrl(int x, int y, int zoom) {
                    zoom = max-zoom;
                    String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
                    return url;
                }
                
            };
            TileFactory tf = new DefaultTileFactory(info);
            setTileFactory(tf);
            setZoom(11);
            setAddressLocation(new GeoPosition(51.5,0));
        }
        firePropertyChange("defaultProvider", old, prov);
        repaint();
    }
    public DefaultProviders getDefaultProvider() {
        return this.defaultProvider;
    }
    
    
    
    
    private AbstractPainter dataProviderCreditPainter = new AbstractPainter<JXMapViewer>(false) {
        protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height) {
            g.setPaint(Color.WHITE);
            g.drawString("data ",50,
                    map.getHeight()-10);
        }
    };
    
    private WaypointPainter addressLocationPainter = new WaypointPainter() {
        public Set<Waypoint> getWaypoints() {
            Set set = new HashSet();
            if(getAddressLocation() != null) {
                set.add(new Waypoint(getAddressLocation()));
            } else {
                set.add(new Waypoint(0,0));
            }
            return set;
        }
    };
    
    public static void main(String ... args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JXMapViewerTest kit = new JXMapViewerTest();
                
                
                final int max = 17;
                TileFactoryInfo info = new TileFactoryInfo(1,max-2,max,
                        256, true, true, // tile size is 256 and x/y orientation is normal
                        "http://tile.openstreetmap.org",//5/15/10.png",
                        "x","y","z") {
                    public String getTileUrl(int x, int y, int zoom) {
                        zoom = max-zoom;
                        String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
                        return url;
                    }

                };
                
                
                
                TileFactory tf = new DefaultTileFactory(info);
                kit.setTileFactory(tf);
                
//                TileFactory tf = new WMSTileFactory(new WMSService("http://localhost:8080/geoserver/wms?service=WMS&","mz:beatty"));
//                kit.setTileFactory(tf);
                
                
                kit.setZoom(14);
                kit.setCenterPosition(new GeoPosition(-116.76,36.9));
                kit.getMainMap().setDrawTileBorders(true);
                kit.getMainMap().setRestrictOutsidePanning(true);
                kit.getMainMap().setHorizontalWrapped(false);
                
                ((DefaultTileFactory)kit.getMainMap().getTileFactory()).setThreadPoolSize(8);
                JFrame frame = new JFrame("JXMapKit test");
                frame.add(kit);
                frame.pack();
                frame.setSize(500,300);
                frame.setVisible(true);
            }
        });
    }

}
