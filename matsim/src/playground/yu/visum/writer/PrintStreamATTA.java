package playground.yu.visum.writer;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.matsim.core.network.NetworkLayer;

/**
 * prints much imformation in a .att-file of VISUM.
 * 
 * @author ychen
 */
public abstract class PrintStreamATTA extends PrintStreamVisum9_3A {
	/*-------------------------MEMBER VARIABLE-------------------*/
	protected static final DecimalFormat DoubleDF = (DecimalFormat) NumberFormat
			.getNumberInstance(Locale.UK);

	protected final static String SPRT = ";";

	protected Map<String, List<Double>> udaws = new HashMap<String, List<Double>>();

	protected List<UserDefAtt> udas = new ArrayList<UserDefAtt>();

	protected NetworkLayer network;

	/* --------------------------CONSTUCTOR----------------------------- */
	public PrintStreamATTA(String fileName, NetworkLayer network) {
		this.network = network;
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName))));
			out.writeBytes("$VISION\n*\n*\n* Tabelle: ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* -------------------------IMPLEMENTS METHODS--------------------- */
	/**
	 * Prints a total table for object with attributs defined by VISUM-user
	 * including tablehead
	 * 
	 * @throws IOException
	 */
	public void printTable() throws IOException {
		for (UserDefAtt uda : this.udas) {
			out.writeBytes(SPRT + uda.getATTID());
		}
		out.writeBytes("\n");
		for (String objId : this.udaws.keySet())
			printRow(objId);
	}

	/**
	 * should Print the objNo. corresponding object identification und its value
	 * of the attributs defined by VISUM-user in a line of the table for
	 * attribut defined by VISUM-user without printing the tabelhead. The
	 * function does nothing here, und must be overrided by subclasses.
	 * 
	 * @param objID -
	 *            object-No., the corresponding object identification und its
	 *            value of the attributs defined by VISUM-user will be print in
	 *            a line of the table for attribut defined by VISUM-user
	 * @throws IOException
	 */
	public abstract void printRow(String objID) throws IOException;
}
