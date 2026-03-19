/**
 * Copyright (C) 2004-2011 Jive Software. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.spark.plugin.battleship.packets;

import java.io.IOException;
import java.util.Random;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jxmpp.JxmppContext;

/**
 * The Game Offer Packet to start a new game.
 * The offer indicates whether the player making the offer will be the starting player.
 * The starting player is selected randomly by default, which is recommended.
 *
 * @author Wolf Posdorfer
 */
public class GameOffer extends IQ {
    public static final String ELEMENT_NAME = "battleship";
    public static final String NAMESPACE = "http://jabber.org/protocol/games/battleship";

    private int gameID;

    /**
     * The user making the game invitation is the starting player.
     */
    private boolean startingPlayer;

    public GameOffer() {
        super(ELEMENT_NAME, NAMESPACE);
        // Randomly choose if the user making the game offer will be the starting player (black).
        Random random = new Random();
        startingPlayer = random.nextBoolean();
        gameID = Math.abs(random.nextInt());
    }

    public int getGameID() {
        return gameID;
    }

    public void setGameID(int gameID) {
        this.gameID = gameID;
    }

    public boolean isStartingPlayer() {
        return startingPlayer;
    }

    public void setStartingPlayer(boolean startingPlayer) {
        this.startingPlayer = startingPlayer;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();
        if (getType() == IQ.Type.get) {
            buf.element("gameID", String.valueOf(gameID));
            buf.element("startingPlayer", String.valueOf(startingPlayer));
            buf.append(getExtensions());
        }
        return buf;
    }

    public static class Provider extends IqProvider<GameOffer> {
        public Provider() {
            super();
        }

        @Override
        public GameOffer parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
            final GameOffer gameOffer = new GameOffer();
            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    if (parser.getName().equals("gameID")) {
                        final int gameID = Integer.parseInt(parser.nextText());
                        gameOffer.setGameID(gameID);
                    } else if (parser.getName().equals("startingPlayer")) {
                        boolean startingPlayer = Boolean.parseBoolean(parser.nextText());
                        gameOffer.setStartingPlayer(startingPlayer);
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT && parser.getName().equals(ELEMENT_NAME)) {
                    done = true;
                }
            }
            return gameOffer;
        }
    }
}
