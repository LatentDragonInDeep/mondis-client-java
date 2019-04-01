import mondis.Mondis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) {
        try {

            Socket socket = new Socket("127.0.0.1", 2379);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            new Thread() {
                public void run () {
                    try {
                        while (true) {
                            Mondis.Message msg = Mondis.Message.parseFrom(in);
                            if (msg.getMsgType() == Mondis.MsgType.EXEC_RES) {
                                System.out.println(msg.getContent());
                            }
                        }
                    }
                    catch (IOException e) {

                    }
                }
            }.start();
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String command = scanner.nextLine();
                Mondis.Message.Builder builder = Mondis.Message.newBuilder();
                builder.setMsgType(Mondis.MsgType.COMMAND);
                builder.setCommandType(Mondis.CommandType.CLIENT_COMMAND);
                builder.setContent(command);
                builder.build().writeTo(out);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
