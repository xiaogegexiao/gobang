package cn.m.xys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GoodActivity extends Activity {
    
    public static final int REQUEST_GAME = 1;
    
    public static final int REPLY_ACCEPT = 2;
    public static final int CHESS_MOVE = 3;
    
    private static final int MSG_NEW_GAME_REQUEST = 0;
    private static final int MSG_NEW_GAME_ACCEPT = 1;
    
    private static final String TAG = "GoodActivity";
    
    private LayoutInflater mInflater = null;
    
    private LinearLayout optionLayout = null;
    private RelativeLayout ipConnectBtn = null;
    private RelativeLayout quitBtn = null;
    private View ipConnectView = null;
    private LinearLayout gameLayout = null;
    private TextView tvSelfIp = null;
    
    private Resources mResources = null;
    
    private ServerSocket server = null;
    private boolean isStop = true;
    
    private boolean nowInChess = false;
    
    private static Hashtable<String, Socket> receivedSocks = new Hashtable<String, Socket>();
    private static Socket clientSock = null;
    
    private GameView gameView = null;
    private int chessViewWidth = 0;
    private int chessViewHeight = 0;
    
    public static final int PROXY_PORT = 2425;
    
    private InetAddress localaddr;
    
    private AlertDialog mRequestDialog = null;
    
    Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_NEW_GAME_REQUEST:
                AlertDialog.Builder builder = new AlertDialog.Builder(GoodActivity.this);
                Bundle info = msg.getData();
                final String ipaddress = info.getString("ipaddress");
                builder.setTitle(mResources.getString(R.string.invite_game_param, ipaddress));
                builder.setNegativeButton(R.string.reject_game, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Socket sock = receivedSocks.get(ipaddress);
                        closeSocket(sock);
                        sock = null;
                        receivedSocks.remove(ipaddress);
                    }
                });
                builder.setPositiveButton(R.string.receive_game, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Socket sock = receivedSocks.get(ipaddress);
                        try {
                            OutputStream os = sock.getOutputStream();
                            os.write(REPLY_ACCEPT);
                            os.flush();
                            GoodActivity.this.setContentView(gameLayout);
                            GoodActivity.this.gameView.setSelf(GameView.CAMP_ENEMY);
                            GoodActivity.this.gameView.setSocket(sock);
                            new AcceptChessThread(GoodActivity.this.gameView, sock).start();
                            nowInChess = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.create().show();
                break;
            case MSG_NEW_GAME_ACCEPT:
                if(mRequestDialog != null && mRequestDialog.isShowing()) {
                    mRequestDialog.dismiss();
                }
                GoodActivity.this.setContentView(gameLayout);
                GoodActivity.this.gameView.setSelf(GameView.CAMP_HERO);
                GoodActivity.this.gameView.setSocket(clientSock);
                new AcceptChessThread(GoodActivity.this.gameView, clientSock).start();
                nowInChess = true;
                break;
            }
        }
        
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Display display = getWindowManager().getDefaultDisplay();
        
        mResources = getResources();
        mInflater = (LayoutInflater)this.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        optionLayout = (LinearLayout)mInflater.inflate(R.layout.main, null);
        gameLayout = (LinearLayout)mInflater.inflate(R.layout.wuziqi, null);
        
        localaddr = getIpv4HostAddress();
        
        adjustOptionUIComponents(display.getWidth(), display.getHeight());
        adjustGameUIComponents(display.getWidth(), display.getHeight());
        setContentView(optionLayout);
        setListeners();
        
        startReceiveSocket();
    }
    
    public static InetAddress getIpv4HostAddress(){
        try{
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> ipAddr = intf.getInetAddresses();ipAddr.hasMoreElements();){
                    InetAddress inetAddress = ipAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4){
                        return inetAddress;
                    }
                }
            }
        } catch(SocketException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    private void adjustOptionUIComponents(int screenwidth, int screenheight) {
        optionLayout.setGravity(Gravity.CENTER);
        tvSelfIp = (TextView)optionLayout.findViewById(R.id.TVSelfip);
        tvSelfIp.setText(localaddr.getHostAddress());
        ipConnectBtn = (RelativeLayout)optionLayout.findViewById(R.id.BTNIPConnect);
        quitBtn = (RelativeLayout)optionLayout.findViewById(R.id.BTNQuit);
        LinearLayout.LayoutParams lp = null;
        lp = (LinearLayout.LayoutParams)ipConnectBtn.getLayoutParams();
        lp.leftMargin = lp.rightMargin = screenwidth * 15/100;
        lp.bottomMargin = screenheight * 20 / 100;
        lp.height = screenheight * 15 / 100;
        ipConnectBtn.setLayoutParams(lp);
        
        lp = (LinearLayout.LayoutParams)quitBtn.getLayoutParams();
        lp.leftMargin = lp.rightMargin = screenwidth *15 / 100;
        lp.height = screenheight * 15 / 100;
        quitBtn.setLayoutParams(lp);
    }
    
    private void adjustGameUIComponents(int screenwidth, int screenheight) {
        LinearLayout optionsView = (LinearLayout)gameLayout.findViewById(R.id.LLOptions);
        LinearLayout pointsView = (LinearLayout)gameLayout.findViewById(R.id.LLPoints);
        LinearLayout chessView = (LinearLayout)gameLayout.findViewById(R.id.LLChessView);
        LinearLayout.LayoutParams lp = null;
        if (screenwidth >= screenheight) {
            lp = (LinearLayout.LayoutParams)optionsView.getLayoutParams();
            lp.height = 0;
            lp = (LinearLayout.LayoutParams)pointsView.getLayoutParams();
            lp.height = 0;
            lp = (LinearLayout.LayoutParams)chessView.getLayoutParams();
            lp.width = screenwidth;
            lp.height = screenheight;
            chessViewWidth = screenwidth;
            chessViewHeight = screenheight;
            // 现实GameView
            GameView.init(this, screenwidth, screenheight);
            
        } else {
            lp = (LinearLayout.LayoutParams)chessView.getLayoutParams();
            lp.width = lp.height = screenwidth;
            lp = (LinearLayout.LayoutParams)optionsView.getLayoutParams();
            lp.width = screenwidth;
            lp.height = (screenheight - screenwidth)/3;
            lp = (LinearLayout.LayoutParams)pointsView.getLayoutParams();
            lp.width = screenwidth;
            lp.height = (screenheight - screenwidth) * 2 / 3;
            chessViewWidth = chessViewHeight = screenwidth;
            // 现实GameView
            GameView.init(this, screenwidth, screenwidth);
        }
        gameView = GameView.getInstance();
        
        chessView.removeAllViews();
        chessView.addView(gameView, chessViewWidth, chessViewHeight);
        Log.d(TAG,"chessview width ========== " + chessView.getWidth() + " , chessview height ============ " + chessView.getHeight());
    }
    
    private void setListeners() {
        ipConnectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mRequestDialog = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(GoodActivity.this);
                builder.setTitle(R.string.ipconnect_dialog_title);
                ipConnectView = mInflater.inflate(R.layout.ipconnect_alert, null);
                builder.setView(ipConnectView);
                builder.setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface arg0) {
                        mRequestDialog = null;
                    }
                });
                mRequestDialog = builder.create();
                mRequestDialog.show();
                final EditText etIp = (EditText)ipConnectView.findViewById(R.id.ETIp);
                Button btnGo = (Button)ipConnectView.findViewById(R.id.BTNGo);
                btnGo.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {
                        String ip = etIp.getText().toString();
                        Pattern p = Pattern
                                .compile("^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$");
                        Matcher ma = p.matcher(ip);
                        if (!ma.matches()) {
                            Toast.makeText(GoodActivity.this, "ip 错误", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                Socket sock = new Socket(ip, PROXY_PORT);
                                new SendThread(sock).start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
        
        quitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GoodActivity.this.finish();
            }
        });
    }
    
    private void startReceiveSocket(){
        try
        {
            server = new ServerSocket(GoodActivity.PROXY_PORT);
            isStop = false;
            new Thread(){
                public void run() {
                    while (!isStop)
                    {
                        try
                        {
                            Socket socket = server.accept();
                            new ReceiveThread(socket).start();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                            break;
                        }

                    }
                }
            }.start();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    class SendThread extends Thread {
        private Socket sock;
        private InputStream is = null;
        private OutputStream os = null;
        
        public SendThread(Socket socket) {
            sock = socket;
        }
        
        public void run() {
            try
            {
                is = sock.getInputStream();
                os = sock.getOutputStream();

                int request_type = REQUEST_GAME;
                os.write(request_type);
                os.flush();
                int reply = is.read();
                if (reply == REPLY_ACCEPT) {
                    mHandler.obtainMessage(MSG_NEW_GAME_ACCEPT).sendToTarget();
                    clientSock = sock;
                    return;
                } else {
                    throw new Exception();
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }

            try {
                if (is != null) {
                    is.close();
                }
                if (sock != null) {
                    sock.close();
                }

                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    
    class ReceiveThread extends Thread {
        
        private Socket sock;
        private InputStream is = null;
        private OutputStream os = null;
        private byte[] but = new byte[400];
        
        public ReceiveThread(Socket socket) {
            sock = socket;
        }
        
        public void run() {
            try
            {
                is = sock.getInputStream();
                os = sock.getOutputStream();

                int request_type = is.read();
                if(request_type == -1) {
                    throw new Exception();
                }
                if ((request_type & 0x01) == REQUEST_GAME) {
                    if(nowInChess) {
                        throw new Exception("now in chess!!!");
                    } else {
                        Socket deletesock = receivedSocks.get(sock.getInetAddress().getHostAddress());
                        if(deletesock != null) {
                            closeSocket(deletesock);
                        }
                        deletesock = null;
                        receivedSocks.put(sock.getInetAddress().getHostAddress(), sock);
                        Message msg = mHandler.obtainMessage(MSG_NEW_GAME_REQUEST);
                        Bundle request_info = new Bundle();
                        request_info.putString("ipaddress", sock.getInetAddress().getHostAddress());
                        msg.setData(request_info);
                        msg.sendToTarget();
                        Log.d(TAG,"show request window ============================================");
                        return;
                    }
                } else {
                    throw new Exception();
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            } 
            Log.d(TAG,
                    "receive finally close sock ======================================== ");
            try {
                if (is != null) {
                    is.close();
                }
                if (sock != null) {
                    sock.close();
                }

                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }
    
    class AcceptChessThread extends Thread {
        private GameView gameView = null;
        private Socket sock = null;
        
        public AcceptChessThread(GameView paramGameView, Socket paramSocket) {
            gameView = paramGameView;
            sock = paramSocket;
        }

        public void run() {
            byte[] buf = new byte[3];
            int request_type = 0, x_coordinate = 0, y_coordinate = 0;
            try {
                while(true) {
                    InputStream is = sock.getInputStream();
                    int length = is.read(buf);
                    if(length < 3) {
                        break;
                    }
                    request_type = buf[0];
                    x_coordinate = buf[1];
                    y_coordinate = buf[2];
                    if (request_type == CHESS_MOVE)
                        GoodActivity.this.gameView.setEnemyPosition(x_coordinate, y_coordinate);
                }    
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if(receivedSocks.size() > 0) {
            Socket s = null;
            Enumeration<Socket> enumer = receivedSocks.elements();
            while(enumer.hasMoreElements()) {
                try
                {
                    s = enumer.nextElement();
                    closeSocket(s);
                } catch (Exception e)
                {
                    e.printStackTrace();
                } finally {
                    s = null;
                }
            }
            receivedSocks.clear();
        }
        receivedSocks = null;
        super.onDestroy();
    }
    
    private void closeSocket(Socket s){
        InputStream is = null;
        OutputStream os = null;
        try {
            if (s != null) {
                is = s.getInputStream();
                os = s.getOutputStream();
            }

            if (is != null) {
                is.close();
            }
            if (s != null) {
                s.close();
            }

            if (os != null) {
                os.close();
            }
            is = null;
            os = null;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            is = null;
            s = null;
            os = null;
        }
    }
}
