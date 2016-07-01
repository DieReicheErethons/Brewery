package com.dre.brewery;

import java.util.List;

public class BUtil {

	// Returns the Index of a String from the list that contains this substring
	public static int indexOfSubstring(List<String> list, String substring) {
		if (list.isEmpty()) return -1;
		for (int index = 0, size = list.size(); index < size; index++) {
			String string = list.get(index);
			if (string.contains(substring)) {
				return index;
			}
		}
		return -1;
	}

	// Returns the index of a String from the list that starts with 'lineStart', returns -1 if not found;
	public static int indexOfStart(List<String> list, String lineStart) {
		for (int i = 0, size = list.size(); i < size; i++) {
			if (list.get(i).startsWith(lineStart)) {
				return i;
			}
		}
		return -1;
	}
}
