package zlq.com.onlinecrash.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import zlq.com.onlinecrash.data.TcpClient;

/**
 * author: ZlqPC
 * created on: 2019/2/15 13:06
 * description:发送startLog标识，接收true
 */
public class ClientService extends Service implements TcpClient.NioResponse {

    private boolean beConnect;
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

    public void startClient(String ip, int port, String deviceId) {
        if (ip != null && port != 0) {
            TcpClient.getInstance().startResponse(this);
            TcpClient.getInstance().startClient(ip, port);
            TcpClient.getInstance().sendMessage("startLog:" + deviceId);
        }
    }


    public void sendMessage(String message) {
        TcpClient.getInstance().sendMessage(message);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String ip = intent.getStringExtra("ip");
        int port = intent.getIntExtra("port", 0);
        String deviceId = intent.getStringExtra("deviceId");
        startClient(ip, port, deviceId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        binder = new ClientBinder();
        return binder;
    }

    @Override
    public void sendNoConnect() {
        Toast.makeText(this, "device not connected", Toast.LENGTH_SHORT).show();
        binder.listener.unbind();
    }

    @Override
    public void clientError() {
        Toast.makeText(this, "please check you address", Toast.LENGTH_SHORT).show();
        binder.listener.unbind();
    }

    @Override
    public void receive(String res) {
        if (!res.equals("true")) {
            binder.listener.unbind();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TcpClient.getInstance().release();
    }

    public class ClientBinder extends Binder {


        private ClientServiceListener listener;

        public ClientService getService(ClientServiceListener listener) {
            this.listener = listener;
            return ClientService.this;
        }

        public ClientService getService(String ip, int port, String deviceId, ClientServiceListener listener) {
            this.listener = listener;
            startClient(ip, port, deviceId);
            return ClientService.this;
        }

    }


}
