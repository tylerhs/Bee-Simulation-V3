/////////////////////////////////////////
//  Calculates the size and locations  //
//  of flowers given a density map     //
/////////////////////////////////////////

import gab.opencv.*;
import java.awt.Rectangle;
OpenCV opencv;

//GLOBALS
int w, h;
Table densityTable;
Table flowerTable;
PImage src;
PImage dst;
ArrayList<Contour> contours;
ArrayList<PVector> points;
float maxDensity = -Float.MAX_VALUE;
float minDensity = Float.MAX_VALUE;
color red = color(255,0,0);

//PARAMETERS
int thresh = 237;  // threshold for flowers (out of 255)

void setup(){
  
  // create new table for storing flower info
  flowerTable = new Table();
  for(int i=0; i<4; i++)
    flowerTable.addColumn();
    
  // load density data and setup image
  densityTable = loadTable("densityMap.csv");
  w = densityTable.getColumnCount();
  h = densityTable.getRowCount();
  src = createImage(w,h,ALPHA);
  println("size of map: " + w + ", " + h);
  
  // set the first row of the output table to width and height
  // this way the simulation will know the size of the window
  flowerTable.addRow();
  flowerTable.setInt(0,0,w);
  flowerTable.setInt(0,1,h);
  
  // find the max and min densities for scaling later
  for(int i=0; i<w; i++){
    for(int j=0; j<h; j++){
      if(densityTable.getFloat(j,i) > maxDensity){
        maxDensity = densityTable.getFloat(j,i);
      }
      if(densityTable.getFloat(j,i) < minDensity){
        minDensity = densityTable.getFloat(j,i);
      }
    }
  }
  
  // load pixels of src image to be drawn to screen
  src.loadPixels();
  for(int i=0; i<w; i++){
    for(int j=0; j<h; j++){
      // draw the densities to the screen in grayscale
      float temp = (densityTable.getFloat(j,i) - minDensity)
                    *255.0/(maxDensity-minDensity);
      src.pixels[j*w+i] = color(temp);
    }
  }
  
  //////////////////
  // OPENCV STUFF //
  ////////////////// 
  opencv = new OpenCV(this, src);
  opencv.threshold(thresh);          // threshold density map
  dst = opencv.getOutput();          // output thresholded image
  contours = opencv.findContours();  // find contours in image
  println("found " + contours.size() + " contours");
  
  // loop across all contours
  for(int m=0; m<contours.size(); m++){
    // add new row in output table for each contour
    flowerTable.addRow();
    
    // get all points that make up contour
    points = contours.get(m).getPoints();
    
    // calculate average coordinates (center of contour)
    int sumX = 0;
    int sumY = 0;
    int avgX, avgY;
    for(int n=0; n<points.size(); n++){
      sumX += points.get(n).x;
      sumY += points.get(n).y;
    }
    avgX = (int)(sumX/points.size());
    avgY = (int)(sumY/points.size());
    //println(avgX + ", " + avgY);
    
    dst.loadPixels();
    // get the area of the bounding box
    Rectangle rect = contours.get(m).getBoundingBox();
    int sumPixels = 0;
    for(int i=rect.x; i<rect.x+rect.width; i++){
      for(int j=rect.y; j<rect.y+rect.height; j++){
        //color c = dst.pixels[j*w+i]
        if(brightness(dst.pixels[j*w+i]) > 50){
          sumPixels++;
        }
      }
    }
    //println(sumPixels);
    
    // save the coordinates and size of flower to output table
    flowerTable.setInt(m+1,0,avgX);
    flowerTable.setInt(m+1,1,avgY);
    flowerTable.setInt(m+1,2,(int)contours.get(m).area());
    flowerTable.setInt(m+1,3,sumPixels);
    
    points.clear();
  }
  // save the table to be accessed by simulation
  saveTable(flowerTable, "flowers.csv");
  size(w,h);
}

void draw(){
  // draw the thresholded image to the screen
  image(dst,0,0);
  fill(255,0,0,160);
  stroke(200,0,0);
  for(int i=0; i<flowerTable.getRowCount(); i++){
      ellipse(flowerTable.getInt(i,0),flowerTable.getInt(i,1),
              (int)sqrt(flowerTable.getFloat(i,2)),
              (int)sqrt(flowerTable.getFloat(i,2)));
  }
}
