package playground.artemc.networkTools.jcoord;

/**
 * Class to represent a reference ellipsoid. Also provides a number of
 * pre-determined reference ellipsoids as constants.
 * 
 * (c) 2006 Jonathan Stott
 * 
 * Created on 11-Feb-2006
 * 
 * @author Jonathan Stott
 * @version 1.0
 * @since 1.0
 */
public class RefEll {

  /**
   * Airy 1830 Reference Ellipsoid
   */
  public static final RefEll AIRY_1830 = new RefEll(6377563.396, 6356256.909);

  /**
   * WGS84 Reference Ellipsoid
   */
  public static final RefEll WGS84     = new RefEll(6378137.000, 6356752.3141);

  /**
   * Semi-major axis
   */
  private double             maj;

  /**
   * Semi-minor axis
   */
  private double             min;

  /**
   * Eccentricity
   */
  private double             ecc;


  /**
   * Create a new reference ellipsoid
   * 
   * @param maj
   *          semi-major axis
   * @param min
   *          semi-minor axis
   * @since 1.0
   */
  public RefEll(double maj, double min) {
    this.maj = maj;
    this.min = min;
    this.ecc = ((maj * maj) - (min * min)) / (maj * maj);
  }


  /**
   * Return the semi-major axis.
   * 
   * @return the semi-major axis
   * @since 1.0
   */
  public double getMaj() {
    return maj;
  }


  /**
   * Return the semi-minor axis
   * 
   * @return the semi-minor axis
   * @since 1.0
   */
  public double getMin() {
    return min;
  }


  /**
   * Return the eccentricity.
   * 
   * @return the eccentricity
   * @since 1.0
   */
  public double getEcc() {
    return ecc;
  }
}
