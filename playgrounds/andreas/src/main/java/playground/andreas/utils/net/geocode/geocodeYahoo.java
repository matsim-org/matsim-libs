package playground.andreas.utils.net.geocode;
//package playground.andreas.bln;
//
//import java.io.IOException;
//
//import java.io.InputStream;
//
//import java.io.UnsupportedEncodingException;
//
//import java.net.URL;
//
//import java.net.URLConnection;
//
//import java.net.URLEncoder;
//
//import org.apache.commons.digester.Digester;
//
//import org.xml.sax.SAXException;
//
//class GeoPointUtil {
//
//	public static final String YAHOO_GEOCODER = "http://api.local.yahoo.com/MapsService/V1/geocode?appid=YahooDemo&";
//
//	public static GeoPoint createGeoPoint(String lat, String lng) {
//
//		return new GeoPoint(lat, lng);
//
//	}
//
//	public static GeoPoint createGeoPoint(String location) throws UnsupportedEncodingException,
//
//	IOException, SAXException {
//
//		String query = YAHOO_GEOCODER + "location=" + URLEncoder.encode(location, "UTF-8");
//		;
//
//		URL url = new URL(query);
//
//		URLConnection conn = url.openConnection();
//
//		Digester digester = new Digester();
//
//		digester.setValidating(false);
//
//		digester.addObjectCreate("ResultSet/Result", GeoPoint.class);
//
//		digester.addBeanPropertySetter("ResultSet/Result/Latitude", "lat");
//
//		digester.addBeanPropertySetter("ResultSet/Result/Longitude", "lng");
//
//		InputStream stream = conn.getInputStream();
//
//		GeoPoint point = (GeoPoint) digester.parse(stream);
//
//		stream.close();
//
//		return point;
//
//	}
//
//	public static void main(String[] args) {
//
//		try {
//
//			GeoPoint pt = GeoPointUtil.createGeoPoint("200 Oracle Pkwy Redwood Shores CA 94065");
//
//			System.out.println(pt);
//
//		} catch (Exception e) {
//
//			e.printStackTrace();
//
//		}
//
//	}
//
//}
//
//class GeoPoint {
//
//	private String lat;
//
//	private String lng;
//
//	public GeoPoint() {
//	}
//
//	public GeoPoint(String lat, String lng) {
//
//		this.lat = lat;
//
//		this.lng = lng;
//
//	}
//
//	public void setLat(String lat) {
//
//		this.lat = lat;
//
//	}
//
//	public String getLat() {
//
//		return lat;
//
//	}
//
//	public void setLng(String lng) {
//
//		this.lng = lng;
//
//	}
//
//	public String getLng() {
//
//		return lng;
//
//	}
//
//	public String toString() {
//
//		return lat + "," + lng;
//
//	}
//
//}
