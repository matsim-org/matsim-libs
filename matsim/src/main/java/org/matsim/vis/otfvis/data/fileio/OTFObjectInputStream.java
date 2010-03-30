/**
 *
 */
package org.matsim.vis.otfvis.data.fileio;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.data.OTFServerQuad2;

public class OTFObjectInputStream extends ObjectInputStream {

	private final static Logger log = Logger.getLogger(OTFObjectInputStream.class);

	public OTFObjectInputStream(final InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected Class<?> resolveClass(final ObjectStreamClass desc)
			throws IOException, ClassNotFoundException {
		String name = desc.getName();
		log.info("try to resolve " + name);
		// these remappings only happen with older file versions
		if (name.equals("playground.david.vis.data.OTFServerQuad")) {
			return OTFServerQuad2.class;
		} else if (name.startsWith("org.matsim.utils.vis.otfvis")) {
			name = name.replaceFirst("org.matsim.utils.vis.otfvis",
					"org.matsim.vis.otfvis");
			return Class.forName(name);
		} else if (name.startsWith("playground.david.vis")) {
			name = name.replaceFirst("playgrounidd.david.vis",
					"org.matsim.utils.vis.otfvis");
			return Class.forName(name);
		} else if (name.startsWith("org.matsim.utils.vis.otfivs")) {
			name = name.replaceFirst("org.matsim.utils.vis.otfivs",
					"org.matsim.vis.otfvis");
			return Class.forName(name);
		} else if (name.startsWith("org.matsim.mobsim")) {
			name = name.replaceFirst("org.matsim.mobsim",
					"org.matsim.core.mobsim");
			return Class.forName(name);
		} else if (name.startsWith("org.matsim.utils.collections")) {
			name = name.replaceFirst("org.matsim.utils.collections",
					"org.matsim.core.utils.collections");
			return Class.forName(name);
		} else if (name
				.startsWith("playground.gregor.otf.readerwriter.InundationData")) {
			name = name.replaceFirst("playground.gregor.otf.readerwriter",
					"org.matsim.evacuation.otfvis.legacy.readerwriter");
			return Class.forName(name);
		} else if (name.startsWith("playground.gregor.otf.readerwriter")) {
			name = name.replaceFirst("playground.gregor.otf.readerwriter",
					"org.matsim.evacuation.otfvis.readerwriter");
			return Class.forName(name);
		} else if (name.startsWith("playground.gregor.otf.drawer")) {
			name = name.replaceFirst("playground.gregor.otf.drawer",
					"org.matsim.evacuation.otfvis.drawer");
			return Class.forName(name);
		} else if (name.startsWith("playground.gregor.collections")) {
			name = name.replaceFirst("playground.gregor.collections",
					"org.matsim.evacuation.otfvis.legacy.collections");
			return Class.forName(name);
		} else if (name
				.startsWith("org.matsim.evacuation.otfvis.readerwriter.InundationData")) {
			name = name
					.replaceFirst(
							"org.matsim.evacuation.otfvis.readerwriter.InundationData",
							"org.matsim.evacuation.otfvis.legacy.readerwriter.InundationData");
			return Class.forName(name);
		}
		return super.resolveClass(desc);
	}

}