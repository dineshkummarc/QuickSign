package me.DDoS.Quicksign.handler;

import java.util.Map.Entry;

import me.DDoS.Quicksign.session.StandardEditSession;
import me.DDoS.Quicksign.session.EditSession;
import me.DDoS.Quicksign.util.QSUtil;
import me.DDoS.Quicksign.permission.Permission;
import me.DDoS.Quicksign.QuickSign;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.Vector;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import static com.sk89q.worldguard.bukkit.BukkitUtil.toVector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;

import couk.Adamki11s.Regios.API.RegiosAPI;
import couk.Adamki11s.Regios.Regions.Region;

import com.Acrobot.ChestShop.Utils.uSign;


/**
 *
 * @author DDoS
 */
@SuppressWarnings("unchecked")
public class SelectionHandler {

    private final QuickSign plugin;
    //
    private WorldGuardPlugin wg = null;
    private RegiosAPI regiosAPI = null;
    private LWC lwc = null;
    private boolean chestShop = false;
    private boolean residence = false;

    public SelectionHandler(QuickSign instance) {

        plugin = instance;

    }

    public void setWG(WorldGuardPlugin wg) {

        this.wg = wg;

    }

    public void setRegiosAPI(RegiosAPI regiosAPI) {

        this.regiosAPI = regiosAPI;

    }

    public void setLWC(LWC lwc) {

        this.lwc = lwc;

    }

    public void setResidence(boolean residence) {

        this.residence = residence;

    }

    public void setChestShop(boolean chestShop) {
		this.chestShop = chestShop;
	}

	public void handleSignSelection(PlayerInteractEvent event, Sign sign, Player player) {

        if (!plugin.getBlackList().allows(sign, player)) {

            QSUtil.tell(player, "You cannot select this sign.");
            return;

        }

		if (chestShop) {
			if (!plugin.hasPermissions(player, Permission.CHESTSHOP_EDIT)) {
				if (uSign.isValid(sign)) {
					QSUtil.tell(player,
							"You are not allowed to edit ChestShop signs");
					return;
				}
			}
		}

        if (checkForSelectionRights(player, sign.getBlock().getLocation())) {

            if (event != null) {

                event.setCancelled(true);

            }

            Player owner = getOwner(sign);

            if (owner != null && !owner.equals(player)) {

                QSUtil.tell(player, "This sign is already selected.");
                return;

            }

            StandardEditSession session = (StandardEditSession) plugin.getSession(player);

            if (session.addSign(sign)) {

                QSUtil.tell(player, "Sign " + ChatColor.GREEN + "added " + ChatColor.GRAY + "to selection, "
                        + ChatColor.WHITE + session.getNumberOfSigns() + ChatColor.GRAY + " total.");
                return;

            } else {

                session.removeSign(sign);
                QSUtil.tell(player, "Sign " + ChatColor.RED + "removed " + ChatColor.GRAY + "from selection, "
                        + ChatColor.WHITE + session.getNumberOfSigns() + ChatColor.GRAY + " total.");
                return;

            }

        } else {

            QSUtil.tell(player, "You don't own this sign.");

        }
    }

    private boolean checkForWGMembership(Player player, Location location, World world) {

        Vector pt = toVector(location);
        LocalPlayer localPlayer = wg.wrapPlayer(player);
        RegionManager regionManager = wg.getRegionManager(world);
        ApplicableRegionSet set = regionManager.getApplicableRegions(pt);

        if (set.size() != 0) {

            return set.isMemberOfAll(localPlayer);

        }

        return false;

    }

    private boolean checkForWGOwnership(Player player, Location location, World world) {

        Vector pt = toVector(location);
        LocalPlayer localPlayer = wg.wrapPlayer(player);
        RegionManager regionManager = wg.getRegionManager(world);
        ApplicableRegionSet set = regionManager.getApplicableRegions(pt);

        if (set.size() != 0) {

            return set.isOwnerOfAll(localPlayer);

        }

        return false;

    }

    private boolean checkForWGBuildPermissions(Player player, Location location, World world) {

        Vector pt = toVector(location);
        LocalPlayer localPlayer = wg.wrapPlayer(player);
        RegionManager regionManager = wg.getRegionManager(world);
        ApplicableRegionSet set = regionManager.getApplicableRegions(pt);
        return set.canBuild(localPlayer);

    }

    private boolean checkForResidencePerms(World world, Location location, Player player, boolean forceRegion) {

        FlagPermissions.addFlag("build");
        ClaimedResidence res = Residence.getResidenceManager().getByLoc(location);

        if (res == null && !forceRegion) {

            return true;

        }

        if (res == null && forceRegion) {

            return false;

        }

        if (res != null) {

            ResidencePermissions perms = res.getPermissions();
            return perms.playerHas(player.getName(), world.getName(), "build", true);

        }

        return false;

    }

    private boolean checkForRegiosPerms(Player player, boolean forceRegion) {

        Region region = regiosAPI.getRegion(player);

        if (region == null && !forceRegion) {

            return true;

        }

        if (region == null && forceRegion) {

            return false;

        }

        if (region != null) {

            return region.canBuild(player);

        }

        return false;

    }

    private boolean checkForLWCPerms(Player player, Location loc, boolean forceProtection) {

        Protection protection = lwc.findProtection(loc.getBlock());

        if (protection == null && !forceProtection) {

            return true;

        }

        if (protection == null && forceProtection) {

            return false;

        }

        if (protection != null) {

            return lwc.canAccessProtection(player, protection);

        }

        return false;

    }

    public boolean checkForSelectionRights(Player player, Location location) {

        World world = location.getWorld();
		boolean wgperm = false, residenceperm = false, regiosperm = false, lwcperm = false;

        if (wg == null && !residence && regiosAPI == null && lwc == null) {

            return plugin.hasPermissions(player, Permission.USE);

        }

        if (plugin.hasPermissions(player, Permission.FREE_USE)) {

            return true;

        }

        if (wg != null) {

            if (plugin.hasPermissions(player, Permission.WG_MEMBER)
                    && checkForWGMembership(player, location, world)) {

                wgperm = true;

            }

            if (plugin.hasPermissions(player, Permission.WG_OWNER)
                    && checkForWGOwnership(player, location, world)) {

                wgperm = true;

            }

            if (plugin.hasPermissions(player, Permission.WG_CAN_BUILD)
                    && checkForWGBuildPermissions(player, location, world)) {

                wgperm = true;

            }
        } else {
		wgperm = true;
        }

        if (residence) {

            if (plugin.hasPermissions(player, Permission.RS_CAN_BUILD_FP)
                    && checkForResidencePerms(world, location, player, true)) {

                residenceperm = true;

            }

            if (plugin.hasPermissions(player, Permission.RS_CAN_BUILD)
                    && checkForResidencePerms(world, location, player, false)) {

                residenceperm = true;

            }
        } else {
		residenceperm = true;
        }

        if (regiosAPI != null) {

            if (plugin.hasPermissions(player, Permission.RE_CAN_BUILD_FP)
                    && checkForRegiosPerms(player, true)) {

                regiosperm = true;

            }

            if (plugin.hasPermissions(player, Permission.RE_CAN_BUILD)
                    && checkForRegiosPerms(player, false)) {

                regiosperm = true;

            }
        } else {
		regiosperm = true;
        }

        if (lwc != null) {

            if (plugin.hasPermissions(player, Permission.LWC_CAN_ACCESS_FP)
                    && checkForLWCPerms(player, location, true)) {

                lwcperm = true;

            }

            if (plugin.hasPermissions(player, Permission.LWC_CAN_ACCESS)
                    && checkForLWCPerms(player, location, false)) {

                lwcperm = true;

            }
        } else {
		lwcperm = true;
        }

        return wgperm && residenceperm && regiosperm && lwcperm;

    }

    private Player getOwner(Sign sign) {

        for (Entry<Player, EditSession> entry : plugin.getSessions()) {

            if (entry.getValue().checkIfSelected(sign)) {

                return entry.getKey();

            }
        }

        return null;

    }
}
