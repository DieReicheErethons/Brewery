package com.dre.brewery;

import org.bukkit.block.Block;

public class Util {

	// Check if the Chunk of a Block is loaded !without loading it in the process!
	public static boolean isChunkLoaded(Block block) {
		return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
	}

}
