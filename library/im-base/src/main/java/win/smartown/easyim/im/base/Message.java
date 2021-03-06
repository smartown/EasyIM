package win.smartown.easyim.im.base;

/**
 * @param <Data> SDK消息元数据对象
 * @author 雷小武
 * 创建时间：2018/9/29 11:42
 * 版权：成都智慧一生约科技有限公司
 * 类描述：封装统一message对象，需重写equals方法
 */
public abstract class Message<Data> {

    public static final int TYPE_TEXT = 1;
    public static final int TYPE_IMAGE = 2;
    public static final int TYPE_PRODUCT_MESSAGE = 3;
    public static final int TYPE_PRODUCT_INFO = 4;
    public static final int TYPE_NOTIFICATION = 5;
    public static final int TYPE_LOCATION = 6;
    public static final int TYPE_VIDEO = 7;

    protected Data data;

    public Message(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    /**
     * @return 消息发送人账号
     */
    public abstract String getConversationId();

    /**
     * 此消息是否是我发送的
     *
     * @return 是否是我发送的
     */
    public abstract boolean isSend();

    /**
     * 获取消息内容
     *
     * @return 消息内容
     */
    public abstract String getContent();

    /**
     * 获取通知消息内容
     *
     * @return 通知消息内容
     */
    public abstract String getNotificationContent();

    /**
     * 获取消息类型
     *
     * @return 消息类型
     * 文本:{@link Message#TYPE_TEXT}
     * 图片:{@link Message#TYPE_IMAGE}
     */
    public abstract int getType();

    /**
     * 获取图片链接
     *
     * @return 图片链接
     */
    public abstract String getImageUrl();

    /**
     * 获取图片宽度
     *
     * @return 图片宽度
     */
    public abstract int getImageWidth();

    /**
     * 获取图片高度
     *
     * @return 图片高度
     */
    public abstract int getImageHeight();

    /**
     * 获取视频链接
     *
     * @return 视频链接
     */
    public abstract String getVideoUrl();

    /**
     * 获取视频宽度
     *
     * @return 视频宽度
     */
    public abstract int getVideoWidth();

    /**
     * 获取视频高度
     *
     * @return 视频高度
     */
    public abstract int getVideoHeight();

    /**
     * @return 消息发送人账号
     */
    public abstract String getFromAccount();

    /**
     * @return 消息发送人昵称
     */
    public abstract String getFromNick();

    /**
     * @return 商品信息
     */
    public abstract ProductInfo getProductInfo();

    /**
     * @return 消息时间
     */
    public abstract long getTime();

    /**
     * 获取纬度
     *
     * @return 纬度
     */
    public abstract double getLatitude();

    /**
     * 获取经度
     *
     * @return 经度
     */
    public abstract double getLongitude();

    /**
     * 获取地理位置描述信息
     *
     * @return 地理位置描述
     */
    public abstract String getAddress();

}
