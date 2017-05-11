package playground.clruch.gheat.graphics;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.RasterFormatException;

/**
 * <p>
 * A blend composite defines the rule according to which a drawing primitive
 * (known as the source) is mixed with existing graphics (know as the
 * destination.)
 * </p>
 * <p>
 * <code>BlendComposite</code> is an implementation of the
 * {@link java.awt.Composite} interface and must therefore be set as a state on
 * a {@link java.awt.Graphics2D} surface.
 * </p>
 * <p>
 * Please refer to {@link java.awt.Graphics2D#setComposite(java.awt.Composite)}
 * for more information on how to use this class with a graphics surface.
 * </p>
 * <h2>Blending Modes</h2>
 * <p>
 * This class offers a certain number of blending modes, or compositing
 * rules. These rules are inspired from graphics editing software packages,
 * like <em>Adobe Photoshop</em> or <em>The GIMP</em>.
 * </p>
 * <p>
 * Given the wide variety of implemented blending modes and the difficulty
 * to describe them with words, please refer to those tools to visually see
 * the result of these blending modes.
 * </p>
 * <h2>Opacity</h2>
 * <p>
 * Each blending mode has an associated opacity, defined as a float value
 * between 0.0 and 1.0. Changing the opacity controls the force with which the
 * compositing operation is applied. For instance, a composite with an opacity
 * of 0.0 will not draw the source onto the destination. With an opacity of
 * 1.0, the source will be fully drawn onto the destination, according to the
 * selected blending mode rule.
 * </p>
 * <p>
 * The opacity, or alpha value, is used by the composite instance to mutiply
 * the alpha value of each pixel of the source when being composited over the
 * destination.
 * </p>
 * <h2>Creating a Blend Composite</h2>
 * <p>
 * Blend composites can be created in various manners:
 * </p>
 * <ul>
 * <li>Use one of the pre-defined instance. Example:
 * <code>BlendComposite.Average</code>.</li>
 * <li>Derive one of the pre-defined instances by calling
 * {@link #derive(float)} or {@link #derive(BlendingMode)}. Deriving allows
 * you to change either the opacity or the blending mode. Example:
 * <code>BlendComposite.Average.derive(0.5f)</code>.</li>
 * <li>Use a factory method: {@link #getInstance(BlendingMode)} or
 * {@link #getInstance(BlendingMode, float)}.</li>
 * </ul>
 * <h2>Implementation Caveat</h2>
 * <p>
 * TThe blending mode <em>SoftLight</em> has not been implemented yet.
 * </p>
 *
 * @author Romain Guy <romain.guy@mac.com>
 * @see java.awt.Graphics2D
 * @see java.awt.Composite
 * @see java.awt.AlphaComposite
 */
public final class BlendComposite implements Composite {
    /**
     * <p>
     * A blending mode defines the compositing rule of a
     * {@link BlendComposite}.
     * </p>
     *
     * @author Romain Guy <romain.guy@mac.com>
     */
    public enum BlendingMode {
        AVERAGE, MULTIPLY, SCREEN, DARKEN, LIGHTEN, OVERLAY, HARD_LIGHT, SOFT_LIGHT, DIFFERENCE, NEGATION, EXCLUSION, COLOR_DODGE, INVERSE_COLOR_DODGE, SOFT_DODGE, COLOR_BURN, INVERSE_COLOR_BURN, SOFT_BURN, REFLECT, GLOW, FREEZE, HEAT, ADD, SUBTRACT, STAMP, RED, GREEN, BLUE, HUE, SATURATION, COLOR, LUMINOSITY
    }

    public static final BlendComposite Average = new BlendComposite(BlendingMode.AVERAGE);
    public static final BlendComposite Multiply = new BlendComposite(BlendingMode.MULTIPLY);
    public static final BlendComposite Screen = new BlendComposite(BlendingMode.SCREEN);
    public static final BlendComposite Darken = new BlendComposite(BlendingMode.DARKEN);
    public static final BlendComposite Lighten = new BlendComposite(BlendingMode.LIGHTEN);
    public static final BlendComposite Overlay = new BlendComposite(BlendingMode.OVERLAY);
    public static final BlendComposite HardLight = new BlendComposite(BlendingMode.HARD_LIGHT);
    public static final BlendComposite SoftLight = new BlendComposite(BlendingMode.SOFT_LIGHT);
    public static final BlendComposite Difference = new BlendComposite(BlendingMode.DIFFERENCE);
    public static final BlendComposite Negation = new BlendComposite(BlendingMode.NEGATION);
    public static final BlendComposite Exclusion = new BlendComposite(BlendingMode.EXCLUSION);
    public static final BlendComposite ColorDodge = new BlendComposite(BlendingMode.COLOR_DODGE);
    public static final BlendComposite InverseColorDodge = new BlendComposite(BlendingMode.INVERSE_COLOR_DODGE);
    public static final BlendComposite SoftDodge = new BlendComposite(BlendingMode.SOFT_DODGE);
    public static final BlendComposite ColorBurn = new BlendComposite(BlendingMode.COLOR_BURN);
    public static final BlendComposite InverseColorBurn = new BlendComposite(BlendingMode.INVERSE_COLOR_BURN);
    public static final BlendComposite SoftBurn = new BlendComposite(BlendingMode.SOFT_BURN);
    public static final BlendComposite Reflect = new BlendComposite(BlendingMode.REFLECT);
    public static final BlendComposite Glow = new BlendComposite(BlendingMode.GLOW);
    public static final BlendComposite Freeze = new BlendComposite(BlendingMode.FREEZE);
    public static final BlendComposite Heat = new BlendComposite(BlendingMode.HEAT);
    public static final BlendComposite Add = new BlendComposite(BlendingMode.ADD);
    public static final BlendComposite Subtract = new BlendComposite(BlendingMode.SUBTRACT);
    public static final BlendComposite Stamp = new BlendComposite(BlendingMode.STAMP);
    public static final BlendComposite Red = new BlendComposite(BlendingMode.RED);
    public static final BlendComposite Green = new BlendComposite(BlendingMode.GREEN);
    public static final BlendComposite Blue = new BlendComposite(BlendingMode.BLUE);
    public static final BlendComposite Hue = new BlendComposite(BlendingMode.HUE);
    public static final BlendComposite Saturation = new BlendComposite(BlendingMode.SATURATION);
    public static final BlendComposite Color = new BlendComposite(BlendingMode.COLOR);
    public static final BlendComposite Luminosity = new BlendComposite(BlendingMode.LUMINOSITY);
    private final float alpha;
    private final BlendingMode mode;

    private BlendComposite(BlendingMode mode) {
        this(mode, 1.0f);
    }

    private BlendComposite(BlendingMode mode, float alpha) {
        this.mode = mode;
        if (alpha < 0.0f || alpha > 1.0f) {
            throw new IllegalArgumentException("alpha must be comprised between 0.0f and 1.0f");
        }
        this.alpha = alpha;
    }

    /**
     * <p>
     * Creates a new composite based on the blending mode passed
     * as a parameter. A default opacity of 1.0 is applied.
     * </p>
     *
     * @param mode
     *            the blending mode defining the compositing rule
     * @return a new <code>BlendComposite</code> based on the selected blending
     *         mode, with an opacity of 1.0
     */
    public static BlendComposite getInstance(BlendingMode mode) {
        return new BlendComposite(mode);
    }

    /**
     * <p>
     * Creates a new composite based on the blending mode and opacity passed
     * as parameters. The opacity must be a value between 0.0 and 1.0.
     * </p>
     *
     * @param mode
     *            the blending mode defining the compositing rule
     * @param alpha
     *            the constant alpha to be multiplied with the alpha of the
     *            source. <code>alpha</code> must be a floating point between 0.0 and 1.0.
     * @return a new <code>BlendComposite</code> based on the selected blending
     *         mode and opacity
     * @throws IllegalArgumentException
     *             if the opacity is less than 0.0 or
     *             greater than 1.0
     */
    public static BlendComposite getInstance(BlendingMode mode, float alpha) {
        return new BlendComposite(mode, alpha);
    }

    /**
     * <p>
     * Returns a <code>BlendComposite</code> object that uses the specified
     * blending mode and this object's alpha value. If the newly specified
     * blending mode is the same as this object's, this object is returned.
     * </p>
     *
     * @param mode
     *            the blending mode defining the compositing rule
     * @return a <code>BlendComposite</code> object derived from this object,
     *         that uses the specified blending mode
     */
    public BlendComposite derive(BlendingMode mode) {
        return this.mode == mode ? this : new BlendComposite(mode, getAlpha());
    }

    /**
     * <p>
     * Returns a <code>BlendComposite</code> object that uses the specified
     * opacity, or alpha, and this object's blending mode. If the newly specified
     * opacity is the same as this object's, this object is returned.
     * </p>
     *
     * @param alpha
     *            the constant alpha to be multiplied with the alpha of the
     *            source. <code>alpha</code> must be a floating point between 0.0 and 1.0.
     * @return a <code>BlendComposite</code> object derived from this object,
     *         that uses the specified blending mode
     * @throws IllegalArgumentException
     *             if the opacity is less than 0.0 or
     *             greater than 1.0
     */
    public BlendComposite derive(float alpha) {
        return this.alpha == alpha ? this : new BlendComposite(getMode(), alpha);
    }

    /**
     * <p>
     * Returns the opacity of this composite. If no opacity has been defined,
     * 1.0 is returned.
     * </p>
     *
     * @return the alpha value, or opacity, of this object
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * <p>
     * Returns the blending mode of this composite.
     * </p>
     *
     * @return the blending mode used by this object
     */
    public BlendingMode getMode() {
        return mode;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Float.floatToIntBits(alpha) * 31 + mode.ordinal();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlendComposite)) {
            return false;
        }
        BlendComposite bc = (BlendComposite) obj;
        return mode == bc.mode && alpha == bc.alpha;
    }

    private static boolean checkComponentsOrder(ColorModel cm) {
        if (cm instanceof DirectColorModel && cm.getTransferType() == DataBuffer.TYPE_INT) {
            DirectColorModel directCM = (DirectColorModel) cm;
            return directCM.getRedMask() == 0x00FF0000 && directCM.getGreenMask() == 0x0000FF00 && directCM.getBlueMask() == 0x000000FF
                    && (directCM.getNumComponents() != 4 || directCM.getAlphaMask() == 0xFF000000);
        }
        return false;
    }

    /** {@inheritDoc} */
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        if (!checkComponentsOrder(srcColorModel) || !checkComponentsOrder(dstColorModel)) {
            throw new RasterFormatException("Incompatible color models");
        }
        return new BlendingContext(this);
    }
}