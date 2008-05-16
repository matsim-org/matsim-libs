/**
 * Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following 
 *     disclaimer in the documentation and/or other materials provided 
 *     with the distribution.
 *   * Neither the name of the PhotoCube project nor the names of its
 *     contributors may be used to endorse or promote products derived 
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.matsim.utils.vis.otfivs.opengl.gl;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLJPanel;
import javax.media.opengl.glu.GLU;

/**
 * A simplified version of GLJPanel that allows for easy mixing of 2D and 3D
 * elements in a Swing application.  The intention of this class is to lower
 * the barrier of entry for developers who are familiar with Java2D and Swing
 * but less familiar with OpenGL and JOGL.  This class handles much of the
 * "dirty work" so that the developer can concentrate more on adding content
 * than on (potentially confusing) OpenGL concepts.
 * <p>
 * This class has two modes:
 * <ul>
 * <li> 2D mode, which uses an orthographic projection to allow for rendering
 *      two-dimensional content via OpenGL.  This mode allows developers to
 *      use JOGL to render more complex two-dimensional elements in an
 *      existing Java2D scene.  For example, one could use OpenGL fragment
 *      shaders to achieve advanced visual effects.
 * <li> 3D mode, which uses a perspective projection to allow for rendering
 *      three-dimensional content via OpenGL.  This mode allows developers to
 *      use JOGL to render three-dimensional elements above, below, or
 *      alongside other two-dimensional elements.  For example, one could use
 *      OpenGL to render a complex 3D scene, and then use Java2D and Swing
 *      to overlay a translucent "heads-up" display over the 3D scene.
 * </ul>
 * <p>
 * In both modes, it is possible to mix Java2D, Swing, and JOGL elements.  To
 * simplify this process (and to help guide the developer), this class defines
 * four protected methods that can be overridden by the developer, which are
 * called in (approximately) the following order:
 * <ol>
 * <li>{@code render2DBackground()}
 * <li>{@code init3DResources()}
 * <li>{@code render3DScene()}
 * <li>{@code render2DForeground()}
 * </ol>
 * <p>
 * Most developers should only need to concern themselves with these four
 * methods.  Only advanced users should find it necessary to call or
 * override the other public methods in this class.
 *
 * @author Chris Campbell
 * @author Romain Guy
 */
public class CompositeGLJPanel
    extends GLJPanel
    implements GLEventListener
{
    private static final boolean DEBUG = false;
    private static final GLU glu = new GLU();
    private boolean hasDepth;
    
    /**
     * Creates a new instance of CompositeGLJPanel.
     *
     * @param isOpaque if true, the OpenGL drawable (and therefore the
     * panel) will be completely opaque; if false, the OpenGL drawable
     * will have an alpha channel and therefore will allow any components
     * behind the panel to "show through"
     * @param hasDepth if true, the OpenGL drawable will be created with
     * a depth buffer and a perspective (3D) projection; if false, the
     * OpenGL drawable will not have a depth buffer and will instead use
     * an orthographic (2D) projection with a viewport originating in
     * the upper-left corner of the window (just like Swing/Java2D)
     */
    public CompositeGLJPanel(boolean isOpaque, boolean hasDepth) {
        super(getCaps(isOpaque), null, null);
        setOpaque(isOpaque);
        this.hasDepth = hasDepth;
        addGLEventListener(this);
    }

    /**
     * This method is responsible for implementing any application-specific
     * OpenGL resources (e.g. textures, display lists).  This method is
     * guaranteed to be called once at startup, but be aware that it may be
     * called again later in the application lifecycle, such as after a
     * window resize event.
     * <p>
     * The default implementation of this method does nothing.  Developers
     * should override this method to perform one-time initialization
     * of 3D resources that are used repeatedly by {@code render3DScene()}.
     */
    protected void init3DResources(GL gl, GLU glu) {
    }

    /**
     * This method is responsible for rendering the two-dimensional
     * background content of the scene via Java2D.
     * <p>
     * The default implementation of this method does nothing.  Developers
     * should override this method to add 2D background content in their panel.
     */
    protected void render2DBackground(Graphics2D g) {
    }
    
    /**
     * This method is responsibel for rendering the three-dimensional
     * content of the scene via JOGL and OpenGL.
     * <p>
     * The default implementation of this method does nothing.  Developers
     * should override this method to add 3D content in their panel.
     */
    protected void render3DScene(GL gl, GLU glu) {
    }
    
    /**
     * This method is responsible for rendering the two-dimensional
     * background content of the scene via Java2D.
     * <p>
     * The default implementation of this method does nothing.  Developers
     * should override this method to add 2D foreground content in their panel.
     */
    protected void render2DForeground(Graphics2D g) {
    }
    
    private static GLCapabilities getCaps(boolean opaque) {
        GLCapabilities caps = new GLCapabilities();
        if (!opaque) {
            caps.setAlphaBits(8);
        }
        return caps;
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        render2DBackground(g2d);
        super.paintComponent(g2d);
        render2DForeground(g2d);
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        if (DEBUG) {
            System.err.println("INIT GL IS: " + gl.getClass().getName());
        }
        if (hasDepth) {
            gl.glEnable(GL.GL_DEPTH_TEST);
        }
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        init3DResources(gl, glu);
    }
    
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        int clearBits = 0;
        if (hasDepth) {
            clearBits |= GL.GL_DEPTH_BUFFER_BIT;
        }
        if (!shouldPreserveColorBufferIfTranslucent()) {
            clearBits |= GL.GL_COLOR_BUFFER_BIT;
        }
        if (clearBits != 0) {
            gl.glClear(clearBits);
        }
        render3DScene(gl, glu);
    }

    public void reshape(GLAutoDrawable drawable,
                        int x, int y, int width, int height)
    {
        GL gl = drawable.getGL();

        if (DEBUG) {
            System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
            System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        }
        
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        if (hasDepth) {
            double aspectRatio = (double)width / (double)height;
            glu.gluPerspective(45.0, aspectRatio, 1.0, 400.0);
        } else {
            gl.glOrtho(0.0, width, height, 0.0, -100.0, 100.0);
        }
        
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void displayChanged(GLAutoDrawable drawable,
                               boolean modeChanged, boolean deviceChanged)
    {
    }
}
