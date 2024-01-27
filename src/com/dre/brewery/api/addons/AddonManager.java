package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

public class AddonManager {

	private final BreweryPlugin plugin;
	private final File addonsFolder;
	private static List<Addon> addons;

	public AddonManager(BreweryPlugin plugin) {
		this.plugin = plugin;
		this.addonsFolder = new File(plugin.getDataFolder(), "addons");
		if (!addonsFolder.exists()) {
			addonsFolder.mkdirs();
		}
	}

	public void loadAddons() {
		addons = getAllAddonClasses();
		plugin.getLogger().info("Loaded " + addons.size() + " addons");
	}

	public void unloadAddons() {
		for (Addon addon : addons) {
			addon.onAddonDisable();
		}
		addons = null;
	}

	/**
	 * Get all classes that extend Addon and instantiates them
	 * @return A list of all instantiated Addons
	 */
	public List<Addon> getAllAddonClasses() {
		File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
		if (files == null) {
			return Collections.emptyList();
		}

		List<Addon> addons = new ArrayList<>();

		for (File file : files) {
			List<CompletableFuture<Class<? extends Addon>>> addonClasses = findClassesAsync(file, Addon.class);
			for (CompletableFuture<Class<? extends Addon>> addonClass : addonClasses) {
				addonClass.thenAccept(clazz -> {
					try {
						Addon addon = clazz.getConstructor(BreweryPlugin.class, AddonLogger.class).newInstance(plugin, new AddonLogger(clazz));
						addon.onAddonEnable();
						addons.add(addon);
					} catch (Exception e) {
						plugin.getLogger().log(Level.SEVERE,"Failed to load addon class " + clazz.getSimpleName(), e);
					}
				});
			}
		}
		return addons;
	}


	public static <T> @NotNull List<CompletableFuture<Class<? extends T>>> findClassesAsync(@NotNull final File file, @NotNull final Class<T> clazz) throws CompletionException {
		if (!file.exists()) {
			return Collections.emptyList();
		}

		final List<CompletableFuture<Class<? extends T>>> futures = new ArrayList<>();

		final List<String> matches = matchingNames(file);

		for (final String match : matches) {
			futures.add(
				CompletableFuture.supplyAsync(() -> {
						try {
							final URL jar = file.toURI().toURL();
							try (final URLClassLoader loader = new URLClassLoader(new URL[]{jar}, clazz.getClassLoader())) {
								Class<? extends T> addonClass = loadClass(loader, match, clazz);
								if (addonClass != null) {
									return addonClass;
								}
							}
						} catch (final VerifyError ex) {
							//todo, this can't be here it's blocking
							return null;
						} catch (IOException | ClassNotFoundException e) {
							throw new CompletionException(e.getCause());
						}
						return null;
					}
				)
			);
		}


		return futures;
	}

	private static @NotNull List<String> matchingNames(final File file) {
		final List<String> matches = new ArrayList<>();
		try {
			final URL jar = file.toURI().toURL();
			try (final JarInputStream stream = new JarInputStream(jar.openStream())) {
				JarEntry entry;
				while ((entry = stream.getNextJarEntry()) != null) {
					final String name = entry.getName();
					if (!name.endsWith(".class")) {
						continue;
					}

					matches.add(name.substring(0, name.lastIndexOf('.')).replace('/', '.'));
				}
			}
		} catch (Exception e) {
			return Collections.emptyList();
		}
		return matches;
	}

	private static <T> @Nullable Class<? extends T> loadClass(final @NotNull URLClassLoader loader, final String match, @NotNull final Class<T> clazz) throws ClassNotFoundException {
		try {
			final Class<?> loaded = loader.loadClass(match);
			if (clazz.isAssignableFrom(loaded)) {
				return (loaded.asSubclass(clazz));
			}
		} catch (final NoClassDefFoundError ignored) {
		}
		return null;
	}

}
