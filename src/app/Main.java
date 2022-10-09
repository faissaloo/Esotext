/*
 *
 * Copyright (c) 2007, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package app;

import javax.microedition.io.*;
import javax.microedition.io.file.*;
import java.lang.SecurityException;
import java.io.IOException;
import javax.microedition.lcdui.*;
import java.io.*;
import java.util.*;
import javax.microedition.midlet.*;

import filesystem.AppStorageReaderThread;
import filesystem.AppStorageWriterThread;
import filesystem.ReadResponse;
import filesystem.ReadCommand;
import filesystem.WriteCommand;
import filesystem.ReadCallback;
import filesystem.FailedAccessCallback;
import filesystem.FailedPermissionCallback;

public class Main extends MIDlet implements CommandListener {
    private static final Command CMD_EXIT = new Command("Exit", Command.EXIT, 1);
    private static final Command CMD_BACK = new Command("Back", Command.BACK, 1);
    private static final Command CMD_SAVE = new Command("Save", Command.OK, 1);
    private static final Command CMD_OPEN = new Command("Open", Command.OK, 1);
    private static final Command CMD_NEW = new Command("Create/Overwrite", Command.OK, 1);

    private Display display;
    TextField passwordField;
    TextBox notesEntry;
    AppStorageWriterThread appStorageWriter;
    AppStorageReaderThread appStorageReader;
    
    public Main() {
        display = Display.getDisplay(this);
    }

    protected void startApp() {
      //showRoots();
      
      appStorageWriter = new AppStorageWriterThread("notes.etxt");
      appStorageReader = new AppStorageReaderThread("notes.etxt");
      appStorageWriter.start();
      appStorageReader.start();
      appStorageReader.attachFailedAccessCallback(new AccessFailed());
      appStorageWriter.attachFailedAccessCallback(new AccessFailed());
      appStorageReader.attachFailedPermissionCallback(new PermissionFailed());
      appStorageWriter.attachFailedPermissionCallback(new PermissionFailed());
      
      showUnlockForm();
    }
    
    public class PermissionFailed implements FailedPermissionCallback {
      public void execute() {
        showPermissionDenied();
      }
    }
    
    public class AccessFailed implements FailedAccessCallback {
      public void execute() {
        showAccessFailure();
      }
    }
    
    void showRoots() {
      Enumeration drives = FileSystemRegistry.listRoots();
      Form newForm = new Form("Device roots");
      while (drives.hasMoreElements()) {
        newForm.append(new StringItem("DEVICE:", (String) drives.nextElement()));
      } 
      newForm.addCommand(CMD_EXIT);
      newForm.setCommandListener(this);
      display.setCurrent(newForm);
    }
    
    void showUnlockForm() {
      Form newForm = new Form("Unlock notes");

      passwordField = new TextField("Enter password", "", 50, TextField.PASSWORD | TextField.SENSITIVE | TextField.NON_PREDICTIVE);
      newForm.addCommand(CMD_OPEN);
      newForm.addCommand(CMD_NEW);
      newForm.addCommand(CMD_EXIT);
      newForm.setCommandListener(this);
      
      newForm.append(passwordField);

      display.setCurrent(newForm);
    }
    
    void showAccessFailure() {
      Form newForm = new Form("");
      newForm.append(new StringItem("Unable to access notes. Make sure to create the file", ""));
      newForm.append(new StringItem("", "sad!"));
      
      newForm.addCommand(CMD_BACK);
      newForm.setCommandListener(this);
      display.setCurrent(newForm);
    }
    
    void showIncorrectPassword() {
      Form newForm = new Form("");
      newForm.append(new StringItem("Incorrect password!", ""));
      newForm.append(new StringItem("", "Where r ur brains, in ur ass???"));
      
      newForm.addCommand(CMD_BACK);
      newForm.setCommandListener(this);
      display.setCurrent(newForm);
    }
    
    void showCorruptedFile() {
      Form newForm = new Form("");
      newForm.append(new StringItem("File corrupted!", ""));
      newForm.append(new StringItem("", "That's gotta sting..."));
      
      newForm.addCommand(CMD_BACK);
      newForm.setCommandListener(this);
      display.setCurrent(newForm);
    }
    
    void showPermissionDenied() {
      Form newForm = new Form("");
      newForm.append(new StringItem("File access permission denied!", ""));
      newForm.append(new StringItem("", "You weren't supposed to do that"));
      
      newForm.addCommand(CMD_EXIT);
      newForm.setCommandListener(this);
      display.setCurrent(newForm);
    }
    
    void showCreated() {
      Form newForm = new Form("");
      newForm.append(new StringItem("Notes file cleared and overwritten with new password", ""));
      newForm.append(new StringItem("", "Sometimes you just need to burn it all down and start again"));
      
      newForm.addCommand(CMD_BACK);
      newForm.setCommandListener(this);
      display.setCurrent(newForm);
    }
    
    void showEntryForm(String plaintext) {      
      notesEntry = new TextBox("Notes", plaintext, 65535, TextField.SENSITIVE | TextField.NON_PREDICTIVE | TextField.ANY);

      notesEntry.addCommand(CMD_BACK);
      notesEntry.addCommand(CMD_SAVE);
      notesEntry.setCommandListener(this);
      
      display.setCurrent(notesEntry);
    }

    protected void destroyApp(boolean unconditional) {
    }

    protected void pauseApp() {
    }
    
    public class EncryptedDataLoaded implements ReadCallback {
      public void execute(ReadResponse response) {
        try {
          showEntryForm(
            EncryptedTextContainer.Decrypt(
              passwordField.getString(),
              response.data()
            )
          );
        } catch (EncryptedTextContainer.PasswordException e) {
          showIncorrectPassword();
        } catch (EncryptedTextContainer.InvalidFileException e) {
          showCorruptedFile();
        }
      }
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == CMD_EXIT) {
          destroyApp(false);
          notifyDestroyed();
        } else if (c == CMD_NEW) {
          byte[] data = EncryptedTextContainer.Encrypt(
            passwordField.getString(),
            ""
          );
          appStorageWriter.write(new WriteCommand(
            true,
            data
          ));
          showCreated();
        } else if (c == CMD_OPEN) {
          appStorageReader.read(new ReadCommand(new EncryptedDataLoaded()));
        } else if (c == CMD_BACK) {
          showUnlockForm();
        } else if (c == CMD_SAVE) {
          appStorageWriter.write(new WriteCommand(
            false,
            EncryptedTextContainer.Encrypt(
              passwordField.getString(),
              notesEntry.getString()
            )
          ));
          
          showUnlockForm();
        }
    }
}
