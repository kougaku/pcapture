# PCapture
Video Capture Library for Processing based on the [sarxos's Webcam Capture API](https://github.com/sarxos/webcam-capture)


## Same as Capture
This library was created as an alternative when the Capture class in the official video library does not work well.
PCapture has the same methods as Capture.

```scala
import pcapture.*;

PCapture cam;

void setup() {
  size(640, 480);
  cam = new PCapture(this, 640, 480);
  // cam = new PCapture(this, 640, 480, PCapture.list()[0]); // when use 0th device
  cam.start();
}

void draw() {
  if ( !cam.available() ) return;
  cam.read();
  image(cam, 0, 0);
}

// captureEvent() is also available.
/*
void captureEvent(PCapture cap) {
  cap.read();
}
*/
```

## Note
The frameRate() method is not supported.
This is because webcam-capture does not have a method to set the frame rate.

## Acknowledgements
Thanks a lot for the following projects.
- [sarxos/webcam-capture](https://github.com/sarxos/webcam-capture)
- [processing/processing-video](https://github.com/processing/processing-video)
