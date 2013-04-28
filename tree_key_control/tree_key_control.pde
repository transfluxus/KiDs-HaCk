float curlx = 0;
float curly = 0;
float f = sqrt(2)/2.;
float deley = 10;
float growth = 0;
float growthTarget = 0;

int depth = 17;
int strokeW = 1;

int xCursor, yCursor;  
boolean[] switchOn = new boolean[4];

void setup()
{
  size(950, 450, P2D);
  //smooth();
  addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
    public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
      mouseWheel(evt.getWheelRotation());
    }
  }
  );
  xCursor = width/2;
  yCursor = height/2;
}

void draw()
{
  background(250);
  stroke(switchOn[0]? 255: 0, switchOn[1]? 255: 0, switchOn[2]? 255: 0);
  strokeWeight(strokeW);
  curlx += (radians(360./height*xCursor)-curlx)/deley;
  curly += (radians(360./height*yCursor)-curly)/deley;
  //  curlx += (radians(360./height*mouseX)-curlx)/deley;
  //  curly += (radians(360./height*mouseY)-curly)/deley;
  translate(width/2, height/3*2);
  line(0, 0, 0, height/2);
  branch(height/4., depth);
  growth += (growthTarget/10-growth+1.)/deley;
}

void mouseWheel(int delta)
{
  growthTarget += delta;
}


void keyPressed() {
  //println(ke
  if (keyCode == UP)
    yCursor--;
  else if (keyCode == DOWN)
    yCursor++;
  else if (keyCode == LEFT)
    xCursor--;
  else if (keyCode == RIGHT)
    xCursor++;

  else if (key == 'a')
    growthTarget+=0.1f;
  else if (key == 'y')
    growthTarget-=0.1f;

  else if (key == 's')
    depth++;
  else if (key == 'x')
    depth--;    

  else if (key== 'd')
    strokeW++;
  else if (key== 'c')
    strokeW--;

  else if (key == 'q')
    switchOn[0]=true;
  else if (key == 'w')
    switchOn[1]=true;
  else if (key == 'e')
    switchOn[2]=true;
  else if (key == 'r')
    switchOn[3]=true;    

  depth = constrain(depth, 1, 20);
  strokeW = constrain(strokeW, 1, 10);
}

void keyReleased() {
  if (key == 'w')
    switchOn[0]=false;
  else if (key == 'e')
    switchOn[1]=false;
  else if (key == 'r')
    switchOn[2]=false;
  else if (key == 't')
    switchOn[3]=false;
}

void branch(float len, int num)
{
  len *= f;
  num -= 1;
  if ((len > 1) && (num > 0))
  {
    pushMatrix();
    rotate(curlx);
    line(0, 0, 0, -len);
    translate(0, -len);
    branch(len, num);
    popMatrix();

    //    pushMatrix();
    //    line(0,0,0,-len);
    //    translate(0,-len);
    //    branch(len);
    //    popMatrix();
    len *= growth;
    pushMatrix();
    rotate(curlx-curly);
    line(0, 0, 0, -len);
    translate(0, -len);
    branch(len, num);
    popMatrix();
    //len /= growth;
  }
}

