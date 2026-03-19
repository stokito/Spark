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

import javax.swing.ImageIcon;

import java.awt.*;

import static tic.tac.toe.TTTRes.*;

/**
 * The Variations of Marks
 */
public enum Mark {
    BLANK(0, ICON_EMPTY, ICON_EMPTY),
    X(1, ICON_X, ICON_X_BLUE),
    O(2, ICON_O, ICON_O_BLUE);

    private final int value;
    private final ImageIcon image;
    private final ImageIcon redImage;
    private final ImageIcon imageSmall;

    public static Mark valueOf(int x) {
        switch (x) {
            case 1:
                return X;
            case 2:
                return O;
            default:
                return BLANK;
        }
    }

    public ImageIcon getImage() {
        return image;
    }

    public ImageIcon getImageSmall() {
        return imageSmall;
    }

    public ImageIcon getRedImage() {
        return redImage;
    }

    public int getValue() {
        return value;
    }

    Mark(int value, ImageIcon image, ImageIcon redImage) {
        this.value = value;
        this.image = image;
        this.imageSmall = new ImageIcon(image.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        this.redImage = redImage;
    }
}
