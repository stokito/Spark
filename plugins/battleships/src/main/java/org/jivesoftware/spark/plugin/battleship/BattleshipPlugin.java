package org.jivesoftware.spark.plugin.battleship;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jivesoftware.resource.Res;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.plugin.Plugin;
import org.jivesoftware.spark.ui.ChatRoom;

import org.jivesoftware.spark.plugin.battleship.listener.ChatRoomOpeningListener;
import org.jivesoftware.spark.plugin.battleship.packets.GameOffer;
import org.jivesoftware.spark.plugin.battleship.packets.MoveAnswer;
import org.jivesoftware.spark.plugin.battleship.packets.Move;
import org.jivesoftware.spark.util.ResourceUtils;
import org.jivesoftware.spark.util.log.Log;
import org.jxmpp.jid.EntityJid;

public class BattleshipPlugin implements Plugin {

    @Override
    public void initialize() {
        ProviderManager.addIQProvider(GameOffer.ELEMENT_NAME, GameOffer.NAMESPACE, new GameOffer.Provider());
        ProviderManager.addExtensionProvider(Move.ELEMENT_NAME, Move.NAMESPACE, new Move.Provider());
        ProviderManager.addExtensionProvider(MoveAnswer.ELEMENT_NAME, MoveAnswer.NAMESPACE, new MoveAnswer.Provider());

        StanzaListener _gameOfferListener = stanza -> {
            GameOffer invitation = (GameOffer) stanza;
            if (invitation.getType() == IQ.Type.get) {
                showInvitationInChat(invitation);
            }
        };

        SparkManager.getConnection().addAsyncStanzaListener(_gameOfferListener,
            new StanzaTypeFilter(GameOffer.class));
        ChatRoomOpeningListener _chatRoomListener = new ChatRoomOpeningListener();
        SparkManager.getChatManager().addChatRoomListener(_chatRoomListener);
    }

    private void showInvitationInChat(final GameOffer invitation) {
        invitation.setType(IQ.Type.result);
        invitation.setTo(invitation.getFrom());

        final ChatRoom room = SparkManager.getChatManager().getChatRoom(invitation.getFrom().asEntityBareJidIfPossible());
        String name = invitation.getFrom().getLocalpartOrNull().toString();
        final JPanel panel = new JPanel();
        JLabel text = new JLabel("Game request from" + name);
        JLabel game = new JLabel("Battleships");
        game.setFont(new Font("Dialog", Font.BOLD, 24));
        game.setForeground(Color.RED);
        JButton acceptButton = new JButton();
        ResourceUtils.resButton(acceptButton, Res.getString("button.accept"));
        JButton declineButton = new JButton();
        ResourceUtils.resButton(declineButton, Res.getString("button.decline"));
        panel.add(text);
        panel.add(game);
        panel.add(acceptButton);
        panel.add(declineButton);
        room.getTranscriptWindow().addComponent(panel);

        declineButton.addActionListener(e -> {
            try {
                SparkManager.getConnection().sendStanza(invitation);
            } catch (SmackException.NotConnectedException | InterruptedException e1) {
                Log.warning("Unable to send invitation accept to " + invitation.getTo(), e1);
                return;
            }
            invitation.setStartingPlayer(!invitation.isStartingPlayer());
            EntityJid opponentJID = invitation.getFrom().asEntityBareJidIfPossible();
            String opponentName = opponentJID.getLocalpart().asUnescapedString();
            ChatRoomOpeningListener.createWindow(invitation, opponentName);
            panel.remove(3);
            panel.remove(2);
            panel.repaint();
            panel.revalidate();
        });

        acceptButton.addActionListener(e -> {
            invitation.setType(IQ.Type.error);
            try {
                SparkManager.getConnection().sendStanza(invitation);
            } catch (SmackException.NotConnectedException | InterruptedException e1) {
                Log.warning("Unable to send invitation decline to " + invitation.getTo(), e1);
                return;
            }
            panel.remove(3);
            panel.remove(2);
            panel.repaint();
            panel.revalidate();
        });
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean canShutDown() {
        return false;
    }

    @Override
    public void uninstall() {
    }

}
