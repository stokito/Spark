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
package org.jivesoftware.spark.plugin.battleship.types;

import java.awt.Image;
import java.util.Objects;

import javax.swing.ImageIcon;

public enum Ship {
    EMPTY("empty.png"),
    TWO("2.png"),
    THREE("3.png"),
    THREE2("3.png"),
    FOUR("4.png"),
    FIVE("5.png");

    private final ImageIcon image;
    private final ImageIcon imageScaled;

    Ship(String s) {
        ClassLoader cl = getClass().getClassLoader();
        image = new ImageIcon(Objects.requireNonNull(cl.getResource(s)));
        imageScaled = new ImageIcon(image.getImage().getScaledInstance(100, 50, Image.SCALE_SMOOTH));
    }

    public ImageIcon getImage() {
        return image;
    }

    public static Ship valueOf(int x) {
        switch (x) {
            case 2:
                return TWO;
            case 3:
                return THREE;
            case 4:
                return THREE2;
            case 5:
                return FOUR;
            case 6:
                return FIVE;
            default:
                return EMPTY;
        }
    }

    /**
     * Returns the Position inside the Array
     */
    public int inArrayPosition() {
        switch (this) {
            case TWO:
                //noinspection DuplicateBranchesInSwitch
                return 0;
            case THREE:
                return 1;
            case THREE2:
                return 2;
            case FOUR:
                return 3;
            case FIVE:
                return 4;
            default:
                return 0;
        }
    }

    /**
     * Returns the Occupying Fields
     */
    public int getFields() {
        switch (this) {
            case TWO:
                return 2;
            case THREE:
                return 3;
            case THREE2:
                //noinspection DuplicateBranchesInSwitch
                return 3;
            case FOUR:
                return 4;
            case FIVE:
                return 5;
        }
        return 0;
    }

    public ImageIcon getScaledInstance() {
        return imageScaled;
    }

    /**
     * Returns the next Bigger Ship<br>
     * 2 -> 3.1 -> 3.2 -> 4 -> 5 -> 2.....
     */
    public Ship increment() {
        switch (this) {
            case TWO:
                return THREE;
            case THREE:
                return THREE2;
            case THREE2:
                return FOUR;
            case FOUR:
                return FIVE;
            case FIVE:
                return TWO;
        }
        return TWO;
    }
}
