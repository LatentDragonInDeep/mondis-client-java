import mondis.Mondis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class SimpleClient {
    public static void main (String[] args) {
        try {
            Socket socket = new Socket("127.0.0.1",2379);
            InputStream in = socket.getInputStream();
            Mondis.Message.Builder mb = Mondis.Message.newBuilder();
            mb.setMsgType(Mondis.MsgType.COMMAND);
            mb.setCommandType(Mondis.CommandType.CLIENT_COMMAND);
            mb.setContent("NEW_CLIENT");
            OutputStream out=socket.getOutputStream();
            mb.build().writeTo(out);
            Scanner scanner = new Scanner(System.in);
            new Thread(){
                @Override
                public void run() {
                    while (true) {
                        try {
                            Mondis.Message msg = Mondis.Message.parseFrom(in);
                            if (msg.getMsgType() == Mondis.MsgType.EXEC_RES) {
                                System.out.println(msg.getContent());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
            while (true) {
                System.out.println(scanner.nextLine());
            }
        }
        catch (UnknownHostException e){

        }catch (IOException e){

        }
    }
}