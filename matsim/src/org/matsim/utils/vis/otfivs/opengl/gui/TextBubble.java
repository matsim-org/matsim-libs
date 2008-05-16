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

package org.matsim.utils.vis.otfivs.opengl.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * A simple component that renders text inside a bubble (optionally
 * translucent).  In its current form it isn't much more complex than a
 * JLabel, but it was intended to have other features that JLabel doesn't
 * have, which is why I created this separate class.
 *
 * @author Chris Campbell
 */
public class TextBubble {

    private static final int GAP = 30;
    private final String text;
    private Color fgColor, bgColor;
    private float alpha;
    
    public TextBubble(String text) {
        this.text = text;
        this.alpha = 1f;
        this.fgColor = Color.GREEN;
        this.bgColor = Color.DARK_GRAY;
    }

    public void render(Graphics2D g, int x, int y) {
        if (alpha == 0f) {
            return;
        }
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        AlphaComposite comp = AlphaComposite.SrcOver;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(bgColor);
        g2d.setFont(g2d.getFont().deriveFont(20.0f));
        TextLayout tl =
            new TextLayout(text, g2d.getFont(), g2d.getFontRenderContext());
        Rectangle2D bounds = tl.getBounds();
        int tw = (int)bounds.getWidth();
        int th = (int)bounds.getHeight();
        g2d.fillRoundRect(x, y, tw+GAP*2, th+GAP/4, 30, 30);
        g2d.setColor(fgColor);
        tl.draw(g2d, x+GAP, y+GAP/4+th-tl.getDescent());
        g2d.dispose();
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    public Color getForeground() {
        return fgColor;
    }
    
    public void setForeground(Color color) {
        this.fgColor = color;
    }
    
    public Color getBackground() {
        return bgColor;
    }
    
    public void setBackground(Color color) {
        this.bgColor = color;
    }
}
