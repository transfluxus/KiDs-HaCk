import processing.serial.*;

final int WIDTH = 1162;
final int HEIGHT = 652;

void setup() {
  size(WIDTH, HEIGHT);

  esploraBkg = loadImage("esplora.png");

  Esplora.begin(this);

  for (SlideControl rc : rgbControls) {
    rc.send();
  }
}


void draw() {
  Esplora.update();
  println("stickX "+ Esplora.joystickX);
  println("stickY "+ Esplora.joystickY);
  for (int i=0; i < 4; i++)
    println("button "+i + " "+Esplora.switches(i));
  println("AccelX "+Esplora.accelX); 
  println("AccelY "+Esplora.accelY);
  println("AccelZ "+Esplora.accelZ
  println("mic "+Esplora.mic);
  println("slider "+Esplora.slider);
  println("light "+Esplora.light);
  println("temperature "+Esplora.temperatureC
    handleInputControls();
}

