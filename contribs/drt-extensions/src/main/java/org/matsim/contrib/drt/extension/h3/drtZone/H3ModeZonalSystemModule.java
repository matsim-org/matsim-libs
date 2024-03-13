package org.matsim.contrib.drt.extension.h3.drtZone;

import com.google.common.base.Preconditions;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigGroup;

import java.util.Map;

import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtils.filterGridWithinServiceArea;
import static org.matsim.utils.gis.shp2matsim.ShpGeometryUtils.loadPreparedGeometries;

/**
 * @author nkuehnel / MOIA
 */
public class H3ModeZonalSystemModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtCfg;
	private final String crs;
	private final int resolution;

	public H3ModeZonalSystemModule(DrtConfigGroup drtCfg, String crs, int resolution) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.crs = crs;
		this.resolution = resolution;
	}
	@Override
	public void install() {

		DrtZonalSystemParams params = drtCfg.getZonalSystemParams().orElseThrow();

		bindModal(DrtZonalSystem.class).toProvider(modalProvider(getter -> {
			Network network = getter.getModal(Network.class);
			switch (params.zonesGeneration) {
				case ShapeFile:
					throw new IllegalArgumentException("Cannot use H3 system with self-provided shapefile");
				case GridFromNetwork:
					Preconditions.checkNotNull(params.cellSize);
					Map<String, PreparedGeometry> gridFromNetwork = H3GridUtils.createH3GridFromNetwork(network, resolution, crs);
					var gridZones =
						switch (drtCfg.operationalScheme) {
							case stopbased, door2door -> gridFromNetwork;
							case serviceAreaBased -> filterGridWithinServiceArea(gridFromNetwork,
								loadPreparedGeometries(ConfigGroup.getInputFileURL(getConfig().getContext(),
									drtCfg.drtServiceAreaShapeFile)));
						};
					return H3ZonalSystem.createFromPreparedGeometries(network, gridZones, crs, resolution);

				default:
					throw new RuntimeException("Unsupported zone generation");
			}
		})).asEagerSingleton();
	}
}
