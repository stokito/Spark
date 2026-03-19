package org.jivesoftware.game.reversi;

import org.jivesoftware.spark.util.log.Log;

import java.awt.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

public class ReversiRes {
    private static final PropertyResourceBundle prb = (PropertyResourceBundle) ResourceBundle.getBundle("i18n/reversi_i18n");
    private static final ClassLoader cl = ReversiRes.class.getClassLoader();
    public static final ImageIcon REVERSI_ICON = getIcon("images/reversi-icon.png");
    public static final Image REVERSI_BOARD = getIcon("images/reversi-board.png").getImage();
    public static final Image REVERSI_SCORE_WHITE = getIcon("images/score-button-white.png").getImage();
    public static final Image REVERSI_SCORE_BLACK = getIcon("images/score-button-black.png").getImage();
    public static final Image REVERSI_LABEL_BLACK = getIcon("images/turn-label-black.png").getImage();
    public static final Image REVERSI_LABEL_WHITE = getIcon("images/turn-label-white.png").getImage();
    public static final Image REVERSI_RESIGN = getIcon("images/button-resign.png").getImage();
    public static final Image REVERSI_YOU = getIcon("images/you.png").getImage();
    public static final Image REVERSI_THEM = getIcon("images/them.png").getImage();

    private ReversiRes() {
    }

    public static String getString(String propertyName) {
        try {
            return prb.getString(propertyName);
        } catch (Exception e) {
            Log.warning(e.getMessage());
            return propertyName;
        }
    }
    public static String getString(String propertyName, Object... obj) {
        String str = prb.getString(propertyName);
        return MessageFormat.format(str, obj);
    }

    private static ImageIcon getIcon(String imageName) {
        try {
            final URL imageURL = cl.getResource(imageName);
            if (imageURL != null) {
                return new ImageIcon(imageURL);
            } else {
                Log.warning(imageName + " not found.");
            }
        } catch (Exception e) {
            Log.warning("Unable to load image " + imageName, e);
        }
        return null;
    }
}
