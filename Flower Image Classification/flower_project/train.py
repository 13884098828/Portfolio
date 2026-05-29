import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, transforms, models
from torch.utils.data import DataLoader, random_split
import matplotlib.pyplot as plt

# 数据预处理
transform = transforms.Compose([
    transforms.Resize((224,224)),
    transforms.RandomHorizontalFlip(),
    transforms.ToTensor(),
])

# 加载数据集
dataset = datasets.ImageFolder(
    root='dataset/flower_photos',
    transform=transform
)

# 划分训练集和测试集
train_size = int(0.8 * len(dataset))
test_size = len(dataset) - train_size

train_dataset, test_dataset = random_split(dataset,[train_size,test_size])

train_loader = DataLoader(train_dataset,batch_size=32,shuffle=True)
test_loader = DataLoader(test_dataset,batch_size=32,shuffle=False)

# 加载VGG16
model = models.vgg16(weights=models.VGG16_Weights.DEFAULT)

# 冻结卷积层
for param in model.features.parameters():
    param.requires_grad = False

# 修改分类层
model.classifier[6] = nn.Linear(4096,5)

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = model.to(device)

# 损失函数和优化器
criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(),lr=0.001)

train_acc_list = []
train_loss_list = []

# 开始训练
epochs = 5

for epoch in range(epochs):

    correct = 0
    total = 0
    running_loss = 0

    model.train()

    for images,labels in train_loader:

        images = images.to(device)
        labels = labels.to(device)

        outputs = model(images)

        loss = criterion(outputs,labels)

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        running_loss += loss.item()

        _,predicted = torch.max(outputs.data,1)

        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    acc = 100 * correct / total

    train_acc_list.append(acc)
    train_loss_list.append(running_loss)

    print(f"Epoch {epoch+1}")
    print(f"Loss:{running_loss:.4f}")
    print(f"Accuracy:{acc:.2f}%")

# 保存模型
torch.save(model.state_dict(),"flower_model.pth")

# Accuracy 曲线
plt.plot(train_acc_list)
plt.title("Training Accuracy")
plt.xlabel("Epoch")
plt.ylabel("Accuracy")
plt.savefig("result/accuracy.png")
plt.show()

# Loss 曲线
plt.plot(train_loss_list)
plt.title("Training Loss")
plt.xlabel("Epoch")
plt.ylabel("Loss")
plt.savefig("result/loss.png")
plt.show()