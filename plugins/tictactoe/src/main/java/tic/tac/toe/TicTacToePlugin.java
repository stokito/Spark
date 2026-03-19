/**
 * Copyright (C) 2004-2011 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tic.tac.toe;

import java.awt.Color;
import java.awt.Font;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jivesoftware.resource.Res;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.plugin.Plugin;
import org.jivesoftware.spark.ui.ChatRoom;
import org.jivesoftware.spark.ui.ChatRoomButton;
import org.jivesoftware.spark.ui.ChatRoomListener;
import org.jivesoftware.spark.ui.rooms.ChatRoomImpl;
import org.jivesoftware.spark.util.ResourceUtils;
import org.jivesoftware.spark.util.log.Log;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.parts.Localpart;
import tic.tac.toe.packet.GameOffer;
import tic.tac.toe.packet.InvalidMove;
import tic.tac.toe.packet.Move;
import tic.tac.toe.ui.GamePanel;

import static org.jivesoftware.smack.packet.StanzaError.Condition.undefined_condition;
import static tic.tac.toe.TTTRes.ICON_BUTTON;


/**
 * Tic-tac-toe plugin for Spark
 *
 * @author Wolf Posdorfer
 * @version 16.06.2011
 */
public class TicTacToePlugin implements Plugin {
    public static final String XMPP_GAMING_NAMESPACE = "http://jabber.org/protocol/games";
    public static final String XMPP_GAMING_TICTACTOE_NAMESPACE = "http://jabber.org/protocol/games/tictactoe";

    private ChatRoomListener _chatRoomListener;
    private IQRequestHandler _gameOfferHandler;

    private HashSet<EntityBareJid> _currentInvitations;

    @Override
    public void initialize() {
        // Add Attention to a discovered items list.
        SparkManager.addFeature(XMPP_GAMING_NAMESPACE);
        SparkManager.addFeature(XMPP_GAMING_TICTACTOE_NAMESPACE);
        _currentInvitations = new HashSet<>(0);

        ProviderManager.addIQProvider(GameOffer.ELEMENT_NAME, GameOffer.NAMESPACE, new GameOffer.Provider());
        ProviderManager.addExtensionProvider(Move.ELEMENT_NAME, Move.NAMESPACE, new Move.Provider());
        ProviderManager.addExtensionProvider(InvalidMove.ELEMENT_NAME, InvalidMove.NAMESPACE, new InvalidMove.Provider());

        // Add an IQ listener to listen for incoming game invitations.
        _gameOfferHandler = new AbstractIqRequestHandler(
            GameOffer.ELEMENT_NAME,
            GameOffer.NAMESPACE,
            IQ.Type.get,
            IQRequestHandler.Mode.async) {
            @Override
            public IQ handleIQRequest(IQ request) {
                showInvitationAlert((GameOffer) request);
                return null;
            }
        };

        SparkManager.getConnection().registerIQRequestHandler(_gameOfferHandler);
        addButtonToToolBar();
    }

    /**
     * Add the TTT-Button to every opening Chatroom and create Listeners for it
     */
    private void addButtonToToolBar() {
        _chatRoomListener = new ChatRoomListener() {
            @Override
            public void chatRoomOpened(final ChatRoom room) {
                if (!(room instanceof ChatRoomImpl)) {
                    // Don't do anything if this is not a 1on1-Chat
                    return;
                }

                ChatRoomImpl chatRoom = (ChatRoomImpl) room;
                final EntityFullJid opponentJID = chatRoom.getJidOnline();
                // If the opponent is offline, then you should not start the game
                if (opponentJID == null) {
                    return;
                }
                if (!clientOfContactSupports(opponentJID)) {
                    return;
                }
                final ChatRoomButton sendGameButton = new ChatRoomButton(ICON_BUTTON);
                room.getToolBar().addChatRoomButton(sendGameButton);
                sendGameButton.addActionListener(e -> {
                    if (_currentInvitations.contains(opponentJID.asEntityBareJid())) {
                        return;
                    }

                    final GameOffer offer = new GameOffer();
                    offer.setTo(opponentJID);
                    offer.setType(IQ.Type.get);

                    _currentInvitations.add(opponentJID.asEntityBareJid());
                    try {
                        SparkManager.getConnection().sendStanza(offer);
                        room.getTranscriptWindow().insertCustomText(TTTRes.getString("ttt.request.sent"), false, false, Color.BLUE);
                    } catch (SmackException.NotConnectedException | InterruptedException e1) {
                        Log.warning("Unable to send offer to " + opponentJID, e1);
                        return;
                    }

                    SparkManager.getConnection().addAsyncStanzaListener(
                        stanza -> {
                            EntityBareJid opponent = opponentJID.asEntityBareJid();
                            if (stanza.getError() != null) {
                                room.getTranscriptWindow().insertCustomText(TTTRes.getString("ttt.request.decline"), false, false, Color.RED);
                                _currentInvitations.remove(opponent);
                                return;
                            }

                            GameOffer answer = (GameOffer) stanza;
                            answer.setStartingPlayer(offer.isStartingPlayer());
                            answer.setGameID(offer.getGameID());
                            if (answer.getType() == IQ.Type.result) {
                                // ACCEPT
                                _currentInvitations.remove(opponent);
                                room.getTranscriptWindow().insertCustomText(TTTRes.getString("ttt.request.accept"), false, false, Color.BLUE);
                                createTTTWindow(answer, opponentJID);
                            } else {
                                // DECLINE
                                room.getTranscriptWindow().insertCustomText(TTTRes.getString("ttt.request.decline"), false, false, Color.RED);
                                _currentInvitations.remove(opponent);
                            }
                        }, new StanzaIdFilter(offer));
                    // TODO: Just filtering by stanza id is insure, should use Smack's IQ send-response mechanisms.
                });
            }

            @Override
            public void chatRoomClosed(ChatRoom room) {
                if (!(room instanceof ChatRoomImpl)) {
                    return;
                }
                ChatRoomImpl cri = (ChatRoomImpl) room;
                _currentInvitations.remove(cri.getParticipantJID());
            }
        };

        SparkManager.getChatManager().addChatRoomListener(_chatRoomListener);
    }

    @Override
    public void shutdown() {
        _currentInvitations = null;
        SparkManager.getChatManager().removeChatRoomListener(_chatRoomListener);
        SparkManager.getConnection().unregisterIQRequestHandler(_gameOfferHandler);
        SparkManager.addFeature(XMPP_GAMING_TICTACTOE_NAMESPACE);
    }

    @Override
    public boolean canShutDown() {
        return true;
    }

    @Override
    public void uninstall() {
    }

    /**
     * Insert the Invitation Dialog into the Chat
     */
    private void showInvitationAlert(final GameOffer invitation) {
        final ChatRoom room = SparkManager.getChatManager().getChatRoom(invitation.getFrom().asEntityBareJidOrThrow());
        Localpart name = invitation.getFrom().getLocalpartOrThrow();

        final JPanel panel = new JPanel();
        JLabel text = new JLabel(TTTRes.getString("ttt.game.request", name));
        JLabel game = new JLabel(TTTRes.getString("ttt.game.name"));
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

        acceptButton.addActionListener(e -> {
            GameOffer invitationAccept = invitation;
            invitationAccept.setType(IQ.Type.result);
            invitationAccept.setTo(invitationAccept.getFrom());
            try {
                SparkManager.getConnection().sendStanza(invitationAccept);
            } catch (SmackException.NotConnectedException | InterruptedException e1) {
                Log.warning("Unable to send invitation accept to " + invitationAccept.getTo(), e1);
                return;
            }
            invitationAccept.setStartingPlayer(!invitationAccept.isStartingPlayer());
            createTTTWindow(invitationAccept, invitationAccept.getTo().asEntityFullJidOrThrow());
            panel.remove(3);
            panel.remove(2);
            panel.repaint();
            panel.revalidate();
        });

        declineButton.addActionListener(e -> {
            GameOffer invitationDecline = invitation;
            invitationDecline.setType(IQ.Type.error);
            invitationDecline.setError(StanzaError.getBuilder().setCondition(undefined_condition).setDescriptiveEnText("User declined your request.").build());
            try {
                SparkManager.getConnection().sendStanza(invitationDecline);
            } catch (SmackException.NotConnectedException | InterruptedException e1) {
                Log.warning("Unable to send invitation decline to " + invitationDecline.getTo(), e1);
                return;
            }
            panel.remove(3);
            panel.remove(2);
            panel.repaint();
            panel.revalidate();
        });
    }

    /**
     * Creates The tic-tac-toe Window and starts the Game
     */
    private void createTTTWindow(GameOffer gop, EntityFullJid opponentJID) {
        String opponentName = opponentJID.getLocalpart().asUnescapedString();
        JFrame f = new JFrame(TTTRes.getString("ttt.window.title", TTTRes.getString("ttt.game.name"), opponentName));
        f.setIconImage(ICON_BUTTON.getImage());
        GamePanel gp = new GamePanel(gop.getGameID(), gop.isStartingPlayer(), opponentJID, f);
        f.add(gp);
        f.pack();
        f.setLocationRelativeTo(SparkManager.getChatManager().getChatContainer());
        f.setVisible(true);
    }

    /**
     * Determine via service discovery if the contact's client supports
     */
    private static boolean clientOfContactSupports(EntityFullJid fullJID) {
        ServiceDiscoveryManager discoManager = SparkManager.getDiscoManager();
        try {
            DiscoverInfo discoverInfo = discoManager.discoverInfo(fullJID);
            return discoverInfo.containsFeature(XMPP_GAMING_TICTACTOE_NAMESPACE);
        } catch (Exception e) {
            Log.warning(e.getMessage());
            return false;
        }
    }
}
