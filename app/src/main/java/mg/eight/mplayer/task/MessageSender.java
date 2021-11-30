/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mg.eight.mplayer.task;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import mg.eight.mplayer.model.Message;

/**
 *
 * @author Mauyz
 */

public class MessageSender implements Runnable{

    private final Socket socket;
    private final ArrayList<Message<?>> messages = new ArrayList<>();

    public MessageSender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
            while(socket != null && !socket.isClosed()){
                if(!messages.isEmpty()){
                    Sender sender = new Sender();
                    sender.start();
                    synchronized (sender) {
                        try {
                            sender.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    messages.remove(0);
                }
            }
    }

    public void addMessage(Message<?> message) {
        messages.add(message);
    }

    private class Sender extends Thread {
        @Override
        public void run() {
            synchronized (this) {
                if (socket != null && !socket.isClosed()) {
                    try {
                        ObjectOutputStream out = new ObjectOutputStream(
                                socket.getOutputStream());
                        out.writeObject(messages.get(0));
                        out.flush();
                    } catch (IOException ex) {

                    }
                }
                notify();
            }
        }
    }
}
