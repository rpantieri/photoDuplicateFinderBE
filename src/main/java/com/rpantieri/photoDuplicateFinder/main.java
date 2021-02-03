package com.rpantieri.photoDuplicateFinder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.rpantieri.photoDuplicateFinder.bo.ImageScanner;

public class main {

    public static void main(String args[]) throws FileNotFoundException, IOException {
        String s = "F:\\GDrive\\Photos\\Alessandro\\01_Marzo 2015\\20150321_091932.jpg";
        String out = "C:\\Users\\rpantieri\\Downloads\\01.jpg";
        byte[] bb = ImageScanner.createThumbnail(s, 200);
        FileOutputStream so = new FileOutputStream(out);
        so.write(bb);
        so.flush();
        so.close();
    }
}
