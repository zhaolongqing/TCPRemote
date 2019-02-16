package zlq.com.onlinecrash.service;

import android.content.Context;

import zlq.com.onlinecrash.data.TcpClient;

/**
 * author: ZlqPC
 * created on: 2019/2/15 16:24
 * description:
 */
public interface ClientServiceListener extends TcpClient.NioResponse {

    void unbind(Context context);

}
