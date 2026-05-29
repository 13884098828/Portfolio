import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import torch
import torch.nn as nn
from torchvision import transforms, models
from PIL import Image
import matplotlib.pyplot as plt

# 花朵类别
classes = ['daisy','dandelion','roses','sunflowers','tulips']

# 加载模型
model = models.vgg16(weights=None)

# 修改分类层
model.classifier[6] = nn.Linear(4096,5)

# 加载训练好的模型
model.load_state_dict(torch.load("flower_model.pth"))

# 设置为预测模式
model.eval()

# 图片预处理
transform = transforms.Compose([
    transforms.Resize((224,224)),
    transforms.ToTensor(),
])

# 测试图片路径
image_path = "test.jpg"

# 打开图片
image = Image.open(image_path)

# 图像处理
img = transform(image)

# 增加batch维度
img = img.unsqueeze(0)

# 开始预测
with torch.no_grad():

    outputs = model(img)

    _, predicted = torch.max(outputs,1)

# 获取预测结果
result = classes[predicted.item()]

# 输出结果
print("预测结果：",result)

# 显示图片
plt.imshow(image)
plt.title(f"Prediction: {result}")
plt.axis('off')
plt.show()