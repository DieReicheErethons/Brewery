package com.dre.brewery;

import com.dre.brewery.api.BreweryApi;
import com.dre.brewery.recipe.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RecipeTests {

	public static void testCauldronRecipe() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta itemMeta = item.getItemMeta();
		List<String> l = new ArrayList<>();
		l.add("Eine Tür");
		l.add("§6Besonders gut geschützt");
		itemMeta.setLore(l);
		itemMeta.setDisplayName("Mauer");
		item.setItemMeta(itemMeta);

		BRecipe recipe = BRecipe.get("Beispiel");
		int x = recipe.amountOf(item);
		int y = recipe.amountOf(new ItemStack(Material.NETHER_BRICK));


		List<Ingredient> list = new ArrayList<>();
		Ingredient ing = new SimpleItem(Material.DIAMOND_HOE);
		ing.setAmount(3);
		list.add(ing);
		ing = new SimpleItem(Material.RED_MUSHROOM);
		list.add(ing);
		for (int i = 1; i < 20; i++) {
			list.get(0).setAmount(i + 3);
			list.get(1).setAmount(i);
			BCauldronRecipe best = null;
			float bestMatch = 0;
			float match;
			for (BCauldronRecipe r : BCauldronRecipe.recipes) {
				match = r.getIngredientMatch(list);
				if (match >= 10) {
					P.p.debugLog("Found match 10 Recipe: " + r);
					return;
				}
				if (match > bestMatch) {
					best = r;
					bestMatch = match;
				}
			}
			P.p.debugLog("Found best for i:" + i + " " + best);
		}

		item = new ItemStack(Material.BARRIER);
		itemMeta = item.getItemMeta();
		l = new ArrayList<>();
		l.add("Eine Tür");
		l.add("§6Besonders gut geschützt");
		itemMeta.setLore(l);
		itemMeta.setDisplayName("Mauer");
		item.setItemMeta(itemMeta);

		RecipeItem.getMatchingRecipeItem(item, false);
	}

	public static void testCustomRecipe() {
		BreweryApi.removeRecipe("Good Build");
		BRecipe recipe = BreweryApi.recipeBuilder("Bad Build", "Good Build", "Uber Build")
			.color(PotionColor.PINK)
			.addIngredient(new ItemStack(Material.FLOWER_POT))
			.alcohol(32)
			.cook(3)
			.difficulty(4)
			.age(3, (byte) 0)
			.get();
		BreweryApi.addRecipe(recipe, false);

		P.p.log(BRecipe.getConfigRecipes().size() + "");

		BreweryApi.removeRecipe("Bier");

		P.p.log(BRecipe.getConfigRecipes().size() + "");

		BCauldronRecipe r = BreweryApi.cauldronRecipeBuilder("Cooler Trank")
			.color(PotionColor.PINK)
			.addIngredient(new SimpleItem(Material.FLOWER_POT))
			.addLore("Schmeckt nAcH TOn?!")
			.get();
		BreweryApi.addCauldronRecipe(r, false);
	}

	public static void onClick() {
		/*try {
			DataInputStream in = new DataInputStream(new Base91DecoderStream(new LoreLoadStream(potion)));

			brew.testLoad(in);

			*//*if (in.readByte() == 27 && in.skip(48) > 0) {
				in.mark(100);
				if (in.readUTF().equals("TESTHalloª∆Ω") && in.readInt() == 34834 && in.skip(4) > 0 && in.readLong() == Long.MAX_VALUE) {
					in.reset();
					if (in.readUTF().equals("TESTHalloª∆Ω")) {
						P.p.log("true");
					} else {
						P.p.log("false3");
					}
				} else {
					P.p.log("false2");
				}
			} else {
				P.p.log("false1");
			}*//*

			in.close();
		} catch (IllegalArgumentException argExc) {
			P.p.log("No Data in Lore");

			try {

				DataOutputStream out = new DataOutputStream(new Base91EncoderStream(new LoreSaveStream(potion, 2)));

				brew.testStore(out);


				*//*out.writeByte(27);
				out.writeLong(1111); //skip
				out.writeLong(1111); //skip
				out.writeLong(1111); //skip
				out.writeLong(1111); //skip
				out.writeLong(1111); //skip
				out.writeLong(1111); //skip
				out.writeUTF("TESTHalloª∆Ω");
				out.writeInt(34834);
				out.writeInt(6436); //skip
				out.writeLong(Long.MAX_VALUE);*//*

				out.close();
				*//*StringBuilder b = new StringBuilder();
				for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!$%&()*+,-./:;<=>?@[]^_`{|}~\"".toCharArray()) {
					b.append('§').append(c);
				}
				List<String> lore = potion.getLore();
				lore.add(b.toString());
				potion.setLore(lore);*//*
				item.setItemMeta(potion);

			} catch (IOException h) {
				h.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	public static void onLoad() {
		//P.p.log("§" + (use1_9 ? "a":"c") + "1.9 " + "§" + (use1_11 ? "a":"c") + "1.11 " + "§" + (use1_13 ? "a":"c") + "1.13 " + "§" + (use1_14 ? "a":"c") + "1.14");

		/*long master = new SecureRandom().nextLong();
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		XORScrambleStream scramble = new XORScrambleStream(new Base91EncoderStream(byteStream), master);
		DataOutputStream data = new DataOutputStream(scramble);
		DataInputStream dataIn = null;
		try {
			scramble.start();
			data.writeLong(12345L);
			scramble.stop();
			data.writeInt(1);
			data.writeInt(1);
			scramble.start();
			data.writeDouble(0.55555D);
			data.writeInt(234323);
			//data.writeUTF("Hallo Peter");
			data.writeLong(5419L); // Skip
			data.writeDouble(0.55555D);

			data.close();

			XORUnscrambleStream unscramble = new XORUnscrambleStream(new Base91DecoderStream(new ByteArrayInputStream(byteStream.toByteArray())), master);
			dataIn = new DataInputStream(unscramble);
			unscramble.start();
			P.p.log(dataIn.readLong() + "");
			unscramble.stop();
			P.p.log(dataIn.readInt() + "");
			P.p.log(dataIn.readInt() + "");
			unscramble.start();
			P.p.log(dataIn.readDouble() + "");
			dataIn.mark(1000);
			P.p.log(dataIn.readInt() + "");
			//P.p.log(dataIn.readUTF());
			dataIn.skip(8);
			P.p.log(dataIn.readDouble() + "");
			P.p.log("reset");
			dataIn.reset();
			P.p.log(dataIn.readInt() + "");
			//P.p.log(dataIn.readUTF());
			dataIn.skip(8);
			P.p.log(dataIn.readDouble() + "");

			dataIn.close();

			*//*for (int i = 0; i < 10; i++) {
				byteStream = new ByteArrayOutputStream();
				scramble = new XORScrambleStream(new Base91EncoderStream(byteStream));
				data = new DataOutputStream(scramble);
				data.writeInt(i);
				scramble.start();
				data.writeLong(12345L);
				data.writeLong(12345L);
				scramble.stop();
				data.writeInt(1);
				data.writeInt(1);
				scramble.start();
				data.writeInt(234323);
				data.writeDouble(0.55555D);

				P.p.log(byteStream.toString());
				data.close();
			}*//*


			long time = System.currentTimeMillis();
			for (int i = 0; i < 100000; i++) {
				unscramble = new XORUnscrambleStream(new Base91DecoderStream(new ByteArrayInputStream(byteStream.toByteArray())), master);
				dataIn = new DataInputStream(unscramble);
				unscramble.start();
				dataIn.readLong();
				unscramble.stop();
				dataIn.readInt();
				dataIn.readInt();
				unscramble.start();
				dataIn.readDouble();
				dataIn.mark(1000);
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();
				dataIn.reset();
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();

				dataIn.close();
			}
			long time2 = System.currentTimeMillis();

			for (int i = 0; i < 100000; i++) {
				unscramble = new XORUnscrambleStream(new ByteArrayInputStream(byteStream.toByteArray()), master);
				dataIn = new DataInputStream(unscramble);
				unscramble.start();
				dataIn.skip(2);
				dataIn.readLong();
				unscramble.stop();
				dataIn.readInt();
				dataIn.readInt();
				unscramble.start();
				dataIn.readDouble();
				dataIn.mark(1000);
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();
				dataIn.reset();
				dataIn.readInt();
				//dataIn.readUTF();
				dataIn.skip(8);
				dataIn.readDouble();

				dataIn.close();
			}
			long time3 = System.currentTimeMillis();

			P.p.log("Time with base91: " + (time2 - time));
			P.p.log("Time without base91: " + (time3 - time2));

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} finally {
			try {
				data.close();
				if (dataIn != null) {
					dataIn.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/

		/*try {
			ItemMeta meta = new ItemStack(Material.POTION).getItemMeta();
			DataOutputStream data = new DataOutputStream(new Base91EncoderStream(new LoreSaveStream(meta, 3)));

			data.writeInt(2);
			data.writeLong(5);

			byte[] test = new byte[128];
			test[1] = 6;
			test[2] = 12;
			test[3] = 21;
			test[127] = 99;
			data.write(test);

			data.writeInt(123324);
			data.writeLong(12343843);

			data.close();
			meta.getLore();

			DataInputStream dataIn = new DataInputStream(new Base91DecoderStream(new LoreLoadStream(meta)));

			P.p.log(dataIn.readInt() + ", " + dataIn.readLong() + ", ");

			byte[] testIn = new byte[128];
			dataIn.read(testIn);
			P.p.log(testIn[1] + ", " + testIn[2] + ", " + testIn[3] + ", " + testIn[127]);

			P.p.log(dataIn.readInt() + ", " + dataIn.readLong() + ", ");

			dataIn.close();



			basE91 basE91 = new basE91();
			int[] input = new int[] {12, 65, 324, 5, 12, 129459, 1234567, Integer.MIN_VALUE, Integer.MAX_VALUE};
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream data = new DataOutputStream(stream);
			for (int i = 0; i < input.length; i++) {
				data.writeInt(input[i]);
			}
			data.flush();
			data.close();
			byte[] in = stream.toByteArray();
			byte[] out = new byte[4096];
			int lenght = basE91.encode(in, in.length, out);
			basE91.encEnd(out);
			String done = new String(out, 0, lenght);

			byte[] tin = done.getBytes();

			byte[] tout = new byte[4096];
			lenght = basE91.decode(tin, tin.length, tout);
			basE91.decEnd(tout);


			ByteArrayInputStream tstream = new ByteArrayInputStream(tout, 0, lenght);
			DataInputStream tdata = new DataInputStream(tstream);
			int[] test = new int[4096];
			for (int j = 0; j < 6; j++) {
				if (tstream.available() <= 0) break;
				test[j] = tdata.readInt();

			}
			tdata.close();
			test = test;*/



			/*basE91 basE91 = new basE91();
			int[] input = new int[] {12, 65, 324, 5, 12, 129459, 1234567, Integer.MIN_VALUE, Integer.MAX_VALUE};
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			DataOutputStream data = new DataOutputStream(stream);
			for (int i = 0; i < input.length; i++) {
				data.writeInt(input[i]);
			}
			data.flush();
			data.close();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayInputStream in = new ByteArrayInputStream(stream.toByteArray());

			encode(in, out, in.available());

			in.close();
			out.flush();
			out.close();

			String done = new String(out.toByteArray());

			ByteArrayInputStream tin = new ByteArrayInputStream(done.getBytes());
			ByteArrayOutputStream tout = new ByteArrayOutputStream();

			decode(tin, tout, tin.available());

			tin.close();
			tout.flush();
			tout.close();

			ByteArrayInputStream tstream = new ByteArrayInputStream(tout.toByteArray());
			DataInputStream tdata = new DataInputStream(tstream);
			int[] test = new int[4096];
			for (int j = 0; j < 9; j++) {
				if (tstream.available() <= 0) break;
				test[j] = tdata.readInt();

			}
			tdata.close();
			test = test;

		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
}
