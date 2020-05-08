package com.dre.brewery.utility;

import com.dre.brewery.P;

public interface StringParser {

	public Object parse(String line);

	public static StringParser cmdParser = new StringParser() {
		@Override
		public Object parse(String line) {
			line = P.p.color(line);
			int plus = 0;
			if (line.startsWith("+++")) {
				plus = 3;
				line = line.substring(3);
			} else if (line.startsWith("++")) {
				plus = 2;
				line = line.substring(2);
			} else if (line.startsWith("+")) {
				plus = 1;
				line = line.substring(1);
			}
			if (line.startsWith("/")) {
				line = line.substring(1);
			}
			return new Tuple<Integer,String>(plus, line);
		}
	};

	public static StringParser loreParser = new StringParser() {
		@Override
		public Object parse(String line) {
			line = P.p.color(line);
			int plus = 0;
			if (line.startsWith("+++")) {
				plus = 3;
				line = line.substring(3);
			} else if (line.startsWith("++")) {
				plus = 2;
				line = line.substring(2);
			} else if (line.startsWith("+")) {
				plus = 1;
				line = line.substring(1);
			}
			if (line.startsWith(" ")) {
				line = line.substring(1);
			}
			if (!line.startsWith("§")) {
				line = "§9" + line;
			}
			return new Tuple<Integer,String>(plus, line);
		}
	};
}
