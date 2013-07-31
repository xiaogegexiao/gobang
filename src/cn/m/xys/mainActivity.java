package cn.m.xys;

import android.app.Activity;
import android.app.Service;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class mainActivity extends Activity {
    
    private static final String TAG = "mainActivity";
    
    private LayoutInflater mInflater = null;
    private LinearLayout gameLayout = null;

    GameView gameView = null;
    
    private int chessViewWidth = 0;
    private int chessViewHeight = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 获取屏幕宽高
        Display display = getWindowManager().getDefaultDisplay();
        
        mInflater = (LayoutInflater)this.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        gameLayout = (LinearLayout)mInflater.inflate(R.layout.wuziqi, null);
        
        adjustUIComponents(display.getWidth(), display.getHeight());
        setContentView(gameLayout);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }
    
    private void adjustUIComponents(int screenwidth, int screenheight) {
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
}