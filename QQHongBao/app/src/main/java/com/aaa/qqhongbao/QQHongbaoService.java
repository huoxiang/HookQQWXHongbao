package com.aaa.qqhongbao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/3/27.
 */
public class QQHongbaoService extends AccessibilityService {
    private static final String TAG = "QQHongbaoService";
    private static final String WECHAT_OPEN_EN = "Open";
    private static final String WECHAT_OPENED_EN = "You've opened";
    private final static String QQ_DEFAULT_CLICK_OPEN = "点击拆开";
    private final static String QQ_HONG_BAO_PASSWORD = "口令红包";
    private final static String QQ_HONG_BAO_TYPE_1 = "恭喜发财";
    private final static String QQ_CLICK_TO_PASTE_PASSWORD = "点击输入口令";
    private boolean mLuckyMoneyReceived;
    private String lastFetchedHongbaoId = null;
    private long lastFetchedTime = 0;
    private static final int MAX_CACHE_TOLERANCE = 5000;
    private AccessibilityNodeInfo rootNodeInfo;
    private List<AccessibilityNodeInfo> mReceiveNode;
    private Boolean needback = false;

    private String pkgNmae = null;
    private static final String weixin_pkg_name = "com.tencent.mm";
    private static final String qq_pkg_name = "com.tencent.mobileqq";

    @Override
    public void onCreate() {

        //android.d

        super.onCreate();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void recycle(AccessibilityNodeInfo info) {
        Log.d(TAG, "解析红包info.getChildCount()=" + info.getChildCount());
        if (info.getChildCount() == 0) {      /*这个if代码的作用是：匹配“点击输入口令的节点，并点击这个节点”*/
            if (info.getText() != null) {
                String HongBaotag = info.getText().toString();
                Log.d(TAG, "解析红包HongBaotag=" + HongBaotag);
                if ("领取红包".equals(HongBaotag)) {
                    Log.d(TAG, "包含领取红包字符");
                    //这里有一个问题需要注意，就是需要找到一个可以点击的View
                    Log.i(TAG, "Click" + ",isClickable:" + info.isClickable());
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null) {
                        Log.i("demo", "parent isClick:" + parent.isChecked());
                        if (parent.isClickable()) {

                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }
                } else if (HongBaotag.equals(QQ_CLICK_TO_PASTE_PASSWORD)) {
                    info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //android123提示如果是服务里调用，必须加入new task标识
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                } else if (info.getClassName().toString().equals("android.widget.Button") && HongBaotag.equals("发送")) {
                     /*这个if代码的作用是：匹配文本编辑框后面的发送按钮，并点击发送口令*/
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                Intent intent = new Intent(Intent.ACTION_MAIN);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //android123提示如果是服务里调用，必须加入new task标识
//                intent.addCategory(Intent.CATEGORY_HOME);
//                startActivity(intent);
                } else if (info.getPackageName().toString().equals(weixin_pkg_name) && info.getClassName().toString().equals("android.widget.Button") && HongBaotag.equals("")) {
                     /*这个if代码的作用是：匹配文本编辑框后面的发送按钮，并点击发送口令*/
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                Intent intent = new Intent(Intent.ACTION_MAIN);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //android123提示如果是服务里调用，必须加入new task标识
//                intent.addCategory(Intent.CATEGORY_HOME);
//                startActivity(intent);
                }
            }


        } else {
            int size = info.getChildCount();
            for (int i = size - 1; i > -1; i--) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void recycle2(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {      /*这个if代码的作用是：匹配“点击输入口令的节点，并点击这个节点”*/
            if (info.getText() == null) {
                if (info.getPackageName().toString().equals(weixin_pkg_name) && info.getClassName().toString().equals("android.widget.Button")) {
                     /*这个if代码的作用是：匹配文本编辑框后面的发送按钮，并点击发送口令*/
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle2(info.getChild(i));
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        Log.d(TAG, "onAccessibilityEvent start");
        this.rootNodeInfo = event.getSource();

        //通知栏截取事件
        getNotify(event);

        if (rootNodeInfo == null) {
            return;
        }
        mReceiveNode = null;
        checkNodeInfo();      /* 如果已经接收到红包并且还没有戳开 */
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void clickHongBao() {
        if (mLuckyMoneyReceived && (mReceiveNode != null)) {
            int size = mReceiveNode.size();
            if (size > 0) {
                String id = getHongbaoText(mReceiveNode.get(size - 1));
                long now = System.currentTimeMillis();
                if (this.shouldReturn(id, now - lastFetchedTime)) return;
                lastFetchedHongbaoId = id;
                lastFetchedTime = now;
                AccessibilityNodeInfo cellNode = mReceiveNode.get(size - 1);
                if (cellNode.getPackageName().toString().equals(weixin_pkg_name)) {
                    return;
                }

                if (cellNode.getText().toString().equals("口令红包已拆开")) {
                    return;
                }
                if (cellNode.getText().toString().equals("你已领取")) {
                    return;
                }
                if (cellNode.getText().toString().equals("你领取了")) {
                    return;
                }
                if (cellNode.getText().toString().equals("已拆开")) {
                    return;
                }
                if (cellNode.getText().toString().equals("发送给好友")) {
                    return;
                }
                cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (cellNode.getText().toString().equals(QQ_HONG_BAO_PASSWORD)) {
                    AccessibilityNodeInfo rowNode = getRootInActiveWindow();
                    if (rowNode == null) {
                        Log.e(TAG, "noteInfo is　null");
                        return;
                    } else {
                        recycle(rowNode);
                    }
                }
                mLuckyMoneyReceived = false;
                //抢完红包设置返回
                needback = true;
            }
        }
    }

    private void getNotify(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {

            //第一步：监听通知栏消息
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// 通知栏事件
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    Log.d(TAG, "onAccessibilityEvent texts=" + texts);
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (content.contains("[微信红包]") || content.contains("QQ红包")) {
                            // 监听到QQ红包的notification，打开通知
                            if (event.getParcelableData() != null
                                    && event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event
                                        .getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pkgNmae = event.getPackageName().toString();
                                    pendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "getNotify TYPE_NOTIFICATION_STATE_CHANGED");
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION:
                Log.d(TAG, "getNotify CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION");
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE:
                Log.d(TAG, "getNotify CONTENT_CHANGE_TYPE_SUBTREE");
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT:
                Log.d(TAG, "getNotify CONTENT_CHANGE_TYPE_TEXT");
                break;
            case AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED:
                Log.d(TAG, "getNotify CONTENT_CHANGE_TYPE_UNDEFINED");
                break;
            case AccessibilityEvent.INVALID_POSITION:
                Log.d(TAG, "getNotify INVALID_POSITION");
                break;
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                Log.d(TAG, "getNotify TYPE_ANNOUNCEMENT");
                break;
            case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT:
                Log.d(TAG, "getNotify TYPE_ASSIST_READING_CONTEXT");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                Log.d(TAG, "getNotify TYPE_GESTURE_DETECTION_END");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                Log.d(TAG, "getNotify TYPE_GESTURE_DETECTION_START");
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                Log.d(TAG, "getNotify TYPE_TOUCH_EXPLORATION_GESTURE_END");
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                Log.d(TAG, "getNotify TYPE_TOUCH_EXPLORATION_GESTURE_START");
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                Log.d(TAG, "getNotify TYPE_TOUCH_INTERACTION_END");
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                Log.d(TAG, "getNotify TYPE_TOUCH_INTERACTION_START");
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                Log.d(TAG, "getNotify TYPE_WINDOWS_CHANGED");
                break;
            //第二步：监听是否进入微信红包消息界面
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.d(TAG, "监听是否进入微信红包消息界面");

                String className = event.getClassName().toString();

                //QQ聊天框
                if (className.equals("com.tencent.mobileqq.activity.SplashActivity")) {
                    //开始抢红包
                    Log.d(TAG, "开始抢QQ红包SplashActivity");
                    getPacket();
                }
                if (pkgNmae != null && pkgNmae.equals(weixin_pkg_name) && className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //开始抢红包
                    pkgNmae = null;
                    Log.d(TAG, "开始抢QQ红包LauncherUI");
                    getPacket();
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    //开始打开红包
                    Log.d(TAG, "开始抢QQ红包LuckyMoneyReceiveUI");
                    openPacket();
                }
                break;

//            if (needback) {
//                    needback = false;
//                    List<CharSequence> texts2 = event.getText();
//                    if (!texts2.isEmpty()) {
//                        Log.d(TAG, "onAccessibilityEvent texts=" + texts2);
//                        for (CharSequence text : texts2) {
//                            String content = text.toString();
//                            if (content.contains("已存入余额")) {
            // 监听到自动抢红包消息
//                                if (event.getParcelableData() != null
//                                        && event.getParcelableData() instanceof Notification) {
//                                    Notification notification = (Notification) event
//                                            .getParcelableData();
//                                    PendingIntent pendingIntent = notification.contentIntent;
//                                    try {
//                                        pendingIntent.send();
//                                    } catch (PendingIntent.CanceledException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                        }
//                }
//                break;
            default:
                Log.d(TAG, "getNotify eventType=" + eventType);
                break;
        }
    }

    //过滤红包
    private void checkNodeInfo() {
        if (rootNodeInfo == null) {
            return;
        }
        /* 聊天会话窗口，遍历节点匹配“点击拆开”，“口令红包”，“点击输入口令” */
        List<AccessibilityNodeInfo> nodes1 = this.findAccessibilityNodeInfosByTexts(this.rootNodeInfo,
                new String[]{QQ_HONG_BAO_TYPE_1,
                        QQ_DEFAULT_CLICK_OPEN,
                        QQ_HONG_BAO_PASSWORD,
                        QQ_CLICK_TO_PASTE_PASSWORD,
                        "发送"});
        if (!nodes1.isEmpty()) {
            String nodeId = Integer.toHexString(System.identityHashCode(this.rootNodeInfo));
            if (!nodeId.equals(lastFetchedHongbaoId)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = nodes1;
                clickHongBao();
            }
            return;
        }
    }

    private String getHongbaoText(AccessibilityNodeInfo node) {    /* 获取红包上的文本 */
        String content;
        try {
            AccessibilityNodeInfo i = node.getParent().getChild(0);
            content = i.getText().toString();
        } catch (NullPointerException npe) {
            return null;
        }
        return content;
    }

    private boolean shouldReturn(String id, long duration) {
        // ID为空
        if (id == null) return true;
        // 名称和缓存不一致
        if (duration < MAX_CACHE_TOLERANCE && id.equals(lastFetchedHongbaoId)) {
            return true;
        }
        return false;
    }

    private List<AccessibilityNodeInfo> findAccessibilityNodeInfosByTexts(AccessibilityNodeInfo nodeInfo, String[] texts) {
        for (String text : texts) {
            if (text == null) continue;
            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(text);
            if (!nodes.isEmpty()) {
                if (text.equals(WECHAT_OPEN_EN) && !nodeInfo.findAccessibilityNodeInfosByText(WECHAT_OPENED_EN).isEmpty()) {
                    continue;
                }
                return nodes;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void onInterrupt() {
    }

    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
    }

    @SuppressLint("NewApi")
    private void openPacket() {

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle2(rootNode);

//        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
//        if (nodeInfo != null) {
//            //com.tencent.mm:id/b_b
////            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("b_b");
////               //     .findAccessibilityNodeInfosByText("拆红包");
////            for (AccessibilityNodeInfo n : list) {
////                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
////            }
//            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
//                if (nodeInfo.getChild(i) != null) {
//                    if (nodeInfo.getChildCount() == 0) {
//                        if (nodeInfo.getPackageName().toString().equals(weixin_pkg_name) && nodeInfo.getClassName().toString().equals("android.widget.Button")) {
//                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        }
//                    }
//
//                }
//            }
//        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.packageNames = new String[]{"com.tencent.mm", "com.tencent.mobileqq"};
        setServiceInfo(info);

        super.onServiceConnected();
    }
}