package playground.gregor.otf.drawer;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_PROJECTION_MATRIX;
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;


import playground.gregor.otf.readerwriter.InundationData;
import playground.gregor.otf.readerwriter.InundationData.InundationGeometry;

public class OTFInundationDrawer extends OTFTimeDependentDrawer {

	
	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];
	

	

	


	private double zoom;
	
	private  short timeSlotIdx = -1;
	private InundationData data;


	

	@Override
	public void onDraw(GL gl, int time) {


		
		this.timeSlotIdx = (((short) ((time - 3*3600)/60)));
		
		if (this.timeSlotIdx < 0) {
			return;
		}
		
		updateMatrices(gl);
		calcCurrentZoom();


		float [] top = getOGLPos(-50,-50);
		float [] bot = getOGLPos(this.viewport[2]+50, this.viewport[3]+50);
		Collection<InundationGeometry> fic = new ArrayList<InundationGeometry>(); 
		double mxX = top[0] > bot[0] ? top[0] : bot[0];
		double miX = top[0] < bot[0] ? top[0] : bot[0];
		double mxY = top[1] > bot[1] ? top[1] : bot[1];
		double miY = top[1] < bot[1] ? top[1] : bot[1];


		//		System.out.println("floodingzoom:" + this.zoom);
		//		this.triangleTree.get(miX,miY,mxX,mxY,fic);
		double zoom = this.zoom;
//		if (zoom > 1) {
//			zoom /= 2;
//		}
		this.data.floodingData.get(zoom).get(miX,miY,mxX,mxY,fic);
		while (fic.size() > 80000) {
			fic.clear();
			zoom *= 4;
			this.data.floodingData.get(zoom).get(miX,miY,mxX,mxY,fic);	
		}
		
//		System.out.println("Num Geos:" + fic.size());
		int empty = 0;
		for (InundationGeometry t : fic) {
				
			t.draw(gl, this.timeSlotIdx);
			
			
			
			
//			byte aidx = this.data.tableMapping.get(t.acol)[this.timeSlotIdx];
//			byte bidx = this.data.tableMapping.get(t.bcol)[this.timeSlotIdx];
//			byte cidx = this.data.tableMapping.get(t.ccol)[this.timeSlotIdx];
//			byte didx = this.data.tableMapping.get(t.dcol)[this.timeSlotIdx];

//			float [] dcol = null;
//			if (this.timeSlotIdx < t.dwalsh[InundationData.RES]) {
//				dcol = InundationData.empty;
//			} else {
//				dcol = this.data.getColor(this.timeSlotIdx,t.dwalsh);
//			}			
			
//			float [] col = ;
//			if (col == null) {
//				continue;
//			}

			//			float [] acol = t.acol.get(time);
			//			if (acol == null) {
			//				acol = color;
			//			}
			//			float [] bcol = t.bcol.get(time);
			//			if (bcol == null) {
			//				bcol = color;
			//			}
			//			float [] ccol = t.ccol.get(time);
			//			if (ccol == null) {
			//				ccol = color;
			//			}
			//			float [] dcol = t.dcol.get(time);
			//			if (dcol == null) {
			//				dcol = color;
			//			}

			
			//			if(this.tx != null) co =  this.tx.getImageTexCoords();

			//			this.tx.enable();
			//			this.tx.bind();
			//			gl.glDisable(GL.GL_BLEND);
			//			gl.glEnable(GL.GL_BLEND);
			//			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//			gl.glBegin(GL.GL_QUADS);
//
//			//			System.out.println("quad:" + t.a.x + "  " + t.a.y + "---"+ t.b.x + "  " + t.b.y + "---"+ t.c.x + "  " + t.c.y + "---"+ t.d.x + "  " + t.d.y + "---");
//			gl.glColor4f(this.data.colorTable[0][aidx],this.data.colorTable[1][aidx],this.data.colorTable[2][aidx],this.data.colorTable[3][aidx]);
//			gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(t.x + t.diff,t.y + t.diff,1.f);
////			gl.glColor4f(col[0],col[1],col[2],1);
//			gl.glColor4f(this.data.colorTable[0][bidx],this.data.colorTable[1][bidx],this.data.colorTable[2][bidx],this.data.colorTable[3][bidx]);
//			gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(t.x + t.diff,t.y - t.diff,1.f);
////			gl.glColor4f(col[0],col[1],col[2],1);
//			gl.glColor4f(this.data.colorTable[0][cidx],this.data.colorTable[1][cidx],this.data.colorTable[2][cidx],this.data.colorTable[3][cidx]);
//			gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(t.x - t.diff,t.y - t.diff,1.f);
////			gl.glColor4f(col[0],col[1],col[2],1);
//			gl.glColor4f(this.data.colorTable[0][didx],this.data.colorTable[1][didx],this.data.colorTable[2][didx],this.data.colorTable[3][didx]);
//			gl.glTexCoord2f(co.left(),co.bottom()); gl.glVertex3f(t.x - t.diff,t.y + t.diff,1.f);
//			gl.glEnd();		

//			gl.glBegin(GL.GL_QUADS);
//
//			//			System.out.println("quad:" + t.a.x + "  " + t.a.y + "---"+ t.b.x + "  " + t.b.y + "---"+ t.c.x + "  " + t.c.y + "---"+ t.d.x + "  " + t.d.y + "---");
//			gl.glColor4f(acol[0],acol[1],acol[2],acol[3]);
//			gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(t.x + t.diff,t.y + t.diff,1.f);
////			gl.glColor4f(col[0],col[1],col[2],1);
//			gl.glColor4f(bcol[0],bcol[1],bcol[2],bcol[3]);
//			gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(t.x + t.diff,t.y - t.diff,1.f);
////			gl.glColor4f(col[0],col[1],col[2],1);
//			gl.glColor4f(ccol[0],ccol[1],ccol[2],ccol[3]);
//			gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(t.x - t.diff,t.y - t.diff,1.f);
////			gl.glColor4f(col[0],col[1],col[2],1);
//			gl.glColor4f(dcol[0],dcol[1],dcol[2],dcol[3]);
//			gl.glTexCoord2f(co.left(),co.bottom()); gl.glVertex3f(t.x - t.diff,t.y + t.diff,1.f);
//			gl.glEnd();		
			
			
		
			

			//			this.tx.disable();
		}
		//		gl.glDisable(GL.GL_BLEND);
		// Drawing Using Triangles
		// Top
		// Bottom Left
		// Bottom Right
		// Finished Drawing The Triangle
//		System.out.println("Empty:" + empty);

	}

//	@Override
//	public void invalidate(SceneGraph graph) {
//		OTFOGLDrawer d = (OTFOGLDrawer) graph.getDrawer();
//		double time = graph.getTime();
//		System.out.println("time:" + time);
//		if (this.timeSlotIdx == -1) {
//			this.timeOffset = time;
//		}
//		this.timeSlotIdx = (short) ((time-this.timeOffset)/60);
//		super.invalidate(graph);
//	}


	private void calcCurrentZoom() {
		float scrWidth = this.viewport[2] - this.viewport[0];
		float [] top = getOGLPos(this.viewport[0], this.viewport[1]);
		float [] bottom = getOGLPos(this.viewport[2], this.viewport[3]);
		float glWidth = Math.abs(top[0]-bottom[0]);
		double ratio = glWidth/scrWidth;

		int idx = -Arrays.binarySearch(this.data.powerLookUp, ratio);
		if (idx > this.data.powerLookUp.length-1) {
			this.zoom = this.data.powerLookUp[this.data.powerLookUp.length-1];
		} else {
			this.zoom = this.data.powerLookUp[idx-1];
		}
		//		System.out.println("zoom:" + this.zoom);

	}


	


	public void updateMatrices(GL gl) { //TODO give me the drawer!!
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL_MODELVIEW_MATRIX, this.modelview,0);
		gl.glGetDoublev( GL_PROJECTION_MATRIX, this.projection,0);
		gl.glGetIntegerv( GL_VIEWPORT, this.viewport,0 );

	}

	private float [] getOGLPos(int x, int y)
	{


		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = this.viewport[3] - y;

		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-koord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], this.modelview,0, this.projection,0, this.viewport,0, w_pos,0);

		glu.gluUnProject( winX, winY, w_pos[2], this.modelview,0, this.projection,0, this.viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];

		return new float []{posX, posY};
	}


	public void setData(InundationData data) {
		this.data = data;
		
	}




	//	private static class Vertex implements Comparable<Vertex>{
	//		Coordinate c;
	//		FloodingInfo f;
	//		double dist;
	//		public int compareTo(Vertex o) {
	//			if (this.dist < o.dist) {
	//				return -1;
	//			} else if (this.dist > o.dist) {
	//				return 1;
	//			}
	//			return 0;
	//		}
	//	}
}
