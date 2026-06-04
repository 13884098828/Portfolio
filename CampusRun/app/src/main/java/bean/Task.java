package bean;

public class Task {

    private int id;
    private String title;
    private String content;
    private double price;
    private String state;
    private String route;
    private String pickupAddress;
    private String deliveryAddress;
    private String phone;
    private String remark;
    private String publisher;
    private String acceptUser;
    private String createTime;
    private String acceptTime;
    private String finishTime;
    private String settleTime;
    private double frozenAmount;

    public Task() {
    }

    public Task(int id, String title, String content, double price, String state,
                String route, String pickupAddress, String deliveryAddress,
                String phone, String remark, String publisher, String acceptUser,
                String createTime, String acceptTime, String finishTime,
                String settleTime, double frozenAmount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.price = price;
        this.state = state;
        this.route = route;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
        this.remark = remark;
        this.publisher = publisher;
        this.acceptUser = acceptUser;
        this.createTime = createTime;
        this.acceptTime = acceptTime;
        this.finishTime = finishTime;
        this.settleTime = settleTime;
        this.frozenAmount = frozenAmount;
    }

    public Task(String title, String pickupAddress, String deliveryAddress,
                String phone, String remark, double price, String publisher) {
        this.title = title;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.phone = phone;
        this.remark = remark;
        this.price = price;
        this.publisher = publisher;
        this.content = pickupAddress + " -> " + deliveryAddress;
        this.route = this.content;
        this.state = "\u5f85\u63a5\u5355";
        this.acceptUser = "";
        this.createTime = "";
        this.acceptTime = "";
        this.finishTime = "";
        this.settleTime = "";
        this.frozenAmount = price;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getAcceptUser() { return acceptUser; }
    public void setAcceptUser(String acceptUser) { this.acceptUser = acceptUser; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getAcceptTime() { return acceptTime; }
    public void setAcceptTime(String acceptTime) { this.acceptTime = acceptTime; }
    public String getFinishTime() { return finishTime; }
    public void setFinishTime(String finishTime) { this.finishTime = finishTime; }
    public String getSettleTime() { return settleTime; }
    public void setSettleTime(String settleTime) { this.settleTime = settleTime; }
    public double getFrozenAmount() { return frozenAmount; }
    public void setFrozenAmount(double frozenAmount) { this.frozenAmount = frozenAmount; }
}
