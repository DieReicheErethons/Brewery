package com.dre.brewery.recipe;

import java.io.DataInputStream;

public class ItemLoader {

	private final int version;
	private final DataInputStream in;
	private final String saveID;

	public ItemLoader(int version, DataInputStream in, String saveID) {
		this.version = version;
		this.in = in;
		this.saveID = saveID;
	}

	public int getVersion() {
		return version;
	}

	public DataInputStream getInputStream() {
		return in;
	}

	public String getSaveID() {
		return saveID;
	}
}
