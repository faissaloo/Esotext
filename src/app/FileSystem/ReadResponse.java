package filesystem;

public class ReadResponse {
  byte[] data;
  public ReadResponse(byte[] data) {
    this.data = data;
  }
  
  public byte[] data() {
    return data;
  }
}
