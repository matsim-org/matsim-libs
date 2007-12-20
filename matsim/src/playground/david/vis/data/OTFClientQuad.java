package playground.david.vis.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.QuadTree;

import playground.david.vis.interfaces.OTFDataReader;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFClientQuad extends QuadTree<OTFDataReader> {
	private final double minEasting;
	private final double maxEasting;
	private final double minNorthing;
	private final double maxNorthing;

	class CreateReceiverExecutor extends Executor {
		final OTFConnectionManager connect;

		public CreateReceiverExecutor(OTFConnectionManager connect2) {
			this.connect = connect2;
		}

		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			Collection<Class> drawerClasses = connect.getEntries(reader.getClass());
			for (Class drawerClass : drawerClasses) {
				try {
					if (drawerClass != null) {
						Object drawer = drawerClass.newInstance();
						reader.connect((OTFData.Receiver) drawer);

					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}

			}
		}
	}

	class ReadDataExecutor extends Executor {
		final DataInputStream in;
		boolean readConst;

		public ReadDataExecutor(DataInputStream in, boolean readConst) {
			this.in = in;
			this.readConst = readConst;
		}

		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			try {
				if (readConst)
					reader.readConstData(in);
				else
					reader.readDynData(in);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	class InvalidateExecutor extends Executor {
		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			reader.invalidate();
		}
	}

	public OTFClientQuad(double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
		this.minEasting = minX;
		this.maxEasting = maxX;
		this.minNorthing = minY;
		this.maxNorthing = maxY;
	}

	public void createReceiver(final OTFConnectionManager connect) {

		Gbl.startMeasurement();
		int colls = this.execute(this.top.getBounds(),
				this.new CreateReceiverExecutor(connect));
		System.out.println("CLIENT DRAWER execute  == " + colls
				+ "objects time");
		Gbl.printElapsedTime();

	}

	public void getData(OTFServerRemote host, boolean readConst)
			throws RemoteException {
		Gbl.startMeasurement();
		byte[] bbyte = readConst ? host.getQuadConstStateBuffer():host.getQuadDynStateBuffer();
		System.out.println("get state time");
		Gbl.printElapsedTime();

		DataInputStream in = new DataInputStream(new ByteArrayInputStream(
				bbyte, 0, bbyte.length));
		Gbl.startMeasurement();
		int colls = this.execute(this.top.getBounds(),
				this.new ReadDataExecutor(in, readConst));
		System.out.println("execute half  == " + colls + " objects time");
		Gbl.printElapsedTime();

	}

	public void getConstData(OTFServerRemote host) throws RemoteException {
		getData(host, true);
	}

	public void getDynData(OTFServerRemote host) throws RemoteException {
		getData(host, false);
	}

	public void invalidate(Rect rect) {
		if (rect == null) {
			rect = this.top.getBounds();
		}

		Gbl.startMeasurement();
		int colls = this.execute(rect, this.new InvalidateExecutor());
		System.out.println("INVALIDATEexecute   == " + colls + " objects time");
		Gbl.printElapsedTime();

	}

	public double getMinEasting() {
		return minEasting;
	}

	public double getMaxEasting() {
		return maxEasting;
	}

	public double getMinNorthing() {
		return minNorthing;
	}

	public double getMaxNorthing() {
		return maxNorthing;
	}

}
