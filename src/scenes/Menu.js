/**
 * ============================================================================
 * 菜单场景 (Menu.js)
 * ============================================================================
 *
 * 功能说明：
 * - 游戏的主菜单界面
 * - 显示游戏标题、开始按钮、声音控制按钮
 * - 显示操作说明和版权信息
 * - 创建科幻风格的星空背景动画
 * - 初始化音频管理器并在用户点击时启动背景音乐
 *
 * 场景生命周期：
 * - constructor: 注册场景名称
 * - create: 创建所有UI元素和背景效果
 * - update: 每帧更新星星动画
 * ============================================================================
 */

// 导入音频管理器（用于播放背景音乐和音效）
import { AudioManager } from '../utils/AudioManager.js';

/**
 * Menu 类 - 游戏主菜单场景
 * 继承自 Phaser.Scene，是游戏启动后显示的第一个场景
 */
export class Menu extends Phaser.Scene {

    /**
     * 构造函数
     * 调用父类构造函数并注册场景名称为 'Menu'
     * 这个名称用于场景切换时的标识
     */
    constructor() {
        super('Menu');
    }

    /**
     * create 方法 - 场景创建时自动调用
     * 在这里初始化所有的游戏对象、UI元素和事件监听器
     * Phaser 会在场景资源加载完成后自动调用此方法
     */
    create() {
        // ==================== 初始化声音状态 ====================
        // 检查注册表中是否已有声音设置
        // 如果是首次运行（undefined），则默认开启声音
        // 注册表(registry)是Phaser提供的跨场景数据共享机制
        if (this.registry.get('soundEnabled') === undefined) {
            this.registry.set('soundEnabled', true);
        }

        // ==================== 创建背景效果 ====================
        // 调用方法创建科幻风格的星空背景
        this.createStars();

        // ==================== 游戏标题 ====================
        // 主标题：使用大号青色字体，带有蓝色描边效果
        this.add.text(640, 150, '航空战机', {
            fontSize: '72px',           // 字体大小：72像素
            fill: '#00ffff',            // 填充颜色：青色（科幻风格）
            fontFamily: 'Arial',        // 字体：Arial
            fontStyle: 'bold',          // 字体样式：粗体
            stroke: '#0066ff',          // 描边颜色：蓝色
            strokeThickness: 6          // 描边粗细：6像素
        }).setOrigin(0.5);              // 设置锚点为中心，便于居中对齐

        // 副标题：英文名称，使用较小的白色字体
        this.add.text(640, 230, 'AIR COMBAT', {
            fontSize: '36px',           // 字体大小：36像素
            fill: '#ffffff',            // 填充颜色：白色
            fontFamily: 'Arial',        // 字体：Arial
            letterSpacing: 10           // 字母间距：10像素（增加科技感）
        }).setOrigin(0.5);

        // ==================== 创建交互按钮 ====================
        // 创建"开始游戏"按钮（带有悬停和点击效果）
        this.createStartButton();

        // 创建"声音开关"按钮（用于控制游戏音效和背景音乐）
        this.createSoundButton();

        // ==================== 操作说明文字 ====================
        // 标题：黄色醒目文字
        this.add.text(640, 550, '操作说明', {
            fontSize: '24px',           // 字体大小：24像素
            fill: '#ffff00',            // 填充颜色：黄色（醒目）
            fontFamily: 'Arial'
        }).setOrigin(0.5);

        // 说明1：移动操作
        this.add.text(640, 590, '← → 或 A D 键移动战机', {
            fontSize: '20px',           // 字体大小：20像素
            fill: '#aaaaaa',            // 填充颜色：灰色（次要信息）
            fontFamily: 'Arial'
        }).setOrigin(0.5);

        // 说明2：射击和得分
        this.add.text(640, 620, '自动发射子弹，击败敌机获得分数', {
            fontSize: '20px',
            fill: '#aaaaaa',
            fontFamily: 'Arial'
        }).setOrigin(0.5);

        // 说明3：BOSS出现条件（使用紫色突出显示）
        this.add.text(640, 650, '每击败20架敌机会出现BOSS', {
            fontSize: '20px',
            fill: '#ff00ff',            // 填充颜色：紫色（强调BOSS信息）
            fontFamily: 'Arial'
        }).setOrigin(0.5);

        // ==================== 版权信息 ====================
        // 显示在屏幕底部，使用深灰色小字
        this.add.text(640, 700, '2024 Phaser Game', {
            fontSize: '16px',           // 字体大小：16像素（较小）
            fill: '#666666',            // 填充颜色：深灰色（不显眼）
            fontFamily: 'Arial'
        }).setOrigin(0.5);
    }

    /**
     * createStars 方法 - 创建科幻风格的星空背景
     *
     * 背景由多个层次组成：
     * 1. 星云层 - 紫色和蓝色的半透明圆形，模拟太空星云
     * 2. 网格层 - 透视效果的网格线，营造科幻感
     * 3. 星球层 - 远处的行星装饰
     * 4. 星星层 - 可移动和闪烁的星星
     */
    createStars() {
        // ==================== 星云背景层 ====================
        // 使用 Graphics 对象绘制星云效果
        const nebula = this.add.graphics();

        // ---------- 紫色星云 ----------
        // 在屏幕上半部分随机生成5个紫色星云
        for (let i = 0; i < 5; i++) {
            const x = Phaser.Math.Between(0, 1280);      // 随机X坐标（全屏宽度）
            const y = Phaser.Math.Between(0, 400);       // 随机Y坐标（上半部分）
            const radius = Phaser.Math.Between(100, 200); // 随机半径：100-200像素

            // 主星云：深紫色，透明度4%
            nebula.fillStyle(0x6600aa, 0.04);
            nebula.fillCircle(x, y, radius);

            // 星云高光：蓝紫色，透明度3%，略微偏移以创造层次感
            nebula.fillStyle(0x4400ff, 0.03);
            nebula.fillCircle(x + 20, y + 20, radius * 0.8);
        }

        // ---------- 蓝色星云 ----------
        // 在屏幕中部随机生成4个蓝色星云
        for (let i = 0; i < 4; i++) {
            const x = Phaser.Math.Between(0, 1280);      // 随机X坐标
            const y = Phaser.Math.Between(100, 500);     // 随机Y坐标（中部区域）
            const radius = Phaser.Math.Between(80, 150); // 随机半径：80-150像素

            // 蓝色星云：透明度4%
            nebula.fillStyle(0x0066ff, 0.04);
            nebula.fillCircle(x, y, radius);
        }

        // ==================== 透视网格层 ====================
        // 创建类似"光速隧道"的透视网格效果
        const grid = this.add.graphics();
        grid.lineStyle(1, 0x00ffff, 0.06);  // 青色线条，透明度6%

        // ---------- 水平线（从远到近） ----------
        // 绘制12条水平线，随着Y轴增加，线条越来越宽（透视效果）
        for (let i = 0; i < 12; i++) {
            const y = 450 + i * 25;                      // Y坐标：从450开始，每条间隔25像素
            const perspectiveScale = 1 + (i * 0.1);     // 透视缩放系数：越远越窄

            grid.beginPath();
            // 线条从中心(640)向两边延伸，宽度随透视缩放
            grid.moveTo(640 - 800 * perspectiveScale, y);
            grid.lineTo(640 + 800 * perspectiveScale, y);
            grid.strokePath();
        }

        // ---------- 垂直线（汇聚到远方） ----------
        // 绘制17条垂直线（-8到8），从地平线汇聚到屏幕底部
        for (let i = -8; i <= 8; i++) {
            const startX = 640 + i * 35;  // 起始X坐标：从中心向两边分布

            grid.beginPath();
            grid.moveTo(startX, 450);                   // 起点：地平线位置
            grid.lineTo(startX + i * 50, 720);          // 终点：向外扩散到屏幕底部
            grid.strokePath();
        }

        // ==================== 远处星球装饰 ====================
        // 在右上角绘制一个装饰性的行星
        const planet = this.add.graphics();

        // 行星主体：深蓝色，透明度20%
        planet.fillStyle(0x2244aa, 0.2);
        planet.fillCircle(1100, 120, 50);               // 中心(1100,120)，半径50

        // 行星高光：较亮的蓝色，透明度10%，略微偏移
        planet.fillStyle(0x3366cc, 0.1);
        planet.fillCircle(1090, 110, 40);               // 偏左上方的高光

        // 行星光环：亮蓝色描边
        planet.lineStyle(2, 0x4488ff, 0.1);
        planet.strokeCircle(1100, 120, 60);             // 半径60的光环

        // ==================== 星星层 ====================
        // 创建可移动的星星数组，用于 update 方法中的动画
        this.stars = [];

        // 星星颜色数组：白色、浅蓝、淡紫、粉紫、青色
        const starColors = [0xffffff, 0x88ccff, 0xaaaaff, 0xff88ff, 0x00ffff];

        // 创建100颗星星
        for (let i = 0; i < 100; i++) {
            const x = Phaser.Math.Between(0, 1280);         // 随机X坐标
            const y = Phaser.Math.Between(0, 720);          // 随机Y坐标
            const size = Phaser.Math.Between(1, 3);         // 随机大小：1-3像素
            const alpha = Phaser.Math.FloatBetween(0.3, 1); // 随机透明度：0.3-1.0
            const color = Phaser.Utils.Array.GetRandom(starColors); // 随机颜色

            // 创建圆形星星
            const star = this.add.circle(x, y, size, color, alpha);

            // 设置星星移动速度（大小越大，移动越快 - 模拟近大远小）
            star.speed = size * 0.5;

            // 将星星添加到数组中，便于后续更新
            this.stars.push(star);

            // ---------- 闪烁效果 ----------
            // 30%的星星会有闪烁动画效果
            if (Math.random() > 0.7) {
                this.tweens.add({
                    targets: star,              // 动画目标
                    alpha: 0.2,                 // 透明度变化到0.2
                    duration: Phaser.Math.Between(500, 1500), // 随机持续时间
                    yoyo: true,                 // 来回播放
                    repeat: -1                  // 无限循环
                });
            }
        }
    }

    /**
     * createStartButton 方法 - 创建"开始游戏"按钮
     *
     * 按钮功能：
     * - 显示绿色圆角矩形背景
     * - 鼠标悬停时变亮并放大文字
     * - 点击时初始化音频管理器并切换到游戏场景
     * - 文字有呼吸式闪烁动画，吸引玩家注意
     */
    createStartButton() {
        // ==================== 按钮背景 ====================
        // 使用 Graphics 绘制圆角矩形按钮
        const buttonBg = this.add.graphics();
        buttonBg.fillStyle(0x00aa00, 1);                              // 填充色：绿色
        buttonBg.fillRoundedRect(490, 330, 300, 70, 15);              // 圆角矩形：宽300，高70，圆角15
        buttonBg.lineStyle(3, 0x00ff00, 1);                           // 描边：亮绿色，3像素
        buttonBg.strokeRoundedRect(490, 330, 300, 70, 15);            // 绘制描边

        // ==================== 按钮文字 ====================
        const startText = this.add.text(640, 365, 'START GAME', {
            fontSize: '36px',           // 字体大小
            fill: '#ffffff',            // 填充色：白色
            fontFamily: 'Arial',
            fontStyle: 'bold'           // 粗体
        }).setOrigin(0.5);              // 居中对齐

        // ==================== 可点击区域 ====================
        // 创建透明矩形作为点击热区
        // 使用矩形而不是Graphics的原因：矩形可以方便地设置交互
        const hitArea = this.add.rectangle(640, 365, 300, 70, 0x000000, 0);  // 透明矩形
        hitArea.setInteractive({ useHandCursor: true });  // 启用交互，鼠标变成手型

        // ==================== 悬停效果 ====================
        // 鼠标移入时：按钮变亮，文字放大
        hitArea.on('pointerover', () => {
            // 重绘按钮背景（更亮的绿色）
            buttonBg.clear();
            buttonBg.fillStyle(0x00cc00, 1);                          // 更亮的绿色
            buttonBg.fillRoundedRect(490, 330, 300, 70, 15);
            buttonBg.lineStyle(3, 0x00ff00, 1);
            buttonBg.strokeRoundedRect(490, 330, 300, 70, 15);
            startText.setScale(1.1);                                   // 文字放大10%
        });

        // 鼠标移出时：恢复原状
        hitArea.on('pointerout', () => {
            // 重绘按钮背景（原始绿色）
            buttonBg.clear();
            buttonBg.fillStyle(0x00aa00, 1);                          // 原始绿色
            buttonBg.fillRoundedRect(490, 330, 300, 70, 15);
            buttonBg.lineStyle(3, 0x00ff00, 1);
            buttonBg.strokeRoundedRect(490, 330, 300, 70, 15);
            startText.setScale(1);                                     // 恢复原始大小
        });

        // ==================== 点击事件 ====================
        // 点击按钮时：初始化音频并开始游戏
        hitArea.on('pointerdown', () => {
            // ---------- 标记用户已交互 ----------
            // 浏览器的自动播放策略要求必须有用户交互才能播放音频
            // 这个标记让游戏知道用户已经进行了交互
            this.registry.set('userInteracted', true);

            // ---------- 创建音频管理器 ----------
            // 获取当前声音设置
            const soundEnabled = this.registry.get('soundEnabled');

            // 实例化音频管理器（会初始化Web Audio API）
            const audioManager = new AudioManager(this);

            // ---------- 根据声音设置处理音频 ----------
            if (soundEnabled === false) {
                // 如果声音被关闭，静音处理
                audioManager.setMuted(true);
            } else {
                // 如果声音开启，立即解锁并播放BGM
                // 必须在用户交互事件处理函数中调用resume()才能解锁AudioContext
                if (audioManager.audioContext) {
                    audioManager.audioContext.resume();  // 解锁AudioContext（绕过自动播放限制）
                    audioManager.startBgm();             // 立即开始播放背景音乐
                }
            }

            // ---------- 存储音频管理器到注册表 ----------
            // 这样Start场景可以继续使用同一个AudioManager实例
            // 确保BGM不会中断，音效也能正常播放
            this.registry.set('audioManager', audioManager);

            // ---------- 切换到游戏场景 ----------
            this.scene.start('Start');
        });

        // ==================== 文字闪烁动画 ====================
        // 创建呼吸式闪烁效果，吸引玩家注意力
        this.tweens.add({
            targets: startText,         // 动画目标：按钮文字
            alpha: 0.7,                 // 透明度变化到0.7
            duration: 800,              // 动画持续时间：800毫秒
            yoyo: true,                 // 来回播放（0.7 -> 1 -> 0.7）
            repeat: -1                  // 无限循环
        });
    }

    /**
     * createSoundButton 方法 - 创建声音开关按钮
     *
     * 按钮功能：
     * - 显示当前声音状态（ON/OFF）
     * - 点击可切换声音开关
     * - 状态会保存到注册表，跨场景共享
     * - 按钮颜色会根据状态变化（绿色=开，红色=关）
     */
    createSoundButton() {
        // 获取当前声音状态
        const soundEnabled = this.registry.get('soundEnabled');

        // ==================== 按钮背景 ====================
        // 将背景存为实例变量，以便在状态切换时更新
        this.soundButtonBg = this.add.graphics();
        this.updateSoundButtonStyle(soundEnabled);  // 根据当前状态绘制按钮样式

        // ==================== 按钮文字 ====================
        // 显示当前声音状态，颜色根据状态变化
        this.soundText = this.add.text(640, 450, soundEnabled ? 'SOUND: ON' : 'SOUND: OFF', {
            fontSize: '24px',
            fill: soundEnabled ? '#00ff00' : '#ff0000',  // 开=绿色，关=红色
            fontFamily: 'Arial',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        // ==================== 可点击区域 ====================
        // 创建透明矩形作为点击热区
        const soundHitArea = this.add.rectangle(640, 450, 200, 50, 0x000000, 0);
        soundHitArea.setInteractive({ useHandCursor: true });  // 启用交互

        // ==================== 悬停效果 ====================
        // 鼠标移入：文字放大
        soundHitArea.on('pointerover', () => {
            this.soundText.setScale(1.1);
        });

        // 鼠标移出：恢复原状
        soundHitArea.on('pointerout', () => {
            this.soundText.setScale(1);
        });

        // ==================== 点击事件 ====================
        // 点击切换声音状态
        soundHitArea.on('pointerdown', () => {
            // 获取当前状态并取反
            const currentState = this.registry.get('soundEnabled');
            const newState = !currentState;

            // 保存新状态到注册表（跨场景共享）
            this.registry.set('soundEnabled', newState);

            // ---------- 更新UI显示 ----------
            // 更新按钮文字
            this.soundText.setText(newState ? 'SOUND: ON' : 'SOUND: OFF');
            // 更新文字颜色
            this.soundText.setFill(newState ? '#00ff00' : '#ff0000');
            // 更新按钮背景样式
            this.updateSoundButtonStyle(newState);

            // ---------- 设置Phaser全局音量 ----------
            // mute属性控制所有Phaser音频的静音状态
            // 注意：Web Audio API的音频由AudioManager单独控制
            this.sound.mute = !newState;
        });
    }

    /**
     * updateSoundButtonStyle 方法 - 更新声音按钮的视觉样式
     *
     * @param {boolean} enabled - 声音是否启用
     *
     * 根据声音状态绘制不同颜色的按钮：
     * - 启用：深绿色背景 + 亮绿色边框
     * - 禁用：深红色背景 + 亮红色边框
     */
    updateSoundButtonStyle(enabled) {
        // 清除之前的绘制
        this.soundButtonBg.clear();

        // 设置填充色：启用=深绿色，禁用=深红色
        this.soundButtonBg.fillStyle(enabled ? 0x004400 : 0x440000, 1);
        // 绘制圆角矩形背景：宽200，高50，圆角10
        this.soundButtonBg.fillRoundedRect(540, 425, 200, 50, 10);

        // 设置描边色：启用=亮绿色，禁用=亮红色
        this.soundButtonBg.lineStyle(2, enabled ? 0x00ff00 : 0xff0000, 1);
        // 绘制圆角矩形描边
        this.soundButtonBg.strokeRoundedRect(540, 425, 200, 50, 10);
    }

    /**
     * update 方法 - 每帧调用的更新函数
     *
     * Phaser 会在每一帧自动调用此方法（通常60fps）
     * 用于处理需要持续更新的动画和逻辑
     *
     * 当前实现：
     * - 更新星星的位置（向下移动）
     * - 当星星移出屏幕时，重置到顶部
     */
    update() {
        // 遍历所有星星，更新它们的位置
        this.stars.forEach(star => {
            // 根据星星的速度向下移动
            // 速度与大小相关（大星星移动更快，模拟近大远小的透视效果）
            star.y += star.speed;

            // 当星星移出屏幕底部时，重置到顶部
            if (star.y > 720) {
                star.y = 0;                                      // 重置Y坐标到顶部
                star.x = Phaser.Math.Between(0, 1280);          // 随机新的X坐标
            }
        });
    }
}
