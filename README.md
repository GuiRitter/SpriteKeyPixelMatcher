# SpriteKeyPixelMatcher
Matches a given image to one in a known collection of sprites by reading the values of a few key pixels. These key pixels are used to uniquely identify each sprite. This class contains a method to find as few as possible of these key pixels. The available means to build a new matcher are described below.
* By using the factory method:
```java
WritableRaster sprites[] = new WritableRaster[4];
int i = 0;
sprites[i++] = ImageIO.read(new File("path/to/sprite/0.png")).getRaster();
sprites[i++] = ImageIO.read(new File("path/to/sprite/1.png")).getRaster();
sprites[i++] = ImageIO.read(new File("path/to/sprite/2.png")).getRaster();
sprites[i++] = ImageIO.read(new File("path/to/sprite/3.png")).getRaster();
SpriteKeyPixelMatcher matcher = SpriteKeyPixelMatcher.findKeyPixels(sprites, 1, 10);
```
This will attempt to build a matcher for the given sprites using as few key pixels as possible, according to the given range. The result will be `null` if that's not possible. This is the preferred use case for a newly created matcher.
* By using the constructor and sprite and key pixel arrays:
```java
WritableRaster sprites[] = new WritableRaster[4];
int i = 0;
sprites[i++] = ImageIO.read(new File("path/to/sprite/0.png")).getRaster();
sprites[i++] = ImageIO.read(new File("path/to/sprite/1.png")).getRaster();
sprites[i++] = ImageIO.read(new File("path/to/sprite/2.png")).getRaster();
sprites[i++] = ImageIO.read(new File("path/to/sprite/3.png")).getRaster();
LinkedList<Point> list = new LinkedList<>();
int z[] = {2, 3};
for (int y : z) {
   for (int x : z) {
       list.add(new Point(x, y));
   }
}
Point points[] = {};
points = list.toArray(points);
SpriteKeyPixelMatcher matcher = new SpriteKeyPixelMatcher(sprites, points);
```
This will construct a matcher for the given sprites and using the given key pixel values for the matching method. A matcher constructed like this is not guaranteed to uniquely identify every sprite, unless the key pixel locations were previously discovered by the factory method.
* By using the constructor and key pixel and sprite values array:

Works like the last one, but uses the pixel values read from the sprites at the key pixel locations. The index of every key pixel value in the second dimension of the sprite values matrix must match the index of the corresponding key pixel in the key pixel location array, and the index returned by the matching method will correspond to the index in the first dimension of the sprite values matrix.
* By using the constructor and the setup string:
```java
SpriteKeyPixelMatcher matcher = new SpriteKeyPixelMatcher("0c0a0d0");
```
This will construct the matcher according to the setup that can be retrieved from a previously built matcher. This is the preferred use case if several matches will be built across different program runs for the same sprite collection. After building the sprite, matching is as easy as:
```java
for (WritableRaster sprite : sprites) {
   System.out.println(matcher.match(sprite, 0, 0).index);
}
```
This will return the index of the sprite when it was used to build the matcher. It's also possible to get how close was the match. A value of zero should mean an exact match. A small value could mean an exact match with noise, but any value greater than zero could mean a match to a variably similar image, so the usage of this value will depend on context. Note that it's not needed to use an input image equal to the sprite being read: it's possible to use a larger image and indicate where in the image to read the sprite from.
