/////////////////////////////////////////
//  Calculates the size and locations  //
//  of flowers given a density map     //
/////////////////////////////////////////

import gab.opencv.*;
import java.awt.Rectangle;
OpenCV opencv;

//GLOBALS
Table densityTable;
Table flowerTable;
PImage src;
PImage dst;
ArrayList<Contour> contours;
ArrayList<PVector> points;
float maxDensity = -Float.MAX_VALUE;
float minDensity = Float.MAX_VALUE;
color red = color(255,0,0);
BufferedReader reader;
String line;
int w, h;

//PARAMETERS
int thresh = 237;  // threshold for flowers (out of 255)

void setup() {
  
  // create new table for storing densities
  densityTable = new Table();
  
  // create new table for storing flower info
  flowerTable = new Table();
  for(int i=0; i<4; i++)
    flowerTable.addColumn();

  // Open the density file
  reader = createReader("densities.txt");  

  try {
    line = reader.readLine();
  } catch (IOException e) {
    e.printStackTrace();
    line = null;
  }
  if (line == null) {
    println("EMPTY OR NONEXISTENT FILE");
  }
  else {
    String[] pieces = split(line, ',');
    println(pieces.length - 2);
    
    w = Integer.parseInt(pieces[0].substring(1));
    h = Integer.parseInt(pieces[1].substring(1));
    
    for(int j=0; j<h; j++){
      densityTable.addRow();
      
      for(int i=0; i<w; i++){
        densityTable.addColumn();
        if(j == h-1 && i == w-1){
          densityTable.setFloat(j,i,Float.parseFloat(pieces[i+j*w+2].substring(1,19)));
        }else{
          densityTable.setFloat(j,i,Float.parseFloat(pieces[i+j*w+2].substring(1)));
        }
      }
    }
  saveTable(densityTable, "new.csv");
  }
}
 
void draw() {
  
} 
