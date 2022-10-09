package filesystem;

public class WriteCommand {
  byte[] data;
  boolean create;
  public WriteCommand(boolean create, byte[] data) {
    this.data = data;
    this.create = create;
  }
  
  public byte[] data() {
    return data;
  }
  
  public boolean create() {
    return create;
  }
}
