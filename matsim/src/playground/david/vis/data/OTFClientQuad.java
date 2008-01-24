package playground.david.vis.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Collection;

import org.matsim.gbl.Gbl;
import org.matsim.utils.collections.QuadTree;

import playground.david.vis.gui.PoolFactory;
import playground.david.vis.interfaces.OTFDataReader;
import playground.david.vis.interfaces.OTFServerRemote;

public class OTFClientQuad extends QuadTree<OTFDataReader> {
	private final double minEasting;
	private final double maxEasting;
	private final double minNorthing;
	private final double maxNorthing;
	private final String id;

	static class CreateReceiverExecutor extends Executor<OTFDataReader> {
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

	class ReadDataExecutor extends Executor<OTFDataReader> {
		final ByteBuffer in;
		boolean readConst;

		public ReadDataExecutor(ByteBuffer in, boolean readConst) {
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

	class InvalidateExecutor extends Executor<OTFDataReader> {
		@Override
		public void execute(double x, double y, OTFDataReader reader) {
			reader.invalidate();
		}
	}

	public OTFClientQuad(String id, double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
		this.minEasting = minX;
		this.maxEasting = maxX;
		this.minNorthing = minY;
		this.maxNorthing = maxY;
		this.id = id;
	}

	public void createReceiver(final OTFConnectionManager connect) {

		int colls = this.execute(this.top.getBounds(),
				new CreateReceiverExecutor(connect));
	}

	private void getData(OTFServerRemote host, QuadTree.Rect bound, boolean readConst)
			throws RemoteException {
		bound = host.isLive() ? bound : this.top.getBounds();
		byte[] bbyte = readConst ? host.getQuadConstStateBuffer(id):host.getQuadDynStateBuffer(id, bound);
		ByteBuffer in = ByteBuffer.wrap(bbyte);
		Gbl.startMeasurement();
		int colls = this.execute(bound, this.new ReadDataExecutor(in, readConst));
		System.out.print("readData: "); Gbl.printElapsedTime();

		PoolFactory.resetAll();
		
	}

	synchronized public void getConstData(OTFServerRemote host) throws RemoteException {
		getData(host, null, true);
	}

	synchronized public void getDynData(OTFServerRemote host, QuadTree.Rect bound) throws RemoteException {
		getData(host, bound, false);
	}

	synchronized public void invalidate(Rect rect) {
		if (rect == null) {
			rect = this.top.getBounds();
		}

		int colls = this.execute(rect, this.new InvalidateExecutor());
	}

	@Override
	public double getMinEasting() {
		return minEasting;
	}

	@Override
	public double getMaxEasting() {
		return maxEasting;
	}

	@Override
	public double getMinNorthing() {
		return minNorthing;
	}

	@Override
	public double getMaxNorthing() {
		return maxNorthing;
	}

}
