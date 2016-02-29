/* *********************************************************************** *
 * project: org.matsim.*
 * OGLAgentPointLayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
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

package org.matsim.vis.otfvis.opengl.layer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import java.awt.*;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;


/**
 * OGLAgentPointLayer is responsible for drawing the agents/vehicles as point sprites.
 * It is a very fast way to draw massive (100ks) of agents in realtime.
 * It does not run too well on ATI cards, though.
 *
 * @author dstrippgen
 *
 */
public class OGLAgentPointLayer extends OTFGLAbstractDrawable implements SceneLayer {

	private final static int BUFFERSIZE = 10000;

	private static OTFOGLDrawer.FastColorizer redToGreenColorizer = new OTFOGLDrawer.FastColorizer(
					new double[] { 0.0, 30., 50.}, new Color[] {Color.RED, Color.YELLOW, Color.GREEN});


	private int count = 0;

	private static int alpha =200;

	private ByteBuffer colorsIN = null;

	private FloatBuffer vertexIN = null;

	private List<FloatBuffer> posBuffers = new LinkedList<FloatBuffer>();

	private List<ByteBuffer> colBuffers= new LinkedList<ByteBuffer>();

	private Map<Integer,Integer> id2coord = new HashMap<Integer,Integer>();

	private static Texture texture;

	private static final Logger log = Logger.getLogger(OGLAgentPointLayer.class);

	public OGLAgentPointLayer() {
		// Empty constructor.
	}

	@Override
	protected void onInit(GL2 gl) {
		texture = OTFOGLDrawer.createTexture(gl, MatsimResource.getAsInputStream("icon18.png"));
	}

	private static int infocnt = 0 ;

	@Override
	public void onDraw(GL2 gl) {
		gl.glEnable(GL2.GL_POINT_SPRITE);

		setAgentSize(gl);

		gl.glEnableClientState (GL2.GL_COLOR_ARRAY);
		gl.glEnableClientState (GL2.GL_VERTEX_ARRAY);

		//texture = null;
		// setting the texture to null means that agents are painted using (software-rendered?) squares.  I have made speed
		// tests and found on my computer (mac powerbook, with "slow" graphics settings) no difference at all between "null"
		// and a jpg.  But it looks weird w/o some reasonable icon.  kai, jan'11

		if (texture != null) {
			texture.enable(gl);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			gl.glTexEnvf(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL2.GL_TRUE);
			texture.bind(gl);
		}

		gl.glDepthMask(false);

		this.drawArray(gl);

		gl.glDisableClientState (GL2.GL_COLOR_ARRAY);
		gl.glDisableClientState (GL2.GL_VERTEX_ARRAY);
		if (texture != null ) {
			texture.disable(gl);
		}

		gl.glDisable(GL2.GL_POINT_SPRITE);
	}

	private static void setAgentSize(GL2 gl) {
		float agentSize = OTFClientControl.getInstance().getOTFVisConfig().getAgentSize() / 10.f;
		gl.glPointSize(agentSize);
	}

	private void drawArray(GL2 gl) {

		// testing if the point sprite is available.  Would be good to not do this in every time step ...
		// ... move to some earlier place in the calling hierarchy.  kai, feb'11
		if ( infocnt < 1 ) {
			infocnt++ ;
			String[] str = {"glDrawArrays", "glVertexPointer", "glColorPointer"} ;
			for ( int ii=0 ; ii<str.length ; ii++ ) {
				if ( gl.isFunctionAvailable(str[ii]) ) {
					log.info( str[ii] + " is available ") ;
				} else {
					log.warn( str[ii] + " is NOT available ") ;
				}
			}
		}

		ByteBuffer colors =  null;
		FloatBuffer vertex =  null;
		for(int i = 0; i < this.posBuffers.size(); i++) {
			colors = this.colBuffers.get(i);
			vertex = this.posBuffers.get(i);
			int remain = i == this.posBuffers.size()-1 ? this.count %OGLAgentPointLayer.BUFFERSIZE : OGLAgentPointLayer.BUFFERSIZE; 
			colors.position(0);
			vertex.position(0);
			gl.glColorPointer (4, GL.GL_UNSIGNED_BYTE, 0, colors);
			gl.glVertexPointer (2, GL.GL_FLOAT, 0, vertex);
			gl.glDrawArrays (GL.GL_POINTS, 0, remain);
		}
	}

	@Override
	public int getDrawOrder() {
		return 100;
	}

	public Point2D.Double getAgentCoords(char [] id) {
		int idNr = Arrays.hashCode(id);
		Integer i = this.id2coord.get(idNr);
		if (i != null) {
			FloatBuffer vertex = this.posBuffers.get(i / BUFFERSIZE);
			int innerIdx = i % BUFFERSIZE;
			float x = vertex.get(innerIdx*2);
			float y = vertex.get(innerIdx*2+1);
			return new Point2D.Double(x,y);
		}
		return null;
	}

	public void addAgent(AgentSnapshotInfo agInfo) {
		Color color;
		switch ( OTFClientControl.getInstance().getOTFVisConfig().getColoringScheme() ) {
		case bvg:
			color = bvgColoringScheme(agInfo);
			break;
		case bvg2:
			color = bvg2ColoringScheme(agInfo);
			break;
		case byId:
			color = byIdColoringScheme(agInfo);
			break;
		case gtfs:
			color = gtfsColoringScheme(agInfo);
			break;
		case standard:
			color = standardColoringScheme(agInfo);
			break;
		case taxicab:
			color = taxicabColoringScheme(agInfo) ;
			break;
		default:
			color = standardColoringScheme(agInfo);
			break;
		}

		if (this.count % OGLAgentPointLayer.BUFFERSIZE == 0) {
			this.vertexIN = Buffers.newDirectFloatBuffer(OGLAgentPointLayer.BUFFERSIZE*2);
			this.colorsIN = Buffers.newDirectByteBuffer(OGLAgentPointLayer.BUFFERSIZE*4);
			this.colBuffers.add(this.colorsIN);
			this.posBuffers.add(this.vertexIN);
		}
		this.vertexIN.put((float)agInfo.getEasting());
		this.vertexIN.put((float)agInfo.getNorthing());
		this.id2coord.put(Arrays.hashCode(agInfo.getId().toString().toCharArray()),this.count);

		this.colorsIN.put( (byte)color.getRed());
		this.colorsIN.put( (byte)color.getGreen());
		this.colorsIN.put((byte)color.getBlue());
		this.colorsIN.put( (byte)alpha);

		this.count++;
	}

	private static Color taxicabColoringScheme(AgentSnapshotInfo agInfo) {
        // ===============TAXI COLOURING===============
        if (agInfo.getId().toString().startsWith("taxi")) {
            if (agInfo.getAgentState() == AgentState.PERSON_DRIVING_CAR) {
                return Color.YELLOW ;
            }
            else {
                return Color.BLACK ;
            }
        } else {
            //===============REGULAR COLOURING===============
            return standardColoringScheme( agInfo ) ;
        }
	}

	private static Color standardColoringScheme(AgentSnapshotInfo agInfo) {
		if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
			return redToGreenColorizer.getColorZeroOne(agInfo.getColorValueBetweenZeroAndOne());
		} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
			return Color.ORANGE;
		} else if ( agInfo.getAgentState()==AgentState.PERSON_OTHER_MODE ) {
			return Color.MAGENTA;
		} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
			return Color.BLUE;
		} else {
			return Color.YELLOW;
		}
	}

	private static Color byIdColoringScheme(AgentSnapshotInfo agInfo) {
		String idstr = agInfo.getId().toString() ;
		int val = 8 ;
		if ( idstr.hashCode()%val==0 ) {
			return Color.red ;
		} else if (idstr.hashCode()%val==1 ) {
			return Color.orange ;
		} else if (idstr.hashCode()%val==2 ) {
			return Color.yellow ;
		} else if (idstr.hashCode()%val==3 ) {
			return Color.green ;
		} else if (idstr.hashCode()%val==4 ) {
			return Color.blue ;
		} else if (idstr.hashCode()%val==5 ) {
			return Color.cyan ;
		} else if (idstr.hashCode()%val==6 ) {
			return Color.magenta ;
		} else if (idstr.hashCode()%val==7 ) {
			return Color.pink ;
		} else {
			return Color.black;
		}

	}

	private static int bvg2cnt = 0 ;

	private Color bvg2ColoringScheme(AgentSnapshotInfo agInfo) {

		if ( bvg2cnt < 1 ) {
			bvg2cnt++ ;
			Logger.getLogger(this.getClass()).info( "using bvg2 coloring scheme ...") ;
		}

		if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
			return Color.DARK_GRAY;
		} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
			return Color.ORANGE;
		} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
			String idstr = agInfo.getId().toString();
			if ( idstr.contains("line_") && idstr.contains("-B-") ) {
				return Color.MAGENTA;
			} else if ( idstr.contains("line_") && idstr.contains("-T-")) {
				return Color.RED;
			} else if ( idstr.contains("line_SB")) {
				return Color.GREEN;
			} else if ( idstr.contains("line_U")) {
				return Color.BLUE;
			} else {
				return Color.ORANGE;
			}
		} else {
			return Color.YELLOW;
		}
	}

	private static Color bvgColoringScheme(AgentSnapshotInfo agInfo) {
		if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
			return Color.DARK_GRAY;
		} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
			return Color.ORANGE;
		} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
			String idstr = agInfo.getId().toString();
			if ( idstr.endsWith("_B") ) {
				return Color.MAGENTA;
			} else if ( idstr.endsWith("_T") ) {
				return Color.RED;
			} else if ( idstr.endsWith("_S")) {
				return Color.GREEN;
			} else if ( idstr.endsWith("_U")) {
				return Color.BLUE;
			} else {
				return Color.ORANGE;
			}
		} else {
			return Color.YELLOW;
		}
	}

	private static Color gtfsColoringScheme(AgentSnapshotInfo agInfo) {
		if ( agInfo.getAgentState()==AgentState.PERSON_DRIVING_CAR ) {
			return Color.DARK_GRAY;
		} else if ( agInfo.getAgentState()==AgentState.PERSON_AT_ACTIVITY ) {
			return Color.ORANGE;
		} else if ( agInfo.getAgentState()==AgentState.TRANSIT_DRIVER ) {
			String idstr = agInfo.getId().toString();
			if ( idstr.endsWith("bus") ) {
				return Color.MAGENTA;
			} else if ( idstr.endsWith("tram") ) {
				return Color.RED;
			} else if ( idstr.endsWith("rail")) {
				return Color.GREEN;
			} else if ( idstr.endsWith("subway")) {
				return Color.BLUE;
			} else {
				return Color.ORANGE;
			}
		} else {
			return Color.YELLOW;
		}
	}

}
