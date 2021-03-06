package win.smartown.easyim.im.netease;

import android.content.Context;
import android.media.MediaMetadataRetriever;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.NIMSDK;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.mixpush.MixPushConfig;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.QueryDirectionEnum;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.netease.nimlib.sdk.util.NIMUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import win.smartown.easyim.im.base.Conversation;
import win.smartown.easyim.im.base.Group;
import win.smartown.easyim.im.base.IM;
import win.smartown.easyim.im.base.LoginListener;
import win.smartown.easyim.im.base.LogoutListener;
import win.smartown.easyim.im.base.Message;
import win.smartown.easyim.im.base.OnConversationChangedListener;
import win.smartown.easyim.im.base.OnMessageChangedListener;
import win.smartown.easyim.im.base.ProductInfo;
import win.smartown.easyim.im.base.User;
import win.smartown.easyim.im.netease.custom.ProductAttachment;
import win.smartown.easyim.im.netease.custom.ProductAttachmentParser;


/**
 * Created by smartown on 2018/2/6 11:52.
 * <br>
 * Desc:
 * <br>
 */
public class NIM extends IM {

    private Observer<List<RecentContact>> recentContactObserver;
    private RequestCallback<List<RecentContact>> recentContactCallback;

    private Observer<List<IMMessage>> receiveMessageObserver;
    private RequestCallbackWrapper<List<IMMessage>> messageCallback;

    private NIMUser user;

    public NIM(Context context) {
        super(context);
        SDKOptions options = new SDKOptions();
        options.checkManifestConfig = true;
        //推送通道配置
        MixPushConfig pushConfig = new MixPushConfig();
        pushConfig.xmAppId = context.getResources().getString(R.string.xmAppId);
        pushConfig.xmAppKey = context.getResources().getString(R.string.xmAppKey);
        pushConfig.xmCertificateName = context.getResources().getString(R.string.xmCertificateName);
        pushConfig.hwCertificateName = context.getResources().getString(R.string.hwCertificateName);
        pushConfig.mzAppId = context.getResources().getString(R.string.mzAppId);
        pushConfig.mzAppKey = context.getResources().getString(R.string.mzAppKey);
        pushConfig.mzCertificateName = context.getResources().getString(R.string.mzCertificateName);
        options.mixPushConfig = pushConfig;
        //通知栏配置
        StatusBarNotificationConfig config = new StatusBarNotificationConfig();
        config.notificationEntrance = null;
        options.statusBarNotificationConfig = config;

        NIMClient.init(this.context, null, options);
        NIMClient.toggleNotification(true);
        if (NIMUtil.isMainProcess(context)) {
            NIMSDK.getMsgService().registerCustomAttachmentParser(new ProductAttachmentParser());
        }

        initCallback();
    }

    private void initCallback() {
        recentContactObserver = new Observer<List<RecentContact>>() {
            @Override
            public void onEvent(List<RecentContact> recentContacts) {
                refreshConversations();
            }
        };
        recentContactCallback = new RequestCallback<List<RecentContact>>() {

            @Override
            public void onSuccess(List<RecentContact> param) {
                List<Conversation> conversations = Utils.getConversations(param);
                for (OnConversationChangedListener listener : onConversationChangedListeners) {
                    listener.onConversationChanged(conversations);
                }
            }

            @Override
            public void onFailed(int code) {

            }

            @Override
            public void onException(Throwable exception) {

            }
        };
        receiveMessageObserver = new Observer<List<IMMessage>>() {
            @Override
            public void onEvent(List<IMMessage> imMessages) {
                notifyMessageChanged(imMessages);
            }
        };
        messageCallback = new RequestCallbackWrapper<List<IMMessage>>() {
            @Override
            public void onResult(int code, List<IMMessage> result, Throwable exception) {
                notifyMessageChanged(result);
            }
        };
    }

    @Override
    public void login(final LoginListener listener, String... params) {
        NIMSDK.getMsgServiceObserve().observeRecentContact(recentContactObserver, false);
        NIMSDK.getMsgServiceObserve().observeReceiveMessage(receiveMessageObserver, false);
        LoginInfo loginInfo = null;
        int paramsLength = params.length;
        if (paramsLength == 2) {
            loginInfo = new LoginInfo(params[0], params[1]);
        } else if (paramsLength == 3) {
            loginInfo = new LoginInfo(params[0], params[1], params[2]);
        }
        if (loginInfo != null) {
            NIMSDK.getAuthService().login(loginInfo).setCallback(new RequestCallback<LoginInfo>() {
                @Override
                public void onSuccess(LoginInfo param) {
                    NimUserInfo userInfo = NIMSDK.getUserService().getUserInfo(param.getAccount());
                    if (userInfo != null) {
                        user = new NIMUser(userInfo);
                    }
                    NIMSDK.getUserService().fetchUserInfo(Collections.singletonList(param.getAccount())).setCallback(new RequestCallback<List<NimUserInfo>>() {
                        @Override
                        public void onSuccess(List<NimUserInfo> param) {
                            NIMSDK.getMsgServiceObserve().observeRecentContact(recentContactObserver, true);
                            NIMSDK.getMsgServiceObserve().observeReceiveMessage(receiveMessageObserver, true);
                            if (param != null && param.size() > 0) {
                                user = new NIMUser(param.get(0));
                            }
                        }

                        @Override
                        public void onFailed(int code) {

                        }

                        @Override
                        public void onException(Throwable exception) {

                        }
                    });
                    listener.onLoginSuccess(param);
                }

                @Override
                public void onFailed(int code) {
                    listener.onLoginFailed(new Exception("onFailed:" + code));
                }

                @Override
                public void onException(Throwable exception) {
                    listener.onLoginFailed(exception);
                }
            });
        }
    }

    @Override
    public void logout(LogoutListener listener, String... params) {
        super.logout(listener, params);
        NIMSDK.getMsgServiceObserve().observeRecentContact(recentContactObserver, false);
        NIMSDK.getMsgServiceObserve().observeReceiveMessage(receiveMessageObserver, false);
        NIMSDK.getAuthService().logout();
    }

    @Override
    public int getStatus() {
        return NIMClient.getStatus().getValue();
    }

    @Override
    public boolean isLogin() {
        return getStatus() == StatusCode.LOGINED.getValue();
    }

    @Override
    public User getLoginUser() {
        return user;
    }

    @Override
    public int getUnreadCount() {
        return NIMSDK.getMsgService().getTotalUnreadCount();
    }

    @Override
    public void refreshConversations() {
        NIMSDK.getMsgService().queryRecentContacts().setCallback(recentContactCallback);
    }

    @Override
    public List<Conversation> getConversations() {
        List<RecentContact> recentContacts = NIMSDK.getMsgService().queryRecentContactsBlock();
        recentContactCallback.onSuccess(recentContacts);
        return Utils.getConversations(recentContacts);
    }

    @Override
    public void removeConversation(Conversation conversation) {
        SessionTypeEnum sessionType = Utils.getSesstionType(conversation.getType());
        NIMSDK.getMsgService().clearChattingHistory(conversation.getId(), sessionType);
        NIMSDK.getMsgService().deleteRecentContact2(conversation.getId(), sessionType);
        refreshConversations();
    }

    @Override
    public void removeMessage(Message message) {
        if (message instanceof NIMMessage) {
            NIMSDK.getMsgService().deleteChattingHistory(((NIMMessage) message).getData());
        }
    }

    @Override
    public User getUser(String account) {
        NimUserInfo info = NIMSDK.getUserService().getUserInfo(account);
        return new NIMUser(info);
    }

    @Override
    public List<Group> getJoinedGroup() {
        List<Group> groups = new ArrayList<>();
        List<Team> list = NIMSDK.getTeamService().queryTeamListBlock();
        for (Team team : list) {
            groups.add(new NIMGroup(team));
        }
        return groups;
    }

    @Override
    public Group getGroup(String id) {
        Team team = NIMSDK.getTeamService().queryTeamBlock(id);
        return new NIMGroup(team);
    }

    @Override
    public void refreshMessages(String account, int type) {
        IMMessage anchor = MessageBuilder.createEmptyMessage(account, Utils.getSesstionType(type), System.currentTimeMillis());
        NIMSDK.getMsgService().queryMessageListEx(anchor, QueryDirectionEnum.QUERY_OLD, 1000, true).setCallback(messageCallback);
    }

    @Override
    public void sendTextMessage(String account, int type, String text) {
        IMMessage message = MessageBuilder.createTextMessage(account, Utils.getSesstionType(type), text);
        sendMessage(message);
    }

    @Override
    public void sendImageMessage(String account, int type, File file) {
        IMMessage message = MessageBuilder.createImageMessage(account, Utils.getSesstionType(type), file);
        sendMessage(message);
    }

    @Override
    public void sendVideoMessage(String account, int type, File file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(file.getPath());
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);//时长(毫秒)
            String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);//宽
            String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);//高
            IMMessage message = MessageBuilder.createVideoMessage(account, Utils.getSesstionType(type), file, Long.parseLong(duration), Integer.parseInt(width), Integer.parseInt(height), "");
            sendMessage(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            mmr.release();
        }
    }

    @Override
    public void sendProductMessage(String account, int type, ProductInfo productInfo) {
        IMMessage message = MessageBuilder.createCustomMessage(account, Utils.getSesstionType(type), String.format("[%s]", productInfo.getProductName()), new ProductAttachment(ProductAttachment.PRODUCT_MESSAGE, productInfo));
        sendMessage(message);
    }

    @Override
    public Message createProductMessage(String account, int type, boolean message, ProductInfo productInfo) {
        return new NIMMessage(MessageBuilder.createCustomMessage(account, Utils.getSesstionType(type), String.format("[%s]", productInfo.getProductName()), new ProductAttachment(message ? ProductAttachment.PRODUCT_MESSAGE : ProductAttachment.PRODUCT_INFO, productInfo)));
    }

    @Override
    public void sendLocationMessage(String account, int type, double latitude, double longitude, String address) {
        IMMessage message = MessageBuilder.createLocationMessage(account, Utils.getSesstionType(type), latitude, longitude, address);
        sendMessage(message);
    }

    @Override
    public void onConversationFragmentResume() {
        NIMSDK.getMsgService().setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_ALL, SessionTypeEnum.None);
    }

    @Override
    public void onConversationFragmentPause() {
        NIMSDK.getMsgService().setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.None);
    }

    @Override
    public void onChatFragmentResume(String account, int type) {
        NIMSDK.getMsgService().setChattingAccount(account, Utils.getSesstionType(type));
    }

    @Override
    public void onChatFragmentPause() {
        NIMSDK.getMsgService().setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.None);
    }

    @Override
    public String getAccount() {
        if (user != null) {
            return user.getAccount();
        }
        return "";
    }

    public void sendMessage(final IMMessage message) {
        onSendMessage(message);
        NIMSDK.getMsgService().sendMessage(message, false).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void param) {
                onMessageStatusChanged(message);
            }

            @Override
            public void onFailed(int code) {
                onMessageStatusChanged(message);
            }

            @Override
            public void onException(Throwable exception) {
                onMessageStatusChanged(message);
            }
        });
    }

    private void notifyMessageChanged(List<IMMessage> imMessages) {
        if (imMessages != null && !imMessages.isEmpty()) {
            String account = imMessages.get(0).getSessionId();
            OnMessageChangedListener listener = onMessageChangedListeners.get(account);
            if (listener != null) {
                List<Message> messages = Utils.getMessages(imMessages);
                listener.onReceivedMessage(messages);
            }
        } else {
            for (OnMessageChangedListener listener : onMessageChangedListeners.values()) {
                listener.onReceivedMessage(Collections.<Message>emptyList());
            }
        }
    }

    private void onSendMessage(IMMessage imMessage) {
        String account = imMessage.getSessionId();
        OnMessageChangedListener listener = onMessageChangedListeners.get(account);
        if (listener != null) {
            NIMMessage message = new NIMMessage(imMessage);
            listener.onSendMessage(message);
        }
    }

    private void onMessageStatusChanged(IMMessage imMessage) {
        String account = imMessage.getSessionId();
        OnMessageChangedListener listener = onMessageChangedListeners.get(account);
        if (listener != null) {
            NIMMessage message = new NIMMessage(imMessage);
            listener.onMessageStatusChanged(message);
        }
    }

}