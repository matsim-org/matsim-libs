/* *********************************************************************** *
 * project: org.matsim.contrib.networkEditor
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 Daniel Ampuero
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.networkEditor.visualizing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.networkEditor.utils.GeometryTools;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * @author danielmaxx
 */
public class NetBlackboard extends javax.swing.JPanel {

	/**
	 * The working network
	 */
	protected NetworkImpl net;
	/**
	 * The counts for validation
	 */
	protected Counts<Link> counts;
	/**
	 * The bounding box of the network
	 */
	protected CalcBoundingBox box;
	/**
	 * The minumum/maximum capacity over the network
	 */
	protected double minCap, maxCap;
	/**
	 * The points that delimit the viewbox of the board
	 */
	protected double curX, curY, offX, offY;
	/**
	 * The active link to be marked when painting
	 */
	protected LinkImpl activeLink;
	/**
	 * The active node to be marked when painting
	 */
	protected Node activeNode;
	/**
	 * Controls to manipulate the viewbox and the net
	 */
	protected NetControls controls;
	/**
	 * The possible modes of the board
	 */
	public enum Mode{NONE, MOVING, SELECTION, PAINTING, CUTTING};
	/**
	 * The current mode of the board
	 */
	protected Mode actualMode = Mode.SELECTION;
	/**
	 * The line draw over the board for making a new node
	 */
	private LineOnBoard line;
	/**
	 * The move done over the board
	 */
	private MoveOnBoard move;
	/**
	 * Manages the undo/redo functionality
	 */
	public DifferenceManager diffManager;
	/**
	 * A list with the selected Links.
	 */
	private ArrayList<Link> selectedLinkList;
	/**
	 * Set to true if the Control key is pressed.
	 */
	private boolean isControlPressed;
	/**
	 * The link selection square.
	 */
	private SquareOnBoard selectionSquare;
	/**
	 * The selection magnetism tolerance
	 */
	private double tolerance;
	/**
	 * Maximum capacity to consider on a link
	 */
	final private double MaxAllowableCap = 60000.0;
	/**
	 * Minimum capacity to consider on a link
	 */
	final private double MinAllowableCap = 10.0;

	private Cursor pencilCursor = null;
	private Cursor moveCursor = null;
	private Cursor scissorCursor = null;
	
	/** Creates new form NetBlackboard */
	public NetBlackboard() {
		initComponents();
		initVars();
	}

	/**
	 * Creates a new form NetBlackboard from a net
	 * @param Net
	 */
	public NetBlackboard(NetworkImpl Net) {
		initVars();
		setNetwork(Net);
		initComponents();
	}

	/**
	 * Initializes the variables
	 */
	private void initVars() {
		actualMode = Mode.SELECTION;
		activeNode = null;
		activeLink = null;
		activeLink = null;
		net = null;
		controls = null;
		line = new LineOnBoard();
		move = new MoveOnBoard();
		selectionSquare = new SquareOnBoard();
		selectedLinkList = new ArrayList<Link>();
		isControlPressed = false;
		tolerance = 0.03;
		initCounts();

		try {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Image image = ImageIO.read(getClass().getResource("/org/matsim/contrib/networkEditor/images/pencil.gif"));
			Point hotSpot = new Point(0,0);
			this.pencilCursor = toolkit.createCustomCursor(image, hotSpot, "Pencil");
	
			image = ImageIO.read(getClass().getResource("/org/matsim/contrib/networkEditor/images/move.png"));
			hotSpot = new Point(0,0);
			this.moveCursor = toolkit.createCustomCursor(image, hotSpot, "Move");
	
			image = ImageIO.read(getClass().getResource("/org/matsim/contrib/networkEditor/images/scissors.png"));
			hotSpot = new Point(20,10);
			this.scissorCursor = toolkit.createCustomCursor(image, hotSpot, "Scissor");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the counts variable
	 */
	public void initCounts() {
		counts = new Counts<Link>();
		Calendar cal = Calendar.getInstance();
		counts.setYear(cal.get(Calendar.YEAR));
		if(net != null)
			counts.setName("Counts for net: " + net.getName() + "\n");
	}

	/**
	 * Sets the network to the board
	 * @param Net
	 */
	public void setNetwork(NetworkImpl Net) {
		net = Net;
		diffManager = new DifferenceManager(net);
		controls.setDifferenceManager(diffManager);
		controls.setButtonsEnabled(true);
		getBoundingBox();
		setMinMaxCaps();
		nameLabel.setText(net.getName());
		clearCounts();
		activeLink = null;
		activeNode = null;
		controls.updateTable();
	}

	/**
	 * Sets the controls to the board
	 * @param controls
	 */
	public void setNetControls(NetControls controls) {
		this.controls = controls;
	}

	/**
	 * Sets the mode mode of the board, changing its current mode
	 * @param m the new mode to be set
	 */
	public void setMode(Mode m) {
		actualMode = m;
		//activeLink = null;
		activeNode = null;
		controls.updateTable();
		line = new LineOnBoard();
	}

	/**
	 * Cleans the counts already stored
	 */
	public void clearCounts() {
		initCounts();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        capacityToggle = new javax.swing.JToggleButton();
        degradeLabel = new javax.swing.JLabel();
        minLabel = new javax.swing.JLabel();
        maxLabel = new javax.swing.JLabel();
        maxLabel.setVisible(false);
        jLabel2 = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        btnTolerance = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        lblCoordinates = new javax.swing.JLabel();

        setBackground(java.awt.Color.white);
        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
						public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                formMouseWheelMoved(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
						public void mouseClicked(java.awt.event.MouseEvent evt) {
                clickEvent(evt);
            }
            @Override
						public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
            @Override
						public void mouseExited(java.awt.event.MouseEvent evt) {
                formMouseExited(evt);
            }
            @Override
						public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            @Override
						public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
						public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
						public void mouseDragged(java.awt.event.MouseEvent evt) {
                formMouseDragged(evt);
            }
            @Override
						public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });
        addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
            @Override
						public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
            }
            @Override
						public void ancestorResized(java.awt.event.HierarchyEvent evt) {
                antecestorResized(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
						public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
            @Override
						public void keyReleased(java.awt.event.KeyEvent evt) {
                formKeyReleased(evt);
            }
            @Override
						public void keyTyped(java.awt.event.KeyEvent evt) {
                formKeyTyped(evt);
            }
        });

        capacityToggle.setBackground(java.awt.Color.white);
        capacityToggle.setText("C");
        capacityToggle.setToolTipText("Show Capacities");
        capacityToggle.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        capacityToggle.addActionListener(new java.awt.event.ActionListener() {
            @Override
						public void actionPerformed(java.awt.event.ActionEvent evt) {
                capacityToggleActionPerformed(evt);
            }
        });

        degradeLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/matsim/contrib/networkEditor/images/degradade.png"))); // NOI18N
        degradeLabel.setVisible(false);

        minLabel.setVisible(false);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        jLabel2.setText("Network name:");

        nameLabel.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        nameLabel.setText(" ");
        nameLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
						public void mouseClicked(java.awt.event.MouseEvent evt) {
                nameLabelMouseClicked(evt);
            }
        });

        btnTolerance.setBackground(java.awt.Color.white);
        btnTolerance.setText("T");
        btnTolerance.setToolTipText("Node Selection Tolerance");
        btnTolerance.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        btnTolerance.setMaximumSize(new java.awt.Dimension(15, 15));
        btnTolerance.setMinimumSize(new java.awt.Dimension(15, 15));
        btnTolerance.setPreferredSize(new java.awt.Dimension(15, 15));
        btnTolerance.addActionListener(new java.awt.event.ActionListener() {
            @Override
						public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToleranceActionPerformed(evt);
            }
        });

        jButton2.setBackground(java.awt.Color.white);
        jButton2.setText("D");
        jButton2.setToolTipText("Add Reverse Link");
        jButton2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jButton2.setMaximumSize(new java.awt.Dimension(15, 15));
        jButton2.setMinimumSize(new java.awt.Dimension(15, 15));
        jButton2.setPreferredSize(new java.awt.Dimension(15, 15));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            @Override
						public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        lblCoordinates.setBackground(java.awt.Color.white);
        lblCoordinates.setText("(0, 0)");
        lblCoordinates.setOpaque(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                    .addComponent(lblCoordinates, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(minLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(maxLabel))
                            .addComponent(degradeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(capacityToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnTolerance, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(nameLabel)
                    .addComponent(capacityToggle, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(degradeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(maxLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 291, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnTolerance, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(minLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblCoordinates)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	private void antecestorResized(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_antecestorResized
		this.setSize(this.getParent().getSize());
		this.repaint();
	}//GEN-LAST:event_antecestorResized

	private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
		this.repaint();
	}//GEN-LAST:event_formComponentResized

	private void clickEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clickEvent
		if (SwingUtilities.isRightMouseButton(evt)) {
			if(this.line.active) {
				line.active = false;
			} else {
				centerAt(inverseTransform(new Coord((double) evt.getPoint().x, (double) evt.getPoint().y)));
				this.zoom(0.05);
			}
			this.repaint();
		} else if (SwingUtilities.isLeftMouseButton(evt)) {
			if (this.net == null) {
				return;
			}
			if(actualMode == Mode.NONE)
				return;
			else if(actualMode == Mode.SELECTION) {
				Coord mousePos = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
				this.setActiveSomething(mousePos);
				this.controls.updateTable();
				/*if(activeLink != null)
                    addLinkInSelectedLinkList(activeLink, true);*/
			} else if(actualMode == Mode.PAINTING) {
				if(line.active == false) {
					Coord c = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
					LinkNodeCoord lnc = setLinePoint(c);
					line.start = inverseTransform(lnc.coord);
					line.linkClosestToStart = null;//lnc.link;
					line.nodeClosestToStart = lnc.node;
					//System.out.println(line.start);
					line.active = true;
				} else {
					Coord c = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
					LinkNodeCoord lnc = setLinePoint(c);
					line.end = lnc.coord;
					line.start = transform(line.start);
					line.linkClosestToEnd = null;//lnc.link;
					line.nodeClosestToEnd = lnc.node;
					createLink(line);
					line = new LineOnBoard();
				}
			} else if(actualMode == Mode.CUTTING) {
				if(activeLink == null) {
					javax.swing.JOptionPane.showMessageDialog(this, "You must select a link.", null, javax.swing.JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				Coord c = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
				if(this.splitActiveLink(inverseTransform(c))==false)
					javax.swing.JOptionPane.showMessageDialog(this, "Invalid selection, link not cut.", null, javax.swing.JOptionPane.WARNING_MESSAGE);
			}
			this.repaint();
		}
	}//GEN-LAST:event_clickEvent

	private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
		Point p = this.getMousePosition();
		if(p==null)
			return;
		Coord mousePos = new Coord((double) p.x, (double) p.y);
		Coord transformed = inverseTransform(mousePos);
		this.lblCoordinates.setText("("+String.format("%d", (int)transformed.getX())+","+String.format("%d", (int)transformed.getY())+")");
		this.repaint();
	}//GEN-LAST:event_formMouseMoved

	private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
		this.repaint();
	}//GEN-LAST:event_formMouseDragged

	private void formKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyTyped
		final int keyCode = evt.getKeyCode();
		final char keyChar = evt.getKeyChar();
		/*System.out.println(keyChar);
        System.out.println(keyCode);*/
		if(keyChar == java.awt.event.KeyEvent.VK_DELETE) {
			if(this.selectedLinkList.isEmpty()) return;
			this.deleteActiveLinks();
		}
	}//GEN-LAST:event_formKeyTyped

	private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
		this.requestFocusInWindow();
		if(this.actualMode == Mode.NONE)
			this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		else if(this.actualMode == Mode.PAINTING) {
			this.setCursor(this.pencilCursor);
		} else if(this.actualMode == Mode.SELECTION)
			this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		else if(actualMode == Mode.MOVING) {
			this.setCursor(this.moveCursor);
		} else if(actualMode == Mode.CUTTING) {
			this.setCursor(this.scissorCursor);
		}
	}//GEN-LAST:event_formMouseEntered

	private void formMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseExited
	}//GEN-LAST:event_formMouseExited

	private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
		int keyCode = evt.getKeyCode();
		if(keyCode == java.awt.event.KeyEvent.VK_DOWN) {
			moveViewBox(0, getHeight()*0.02);
			repaint();
		} else if(keyCode == java.awt.event.KeyEvent.VK_UP) {
			moveViewBox(0, -getHeight()*0.02);
			repaint();
		} else if(keyCode == java.awt.event.KeyEvent.VK_LEFT) {
			moveViewBox(-getWidth()*0.02, 0);
			repaint();
		} else if(keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
			moveViewBox(getWidth()*0.02, 0);
			repaint();
		} else if(keyCode == java.awt.event.KeyEvent.VK_CONTROL) {
			isControlPressed = true;
		}
	}//GEN-LAST:event_formKeyPressed

	private void formMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_formMouseWheelMoved
		if(this.getMousePosition()==null) return;
		//centerAt(inverseTransform(new CoordImpl(getMousePosition().x, getMousePosition().y)));
		int factor = evt.getWheelRotation()>0?1:-1;
		if(factor<0)
			zoom(0.03);
		else
			zoom(-0.02);
		repaint();
	}//GEN-LAST:event_formMouseWheelMoved

	private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
		if(actualMode == Mode.MOVING && move.active == false) {
			move.active = true;
			move.start = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
		} else if(actualMode == Mode.SELECTION && selectionSquare.active == false) {
			if(isControlPressed == false)
				this.selectedLinkList.clear();
			selectionSquare.active = true;
			selectionSquare.start = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
			this.repaint();
		}
	}//GEN-LAST:event_formMousePressed

	private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
		if (actualMode == Mode.MOVING && move.active == true) {
			move.end = new Coord((double) this.getMousePosition().x, (double) this.getMousePosition().y);
			this.moveViewBox(-(move.end.getX()-move.start.getX()), -(move.end.getY()-move.start.getY()));
			move.active = false;
			this.repaint();
		} else if(actualMode == Mode.SELECTION && (this.net != null) && selectionSquare.active == true) {
			Point p = this.getMousePosition();
			if(p != null) {
				selectionSquare.end = new Coord((double) p.x, (double) p.y);
				addLinkInsideSquareOnList();
			}
			selectionSquare.active = false;
			this.repaint();
		}
	}//GEN-LAST:event_formMouseReleased

	/*if(actualMode == mode.PAINTING) return;
        boolean state = capacityToggle.isSelected();
        setVisibleLabels(state);
        this.repaint();*/

	private void nameLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nameLabelMouseClicked
		if(net == null) return;
		if(evt.getClickCount() == 2) {
			String name = javax.swing.JOptionPane.showInputDialog("Network name", net.getName());
			net.setName(name);
			nameLabel.setText(name);
		}
	}//GEN-LAST:event_nameLabelMouseClicked

	private void formKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyReleased
		int keyCode = evt.getKeyCode();
		if(keyCode == java.awt.event.KeyEvent.VK_CONTROL) {
			isControlPressed = false;
			//selectedLinkList.clear();
		}
	}//GEN-LAST:event_formKeyReleased

	private void capacityToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capacityToggleActionPerformed
		boolean state = capacityToggle.isSelected();
		setVisibleLabels(state);
		this.repaint();
	}//GEN-LAST:event_capacityToggleActionPerformed

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		if(this.isDoubleWay(activeLink)) {
			javax.swing.JOptionPane.showMessageDialog(this, "El enlace ya es doble vía", "", javax.swing.JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		activeLink = (LinkImpl)makeDoubleWay(activeLink);
		controls.updateTable();
		repaint();
	}//GEN-LAST:event_jButton2ActionPerformed

	private void btnToleranceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnToleranceActionPerformed
		double old_tolerance = tolerance;
		do {
			String newTol = javax.swing.JOptionPane.showInputDialog("Nueva tolerancia. Ingrese un valor entre [5-99]", old_tolerance*1000.0);
			if(newTol == null) {
				tolerance = old_tolerance;
				return;
			}
			try {
				tolerance = Double.parseDouble(newTol)/1000.0;
			} catch(Exception e) {
				javax.swing.JOptionPane.showMessageDialog(this, "Values must be numeric", "Error de formato", javax.swing.JOptionPane.WARNING_MESSAGE);
				tolerance = old_tolerance;
				btnToleranceActionPerformed(evt);
			}
			if(tolerance < 0.005 && tolerance > 0.099)
				javax.swing.JOptionPane.showMessageDialog(this, "Valor de tolerancia incorrecto. Debe asignar un valor en el rango [5, 99]");
		} while(tolerance < 0.005 && tolerance > 0.099);
	}//GEN-LAST:event_btnToleranceActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnTolerance;
    private javax.swing.JToggleButton capacityToggle;
    private javax.swing.JLabel degradeLabel;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblCoordinates;
    private javax.swing.JLabel maxLabel;
    private javax.swing.JLabel minLabel;
    private javax.swing.JLabel nameLabel;
    // End of variables declaration//GEN-END:variables

	/**
	 * Creates a new Counting Station (CS)
	 * @param link The link wher the Counting Station will be
	 * @param stationName The name of the Counting Station
	 * @return Count the CS created. If this CS already exists (at the given
	 *         lik) a null value is returned.
	 */
	public Count<Link> addCountingStation(Link link, String stationName) {
		Count<Link> c = counts.createAndAddCount(link.getId(), stationName);
		return c;
	}

	/**
	 * Gets a Counting Station
	 * @param link
	 * @return A Count object. Null if there is no such CS
	 */
	public Count<Link> getCountingStation(Link link) {
		return counts.getCount(link.getId());
	}

	/**
	 * Adds a count to an existing Counting Station
	 * @param link where the Counting Station is supposed to be
	 * @param h the hour of the day to create the volumen
	 * @param volumen
	 * @return true if the count was added, false else
	 */
	public boolean addCount(Link link, int h, double volumen) {
		if(h < 1 || h > 24)
			return false;
		Count<Link> c = counts.getCount(link.getId());
		if(c == null)
			return false;
		if(c.getVolume(h)==null) {
			c.createVolume(h, volumen);
			return true;
		} else
			return false;
	}

	/**
	 * Edit a volumen of an existing Counting Station at a given hour
	 * @param link
	 * @param h
	 * @param volumen
	 * @return true if the count exists and it was succesfully modified
	 */
	public boolean editCount(Link link, int h, double volumen) {
		Count<Link> c = counts.getCount(link.getId());
		if(c == null)
			return false;
		if(c.getVolume(h)==null)
			return false;
		c.getVolumes().get(h).setValue(volumen);
		return true;
	}

	/**
	 * Deletes a count from an existing Counting Station at a given hour
	 * @param link
	 * @param h
	 * @return false if the counting station doesn't exist, or if there is
	 *         no such hour in the Counting Station.
	 */
	public boolean deleteCount(Link link, int h) {
		Count<Link> c = counts.getCount(link.getId());
		if(c == null)
			return false;
		if(c.getVolume(h) == null)
			return false;
		c.getVolumes().remove(h);
		return true;
	}

	/**
	 * Gets the bounding box of the network
	 */
	protected void getBoundingBox(){
		if(net==null)
			return;
		box = new CalcBoundingBox();
		box.run(net);
		curX = box.getMinX();
		curY = box.getMinY();
		offX = Math.abs(box.getMaxX() - box.getMinX());
		offY = Math.abs(box.getMaxY() - box.getMinY());
	}

	/**
	 * Refresh the maxCap and minCap values when a new capacity is set
	 * @param newCap
	 */
	protected void refreshCapacity(double newCap) {
		maxCap = Math.max(maxCap, newCap);
		minCap = Math.min(minCap, newCap);
		updateCapacityLabels();
	}

	protected void updateCapacityLabels() {
		if(net == null) {
			minLabel.setText("");
			maxLabel.setText("");
		}else {
			minLabel.setText(Double.toString(minCap));
			maxLabel.setText(Double.toString(maxCap));
		}
	}

	protected void setVisibleLabels(boolean flag) {
		degradeLabel.setVisible(flag);
		minLabel.setVisible(flag);
		maxLabel.setVisible(flag);
	}

	private boolean validLink(Link link) {
		if(link.getCapacity() >= MaxAllowableCap) return false;
		if(link.getCapacity() <= MinAllowableCap) return false;
		return true;
	}

	/**
	 * Sets the variables miniCap and maxiCap
	 */
	public void setMinMaxCaps() {
		if(net == null)
			return;
		maxCap = Double.MIN_VALUE;
		minCap = Double.MAX_VALUE;
		for(Link link : net.getLinks().values()) {
			if(!validLink(link)) continue;
			maxCap = Math.max(maxCap, link.getCapacity());
			minCap = Math.min(minCap, link.getCapacity());
		}
		updateCapacityLabels();
	}

	/**
	 * Gets the nearest Link to a Coordinate using brute force to find it
	 * @param coord
	 * @return The closest link
	 */
	protected Link getNearestLinkBrute(Coord coord){
		Link selected = null;
		double minDist = Double.MAX_VALUE;
		for(Link link:net.getLinks().values()){
			double thisDist = ((LinkImpl) link).calcDistance(coord);
			if(thisDist < minDist) {
				minDist = thisDist;
				selected = link;
			}
		}
		return selected;        
	}

	/**
	 * Gets the nearest Link to a Coordinate and certanly distance. It was
	 * improved to use the functionality of the quadtree implemented in the
	 * NetworkImpl class.
	 * @param coord
	 * @param dist
	 * @return The nearest link
	 */
	protected Link getNearestLinkImproved(Coord coord, double dist){
		Link selectedLink = null;
		Collection<Node> nodes = net.getNearestNodes(coord, dist);
		double minDist = Double.MAX_VALUE;
		for(Node node:nodes) {
			for(Link link:node.getInLinks().values()) {
				double curDist = ((LinkImpl) link).calcDistance(coord);
				if(curDist < minDist) {
					selectedLink = link;
					minDist = curDist;
				}
			}
			for(Link link:node.getOutLinks().values()) {
				double curDist = ((LinkImpl) link).calcDistance(coord);
				if(curDist < minDist) {
					selectedLink = link;
					minDist = curDist;
				}
			}
		}
		return selectedLink;
	}

	private boolean insideSquare(Link l, Coord startS, Coord endS) {
		Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
		Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY());
		LineSegment lineSegment = new LineSegment(start, end);
		Coord p1 = inverseTransform(startS), p2 = inverseTransform(endS);
		Coordinate c1 = new Coordinate(p1.getX(), p1.getY()), c2 = new Coordinate(p2.getX(), p2.getY());
		boolean flag = GeometryTools.intersectRectangle(c1, c2, lineSegment)
				|| GeometryTools.isInside(GeometryTools.getRectangle(c1, c2), lineSegment);
		/*if(flag==true)
            System.out.println("Link " + l.getId() + " inside (" + p1 + ";" + p2 + ")");*/
		return flag;
	}

	private void addLinkInsideSquareOnList() {
		final double midX = (selectionSquare.start.getX()+selectionSquare.end.getX())/2.0;
		final double midY = (selectionSquare.start.getY()+selectionSquare.end.getY())/2.0;
		Coord mid = inverseTransform(new Coord(midX, midY));
		final double dist = CoordUtils.calcEuclideanDistance(mid, inverseTransform(selectionSquare.end));
		//System.out.println("mid = " + mid + ", dist = " + dist);
		Collection<Node> nodes = net.getNearestNodes(mid, dist*8);
		Coord start = new Coord(selectionSquare.start.getX(), selectionSquare.start.getY()), end = new Coord(selectionSquare.end.getX(), selectionSquare.end.getY());
		for(Node node : nodes) {
			for(Link l : node.getInLinks().values()) {
				if(insideSquare(l, start, end))
					this.addLinkInSelectedLinkList(l, false);
			}
			for(Link l : node.getOutLinks().values()) {
				if(insideSquare(l, start, end))
					this.addLinkInSelectedLinkList(l, false);
			}
		}
	}

	/**
	 *  Add a link to the selected link list.
	 *  @param link The link to be inserted.
	 *  @param remove if the link is already in the list, remove it.
	 *  @return true or false weather the link was already in the list.
	 */
	public boolean addLinkInSelectedLinkList(Link link, boolean remove) {
		if(isControlPressed==false && selectionSquare.active==false) {
			selectedLinkList.clear();
		}
		//System.out.println("list.size() = " + selectedLinkList.size());
		if(link!=null) {
			for(Link l : selectedLinkList) {
				if(l.getId().compareTo(link.getId())==0) {
					if(remove) selectedLinkList.remove(l);
					return false;
				}
			}
		}
		if(link != null) {
			//System.out.println("Adding link " + link.getId());
			selectedLinkList.add(link);
		}
		activeLink = (LinkImpl)link;
		return true;
	}

	/**
	 *  Cleans the list of deleted links
	 */
	public void updateSelectedLinkList() {
		for(Link l : selectedLinkList) {
			if(!net.getLinks().containsKey(l.getId())) {
				selectedLinkList.remove(l);
				return;
			}
		}
	}

	/**
	 *  Returns the size of the selected link list
	 *  @return and integer indicating the size of the selected links.
	 */
	public int selectedLinkSize() {
		return selectedLinkList.size();
	}

	/**
	 * Deletes the active link of the board from the network.
	 */
	public void deleteActiveLinks() {
		if(selectedLinkList.isEmpty()) return;
		for(Link link : selectedLinkList) {
			pairNodeNode p = deleteLink(link);
			diffManager.saveState(diffManager.cloneLink(link), DifferenceManager.type.DELETE, p.node1, p.node2, null, null, null, null, null, null);
			activeLink = null;
		}
		selectedLinkList.clear();
		controls.updateButtons();
		activeLink = null;
		repaint();
	}


	/**
	 * Takes two links and build a new one that unify them. The method fails if
	 * both segments are not joined in sequence.
	 * @param segment1
	 * @param segment2
	 */
	private void unifyLinks(Link segment1, Link segment2) {
		if(segment1.getToNode().getId().equals(segment2.getFromNode().getId()) == false)
			return;
		Link newLink = net.getFactory().createLink(this.getRandomLinkId(),
				segment1.getFromNode(), segment2.getToNode(), net,
				segment1.getLength()+segment2.getLength(),
				(segment1.getFreespeed()+segment2.getFreespeed())/2.0,
				(segment1.getCapacity()+segment2.getCapacity())/2.0,
				Math.min(segment1.getNumberOfLanes(), segment2.getNumberOfLanes()));
		net.addLink(newLink);
		net.removeLink(segment1.getId());
		net.removeLink(segment2.getId());
	}

	/**
	 * Deletes a link from the network
	 * @param link
	 * @return The nodes at the endpoints of the link as a pairNodeNode
	 */
	private pairNodeNode deleteLink(Link link) {
		pairNodeNode p = new pairNodeNode();
		net.removeLink(link.getId());
		if(link.getFromNode().getInLinks().isEmpty() && link.getFromNode().getOutLinks().isEmpty()) {
			p.node1 = link.getFromNode();
			net.removeNode(link.getFromNode().getId());
		}
		if(link.getToNode().getInLinks().isEmpty() && link.getToNode().getOutLinks().isEmpty()) {
			p.node2 = link.getToNode();
			net.removeNode(link.getToNode().getId());
		}
		return p;
	}

	public void cleanNetwork() {
		new NetworkCleaner().run(net);
	}

	/**
	 * Gets the nearest nodes to a coordinate in a radius of dist distance
	 * @param coord
	 * @param dist
	 * @return The nearest node from the coordinates at a radius distance of dist
	 */
	protected Node getNearestNodes(Coord coord, double dist) {
		Collection<Node> nodes = net.getNearestNodes(coord, dist);
		double minDist = Double.MAX_VALUE;
		Node selectedNode = null;
		for(Node node:nodes) {            
			double curDist = CoordUtils.calcEuclideanDistance(coord, node.getCoord());
			if(curDist < minDist) {
				selectedNode = node;
				minDist = curDist;
			}           
		}
		return selectedNode;
	}

	/**
	 * Gets the absolute nearest node from a Coordinate
	 * @param coord
	 * @return The nearest Node
	 */
	protected Node getNearestNode(Coord coord) {
		return net.getNearestNode(coord);
	}

	/**
	 * Gets the closest Link/Node and its coordinates from a mouse position.
	 * @param mouse A coordinate in the screen coordinate system
	 * @return A triplet formed by Link-Node-Coord. The fields Link and Node
	 *         cannot be non-null at the same time. If they are null, it means
	 *         that no nearest link or node can be picked.
	 */
	private LinkNodeCoord setLinePoint(Coord mouse) {
		LinkNodeCoord lnc = new LinkNodeCoord();
		PairLinkNode p = this.getClosestThing(mouse);
		if(p.node != null) {
			lnc.coord = transform(p.node.getCoord());
			lnc.node = p.node;            
		} else if(p.link != null) {
			lnc.coord = calculatePointOnLine(transform(p.link.getFromNode().getCoord()), transform(p.link.getToNode().getCoord()), mouse);
			if(isOutside(transform(p.link.getFromNode().getCoord()), transform(p.link.getToNode().getCoord()), lnc.coord)){
				Node nearest = getNearestNode(inverseTransform(mouse));
				lnc.coord = transform(nearest.getCoord());
				lnc.node = nearest;
			} else {
				lnc.link = p.link;
			}
		} else {
			lnc.coord = mouse;
		}
		return lnc;
	}

	private LinkNodeCoord setLinePointForceNode(Coord mouse) {
		LinkNodeCoord lnc = new LinkNodeCoord();
		Node nearest = getNearestNode(inverseTransform(mouse));
		lnc.coord = transform(nearest.getCoord());
		lnc.node = nearest;
		return lnc;
	}

	/**
	 * Moves the view box by some difference over x and y.
	 * @param dx
	 * @param dy
	 */
	public void moveViewBox(double dx, double dy) {
		if(Math.abs(dx) > getWidth() || Math.abs(dy) > getHeight())
			return;
		Coord origin = inverseTransform(new Coord((double) 0, (double) 0));
		Coord moves = inverseTransform(new Coord(dx, dy));
		curX += (moves.getX()-origin.getX());
		curY += (moves.getY()-origin.getY());
	}

	public void centerAt(Coord point) {
		Coord mid = getMidPoint();
		curX += point.getX() - mid.getX();
		curY += point.getY() - mid.getY();
	}

	/**
	 * Zoom the view.
	 * @param qt It must be possitive if a zoom-in is requiered, negative if the
	 *           opposite is needed.
	 */
	public void zoom(double qt) {
		curX += (offX*qt)/2.0; curY += (offY*qt)/2.0;
		offX -= (offX*qt); offY -= (offY*qt);        
	}

	/**
	 * Transform a coordinate from the screen coordinate system to the network
	 * coordinate system
	 * @param coord
	 * @return The transformed coordinate
	 */
	protected Coord transform(Coord coord) {
		double x = coord.getX() - curX, y = coord.getY() - curY;
		final double boxSizeX = offX + offX*0.05;
		final double boxSizeY = offY + offY*0.05;
		x = (x*this.getWidth())/boxSizeX + 10;
		y = (y*this.getHeight())/boxSizeY + 10;
		y = getHeight() - y;
		Coord result = new Coord(x, y);
		return result;
	}

	/**
	 * Transform a coordinate from a network coordinate system to a screen coordinate.
	 * @param coord
	 * @return The transformed coordinate
	 */
	protected Coord inverseTransform(Coord coord) {
		final double boxSizeX = offX + offX*0.05;
		final double boxSizeY = offY + offY*0.05;
		double x = coord.getX() - 10, y = coord.getY() + 10;
		x = (x*boxSizeX)/getWidth();
		y = getHeight() - y;
		y = (y*boxSizeY)/getHeight();
		x = x + curX;
		y = y + curY;
		Coord result = new Coord(x, y);
		return result;
	}

	/**
	 * Builds a node with a random Id at certanly coordinate
	 * @param coord
	 * @return A random Id node
	 */
	private Node getRandomNode(Coord coord) {
		Random r = new Random();
		Id<Node> id = Id.create(r.nextInt(net.getNodes().size()*3), Node.class); //PARCHE
		while(net.getNodes().keySet().contains(id))
			id = Id.create(r.nextInt(net.getNodes().size()*3), Node.class); //PARCHE
		Node result = net.getFactory().createNode(id, coord);
		return result;
	}

	/**
	 * Gets a random-valid Id for a link
	 * @return A random-valid Id
	 */
	private Id<Link> getRandomLinkId() {
		Random r = new Random();
		Id<Link> id = Id.create(r.nextInt(net.getLinks().size()*3), Link.class); //PARCHE
		while(net.getLinks().keySet().contains(id))
			id = Id.create(r.nextInt(net.getLinks().size()*3), Link.class); //PARCHE;
		return id;
	}

	public Link makeDoubleWay(Link link){
		LinkImpl newLink = (LinkImpl)net.getFactory().createLink(getRandomLinkId(), link.getToNode(), link.getFromNode());
		newLink.setCapacity(link.getCapacity());
		newLink.setLength(link.getLength());
		newLink.setFreespeed(link.getFreespeed());
		newLink.setNumberOfLanes(link.getNumberOfLanes());
		net.addLink(newLink);
		diffManager.saveState(diffManager.cloneLink(newLink), DifferenceManager.type.CREATE, null, null, null, null, null, null, null, null);
		controls.updateButtons();
		return newLink;
	}

	/**
	 * Creates a Link on the network based in the information provided by the
	 * structure LineOnBoard.
	 * @param line
	 */
	private void createLink(LineOnBoard line) {
		Node start = null, end = null;
		boolean startAdded = false, endAdded = false;
		if(line.nodeClosestToStart!=null)
			start = line.nodeClosestToStart;
		else {
			start = getRandomNode(inverseTransform(line.start));
			net.addNode(start);
			startAdded = true;
		}
		if(line.nodeClosestToEnd != null)
			end = line.nodeClosestToEnd;
		else {
			end = getRandomNode(inverseTransform(line.end));
			net.addNode(end);
			endAdded = true;
		}
		Id<Link> id = getRandomLinkId();
		LinkImpl newLink = (LinkImpl)net.getFactory().createLink(id, start, end);
		newLink.setLength(newLink.getEuklideanLength());
		newLink.setCapacity(600.0);
		newLink.setFreespeed(8.3333);
		net.addLink(newLink);
		pairLinkLink p1 = new pairLinkLink(), p2 = new pairLinkLink();
		if(line.linkClosestToStart != null) {
			p1 = fixLink(line.linkClosestToStart, start);
		}
		if(line.linkClosestToEnd != null) {
			p2 = fixLink(line.linkClosestToEnd, end);
		}
		this.addLinkInSelectedLinkList(newLink, false);
		diffManager.saveState(diffManager.cloneLink(newLink), DifferenceManager.type.CREATE, startAdded?start:null, endAdded?end:null, p1.link1, p1.link2, p2.link1, p2.link2, line.linkClosestToStart, line.linkClosestToEnd);
		controls.updateButtons();
		controls.updateTable();
		setMinMaxCaps();
	}

	/**
	 * Splits the active link at the projection of the given coordinate p on
	 * it.
	 * @param p the coordinate where the projection must be made.
	 * @return true if the link can be cutted, that is, p is perpendicular to
	 *          the active link.
	 */
	public boolean splitActiveLink(Coord p) {
		Coord projection = calculatePointOnLine(activeLink.getFromNode().getCoord(), activeLink.getToNode().getCoord(), p);
		//System.out.println(projection);
		double rel = calculateRelation(activeLink.getFromNode().getCoord(), activeLink.getToNode().getCoord(), projection);
		if(rel <= 0.0 || rel >=1.0)
			return false;
		else {
			pairLinkLink pair = cutLink(activeLink, projection);
			this.updateSelectedLinkList();
			this.addLinkInSelectedLinkList(pair.link1, true);
			return true;
		}
	}

	/**
	 * Splits a link L in the coordinate given by p.
	 * @param toCut link to be cut
	 * @param p coordinate to cut
	 */
	private pairLinkLink cutLink(Link toCut, Coord p) {
		Node endPoint = getRandomNode(p);
		net.addNode(endPoint);
		pairLinkLink pair = fixLink(toCut, endPoint);
		diffManager.saveState(null, DifferenceManager.type.CREATE, endPoint, null, pair.link1, pair.link2, null, null, activeLink, null);
		controls.updateButtons();
		controls.updateTable();
		return pair;
	}

	/**
	 * If a endpoint of a new link is placed on an existent link L, L must be
	 * partitioned into two links joined by a node.
	 * @param toFix
	 * @param endPoint
	 * @return The new links builded for replacing L.
	 */
	private pairLinkLink fixLink(Link toFix, Node endPoint) {
		Node from = toFix.getFromNode();
		Node to = toFix.getToNode();
		net.removeLink(toFix.getId());
		double rel = calculateRelation(toFix.getFromNode().getCoord(), toFix.getToNode().getCoord(), endPoint.getCoord());
		System.out.println(rel);
		Id<Link> aux = getRandomLinkId();
		Link newLink1 = createLinkFromExistent(toFix, rel, aux, from, endPoint);
		net.addLink(newLink1);
		aux = getRandomLinkId();
		Link newLink2 = createLinkFromExistent(toFix, 1-rel, aux, endPoint, to);
		net.addLink(newLink2);
		return new pairLinkLink(newLink1, newLink2);
	}

	/**
	 * Used in the fixLik function for creating a new link
	 * @param source
	 * @param distRelation
	 * @param id
	 * @param from
	 * @param to
	 * @return
	 */
	private Link createLinkFromExistent(Link source, double distRelation, Id<Link> id, Node from, Node to) {
		return net.getFactory().createLink(id, from, to, net, source.getLength()*distRelation, source.getFreespeed(), source.getCapacity(), source.getNumberOfLanes());
	}

	/**
	 * Gets the mid point of the view box.
	 * @return
	 */
	protected Coord getMidPoint() {
		return new Coord(curX + offX / 2, curY + offY / 2);
	}

	/*
	 * Gets a overdimensioned distance for picking nodes
	 */
	protected double getMidDistance() {
		return Math.sqrt((offY*offY)+(offX*offX))*4;
	}

	/**
	 * Test whether the point P3 is inside the bounding box of the segment formed
	 * by points P1 and P2
	 * @param P1 first point of the segment
	 * @param P2 second point of the segment
	 * @param P3 the point to be tested
	 * @return true if P3 is inside the bounding box of the segment formed by
	 * P1 and P2.
	 */
	protected boolean isOutside(Coord P1, Coord P2, Coord P3) {
		double minx = Math.min(P1.getX(), P2.getX()), miny = Math.min(P1.getY(), P2.getY());
		double maxx = Math.max(P1.getX(), P2.getX()), maxy = Math.max(P1.getY(), P2.getY());
		double x = P3.getX(), y = P3.getY();
		if(x >= minx && x <= maxx &&  y >= miny && y <= maxy)
			return false;
		else
			return true;
	}

	/**
	 * Calculates the point where P3 is proyected on the line formed by the points
	 * P1 and P2
	 * @param P1 the first point of the line
	 * @param P2 the second point of the line
	 * @param P3 the point to be projected
	 * @return a Coord where is the projected point is located
	 */
	protected Coord calculatePointOnLine(Coord P1, Coord P2, Coord P3) {
		double rel = calculateRelation(P1, P2, P3);
		double dx = Math.abs(P1.getX()-P2.getX()), dy = Math.abs(P1.getY()-P2.getY());
		double nx = P1.getX()+dx*rel, ny = P1.getY()+dy*rel;
		if(P1.getX()>P2.getX())
			nx = P1.getX()-dx*rel;
		if(P1.getY()>P2.getY())
			ny = P1.getY()-dy*rel;
		Coord result = new Coord(nx, ny);
		return result;
	}

	/**
	 * Calculates the relation made by the segment formed by the points P1 and P2
	 * and the point P3.
	 * @param P1
	 * @param P2
	 * @param P3
	 * @return A number in the range [0, 1].
	 */
	private double calculateRelation(Coord P1, Coord P2, Coord P3) {
		double d = CoordUtils.distancePointLinesegment(P1, P2, P3);
		double r = CoordUtils.calcEuclideanDistance(P1, P3);
		double dist1 = Math.sqrt((r*r)-(d*d)); //distance from P1 to the point;
		double dist2 = CoordUtils.calcEuclideanDistance(P1, P2);
		double rel = dist1/dist2;
		return rel;
	}

	/**
	 * Checks wheter a point is inside the view box
	 * @param coord
	 * @return true if it's inside, false if not.
	 */
	protected boolean isInside(Coord coord) {
		if(coord.getX() < 0 || coord.getY() < 0)
			return false;
		if(coord.getX() > getWidth() || coord.getY() > getHeight())
			return false;
		return true;
	}

	/**
	 * Inverts the Y component of a coordinate. Used for transforming the screen
	 * coordinate system to the network coordinate system.
	 * @param coord
	 */
	protected void invertY(Coord coord){
		coord.setY(getHeight()-coord.getY());
	}

	/**
	 * Used by fixLine
	 * @param P
	 * @param m
	 * @param b
	 */
	protected void fixPoint(Coord P, double m, double b) {
		if(m == Double.POSITIVE_INFINITY) {
			if(P.getY()<0)
				P.setY(0.0);
			else
				P.setY(getHeight());
		} else if(P.getX() < 0.0) {
			P.setX(0.0);
			P.setY(b);
		} else if(P.getY()< 0.0) {
			P.setY(0.0);
			P.setX(-b/m);
		} else if(P.getX()>getWidth()) {
			P.setX(getWidth());
			P.setY(m*getWidth()+b);
		} else {
			P.setY(getHeight());
			P.setX((getHeight()-b)/m);
		}
	}


	/**
	 * Fix a segment in order it is possible to be displayed by the board.
	 * @param P1
	 * @param P2
	 */
	protected void fixLine(Coord P1, Coord P2) {
		if(!isInside(P1)&&!isInside(P2)) return;
		if(isInside(P1)&&isInside(P2)) return;
		invertY(P1);
		invertY(P2);
		double m = Double.POSITIVE_INFINITY;
		if(P1.getX() != P2.getX())
			m = (P1.getY()-P2.getY())/(P1.getX()-P2.getX());
		double b = P1.getY() - m*P1.getX();
		if(!isInside(P1)) {
			fixPoint(P1, m, b);
		}else if(!isInside(P2)) {
			fixPoint(P2, m, b);
		}
		invertY(P1);
		invertY(P2);
	}

	/**
	 * Paints a node.
	 */
	protected void paintNode(Node node, Graphics2D g2) {
		if(node == activeNode)
			g2.setColor(Color.RED);
		else
			g2.setColor(Color.BLUE);
		Coord transformed = transform(node.getCoord());
		g2.fillRect((int)transformed.getX()-2, (int)transformed.getY()-2, 4, 4);
	}

	/**
	 * Sets the color of the link to be painted.
	 * @param link
	 */
	protected void setLinkColor(Link link, Graphics2D g2) {
		final double cap = link.getCapacity();
		if(!validLink(link)) {
			g2.setColor(Color.black);
			return;
		}
		final double middle =  (minCap+maxCap)/2.0;
		//System.out.println(cap + " " + minCap + " " + middle + " " + maxCap);
		if(cap < middle) {
			int green = (int)((cap-minCap)*255.0/(middle-minCap));
			green = Math.min(green, 255);
			int blue = 255-green;
			g2.setColor(new Color(0, green, blue));
		} else {
			int red = (int)((cap-middle)*255.0/(maxCap-middle));
			red = Math.min(red, 255);
			int green = 255-red;
			g2.setColor(new Color(red, green, 0));
		}
	}

	/**
	 * Paints a set of links
	 * @param links
	 * @param g2
	 */
	protected void paintLinks(Collection<? extends Link> links, Graphics2D g2) {
		g2.setColor(Color.BLACK);
		for(Link link:links){            
			Coord transformed = transform(link.getFromNode().getCoord());
			Coord transformed2 = transform(link.getToNode().getCoord());
			fixLine(transformed, transformed2);
			if(capacityToggle.isSelected())
				setLinkColor(link, g2);
			g2.drawLine((int)transformed.getX(), (int)transformed.getY(), (int)transformed2.getX(), (int)transformed2.getY());
		}
	}

	/**
	 * Paints the active link
	 * @param g2
	 */
	protected void paintActiveLinks(Graphics2D g2) {
		if(selectedLinkList.isEmpty())
			return;
		for(Link link : selectedLinkList) {
			Coord P1 = transform(link.getFromNode().getCoord());
			Coord P2 = transform(link.getToNode().getCoord());
			g2.setColor(Color.CYAN);
			g2.drawLine((int)P1.getX(), (int)P1.getY(), (int)P2.getX(), (int)P2.getY());
			paintActiveLinkArrow(g2, link);
		}
	}

	/**
	 * Paints the active link direction arrow
	 * @param g2
	 */
	protected void paintActiveLinkArrow(Graphics2D g2, Link activeLink) {
		if(activeLink == null)
			return;
		Coord P1 = transform(activeLink.getFromNode().getCoord());
		Coord P2 = transform(activeLink.getToNode().getCoord());
		AffineTransform tx = new AffineTransform();
		Line2D.Double lines = new Line2D.Double(P1.getX(),P1.getY(),P2.getX(),P2.getY());

		Polygon arrowHead = new Polygon();
		arrowHead.addPoint( 0,4);
		arrowHead.addPoint( -4, -4);
		arrowHead.addPoint( 4,-4);

		tx.setToIdentity();
		double angle = Math.atan2(lines.y2-lines.y1, lines.x2-lines.x1);
		tx.translate(lines.x2, lines.y2);
		tx.rotate((angle-Math.PI/2d));

		Graphics2D g = (Graphics2D)g2.create();
		g.setTransform(tx);
		g.fill(arrowHead);
		g.dispose();
	}

	/**
	 * Checks wether the @link has a "brother" way going in the inverse
	 * direction.
	 * @param link to be checked
	 * @return true if the link has an inverse direction way, false otherwise.
	 */
	protected boolean isDoubleWay(Link link) {
		for(Entry<Id<Link>, ? extends Link> entry : link.getToNode().getOutLinks().entrySet()) {
			if(entry.getValue().getToNode().getId().equals(link.getFromNode().getId()))
				return true;
		}
		return false;
	}

	/**
	 * Gets the inverse direction link of link
	 * @param link to be procesed
	 * @param check if it is necesary to check for the existence of an inverse
	 *        direction link.
	 * @return the inverse direction link if it exist. null otherwise.
	 */
	protected Link getInverseDirectionWay(Link link, boolean check) {
		boolean flag = true;
		if(check)
			flag = isDoubleWay(link);
		if(!flag)
			return null;
		for(Entry<Id<Link>, ? extends Link> entry : link.getToNode().getOutLinks().entrySet()) {
			if(entry.getValue().getToNode().getId().equals(link.getFromNode().getId()))
				return entry.getValue();
		}
		return null;
	}

	/**
	 * Gets the inverse direction link of link
	 * @param link to be procesed
	 * @return the inverse direction link if it exist. null otherwise.
	 */
	protected Link getInverseDirectionWay(Link link) {
		return getInverseDirectionWay(link, true);
	}

	/**
	 * Set the active Link/Node
	 * @param Pos
	 */
	protected void setActiveSomething(Coord Pos){
		PairLinkNode result = getClosestThing(Pos);
		activeNode = result.node;
		if(activeLink != null && result.link!=null && activeLink.getId().equals(result.link.getId())) {
			if(isDoubleWay(activeLink))
				addLinkInSelectedLinkList(getInverseDirectionWay(activeLink, false), true);
			else
				addLinkInSelectedLinkList(result.link, true);
		} else
			addLinkInSelectedLinkList(result.link, true);
	}


	/**
	 * Returns the closest link/node of the Screen position Pos.
	 * @param The screen position Pos
	 * @return a pairLinkNode with the selected Link/Node. If no Link/Node can
	 *          be choose, return null in one or both components.
	 */
	protected PairLinkNode getClosestThing(Coord Pos) {
		Coord transformed = inverseTransform(Pos);
		Link link = getNearestLinkImproved(transformed, offX/4.0);
		Coord P1 = new Coord(Double.MAX_VALUE, Double.MAX_VALUE), P2 = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
		if(link != null) {
			P1 = transform(link.getFromNode().getCoord());
			P2 = transform(link.getToNode().getCoord());
		}
		double dist1 = CoordUtils.distancePointLinesegment(P1, P2, Pos);
		Node node = net.getNearestNode(transformed);
		if(node == null && link == null)
			return new PairLinkNode(null, null);
		Coord P3 = transform(node.getCoord());
		double dist2 = CoordUtils.calcEuclideanDistance(P3, Pos);
		//System.out.println("dist1 = " + dist1 + ", dist2 = " + dist2 + ", tolerance = " + tolerance*(getWidth()+getHeight())/2.0);
		if(dist1 > (tolerance*(getWidth()+getHeight())/2.0))
			link = null;
		if(dist2 > (tolerance*(getWidth()+getHeight())/2.0))
			node = null;
		if(isOutside(P1, P2, Pos))
			link = null;
		else
			node = null;
		//System.out.println(link + " " + node);
		return new PairLinkNode(link, node);
	}

	protected void paintSquare(Graphics g) {
		g.setColor(Color.GRAY);
		Point p = this.getMousePosition();
		if(p == null) return;
		Coord end = new Coord(p.getX(), p.getY());
		LinearRing ring = GeometryTools.getRectangle(GeometryTools.MATSimCoordToCoordinate(selectionSquare.start), GeometryTools.MATSimCoordToCoordinate(end));
		Polygon poly = GeometryTools.toJavaPolygon(ring);
		g.drawPolygon(poly);
	}


	/**
	 * Paints the current Line being draw
	 * @param g
	 */
	protected void paintLine(Graphics g) {
		g.setColor(Color.RED);
		Coord transformed = transform(line.start);
		g.fillOval((int)transformed.getX()-2, (int)transformed.getY()-2, 4, 4);
		Coord end;
		Point p = this.getMousePosition();
		if(p == null) return;
		end = new Coord(p.getX(), p.getY());
		g.setColor(Color.BLACK);
		//System.out.println(end.getX() + " " + end.getY());
		g.drawLine((int)transformed.getX(), (int)transformed.getY(), (int)end.getX(), (int)end.getY());
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		//paintShit(g2);
		if(net==null) return;
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Collection<Node> nodeList = net.getNearestNodes(getMidPoint(), getMidDistance());
		for(Node node: nodeList) {
			paintNode(node, g2);
			paintLinks(node.getOutLinks().values(), g2);
			paintLinks(node.getInLinks().values(), g2);
		}
		paintActiveLinks(g2);
		if(line.active == true) paintLine(g);
		if(selectionSquare.active == true) paintSquare(g);
	}

	/*protected void paintShit(Graphics2D g2) {
        int startx = this.getWidth() - 600;
        int starty = this.getHeight() - 150;
        for(int i=0; i<255; ++i) {
            g2.setColor(new Color(0, i, 255-i));
            g2.drawLine(startx+i, starty, startx+i, starty+100);
        }
        startx += 255;
        for(int i=0; i<255; ++i) {
            g2.setColor(new Color(i, 255-i, 0));
            g2.drawLine(startx+i, starty, startx+i, starty+100);
        }
    }*/

	public class PairLinkNode{
		public Node node;
		public Link link;
		PairLinkNode(){}
		PairLinkNode(Link l, Node n) {
			node = n;
			link = l;
		}
	}

	private class LinkNodeCoord {
		public Node node;
		public Link link;
		public Coord coord;
		LinkNodeCoord(){}
		LinkNodeCoord(Link l, Node n, Coord c) {
			node = n;
			link = l;
			coord = c;
		}
	}

	class LineOnBoard {
		public Coord start, end;
		public Node nodeClosestToStart, nodeClosestToEnd;
		public Link linkClosestToStart, linkClosestToEnd;
		boolean active;
		LineOnBoard() {
			active = false;
			start = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
			end = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
			nodeClosestToStart = nodeClosestToEnd = null;
			linkClosestToStart = linkClosestToEnd = null;
		}
	}

	class MoveOnBoard {
		public Coord start, end;
		boolean active;
		MoveOnBoard() {
			active = false;
			start = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
			end = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
		}
	}

	class SquareOnBoard{
		public Coord start, end;
		boolean active;
		SquareOnBoard() {
			active = false;
			start = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
			end = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
		}
	}

	class pairLinkLink {
		public Link link1, link2;
		pairLinkLink() {
			link1 = link2 = null;
		}
		pairLinkLink(Link link1, Link link2) {
			this.link1 = link1;
			this.link2 = link2;
		}
	}

	class pairNodeNode {
		public Node node1, node2;
		pairNodeNode() {
			node1 = node2 = null;
		}
		pairNodeNode(Node node1, Node node2) {
			this.node1 = node1;
			this.node2 = node2;
		}
	}

}
