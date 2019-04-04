import com.google.protobuf.InvalidProtocolBufferException;
import mondis.Mondis;

import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public class SimpleClient {
    public static void main (String[] args) {
        SimpleClient client = new SimpleClient();
        client.start();
    }
    SocketChannel socketChannel;
    ByteBuffer nextDataLenBuffer = null;
    ByteBuffer halfPacketBuffer = null;
    int nextMessageLen = 0;
    Queue<Mondis.Message> recvMsgs = new LinkedBlockingDeque<>();
    public static byte[] intToBytes(int n){
        byte[] b = new byte[4];

        for(int i = 0;i < 4;i++)
        {
            b[i]=(byte)(n>>(24-i*8));

        }
        return b;
    }

    public static int bytesToInt(byte[] bytes){
        int res = 0;

        for(int i = 0;i < 4;i++)
        {
            res |= (bytes[i]<<(24-i*8));
        }
        return res;
    }

    void writeMessage(Mondis.Message msg) {
        String data = new String(msg.toByteArray());
        int len = data.length();
        byte[] dataLenBytes = intToBytes(len);
        int hasWrite = 0;
        int writed = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap((new String(dataLenBytes)+data).getBytes());
        try {
            socketChannel.write(byteBuffer);
        }catch (IOException e) {

        }
    }

    void readMessage() {
        while (true) {
            int recved = 0;
            if (nextDataLenBuffer == null) {
                nextDataLenBuffer = ByteBuffer.allocate(4);
            }
            if (nextDataLenBuffer != null) {
                while (nextDataLenBuffer.position() < 3) {
                    try {
                        recved = socketChannel.read(nextDataLenBuffer);
                    }catch (IOException e){

                    }
                    if (recved == 0) {
                        return;
                    }
                }
                nextMessageLen = bytesToInt(nextDataLenBuffer.array());
            }
            if (halfPacketBuffer == null) {
                halfPacketBuffer = ByteBuffer.allocate(nextMessageLen);
            }
            while (halfPacketBuffer.position() < nextMessageLen-1) {
                try {
                    recved = socketChannel.read(halfPacketBuffer);
                }catch (IOException e) {

                }
                if (recved == 0) {
                    return;
                }
            }
            Mondis.Message nextMsg = null;
            try {
                nextMsg = Mondis.Message.parseFrom(halfPacketBuffer.array());
            }catch (InvalidProtocolBufferException e) {

            }
            recvMsgs.add(nextMsg);
            nextMessageLen = 0;
            halfPacketBuffer = null;
            nextDataLenBuffer = null;
        }
    }

    Mondis.Message nextMessage() {
        readMessage();
        if (recvMsgs.isEmpty()) {
            return null;
        }
        return recvMsgs.poll();
    }
    public void start() {
        try {
            socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",6379));
            socketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            socketChannel.register(selector,SelectionKey.OP_READ);
            Mondis.Message.Builder mb = Mondis.Message.newBuilder();
            mb.setMsgType(Mondis.MsgType.COMMAND);
            mb.setCommandType(Mondis.CommandType.CLIENT_COMMAND);
            mb.setContent("NEW_CLIENT");
            writeMessage(mb.build());
            Scanner scanner = new Scanner(System.in);
            new Thread(){
                @Override
                public void run() {
                    while (true) {
                        try {
                            selector.select();
                            Set<SelectionKey> keys = selector.selectedKeys();
                            Iterator<SelectionKey> iterator = keys.iterator();
                            while (iterator.hasNext()) {
                                SelectionKey key = iterator.next();
                                if (key.isReadable()) {
                                    Mondis.Message message = null;
                                    while ((message = nextMessage()) != null) {
                                        if (message.getMsgType() == Mondis.MsgType.EXEC_RES) {
                                            System.out.println(message.getContent());
                                        }
                                    }
                                }
                            }
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
            while (true) {
                System.out.println("Mondis>");
                Mondis.Message.Builder builder = Mondis.Message.newBuilder();
                builder.setMsgType(Mondis.MsgType.COMMAND);
                builder.setCommandType(Mondis.CommandType.CLIENT_COMMAND);
                builder.setContent(scanner.nextLine());
                writeMessage(builder.build());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
