/*
 * Copyright 2008-2009, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.model;

import hu.openig.core.Location;
import hu.openig.core.PlanetInfo;
import hu.openig.core.Tile;
import hu.openig.core.TileProvider;
import hu.openig.core.TileStatus;

import java.awt.Rectangle;

/**
 * Actual building instance.
 * @author karnokd
 */
public class GameBuilding implements TileProvider {
	/** The prototype building. */
	public GameBuildingPrototype prototype;
	/** Shortcut for building images for the actual tech id. */
	public GameBuildingPrototype.BuildingImages images;
	/** The planetary information provider. */
	public PlanetInfo planetInfo;
	/** The tile X coordinate of the left edge. */
	public int x;
	/** The tile Y coordinate of the top edge. */
	public int y;
	/** The current damage level. */
	public int health; // FIXME damage percent or hitpoints?
	/** The current build progress percent: 0 to 100. */
	public int progress;
	/** The lazily initialized rectangle. */
	private Rectangle rect;
	/** Is this building enabled? Disabled buildings don't consume/produce energy or workers. */
	public boolean enabled = true;
	/** The current energy received. */
	public int energy;
	/** The current worker amount. */
	public int workers;
	/** Indicator that this building is being repaired. */
	public boolean repairing;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tile getTile(Location location) {
		int dx = location.x - x;
		int dy = y - location.y;
		if (progress == 100) {
			if (dx == 0 || dy == images.regularTile.width - 1) {
				if (health < 50) { // FIXME health level to switch to damaged tile
					return images.damagedTile;
				}
				return images.regularTile;
			}
			return null; // no tile to draw
		}
		// FIXME: maybe the returned tile should depend on the internal location, to have non-uniformly built structure visual effect
		return images.buildPhases.get(images.buildPhases.size() * progress / 100); 
	}
	/**
	 * Returns the rectangle containint this building inclusive the roads around.
	 * Note, that height in this case points to +y whereas rendering is done into the -y direction.
	 * @return the rectangle
	 */
	public Rectangle getRectWithRoad() {
		if (rect == null) {
			rect = new Rectangle(x - 1, y + 1, images.regularTile.height + 2, images.regularTile.width + 2);
		}
		return rect;
	}
	/**
	 * Returns the current energy production/consumption.
	 * This value depend on the current health and progress as well
	 * as the enabled state
	 * @return the energy value
	 */
	public int getEnergy() {
		if (enabled && progress == 100) {
			float w = getWorkerPercent();
			if (w >= 0.5 && health >= 50) {
				return (int)(prototype.energy * (health * w / 100));
			} else
			if (w < 0) {
				return prototype.energy * health / 100;
			}
		}
		return 0;
	}
	/**
	 * Returns the worker demand which depends on the building status.
	 * @return the worker demand
	 */
	public int getWorkerDemand() {
		return enabled && progress == 100 ? prototype.workers : 0;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public TileStatus getStatus() {
		if (health == 0) {
			return TileStatus.DESTROYED;
		} else
		if (health < 100) {
			return TileStatus.DAMAGED;
		} else
		if (!isOperational()) {
			return TileStatus.NO_ENERGY;
		}
		return TileStatus.NORMAL;
	}
	/**
	 * Returns the ratio of assigned/required energy for
	 * energy consuming buildings.
	 * @return the ratio [0..1], -1 signals no energy required
	 */
	public float getEnergyPercent() {
		int e = getEnergy();
		if (e < 0) {
			return -energy * 1.0f / getEnergy();
		}
		return -1;
	}
	/**
	 * Returns the ratio of assigned/required workers
	 * for the buildings.
	 * @return the ratio [0..1]
	 */
	public float getWorkerPercent() {
		int w = getWorkerDemand();
		if (w > 0) {
			return workers * 1.0f / w;
		}
		return -1;
	}
	/**
	 * @return true if the building is enabled, 100% completed and at least on 50% health
	 */
	public boolean requiresEnergy() {
		return getEnergy() < 0;
	}
	/**
	 * 
	 * @return true if the building is enabled, 100% completed
	 */
	public boolean requiresWorkers() {
		return getWorkerDemand() > 0;
	}
	/**
	 * Returns true if this building is operational, e.g the energy and woker levels are at least 50%.
	 * @return true if this building is operational
	 */
	public boolean isOperational() {
		float e = getEnergyPercent();
		float w = getWorkerPercent();
		return (e < 0 || e >= 0.5) && (w < 0 || w >= 0.5);
	}
}
