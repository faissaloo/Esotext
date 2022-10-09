package filesystem;

public class ReadCommand {
  ReadCallback callback;
  public ReadCommand(ReadCallback callback) {
    this.callback = callback;
  }
  
  public void callback(ReadResponse response) {
    callback.execute(response);
  }
}
