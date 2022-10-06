import pcapture.*;
PCapture cam;

void setup() {
  size(640, 480);
  cam = new PCapture(this, 640, 480);
  // cam = new PCapture(this, 640, 480, PCapture.list()[0] ); // when use 0th device
  cam.start();
}

void draw() {
  if ( !cam.available() ) return;
  background(50);
  cam.read();
  image(cam, 0, 0);
}

// captureEvent() is also available.
/*
void captureEvent(PCapture cap) {
 cap.read();
 }
 */
