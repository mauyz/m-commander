/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mg.eight.mplayer.task;

import android.content.SharedPreferences;

import java.io.IOException;
import java.net.Socket;

import androidx.preference.PreferenceManager;

/**
 * @author Mauyz
 */

public class SocketManager implements Runnable {

    private final AppService service;
    private Socket socket, downloadSocket;
    private SharedPreferences preferences;
    private boolean stoppable = false;

    public SocketManager(AppService service) {
        this.service = service;
        preferences = PreferenceManager.getDefaultSharedPreferences(service.getApplicationContext());
    }

    @Override
    public void run() {
        while (!stoppable) {
            String ip = preferences.getString("ipServer", "192.168.56.1");
            int port = Integer.valueOf(preferences.getString("port", "9999"));
            try {
                socket = new Socket(ip, port);
                socket.setSoTimeout(6000);
                downloadSocket = new Socket(ip, port+1);
                MessageSender sender = new MessageSender(socket);
                SocketDownloadManager downloadManager = new SocketDownloadManager(service,downloadSocket);
                service.setRequestManager(sender,downloadManager);
                new Thread(new MessageReceiver(service, socket)).start();
                new Thread(sender).start();
                new Thread(downloadManager).start();
                break;
            } catch (IOException e) {
            }
        }
    }

    public void startHandler() {
        stoppable = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
        if (downloadSocket != null) {
            try {
                downloadSocket.close();
            } catch (IOException e) {
            }
            downloadSocket = null;
        }
        new Thread(this).start();
    }

    public void stopHandler() {
        stoppable = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
        if (downloadSocket != null) {
            try {
                downloadSocket.close();
            } catch (IOException e) {
            }
            downloadSocket = null;
        }
    }
}
