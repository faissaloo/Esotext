package app;

import support.ByteArrayPlus;
import support.ArrayPlus;
import support.Decode;
import crypto.AES256IGE;
import crypto.SecureRandomPlus;
import crypto.SHA256;

public class EncryptedTextContainer {
  public static class PasswordException extends Exception {
    public PasswordException() {
      super("Invalid password provided");
    }
  }
  
  public static class InvalidFileException extends Exception {
    public InvalidFileException() {
      super("Corrupt or unsupported file");
    }
  }
  public static String Decrypt(String password, byte[] data) throws PasswordException, InvalidFileException {
    if (data.length < Decode.INT_LENGTH+32) {
      System.out.println("FILE TOO SMALL");
      System.out.println(data.length);
      throw new InvalidFileException();
    }
    SHA256 sha256 = new SHA256();
    byte[] key = sha256.digest(password.getBytes());
    
    int versionCode = Decode.Little.int_decode(data, 0);
    if (versionCode != 0xEC000001) {
      throw new InvalidFileException();
    }
    
    byte[] initializationVector = ArrayPlus.subarray(data, Decode.INT_LENGTH, Decode.INT_LENGTH+32);
    byte[] encryptedData = ArrayPlus.subarray(data, Decode.INT_LENGTH+32, data.length-(Decode.INT_LENGTH+32));
    
    byte[] decryptedData = AES256IGE.decrypt(key, initializationVector, encryptedData);
    int magicNumber = Decode.Little.int_decode(decryptedData, 0);
    if (magicNumber != 0xBA5ED) {
      throw new PasswordException();
    }
    int textLength = Decode.Little.int_decode(decryptedData, Decode.INT_LENGTH);
    String plaintext = new String(ArrayPlus.subarray(decryptedData, Decode.INT_LENGTH*2, textLength));
    
    return new String(plaintext);
  }
  
  public static byte[] Encrypt(String password, String plaintext) {
    SecureRandomPlus rng = new SecureRandomPlus();
    
    SHA256 sha256 = new SHA256();
    byte[] key = sha256.digest(password.getBytes());
    byte[] initializationVector = rng.nextBytes(32);
    
    
    ByteArrayPlus dataByteArray = new ByteArrayPlus();
    dataByteArray.append_int(0xBA5ED);
    dataByteArray.append_int(plaintext.length());
    dataByteArray.append_raw_bytes(plaintext.getBytes());
    dataByteArray.pad_to_alignment(16, rng);

    System.out.println(dataByteArray.toByteArray().length);
    byte[] ciphertext = AES256IGE.encrypt(key, initializationVector, dataByteArray.toByteArray());
    
    ByteArrayPlus containerByteArray = new ByteArrayPlus();
    int versionCode = 0xEC000001;
    containerByteArray.append_int(versionCode);
    containerByteArray.append_raw_bytes(initializationVector);
    containerByteArray.append_raw_bytes(ciphertext);
    
    return containerByteArray.toByteArray();
  }
}
