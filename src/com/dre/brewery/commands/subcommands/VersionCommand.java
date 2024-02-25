package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.api.addons.BreweryAddon;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.filedata.UpdateChecker;
import org.bukkit.command.CommandSender;

import java.util.List;

public class VersionCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        List<BreweryAddon> addons = new AddonManager(breweryPlugin).getAddons();
        StringBuilder addonString = new StringBuilder();

        for (BreweryAddon addon : addons) {
            addonString.append(addon.getClass().getSimpleName());
            if (addons.indexOf(addon) < addons.size() - 1) {
                addonString.append("&f, &a");
            }
        }

        breweryPlugin.msg(sender, "&2BreweryX version&7: &av" + breweryPlugin.getDescription().getVersion() + " &7(Latest: v" + UpdateChecker.getLatestVersion() + ")");
        breweryPlugin.msg(sender, "&2Original Authors&7: &aGrafe&f, &aTTTheKing&f, &aSn0wStorm");
        breweryPlugin.msg(sender, "&2BreweryX Authors/Maintainers&7: &aJsinco");
        breweryPlugin.msg(sender, "&2Loaded addons&7: &a" + addonString);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.version";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
