package io.github.guiritter.spritekeypixelmatcher;

import io.github.guiritter.tallycounter.TallyCounter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * Matches a given image to one in a known collection of sprites
 * by reading the values of a few key pixels. These key pixels are used
 * to uniquely identify each sprite. This class contains a method to find
 * as few as possible of these key pixels. The available means to build
 * a new matcher are described below.
 * <ul>
 * <li>By using the factory method:
 * </ul>
 * <blockquote><pre>WritableRaster sprites[] = new WritableRaster[4];
 *int i = 0;
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/0.png")).getRaster();
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/1.png")).getRaster();
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/2.png")).getRaster();
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/3.png")).getRaster();
 *SpriteKeyPixelMatcher matcher = SpriteKeyPixelMatcher.findKeyPixels(sprites, 1, 10);</pre></blockquote>
 * This will attempt to build a matcher for the given sprites using as few
 * key pixels as possible, according to the given range. The result will be
 * <code>null</code> if that's not possible. This is the preferred use case
 * for a newly created matcher.
 * <ul>
 * <li>By using the constructor and sprite and key pixel arrays:
 * </ul>
 * <blockquote><pre>WritableRaster sprites[] = new WritableRaster[4];
 *int i = 0;
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/0.png")).getRaster();
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/1.png")).getRaster();
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/2.png")).getRaster();
 *sprites[i++] = ImageIO.read(new File("path/to/sprite/3.png")).getRaster();
 *LinkedList&lt;Point> list = new LinkedList&lt;>();
 *int z[] = {2, 3};
 *for (int y : z) {
 *    for (int x : z) {
 *        list.add(new Point(x, y));
 *    }
 *}
 *Point points[] = {};
 *points = list.toArray(points);
 *SpriteKeyPixelMatcher matcher = new SpriteKeyPixelMatcher(sprites, points);</pre></blockquote>
 * This will construct a matcher for the given sprites and using the given
 * key pixel values for the matching method. A matcher constructed like this
 * is not guaranteed to uniquely identify every sprite, unless the key pixel
 * locations were previously discovered by the factory method.
 * <ul>
 * <li>By using the constructor and key pixel and sprite values array:
 * </ul>
 * Works like the last one, but uses the pixel values read from the sprites
 * at the key pixel locations. The index of every key pixel value
 * in the second dimension of the sprite values matrix must match
 * the index of the corresponding key pixel in the key pixel location array, and
 * the index returned by the matching method will correspond to the index
 * in the first dimension of the sprite values matrix.
 * <ul>
 * <li>By using the constructor and the setup string:
 * </ul>
 * <blockquote><pre>SpriteKeyPixelMatcher matcher = new SpriteKeyPixelMatcher("0c0a0d0");</pre></blockquote>
 * This will construct the matcher according to the setup that can be retrieved
 * from a previously built matcher. This is the preferred use case if several
 * matches will be built across different program runs for the same sprite
 * collection.
 * After building the sprite, matching is as easy as:
 * <blockquote><pre>for (WritableRaster sprite : sprites) {
 *    System.out.println(matcher.match(sprite, 0, 0).index);
 *}</pre></blockquote>
 * This will return the index of the sprite when it was used to build
 * the matcher. It's also possible to get how close was the match. A value
 * of zero should mean an exact match. A small value could mean an exact match
 * with noise, but any value greater than zero could mean a match to a variably
 * similar image, so the usage of this value will depend on context. Note that
 * it's not needed to use an input image equal to the sprite being read:
 * it's possible to use a larger image and indicate where in the image
 * to read the sprite from.
 * @author Guilherme Alan Ritter
 */
public final class SpriteKeyPixelMatcher {

    /**
     * Pixel color component array.
     */
    private int color[];

    /**
     * Loop control.
     */
    private int colorI;

    private double distance;

    private double distanceA;

    private double distanceMinimum;

    /**
     * Loop control.
     */
    private int i;

    /**
     * Loop control.
     */
    private int keyPixelI;

    /**
     * Array of 2D points representing the minimum necessary pixels
     * to uniquely identify a sprite in a collection. These key pixels
     * can be stored to be reused on the same collection at a different runtime.
     */
    private final Point keyPixelLocationArray[];

    private Point keyPixelLocation;

    private int keyPixelValue[];

    private int keyPixelValueArray[][];

    /**
     * Loop control.
     */
    private int spriteI;

    private int spriteMinimumI;

    /**
     * Sprite pixel color values to be compared with the image to be matched.
     * The first index represents sprites,
     * the second index represents key pixels,
     * the third index represents color component values;
     */
    private final int spriteValueArray[][][];

    /**
     * Nth dimentional euclidean distance calculator.
     * @param point0 one point in Nth dimentional space
     * @param point1 another point in Nth dimentional space
     * @return Euclidean distance between the points
     */
    public double distanceND(int point0[], int point1[]) {
        if (point0 == null)
            throw new IllegalArgumentException("Point 0 is null.");
        if (point1 == null)
            throw new IllegalArgumentException("Point 1 is null.");
        if (point0.length < 1)
            throw new IllegalArgumentException("Point 0 is empty.");
        if (point0.length != point1.length)
            throw new IllegalArgumentException(
             "Point 0 and point 1 must have the same length.");
        distance = 0;
        for (i = 0; i < point0.length; i++) {
            distance += Math.pow(point0[i] - point1[i], 2);
        }
        return Math.sqrt(distance);
    }

    /**
     * Attempts to build a matcher with a minimum setup capable
     * of uniquely identifying all given sprites. The efforts of this factory
     * method are bound by the parameters: it will attempt to find a matcher
     * with at least <code>minimum</code> key pixels, and with no more than
     * <code>maximum</code> key pixels. This is done because this is a brute
     * force algorithm: given a certain amount of key pixels, all possible
     * combination of pixels will be used in the attempt.
     * @param spriteArray the sprites to be uniquely identified
     * @param minimum the minimum amount of key pixels to be used
     * by the newly built matcher
     * @param maximum the maximum amount of key pixels to be used
     * by the newly built matcher
     * @return a matcher instance set up to uniquely identify the given sprites,
     * with as few as possible key pixels, given the search bounds, or null
     * if it's not possible to build a matcher with these bounds
     */
    public static SpriteKeyPixelMatcher findKeyPixels(
     WritableRaster spriteArray[], int minimum, int maximum) {
        if (spriteArray == null) {
            throw new IllegalArgumentException("spriteArray is null.");
        }
        if (spriteArray.length < 1) {
            throw new IllegalArgumentException("spriteArray is empty.");
        }
        if ((minimum < 0) || (maximum < 0)) {
            throw new IllegalArgumentException(
             "minimum and maximum must be positive integers.");
        }
        if (minimum > maximum) {throw new IllegalArgumentException(
             "minimum must be less than maximum.");
        }
        int width = (spriteArray[0] == null) ? 0 : spriteArray[0].getWidth();
        int height = (spriteArray[0] == null) ? 0 : spriteArray[0].getHeight();
        int color[] = (spriteArray[0] == null) ? new int[0]
         : spriteArray[0].getPixel(0, 0, (int[]) null);
        for (WritableRaster sprite : spriteArray) {
            if (sprite == null) {
                throw new IllegalArgumentException("A sprite is null.");
            }
            if (sprite.getWidth() != width) {
                throw new IllegalArgumentException(
                 "Sprites must have the same width.");
            }
            if (sprite.getHeight() != height) {
                throw new IllegalArgumentException(
                 "Sprites must have the same height.");
            }
            if (sprite.getPixel(0, 0, (int[]) null).length
             != color.length) {
                throw new IllegalArgumentException("Sprites' colors "
                 + "must have the same amount of components.");
            }
        }
        int pixelMaximum = ((height - 1) * width) + (width - 1);
        TallyCounter counter;
        Point keyPixelArray[];
        int keyPixelI;
        long counterArray[];
        SpriteKeyPixelMatcher matcher;
        int spriteI;
        boolean found;
        for (int keyPixelAmount = minimum; keyPixelAmount <= maximum;
         keyPixelAmount++) {
            counter = new TallyCounter(keyPixelAmount,
             TallyCounter.Type.UNIQUE_COMBINATION, pixelMaximum);
            keyPixelArray = new Point[keyPixelAmount];
            while (!counter.overflowFlag) {
                counterArray = counter.getArray();
                for (keyPixelI = 0; keyPixelI < counterArray.length;
                 keyPixelI++) {
                    keyPixelArray[keyPixelI] = new Point(
                     (int) (counterArray[keyPixelI]
                      - ((counterArray[keyPixelI] / width) * width)),
                     (int) (counterArray[keyPixelI] / width));
                }
                matcher = new SpriteKeyPixelMatcher(spriteArray, keyPixelArray);
                found = true;
                for (spriteI = 0; spriteI < spriteArray.length; spriteI++) {
                    if (matcher.match(spriteArray[spriteI], 0, 0).index
                     != spriteI) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return matcher;
                } else {
                    counter.increment();
                }
            }
        }
        return null;
    }

    /**
     * Returns a copy of the key pixel locations used by this matcher instance.
     * @return a copy of the array containing the key pixel locations
     */
    public Point[] getKeyPixels() {
        return Arrays.copyOf(
         keyPixelLocationArray, keyPixelLocationArray.length);
    }

    /**
     * Builds a string that represents the state of this matcher. Can be used
     * to construct a new matcher that works just like this one.
     * @return a string representation of this matcher
     */
    public String getSetup() {
        StringBuilder builder = new StringBuilder();
        for (keyPixelI = 0; keyPixelI < keyPixelLocationArray.length; keyPixelI++) {
            if (keyPixelI > 0) {
                builder.append("b");
            }
            keyPixelLocation = keyPixelLocationArray[keyPixelI];
            builder.append(String.format("%dc%d",
             keyPixelLocation.x, keyPixelLocation.y));
        }
        builder.append("a");
        for (spriteI = 0; spriteI < spriteValueArray.length; spriteI++) {
            if (spriteI > 0) {
                builder.append("b");
            }
            keyPixelValueArray = spriteValueArray[spriteI];
            for (keyPixelI = 0; keyPixelI < keyPixelValueArray.length;
             keyPixelI++) {
                keyPixelValue = keyPixelValueArray[keyPixelI];
                if (keyPixelI > 0) {
                    builder.append("c");
                }
                for (colorI = 0; colorI < keyPixelValue.length; colorI++) {
                    if (colorI > 0) {
                        builder.append("d");
                    }
                    builder.append(keyPixelValue[colorI]);
                }
//                builder.append(Arrays.toString(keyPixelValueArray[keyPixelI]));
            }
        }
        return builder.toString();
    }

    /**
     * Matches the given image with one of the sprites. The image does not need
     * to be cropped to the sprite's bounds. Instead, the offset parameters
     * can be used to specify where to look for the alleged sprite. Together
     * with the matched sprite index, the value that is used to decide which
     * is the sprite that more closely matches the input image is returned.
     * Because of how the algorithm works, any image will be matched
     * to a sprite, regardless how similar it is. This returned value
     * can be used to differentiate between an exact match (should be zero), an
     * exact match with noise (how larger than zero will depend on the context)
     * and a simply similar match. The meaning of each value must be determined
     * by experiments.
     * @param image to be matched to one of the sprites
     * @param offsetX location of the sprites' first pixel in this image, in x
     * @param offsetY location of the sprites' first pixel in this image, in y
     * @return the matched sprite index and how close it was matched
     */
    public MatchResult match(WritableRaster image, int offsetX, int offsetY) {
        if (image == null)
            throw new IllegalArgumentException("Image is null.");
        color = image.getPixel(0, 0, (int[]) null);
        if (color.length != spriteValueArray[0][0].length)
            throw new IllegalArgumentException("Image contains "
             + "a different amount of color components than the images "
             + "used in the construction of this matcher.");
        color = image.getPixel(0, 0, (int[]) null);
        distanceMinimum = Double.MAX_VALUE;
        spriteMinimumI = 0;
        for (spriteI = 0; spriteI < spriteValueArray.length; spriteI++) {
            keyPixelValueArray = spriteValueArray[spriteI];
            distanceA = 0;
            for (keyPixelI = 0; keyPixelI < keyPixelValueArray.length;
             keyPixelI++) {
                keyPixelLocation = keyPixelLocationArray[keyPixelI];
                image.getPixel(keyPixelLocation.x + offsetX,
                 keyPixelLocation.y + offsetY, color);
                distanceA += distanceND(keyPixelValueArray[keyPixelI], color);
            }
            if (distanceMinimum > distanceA) {
                distanceMinimum = distanceA;
                spriteMinimumI = spriteI;
            }
        }
        return new MatchResult(spriteMinimumI, distanceMinimum);
    }

    @Override
    public String toString() {
        return String.format(
         "sprite amount: %d; key pixel amount: %d; pixel color component amount: %d; setup:\n%s",
         spriteValueArray.length,
         spriteValueArray[0].length,
         spriteValueArray[0][0].length, getSetup());
    }

    private static void validateKeyPixelValueArray(
     Point keyPixelLocationArray[], int keyPixelValueArray[][][]) {
        if (keyPixelValueArray == null) {
            throw new IllegalArgumentException("keyPixelValueArray is null.");
        }
        if (keyPixelValueArray.length < 1) {
            throw new IllegalArgumentException("keyPixelValueArray is empty.");
        }
        int length = ((keyPixelValueArray[0] == null)
         || (keyPixelValueArray[0].length < 1)
         || (keyPixelValueArray[0][0] == null))
         ? 0 : keyPixelValueArray[0][0].length;
        for (int sprite[][] : keyPixelValueArray) {
            if (sprite == null) {
                throw new IllegalArgumentException("A sprite (array "
                 + "of key pixels (array of color components)) is null.");
            }
            if (sprite.length != keyPixelLocationArray.length) {
                throw new IllegalArgumentException("A sprite (array "
                 + "of key pixels (array of color components)) does "
                 + "not contain the same amount of key pixels as the key pixel "
                 + "location array.");
            }
            for (int keyPixel[] : sprite) {
                if (keyPixel == null) {
                    throw new IllegalArgumentException("A key pixel "
                     + "(array of color components) is null.");
                }
                if (keyPixel.length < 1) {
                    throw new IllegalArgumentException("A key pixel "
                     + "(array of color components) is empty.");
                }
                if (keyPixel.length != length) {
                    throw new IllegalArgumentException("There are key pixels "
                     + "(array of color components) with different amount "
                     + "of color components.");
                }
            }
        }
    }

    public SpriteKeyPixelMatcher(String setup) {
        String fields[] = setup.split("a");
        if (fields.length != 2) {
            throw new IllegalArgumentException(
             "setup must contain one section for key pixel locations "
              + "and then one for key pixel values.");
        }
        String keyPixelLocationArrayString = fields[0];
        String spriteValueArrayString = fields[1];
        if (keyPixelLocationArrayString.trim().isEmpty()
         || spriteValueArrayString.trim().isEmpty()) {
            throw new IllegalArgumentException(
             "At least one of setup's fields are empty.");
        }
        String keyPixelLocationArrayFields[]
         = keyPixelLocationArrayString.split("b");
        String keyPixelLocationFields[];
        keyPixelLocationArray = new Point[keyPixelLocationArrayFields.length];
        String keyPixelLocationString;
        for (keyPixelI = 0; keyPixelI < keyPixelLocationArray.length;
         keyPixelI++) {
            keyPixelLocationString = keyPixelLocationArrayFields[keyPixelI];
            if (keyPixelLocationString.trim().isEmpty()) {
                throw new IllegalArgumentException(
                 "A key pixel location is empty.");
            }
            keyPixelLocationFields = keyPixelLocationString.split("c");
            if (keyPixelLocationFields.length != 2) {
                throw new IllegalArgumentException(
                 "Key pixel location must have x and y values.");
            }
            keyPixelLocationArray[keyPixelI] = new Point(
             Integer.parseInt(keyPixelLocationFields[0].trim()),
             Integer.parseInt(keyPixelLocationFields[1].trim()));
        }
        String spriteValueArrayFields[] = spriteValueArrayString.split("b");
        spriteValueArray = new int[spriteValueArrayFields.length][][];
        String keyPixelValueArrayString;
        String keyPixelValueArrayFields[];
        String keyPixelValueString;
        String keyPixelValueFields[];
        for (spriteI = 0; spriteI < spriteValueArrayFields.length; spriteI++) {
            keyPixelValueArrayString = spriteValueArrayFields[spriteI];
            if (keyPixelValueArrayString.trim().isEmpty()) {
                throw new IllegalArgumentException(
                 "A key pixel value array is empty.");
            }
            keyPixelValueArrayFields = keyPixelValueArrayString.split("c");
            keyPixelValueArray = new int[keyPixelValueArrayFields.length][];
            spriteValueArray[spriteI] = keyPixelValueArray;
            for (keyPixelI = 0; keyPixelI < keyPixelValueArray.length;
             keyPixelI++) {
                keyPixelValueString = keyPixelValueArrayFields[keyPixelI];
                if (keyPixelValueString.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                     "A key pixel value is empty.");
                }
                keyPixelValueFields = keyPixelValueString.split("d");
                keyPixelValue = new int[keyPixelValueFields.length];
                keyPixelValueArray[keyPixelI] = keyPixelValue;
                for (colorI = 0; colorI < keyPixelValue.length; colorI++) {
                    keyPixelValue[colorI]
                     = Integer.parseInt(keyPixelValueFields[colorI]);
                }
            }
        }
        validateKeyPixelValueArray(keyPixelLocationArray, spriteValueArray);
    }

    /**
     * Constructs a new matcher for the given key pixel locations and values.
     * Every element in <code>keyPixelValueArray</code> represents a sprite
     * as an array of key pixels, and every key pixel index must match the index
     * in <code>keyPixelLocationArray</code>; therefore, the size of
     * <code>keyPixelLocationArray</code> must the the same as the size of every
     * element in <code>keyPixelValueArray</code>. Also, the size of every key
     * pixel must be the same.
     * @param keyPixelLocationArray key pixel locations
     * @param spriteValueArray sprites containing key pixels
     * containing color component values
     */
    public SpriteKeyPixelMatcher(
     int[][][] spriteValueArray, Point[] keyPixelLocationArray) {
        if (keyPixelLocationArray == null) {
            throw new IllegalArgumentException("keyPixelArray is null.");
        }
        if (keyPixelLocationArray.length < 1) {
            throw new IllegalArgumentException("keyPixelArray is empty.");
        }
        for (Point point : keyPixelLocationArray) {
            if (point == null) {
                throw new IllegalArgumentException(
                 "A key pixel location is null.");
            }
        }
        validateKeyPixelValueArray(keyPixelLocationArray, spriteValueArray);
        this.keyPixelLocationArray = keyPixelLocationArray;
        this.spriteValueArray = spriteValueArray;
    }

    /**
     * Builds a new matcher for the given sprite and key pixel collections.
     * @param spriteArray the sprite collection
     * @param keyPixelLocationArray key pixel locations
     */
    public SpriteKeyPixelMatcher(
     WritableRaster spriteArray[], Point keyPixelLocationArray[]) {
        if (spriteArray == null) {
            throw new IllegalArgumentException("spriteArray is null.");
        }
        if (spriteArray.length < 1) {
            throw new IllegalArgumentException("spriteArray is empty.");
        }
        if (keyPixelLocationArray == null) {
            throw new IllegalArgumentException(
             "keyPixelLocationArray is null.");
        }
        if (keyPixelLocationArray.length < 1) {
            throw new IllegalArgumentException(
             "keyPixelLocationArray is empty.");
        }
        {
            int width = (spriteArray[0] == null)
             ? 0 : spriteArray[0].getWidth();
            int height = (spriteArray[0] == null)
             ? 0 : spriteArray[0].getHeight();
            color = (spriteArray[0] == null)
             ? new int[0] : spriteArray[0].getPixel(0, 0, (int[]) null);
            for (WritableRaster sprite : spriteArray) {
                if (sprite == null) {
                    throw new IllegalArgumentException("A sprite is null.");
                }
                if (sprite.getWidth() != width) {
                    throw new IllegalArgumentException(
                     "Sprites must have the same width.");
                }
                if (sprite.getHeight() != height) {
                    throw new IllegalArgumentException(
                     "Sprites must have the same height.");
                }
                if (sprite.getPixel(0, 0, (int[]) null).length
                 != color.length) {
                    throw new IllegalArgumentException("Sprites' colors "
                     + "must have the same amount of components.");
                }
            }
            Rectangle rectangle = new Rectangle(width, height);
            for (Point keyPixel : keyPixelLocationArray) {
                if (!rectangle.contains(keyPixel)) {
                    throw new IllegalArgumentException("Key pixels"
                     + "must lie within the sprites.");
                }
            }
        }
        spriteValueArray
         = new int[spriteArray.length][keyPixelLocationArray.length][];
        WritableRaster sprite;
        Point keyPixel;
        for (spriteI = 0; spriteI < spriteArray.length; spriteI++) {
            keyPixelValueArray = new int[keyPixelLocationArray.length][];
            spriteValueArray[spriteI] = keyPixelValueArray;
            sprite = spriteArray[spriteI];
            for (keyPixelI = 0;
             keyPixelI < keyPixelLocationArray.length; keyPixelI++) {
                keyPixel = keyPixelLocationArray[keyPixelI];
                color = sprite.getPixel(keyPixel.x, keyPixel.y, (int[]) null);
                keyPixelValueArray[keyPixelI] = color;
            }
        }
        validateKeyPixelValueArray(keyPixelLocationArray, spriteValueArray);
        this.keyPixelLocationArray
         = Arrays.copyOf(keyPixelLocationArray, keyPixelLocationArray.length);
    }
}
