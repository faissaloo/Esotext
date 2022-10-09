package filesystem;

import javax.microedition.io.*;
import javax.microedition.io.file.*;
import java.lang.SecurityException;
import java.io.IOException;
import javax.microedition.lcdui.*;
import java.io.*;
import java.util.*;
import javax.microedition.midlet.*;

import support.Queue;
import support.ByteArrayPlus;

public class AppStorageWriterThread extends Thread {
  Queue writeQueue = new Queue();
  Queue failedAccessCallbacks = new Queue();
  Queue failedPermissionCallbacks = new Queue();

  String filename;
  
  public synchronized void write(WriteCommand command) {
    writeQueue.enqueue(command);
    notify();
  }

  public void attachFailedAccessCallback(FailedAccessCallback callback) {
    failedAccessCallbacks.enqueue(callback);
  }
  
  public void attachFailedPermissionCallback(FailedPermissionCallback callback) {
    failedPermissionCallbacks.enqueue(callback);
  }
  
  String noteLocation() throws IOException {
    //fileconn.dir.memorycard is not even slightly reliable and often returns mangled stuff, so we just dump it in the last root found
    StringBuffer sb = new StringBuffer();

    sb.append("file:///");
    Enumeration drives = FileSystemRegistry.listRoots();
    String lastRoot = "";
    while (drives.hasMoreElements()) {
      lastRoot = (String) drives.nextElement();
    }
    sb.append(lastRoot);
    sb.append(filename);
    return sb.toString();
  }
  
  public AppStorageWriterThread(String filename) {
    this.filename = filename;
  }
  
  public synchronized void run() {
    while (true) {
      if (writeQueue.length() > 0) {
        try {
          String location = noteLocation();
          FileConnection file = (FileConnection) Connector.open(location, Connector.READ_WRITE);
          
          WriteCommand writeCommand = (WriteCommand) writeQueue.dequeue();
          if (writeCommand.create() && !file.exists()) {
            file.create();
          }
          
          OutputStream writeStream = file.openOutputStream();
          writeStream.write(writeCommand.data());
          writeStream.close();
          file.close();
        } catch (SecurityException e) {
          for (Enumeration callbacks = failedPermissionCallbacks.elements(); callbacks.hasMoreElements();) {
            ((FailedPermissionCallback)callbacks.nextElement()).execute();
          }
        } catch (IOException e) {
          for (Enumeration callbacks = failedAccessCallbacks.elements(); callbacks.hasMoreElements();) {
            ((FailedAccessCallback)callbacks.nextElement()).execute();
          }
        }
      }
      
      try {
        wait();
      } catch (InterruptedException e) {
        break;
      }
    }
  }
}
