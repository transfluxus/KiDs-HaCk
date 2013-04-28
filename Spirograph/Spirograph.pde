void setup() {
  size(400, 400);
  rectMode(CENTER);
}

void draw() {
  background(255, 0, 0);
  for (int i=0;i<=width;i+=20) {
    for (int j=0;j<=height;j+=20) {
      line(i, j, mouseX, mouseY);
    }
  }
}

