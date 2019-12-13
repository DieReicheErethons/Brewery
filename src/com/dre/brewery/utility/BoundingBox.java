package com.dre.brewery.utility;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class BoundingBox {

	private final int x1, y1, z1, x2, y2, z2;

	public BoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
		this.x1 = Math.min(x1, x2);
		this.y1 = Math.min(y1, y2);
		this.z1 = Math.min(z1, z2);
		this.x2 = Math.max(x2, x1);
		this.y2 = Math.max(y2, y1);
		this.z2 = Math.max(z2, z1);
	}

	public boolean contains(int x, int y, int z) {
		return (x >= x1 && x <= x2) && (y >= y1 && y <= y2) && (z >= z1 && z <= z2);
	}

	public boolean contains(Location loc) {
		return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public boolean contains(Block block) {
		return contains(block.getX(), block.getY(), block.getZ());
	}

	public long area() {
		return ((long) (x2 - x1 + 1)) * ((long) (y2 - y1 + 1)) * ((long) (z2 - z1 + 1));
	}

	public String serialize() {
		return x1 + "," + y1 + "," + z1 + "," + x2 + "," + y2 + "," + z2;
	}

	public static BoundingBox fromPoints(int[] locations) {
		if (locations.length % 3 != 0) throw new IllegalArgumentException("Locations has to be pairs of three");

		int length = locations.length - 2;

		int minx = Integer.MAX_VALUE,
			miny = Integer.MAX_VALUE,
			minz = Integer.MAX_VALUE,
			maxx = Integer.MIN_VALUE,
			maxy = Integer.MIN_VALUE,
			maxz = Integer.MIN_VALUE;
		for (int i = 0; i < length; i += 3) {
			minx = Math.min(locations[i], minx);
			miny = Math.min(locations[i + 1], miny);
			minz = Math.min(locations[i + 2], minz);
			maxx = Math.max(locations[i], maxx);
			maxy = Math.max(locations[i + 1], maxy);
			maxz = Math.max(locations[i + 2], maxz);
		}
		return new BoundingBox(minx, miny, minz, maxx, maxy, maxz);
	}
}
