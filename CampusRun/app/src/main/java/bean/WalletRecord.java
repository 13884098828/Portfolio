package bean;

public class WalletRecord {

    private int id;
    private String username;
    private double amount;
    private String type;
    private int taskId;
    private String description;
    private String createTime;

    public WalletRecord(int id, String username, double amount, String type,
                        int taskId, String description, String createTime) {
        this.id = id;
        this.username = username;
        this.amount = amount;
        this.type = type;
        this.taskId = taskId;
        this.description = description;
        this.createTime = createTime;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public int getTaskId() { return taskId; }
    public String getDescription() { return description; }
    public String getCreateTime() { return createTime; }
}
