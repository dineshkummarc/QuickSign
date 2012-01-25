package me.DDoS.Quicksign.listener;

import java.util.UUID;

import me.DDoS.Quicksign.session.QSSpoutEditSession;
import me.DDoS.Quicksign.util.QSUtil;
import me.DDoS.Quicksign.QuickSign;
import me.DDoS.Quicksign.permissions.QSPermissions;
import org.bukkit.block.Sign;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.event.screen.ScreenListener;
import org.getspout.spoutapi.gui.GenericTextField;
import org.getspout.spoutapi.gui.PopupScreen;
import org.getspout.spoutapi.player.SpoutPlayer;

/**
 *
 * @author DDoS
 */
public class QSScreenListener extends ScreenListener {

    private QuickSign plugin;

    public QSScreenListener(QuickSign plugin) {

        this.plugin = plugin;

    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {

        if (!plugin.isInUse()) {

            return;

        }

        SpoutPlayer player = event.getPlayer();

        if (!plugin.isUsing(player)) {

            return;

        }

        if (!plugin.getSession(player).isSpoutSession()) {

            return;

        }

        QSSpoutEditSession session = (QSSpoutEditSession) plugin.getSession(player);

        if (session.getPopup() == null) {

            return;

        }

        UUID[] widgets = session.getWidgets();

        if (!event.getButton().getId().equals(widgets[4])) {

            return;

        }

        PopupScreen popup = session.getPopup();

        String text0 = ((GenericTextField) popup.getWidget(widgets[0])).getText();
        String text1 = ((GenericTextField) popup.getWidget(widgets[1])).getText();
        String text2 = ((GenericTextField) popup.getWidget(widgets[2])).getText();
        String text3 = ((GenericTextField) popup.getWidget(widgets[3])).getText();

        Sign sign = session.getSign();

        if (plugin.hasPermissions(player, QSPermissions.COLOR_SPOUT.getPermissionString())) {

            text0 = text0.replaceAll("&([0-9[a-fA-F]])", "\u00A7$1");
            text1 = text1.replaceAll("&([0-9[a-fA-F]])", "\u00A7$1");
            text2 = text2.replaceAll("&([0-9[a-fA-F]])", "\u00A7$1");
            text3 = text3.replaceAll("&([0-9[a-fA-F]])", "\u00A7$1");

        } else {

            QSUtil.tell(player, "You don't have permission for colors. They will not be applied.");
            text0 = text0.replaceAll("&([0-9[a-fA-F]])", "");
            text1 = text1.replaceAll("&([0-9[a-fA-F]])", "");
            text2 = text2.replaceAll("&([0-9[a-fA-F]])", "");
            text3 = text3.replaceAll("&([0-9[a-fA-F]])", "");

        }

        if (!sign.getLine(0).equals(text0)
                || !sign.getLine(1).equals(text1)
                || !sign.getLine(2).equals(text2)
                || !sign.getLine(3).equals(text3)) {

            sign.setLine(0, text0);
            sign.setLine(1, text1);
            sign.setLine(2, text2);
            sign.setLine(3, text3);
            sign.update();

        }

        popup.close();
        session.setPopup(null);
        session.setWidgets(null);

        QSUtil.tell(player, "The sign has been edited.");

    }
}