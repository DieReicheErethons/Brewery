package com.dre.brewery.recipe;

import com.dre.brewery.P;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An Item of a Recipe or as Ingredient in a Brew that corresponds to an item from another plugin.
 * <p>See /integration/item for examples on how to extend this class.
 * <p>This class stores items as name of the plugin and item id
 */
public abstract class PluginItem extends RecipeItem implements Ingredient {

	private static Map<String, Supplier<PluginItem>> constructors = new HashMap<>();

	private String plugin;
	private String itemId;

	/**
	 * New Empty PluginItem
	 */
	public PluginItem() {
	}

	/**
	 * New PluginItem with both fields already set
	 *
	 * @param plugin The name of the Plugin
	 * @param itemId The ItemID
	 */
	public PluginItem(String plugin, String itemId) {
		this.plugin = plugin;
		this.itemId = itemId;
	}


	@Override
	public boolean hasMaterials() {
		return false;
	}

	@Override
	public List<Material> getMaterials() {
		return null;
	}

	public String getPlugin() {
		return plugin;
	}

	public String getItemId() {
		return itemId;
	}

	protected void setPlugin(String plugin) {
		this.plugin = plugin;
	}

	protected void setItemId(String itemId) {
		this.itemId = itemId;
	}

	/**
	 * Called after Loading this Plugin Item from Config, or (by default) from Ingredients.
	 * <p>Allows Override to define custom actions after an Item was constructed
	 */
	protected void onConstruct() {
	}

	/**
	 * Does this PluginItem Match the other Ingredient.
	 * <p>By default it matches exactly when they are similar, i.e. also a PluginItem with same parameters
	 *
	 * @param ingredient The ingredient that needs to fulfill the requirements
	 * @return True if the ingredient matches the required info of this
	 */
	@Override
	public boolean matches(Ingredient ingredient) {
		return isSimilar(ingredient);
	}

	@NotNull
	@Override
	public Ingredient toIngredient(ItemStack forItem) {
		return ((PluginItem) getMutableCopy());
	}

	@NotNull
	@Override
	public Ingredient toIngredientGeneric() {
		return ((PluginItem) getMutableCopy());
	}

	@Override
	public boolean isSimilar(Ingredient item) {
		if (item instanceof PluginItem) {
			return Objects.equals(plugin, ((PluginItem) item).plugin) && Objects.equals(itemId, ((PluginItem) item).itemId);
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PluginItem item = (PluginItem) o;
		return Objects.equals(plugin, item.plugin) &&
			Objects.equals(itemId, item.itemId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), plugin, itemId);
	}

	@Override
	public void saveTo(DataOutputStream out) throws IOException {
		out.writeUTF("PI");
		out.writeUTF(plugin);
		out.writeUTF(itemId);
	}

	/**
	 * Called when loading this Plugin Item from Ingredients (of a Brew).
	 * <p>The default loading is the same as loading from Config
	 *
	 * @param loader The ItemLoader from which to load the data, use loader.getInputStream()
	 * @return The constructed PluginItem
	 */
	public static PluginItem loadFrom(ItemLoader loader) {
		try {
			DataInputStream in = loader.getInputStream();
			String plugin = in.readUTF();
			String itemId = in.readUTF();
			PluginItem item = fromConfig(plugin, itemId);
			if (item == null) {
				// Plugin not found when loading from Item, use a generic PluginItem that never matches other items
				item = new PluginItem(plugin, itemId) {
					@Override
					public boolean matches(ItemStack item) {
						return false;
					}
					@Override
					public String displayName() {
						return "Invalid Item";
					}
				};
			}
			return item;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Registers the chosen SaveID and the loading Method for loading from Brew or BCauldron.
	 * <p>Needs to be called at Server start.
 	 */
	public static void registerItemLoader(P p) {
		p.registerForItemLoader("PI", PluginItem::loadFrom);
	}


	/**
	 * Called when loading trying to find a config defined Plugin Item. By default also when loading from ingredients
	 * <p>Will call a registered constructor matching the given plugin identifier
	 *
	 * @param plugin The Identifier of the Plugin used in the config
	 * @param itemId The Identifier of the Item belonging to this Plugin used in the config
	 * @return The Plugin Item if found, or null if there is no plugin for the given String
	 */
	@Nullable
	public static PluginItem fromConfig(String plugin, String itemId) {
		plugin = plugin.toLowerCase();
		if (constructors.containsKey(plugin)) {
			PluginItem item = constructors.get(plugin).get();
			item.setPlugin(plugin);
			item.setItemId(itemId);
			item.onConstruct();
			return item;
		}
		return null;
	}


	/**
	 * This needs to be called at Server Start before Brewery loads its data.
	 * <p>When implementing this, put Brewery as softdepend in your plugin.yml!
	 * <p>Registers a Constructor that returns a new or cloned instance of a PluginItem
	 * <br>This Constructor will be called when loading a Plugin Item from Config or by default from ingredients
	 * <br>After the Constructor is called, the plugin and itemid will be set on the new instance
	 * <p>Finally the onConstruct is called.
	 *
	 * @param pluginId The ID to use in the config
	 * @param constructor The constructor i.e. YourPluginItem::new
	 */
	public static void registerForConfig(String pluginId, Supplier<PluginItem> constructor) {
		constructors.put(pluginId.toLowerCase(), constructor);
	}

	public static void unRegisterForConfig(String pluginId) {
		constructors.remove(pluginId.toLowerCase());
	}

}
