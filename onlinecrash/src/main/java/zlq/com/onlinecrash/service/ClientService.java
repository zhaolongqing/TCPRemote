package zlq.com.onlinecrash.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import zlq.com.onlinecrash.data.TcpClient;

/**
 * author: ZlqPC
 * created on: 2019/2/15 13:06
 * description:
 */
public class ClientService extends Service {

    private ClientBinder binder;

    public Intent startService(Context context, String ip, int port) {
        Intent intent = new Intent(context, ClientService.class);
        intent.putExtra("ip", ip);
        intent.putExtra("port", port);
        context.startService(intent);
        return intent;
    }

    public void stopService(Context context, Intent intent) {
        context.stopService(intent);
    }

    public void startClient(String ip, int port) {
        if (ip != null && port != 0) {
            TcpClient.getInstance().startResponse(binder.listener);
            TcpClient.getInstance().startClient(ip, port);
        }
    }


    public void sendMessage(String message) {
        TcpClient.getInstance().sendMessage(message);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String ip = intent.getStringExtra("ip");
        int port = intent.getIntExtra("port", 0);
        startClient(ip, port);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        binder = new ClientBinder();
        return binder;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        TcpClient.getInstance().release();
    }

    public class ClientBinder extends Binder {


        private TcpClient.NioResponse listener;

        public ClientService getService(TcpClient.NioResponse listener) {
            this.listener = listener;
            return ClientService.this;
        }

        public ClientService getService(String ip, int port, String deviceId, TcpClient.NioResponse listener) {
            this.listener = listener;
            startClient(ip, port);
            return ClientService.this;
        }

    }


}
