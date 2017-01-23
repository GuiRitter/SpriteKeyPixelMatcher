package io.github.guiritter.spritekeypixelmatcher;

/**
 * Return type for {@link io.github.guiritter.spritekeypixelmatcher.SpriteKeyPixelMatcher#match(java.awt.image.WritableRaster, int, int)}.
 * @author Guilherme Alan Ritter
 */
public final class MatchResult {

    /**
     * How similar was the input image to the matched sprite.
     * Useful to distinguish between an exact match and a close match.
     * Should be zero in an exact match, but can be higher if the input image
     * had noise. Therefore, what this value means must be found empirically.
     */
    public final double distance;

    /**
     * The index of the matched sprite.
     */
    public final int index;

    @Override
    public String toString() {
        return "index: " + index + "; distance: " + distance;
    }

    /**
     * Creates a new match result, containing the index of the matched sprite
     * and how close was the match.
     * @param index of the matched sprite
     * @param distance difference between the input image and matched sprite
     */
    public MatchResult(int index, double distance) {
        this.distance = distance;
        this.index = index;
    }
}
