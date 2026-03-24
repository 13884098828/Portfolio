/**
 * ============================================================================
 * 游戏主场景 (Start.js)
 * ============================================================================
 *
 * 功能说明：
 * - 游戏的核心玩法场景
 * - 玩家控制战机左右移动，自动发射子弹
 * - 敌机从上方生成并向下移动
 * - 每击败20架敌机会出现BOSS
 * - BOSS出现时暂停普通敌机生成
 * - 击败BOSS后恢复敌机生成，并获得火力升级
 *
 * 场景生命周期：
 * - constructor: 注册场景名称
 * - preload: 加载/生成游戏资源
 * - create: 初始化游戏对象和逻辑
 * - update: 每帧更新游戏状态
 * - shutdown: 场景关闭时的清理
 *
 * 游戏机制：
 * - 分数系统：击败普通敌机+10分，击败BOSS+100分
 * - 难度递增：随分数增加，敌机速度和生成频率提升
 * - 火力升级：击败BOSS后增加子弹列数（最多4列）
 * - BOSS机制：血量随击败次数翻倍
 * ============================================================================
 */

// 导入音频管理器
import { AudioManager } from '../utils/AudioManager.js';

/**
 * Start 类 - 游戏主场景
 * 继承自 Phaser.Scene，包含所有核心游戏逻辑
 */
export class Start extends Phaser.Scene {

    /**
     * 构造函数
     * 注册场景名称为 'Start'
     */
    constructor() {
        super('Start');
    }

    /**
     * preload 方法 - 资源预加载
     * Phaser 在场景启动前自动调用此方法
     * 用于加载图片、音频等资源
     */
    preload() {
        // ==================== 加载外部图片资源 ====================
        // 玩家战机贴图（从 assets 文件夹加载）
        this.load.image('playerShip', 'assets/playerShip.png');
        // 子弹贴图
        this.load.image('bullet', 'assets/bullet.png');

        // ==================== 动态生成纹理 ====================
        // 敌机和BOSS不使用外部图片，而是用代码动态绘制
        // 这样可以减少资源文件，同时创造独特的科幻风格

        // 动态生成红色敌机纹理
        this.createEnemyShipTexture();
        // 动态生成紫色科幻BOSS纹理
        this.createBossTexture();
        // 动态生成BOSS子弹纹理
        this.createBossBulletTexture();
    }

    /**
     * createEnemyShipTexture 方法 - 动态生成敌机纹理
     *
     * 使用 Phaser Graphics API 绘制红色敌机外观
     * 绘制完成后生成纹理，供后续创建敌机精灵使用
     *
     * 敌机设计：
     * - 红色为主色调（表示敌方）
     * - 包含机翼、机身、机头、驾驶舱、尾翼、引擎喷口
     * - 科幻战斗机风格
     */
    createEnemyShipTexture() {
        // 创建临时图形对象用于绘制
        const graphics = this.add.graphics();

        // 纹理尺寸定义
        const width = 80;   // 宽度：80像素
        const height = 100; // 高度：100像素

        // ==================== 颜色定义 ====================
        const mainColor = 0xcc0000;      // 主体色：深红色
        const darkRed = 0x990000;        // 暗红色：用于阴影部分
        const lightRed = 0xff3333;       // 亮红色：用于高光部分
        const cockpitColor = 0x333333;   // 驾驶舱色：深灰色

        // ==================== 绘制主机翼 ====================
        // 大三角形，构成飞机的主要轮廓
        graphics.fillStyle(darkRed, 1);
        graphics.beginPath();
        graphics.moveTo(width / 2, height * 0.3);     // 机翼前端（顶点）
        graphics.lineTo(0, height * 0.7);              // 左翼尖
        graphics.lineTo(width, height * 0.7);          // 右翼尖
        graphics.closePath();
        graphics.fillPath();

        // ==================== 绘制机身 ====================
        // 椭圆形机身，位于中央
        graphics.fillStyle(mainColor, 1);
        graphics.fillEllipse(width / 2, height * 0.5, width * 0.35, height * 0.5);

        // ==================== 绘制机头 ====================
        // 尖锐的三角形，指向上方（敌机朝下飞行时变成尾部）
        graphics.fillStyle(lightRed, 1);
        graphics.beginPath();
        graphics.moveTo(width / 2, 0);                 // 机头尖端
        graphics.lineTo(width / 2 - 12, height * 0.25); // 左侧
        graphics.lineTo(width / 2 + 12, height * 0.25); // 右侧
        graphics.closePath();
        graphics.fillPath();

        // ==================== 绘制驾驶舱 ====================
        // 小椭圆形，深灰色表示玻璃窗
        graphics.fillStyle(cockpitColor, 1);
        graphics.fillEllipse(width / 2, height * 0.35, 8, 12);

        // ==================== 绘制尾翼 ====================
        graphics.fillStyle(darkRed, 1);

        // 左尾翼：三角形
        graphics.beginPath();
        graphics.moveTo(width / 2 - 10, height * 0.75);  // 连接点
        graphics.lineTo(width / 2 - 25, height);          // 尾翼尖端
        graphics.lineTo(width / 2 - 5, height * 0.9);     // 内侧点
        graphics.closePath();
        graphics.fillPath();

        // 右尾翼：三角形（对称）
        graphics.beginPath();
        graphics.moveTo(width / 2 + 10, height * 0.75);
        graphics.lineTo(width / 2 + 25, height);
        graphics.lineTo(width / 2 + 5, height * 0.9);
        graphics.closePath();
        graphics.fillPath();

        // ==================== 绘制引擎喷口 ====================
        // 两个橙色椭圆形，模拟推进器火焰
        graphics.fillStyle(0xff6600, 1);  // 橙色
        graphics.fillEllipse(width / 2 - 8, height * 0.95, 4, 6);  // 左引擎
        graphics.fillEllipse(width / 2 + 8, height * 0.95, 4, 6);  // 右引擎

        // ==================== 添加装饰线条 ====================
        // 从驾驶舱向两侧机翼延伸的线条，增加细节
        graphics.lineStyle(2, lightRed, 1);
        graphics.beginPath();
        graphics.moveTo(width / 2, height * 0.35);       // 起点：驾驶舱位置
        graphics.lineTo(width * 0.15, height * 0.65);    // 左翼线条
        graphics.moveTo(width / 2, height * 0.35);       // 重新起点
        graphics.lineTo(width * 0.85, height * 0.65);    // 右翼线条
        graphics.strokePath();

        // ==================== 生成纹理并清理 ====================
        // 将绘制的图形转换为可重复使用的纹理
        graphics.generateTexture('enemyShip', width, height);
        // 销毁临时图形对象，释放内存
        graphics.destroy();
    }

    /**
     * createBossTexture 方法 - 动态生成BOSS纹理
     *
     * 使用 Phaser Graphics API 绘制科幻风格的BOSS飞船
     * BOSS比普通敌机更大、更精细，具有独特的紫色科幻外观
     *
     * BOSS设计特点：
     * - 紫色为主色调（区别于红色敌机）
     * - 六边形主体设计
     * - 能量核心和护盾轮廓
     * - 双引擎和武器挂载点
     * - 尺寸是普通敌机的2倍
     */
    createBossTexture() {
        // 创建临时图形对象用于绘制
        const graphics = this.add.graphics();

        // 纹理尺寸定义（比普通敌机大一倍）
        const width = 160;   // 宽度：160像素
        const height = 140;  // 高度：140像素

        // ==================== 颜色定义 ====================
        // 紫色科幻风格配色方案
        const mainColor = 0x8800ff;      // 主体色：亮紫色
        const darkPurple = 0x5500aa;     // 暗紫色：用于阴影/轮廓
        const lightPurple = 0xaa44ff;    // 浅紫色：用于高光
        const coreColor = 0x00ffff;      // 能量核心色：青色（高科技感）
        const glowColor = 0xff00ff;      // 发光色：品红色

        // ==================== 绘制护盾轮廓 ====================
        // 外层半透明椭圆形护盾
        graphics.lineStyle(3, coreColor, 0.5);  // 青色描边，50%透明度
        graphics.strokeEllipse(width / 2, height / 2, width * 0.9, height * 0.7);

        // ==================== 绘制主机体 ====================
        // 六边形设计，更具科幻感
        graphics.fillStyle(darkPurple, 1);
        graphics.beginPath();
        graphics.moveTo(width / 2, height * 0.15);      // 顶部中心
        graphics.lineTo(width * 0.85, height * 0.35);   // 右上角
        graphics.lineTo(width, height * 0.6);           // 右翼尖端
        graphics.lineTo(width * 0.7, height * 0.85);    // 右下角
        graphics.lineTo(width * 0.3, height * 0.85);    // 左下角
        graphics.lineTo(0, height * 0.6);               // 左翼尖端
        graphics.lineTo(width * 0.15, height * 0.35);   // 左上角
        graphics.closePath();
        graphics.fillPath();

        // ==================== 绘制中央机身 ====================
        // 椭圆形核心舱
        graphics.fillStyle(mainColor, 1);
        graphics.fillEllipse(width / 2, height * 0.5, width * 0.35, height * 0.4);

        // ==================== 绘制机头 ====================
        // 尖锐三角形，指向敌人
        graphics.fillStyle(lightPurple, 1);
        graphics.beginPath();
        graphics.moveTo(width / 2, 0);                   // 尖端
        graphics.lineTo(width / 2 - 20, height * 0.25);  // 左底角
        graphics.lineTo(width / 2 + 20, height * 0.25);  // 右底角
        graphics.closePath();
        graphics.fillPath();

        // ==================== 绘制能量核心 ====================
        // 中央发光的能量源，双层圆形设计
        graphics.fillStyle(coreColor, 1);      // 外层：青色
        graphics.fillCircle(width / 2, height * 0.45, 15);
        graphics.fillStyle(0xffffff, 1);       // 内层：白色（高亮）
        graphics.fillCircle(width / 2, height * 0.45, 8);

        // ==================== 绘制能量引擎 ====================
        // 左右两侧的椭圆形引擎
        graphics.fillStyle(glowColor, 1);  // 品红色外壳
        graphics.fillEllipse(width * 0.25, height * 0.5, 12, 18);  // 左引擎
        graphics.fillEllipse(width * 0.75, height * 0.5, 12, 18);  // 右引擎

        // 引擎内核：青色发光点
        graphics.fillStyle(coreColor, 1);
        graphics.fillCircle(width * 0.25, height * 0.5, 6);  // 左引擎核心
        graphics.fillCircle(width * 0.75, height * 0.5, 6);  // 右引擎核心

        // ==================== 绘制武器挂载点 ====================
        // 两侧的矩形武器舱
        graphics.fillStyle(darkPurple, 1);
        graphics.fillRect(width * 0.1, height * 0.55, 15, 25);   // 左武器舱
        graphics.fillRect(width * 0.8, height * 0.55, 15, 25);   // 右武器舱

        // 武器发光效果：红色瞄准点
        graphics.fillStyle(0xff0000, 1);  // 红色
        graphics.fillCircle(width * 0.1 + 7, height * 0.75, 5);  // 左武器发光
        graphics.fillCircle(width * 0.8 + 7, height * 0.75, 5);  // 右武器发光

        // ==================== 绘制尾部推进器 ====================
        // 三个橙色椭圆形推进器
        graphics.fillStyle(0xff6600, 1);  // 橙色火焰
        graphics.fillEllipse(width * 0.35, height * 0.95, 10, 15);  // 左推进器
        graphics.fillEllipse(width * 0.5, height * 0.95, 10, 15);   // 中央推进器
        graphics.fillEllipse(width * 0.65, height * 0.95, 10, 15);  // 右推进器

        // ==================== 添加装饰线条 ====================
        // 从机头向两侧引擎延伸的能量线
        graphics.lineStyle(2, coreColor, 0.8);  // 青色线条，80%透明度
        graphics.beginPath();
        graphics.moveTo(width / 2, height * 0.25);       // 起点：机头下方
        graphics.lineTo(width * 0.15, height * 0.5);     // 左侧引擎方向
        graphics.moveTo(width / 2, height * 0.25);       // 重新起点
        graphics.lineTo(width * 0.85, height * 0.5);     // 右侧引擎方向
        graphics.strokePath();

        // ==================== 生成纹理并清理 ====================
        // 将绘制的图形转换为纹理
        graphics.generateTexture('bossShip', width, height);
        // 销毁临时图形对象
        graphics.destroy();
    }

    /**
     * createBossBulletTexture 方法 - 动态生成BOSS子弹纹理
     *
     * 创建紫色能量弹外观，与玩家子弹区分
     * 特点：
     * - 紫色/品红色主色调（BOSS配色）
     * - 发光效果
     * - 较大尺寸便于玩家躲避
     */
    createBossBulletTexture() {
        const graphics = this.add.graphics();

        // 子弹尺寸
        const width = 20;
        const height = 30;

        // ==================== 绘制外层光晕 ====================
        graphics.fillStyle(0xff00ff, 0.3);  // 品红色，30%透明度
        graphics.fillEllipse(width / 2, height / 2, width, height);

        // ==================== 绘制中层能量核心 ====================
        graphics.fillStyle(0xff00ff, 0.6);  // 品红色，60%透明度
        graphics.fillEllipse(width / 2, height / 2, width * 0.7, height * 0.7);

        // ==================== 绘制内层高光 ====================
        graphics.fillStyle(0xffffff, 0.8);  // 白色高光
        graphics.fillEllipse(width / 2, height * 0.4, width * 0.3, height * 0.3);

        // ==================== 绘制尾焰效果 ====================
        graphics.fillStyle(0x8800ff, 0.5);  // 紫色尾焰
        graphics.beginPath();
        graphics.moveTo(width / 2 - 5, height * 0.7);
        graphics.lineTo(width / 2, height);
        graphics.lineTo(width / 2 + 5, height * 0.7);
        graphics.closePath();
        graphics.fillPath();

        // 生成纹理并清理
        graphics.generateTexture('bossBullet', width, height);
        graphics.destroy();
    }

    /**
     * create 方法 - 场景创建时自动调用
     *
     * 初始化顺序：
     * 1. 游戏状态变量
     * 2. 音频管理器
     * 3. 背景效果
     * 4. 玩家战机
     * 5. 物理组（子弹、敌机、BOSS）
     * 6. 敌机生成定时器
     * 7. UI文本
     * 8. 键盘输入
     * 9. 碰撞检测
     * 10. 游戏结束界面
     */
    create() {
        // ==================== 游戏状态变量初始化 ====================
        this.score = 0;                   // 当前分数
        this.gameOver = false;            // 游戏是否结束
        this.enemySpeed = 150;            // 敌机初始下落速度（像素/秒）
        this.bulletSpeed = 900;           // 子弹飞行速度（像素/秒）
        this.fireRate = 120;              // 射击间隔（毫秒）
        this.lastFired = 0;               // 上次射击时间戳
        this.bulletColumns = 1;           // 子弹列数（1-4列，击败BOSS后增加）
        this.enemiesDefeated = 0;         // 击败敌机计数（不含BOSS，用于触发BOSS）
        this.enemyBaseHealth = 1;         // 敌机基础血量（击败BOSS后增加50%）
        this.bossDefeated = 0;            // 击败BOSS计数
        this.bossActive = false;          // BOSS是否在场（用于控制敌机生成）
        this.bossHealthMultiplier = 8;    // BOSS血量倍数（每次击败后翻倍）

        // ==================== 音频管理器初始化 ====================
        // 优先从注册表获取（由Menu场景创建，确保BGM无缝衔接）
        this.audioManager = this.registry.get('audioManager');

        // 如果没有AudioManager（例如开发时直接进入Start场景）
        // 则创建新的AudioManager实例
        if (!this.audioManager) {
            this.audioManager = new AudioManager(this);

            // 检查声音设置
            const soundEnabled = this.registry.get('soundEnabled');
            if (soundEnabled === false) {
                this.audioManager.setMuted(true);  // 静音
            }

            // ---------- 添加用户交互监听器解锁音频 ----------
            // 浏览器自动播放策略要求必须有用户交互才能播放音频
            this.audioUnlocked = false;

            // 鼠标点击时解锁
            this.input.on('pointerdown', () => {
                this.unlockAudioAndStartBgm();
            });

            // 键盘按下时解锁
            this.input.keyboard.on('keydown', () => {
                this.unlockAudioAndStartBgm();
            });
        }

        // ==================== 创建背景效果 ====================
        // 调用方法创建科幻风格的星空背景
        this.createStars();

        // ==================== 创建玩家战机 ====================
        // 使用预加载的战机图片创建精灵
        this.player = this.add.image(640, 620, 'playerShip');  // 位置：屏幕底部中央
        this.player.setScale(0.8);                              // 缩放到80%大小

        // 为玩家添加物理属性
        this.physics.add.existing(this.player);
        this.player.body.setCollideWorldBounds(true);           // 限制在屏幕内
        this.player.body.setSize(80, 60);                       // 设置碰撞体大小

        // ==================== 创建物理组 ====================
        // 物理组用于管理同类对象，便于碰撞检测和批量操作

        // 子弹组：存放所有发射的子弹
        this.bullets = this.physics.add.group();

        // 敌机组：存放所有普通敌机
        this.enemies = this.physics.add.group();

        // BOSS组：存放BOSS（虽然同时只有一个BOSS，但用组便于统一管理）
        this.bosses = this.physics.add.group();

        // BOSS子弹组：存放BOSS发射的子弹
        this.bossBullets = this.physics.add.group();

        // ==================== 敌机生成定时器 ====================
        // 每1200毫秒（1.2秒）生成一架敌机
        this.enemyTimer = this.time.addEvent({
            delay: 1200,                    // 间隔时间（毫秒）
            callback: this.spawnEnemy,      // 回调函数
            callbackScope: this,            // 回调函数的作用域
            loop: true                      // 循环执行
        });

        // ==================== UI文本 ====================
        // 分数显示：左上角
        this.scoreText = this.add.text(20, 20, '分数: 0', {
            fontSize: '32px',
            fill: '#ffffff',
            fontFamily: 'Arial'
        });

        // 操作提示：顶部中央
        this.add.text(640, 30, '← → 或 A D 移动（自动发射子弹）', {
            fontSize: '20px',
            fill: '#aaaaaa',      // 灰色，不太显眼
            fontFamily: 'Arial'
        }).setOrigin(0.5);

        // ==================== 键盘输入设置 ====================
        // 方向键：用于移动战机
        this.cursors = this.input.keyboard.createCursorKeys();

        // A/D键：另一种移动方式（类似WASD）
        this.keyA = this.input.keyboard.addKey(Phaser.Input.Keyboard.KeyCodes.A);
        this.keyD = this.input.keyboard.addKey(Phaser.Input.Keyboard.KeyCodes.D);

        // 空格键：保留（当前未使用，自动射击）
        this.spaceKey = this.input.keyboard.addKey(Phaser.Input.Keyboard.KeyCodes.SPACE);

        // ==================== 碰撞检测设置 ====================
        // overlap 检测对象重叠时触发回调，不产生物理碰撞效果

        // 玩家与敌机碰撞 → 游戏结束
        this.physics.add.overlap(this.player, this.enemies, this.playerHit, null, this);

        // 子弹与敌机碰撞 → 敌机受伤/销毁
        this.physics.add.overlap(this.bullets, this.enemies, this.bulletHitEnemy, null, this);

        // 玩家与BOSS碰撞 → 游戏结束
        this.physics.add.overlap(this.player, this.bosses, this.playerHit, null, this);

        // 子弹与BOSS碰撞 → BOSS受伤/销毁
        this.physics.add.overlap(this.bullets, this.bosses, this.bulletHitBoss, null, this);

        // 玩家与BOSS子弹碰撞 → 游戏结束
        this.physics.add.overlap(this.player, this.bossBullets, this.playerHit, null, this);

        // ==================== 游戏结束界面（初始隐藏） ====================
        // 游戏结束标题：红色大字
        this.gameOverText = this.add.text(640, 300, '游戏结束!', {
            fontSize: '64px',
            fill: '#ff4444',
            fontFamily: 'Arial'
        }).setOrigin(0.5).setVisible(false);

        // 重新开始提示
        this.restartText = this.add.text(640, 400, '按 R 键重新开始', {
            fontSize: '32px',
            fill: '#ffffff',
            fontFamily: 'Arial'
        }).setOrigin(0.5).setVisible(false);

        // 返回菜单提示
        this.menuText = this.add.text(640, 450, '按 M 键返回菜单', {
            fontSize: '24px',
            fill: '#aaaaaa',
            fontFamily: 'Arial'
        }).setOrigin(0.5).setVisible(false);

        // ==================== 功能键设置 ====================
        // R键：重新开始游戏
        this.keyR = this.input.keyboard.addKey(Phaser.Input.Keyboard.KeyCodes.R);
        // M键：返回主菜单
        this.keyM = this.input.keyboard.addKey(Phaser.Input.Keyboard.KeyCodes.M);
    }

    /**
     * unlockAudioAndStartBgm 方法 - 解锁音频并启动背景音乐
     *
     * 浏览器自动播放策略要求必须在用户交互事件中解锁AudioContext
     * 此方法在用户首次点击或按键时调用
     *
     * 注意：此方法只在直接进入Start场景时使用
     * 正常流程是从Menu场景进入，音频已经在那里解锁
     */
    unlockAudioAndStartBgm() {
        // 防止重复解锁，或在静音状态下解锁
        if (this.audioUnlocked || this.audioManager.isMuted) return;

        // 标记已解锁，防止后续重复调用
        this.audioUnlocked = true;

        // 恢复音频上下文并启动背景音乐
        if (this.audioManager.audioContext) {
            // resume() 返回一个 Promise
            this.audioManager.audioContext.resume().then(() => {
                console.log('Audio context resumed via user interaction');
                this.audioManager.startBgm();  // 开始播放BGM
            }).catch(e => {
                console.log('Audio resume failed:', e);
                // 即使 resume 失败也尝试播放（某些浏览器可能已自动恢复）
                this.audioManager.startBgm();
            });
        }
    }

    /**
     * createStars 方法 - 创建游戏场景的星空背景
     *
     * 背景由多个层次组成（从下到上）：
     * 1. 星云层 - 多彩的半透明云雾效果
     * 2. 网格层 - 赛博朋克风格的透视网格
     * 3. 星球层 - 远处的行星装饰
     * 4. 星星层 - 可移动的星星点
     * 5. 流星层 - 动态划过的流星
     */
    createStars() {
        // 第一层：星云背景
        this.createNebula();

        // 第二层：网格线效果（赛博朋克/科幻风格）
        this.createGridLines();

        // 第三层：远处的行星装饰
        this.createPlanets();

        // ==================== 第四层：星星 ====================
        // 创建星星数组，用于 update 方法中的动画
        this.stars = [];

        // 星星颜色：白、浅蓝、淡紫、粉紫、青色
        const starColors = [0xffffff, 0x88ccff, 0xaaaaff, 0xff88ff, 0x00ffff];

        // 创建120颗星星（比菜单场景多20颗，增加密度）
        for (let i = 0; i < 120; i++) {
            const x = Phaser.Math.Between(0, 1280);         // 随机X坐标
            const y = Phaser.Math.Between(0, 720);          // 随机Y坐标
            const size = Phaser.Math.Between(1, 3);         // 随机大小
            const alpha = Phaser.Math.FloatBetween(0.3, 1); // 随机透明度
            const color = Phaser.Utils.Array.GetRandom(starColors); // 随机颜色

            // 创建圆形星星
            const star = this.add.circle(x, y, size, color, alpha);
            // 移动速度与大小相关（模拟透视效果）
            star.speed = size * 0.5;
            this.stars.push(star);
        }

        // ==================== 第五层：流星 ====================
        // 创建流星数组，用于管理动态流星
        this.shootingStars = [];

        // 立即创建一颗流星
        this.createShootingStar();

        // 定时器：每3秒创建一颗新流星
        this.time.addEvent({
            delay: 3000,                          // 间隔3000毫秒
            callback: this.createShootingStar,    // 回调函数
            callbackScope: this,                  // 作用域
            loop: true                            // 循环执行
        });
    }

    /**
     * createNebula 方法 - 创建星云背景效果
     *
     * 绘制三种颜色的星云：紫色、蓝色、粉色
     * 每种星云有主体和高光两层，创造层次感
     * 使用低透明度营造朦胧的太空感
     */
    createNebula() {
        const graphics = this.add.graphics();

        // ---------- 紫色星云 ----------
        // 在屏幕上半部分随机生成5个紫色星云
        for (let i = 0; i < 5; i++) {
            const x = Phaser.Math.Between(0, 1280);       // 随机X坐标
            const y = Phaser.Math.Between(0, 400);        // 随机Y坐标（上半部分）
            const radius = Phaser.Math.Between(100, 200); // 随机半径

            // 主星云：深紫色，透明度3%
            graphics.fillStyle(0x6600aa, 0.03);
            graphics.fillCircle(x, y, radius);

            // 高光层：蓝紫色，透明度2%，略微偏移
            graphics.fillStyle(0x4400ff, 0.02);
            graphics.fillCircle(x + 20, y + 20, radius * 0.8);
        }

        // ---------- 蓝色星云 ----------
        // 在屏幕中部随机生成4个蓝色星云
        for (let i = 0; i < 4; i++) {
            const x = Phaser.Math.Between(0, 1280);
            const y = Phaser.Math.Between(100, 500);
            const radius = Phaser.Math.Between(80, 150);

            // 主星云：深蓝色
            graphics.fillStyle(0x0066ff, 0.03);
            graphics.fillCircle(x, y, radius);

            // 高光层：亮蓝色
            graphics.fillStyle(0x00aaff, 0.02);
            graphics.fillCircle(x - 15, y + 15, radius * 0.7);
        }

        // ---------- 粉色星云 ----------
        // 在屏幕上部随机生成3个粉色星云
        for (let i = 0; i < 3; i++) {
            const x = Phaser.Math.Between(0, 1280);
            const y = Phaser.Math.Between(0, 300);
            const radius = Phaser.Math.Between(60, 120);

            // 粉红色星云
            graphics.fillStyle(0xff0066, 0.02);
            graphics.fillCircle(x, y, radius);
        }
    }

    /**
     * createGridLines 方法 - 创建透视网格效果
     *
     * 模拟"光速隧道"或"赛博朋克地面"的视觉效果
     * 水平线从远到近逐渐变宽（透视效果）
     * 垂直线从中心向外辐射
     */
    createGridLines() {
        const graphics = this.add.graphics();

        // 设置线条样式：青色，8%透明度
        graphics.lineStyle(1, 0x00ffff, 0.08);

        // ---------- 水平线（透视效果） ----------
        // 绘制15条水平线，模拟向远处延伸的地面
        for (let i = 0; i < 15; i++) {
            const y = 400 + i * 25;                     // Y坐标：从400开始，间隔25
            const perspectiveScale = 1 + (i * 0.1);    // 透视缩放系数

            graphics.beginPath();
            // 线条宽度随距离增加（近大远小）
            graphics.moveTo(640 - 800 * perspectiveScale, y);
            graphics.lineTo(640 + 800 * perspectiveScale, y);
            graphics.strokePath();
        }

        // ---------- 垂直线（向外辐射） ----------
        // 绘制21条垂直线（-10到10），从地平线向屏幕底部辐射
        for (let i = -10; i <= 10; i++) {
            const startX = 640 + i * 30;  // 起始X坐标

            graphics.beginPath();
            graphics.moveTo(startX, 400);                // 起点：地平线
            graphics.lineTo(startX + i * 50, 720);       // 终点：向外扩散
            graphics.strokePath();
        }
    }

    /**
     * createPlanets 方法 - 创建远处的行星装饰
     *
     * 添加三个行星增加太空感：
     * 1. 带环的棕色小行星（左上角）
     * 2. 蓝色气态巨行星（右侧）
     * 3. 小型灰色卫星（右下）
     */
    createPlanets() {
        // ==================== 棕色小行星（左上角） ====================
        const planet1 = this.add.graphics();

        // 行星主体：棕色
        planet1.fillStyle(0x884422, 0.3);
        planet1.fillCircle(100, 80, 40);

        // 行星高光：较亮的棕色
        planet1.fillStyle(0xaa6633, 0.2);
        planet1.fillCircle(94, 74, 34);

        // 行星环：椭圆形描边
        planet1.lineStyle(2, 0xccaa88, 0.2);
        planet1.strokeEllipse(100, 80, 90, 20);

        // ==================== 蓝色气态巨行星（右侧） ====================
        const planet2 = this.add.graphics();

        // 行星主体：深蓝色
        planet2.fillStyle(0x2244aa, 0.25);
        planet2.fillCircle(1150, 150, 60);

        // 行星高光：较亮的蓝色
        planet2.fillStyle(0x3366cc, 0.15);
        planet2.fillCircle(1140, 140, 50);

        // 大气层光晕：外层描边
        planet2.lineStyle(4, 0x4488ff, 0.1);
        planet2.strokeCircle(1150, 150, 70);

        // ==================== 小型卫星 ====================
        const moon = this.add.graphics();

        // 灰色小卫星
        moon.fillStyle(0x888888, 0.2);
        moon.fillCircle(1200, 200, 16);
    }

    /**
     * createShootingStar 方法 - 创建一颗流星
     *
     * 流星从屏幕上方随机位置出现
     * 以一定角度向左下方划过
     * 包含头部（白色圆点）和尾巴（渐变线条）
     */
    createShootingStar() {
        // 游戏结束时不创建新流星
        if (this.gameOver) return;

        // 随机起始位置
        const startX = Phaser.Math.Between(200, 1280);  // X：200-1280（避免太左边）
        const startY = -10;                              // Y：屏幕上方（-10）
        const length = Phaser.Math.Between(25, 50);     // 尾巴长度：25-50像素

        // ---------- 创建流星头部 ----------
        // 白色小圆点
        const head = this.add.circle(startX, startY, 2, 0xffffff, 1);

        // ---------- 创建流星尾巴 ----------
        // 使用 Graphics 绘制渐变尾巴
        const tail = this.add.graphics();
        tail.x = startX;
        tail.y = startY;

        // 将流星信息存储为对象
        const shootingStar = { head, tail, length, x: startX, y: startY };
        this.shootingStars.push(shootingStar);

        // ---------- 流星动画 ----------
        this.tweens.add({
            targets: shootingStar,               // 动画目标：流星对象
            x: startX - 300,                     // 终点X：向左移动300像素
            y: 800,                              // 终点Y：移出屏幕底部
            duration: 1500,                      // 动画时长：1.5秒

            // 每帧更新时重绘流星
            onUpdate: () => {
                // 更新头部位置
                head.x = shootingStar.x;
                head.y = shootingStar.y;

                // 重绘尾巴
                tail.clear();

                // 尾巴主线：白色，较粗
                tail.lineStyle(2, 0xffffff, 0.8);
                tail.beginPath();
                tail.moveTo(0, 0);                           // 起点：头部位置
                tail.lineTo(length * 0.3, -length * 0.5);    // 向右上延伸
                tail.strokePath();

                // 尾巴次线：浅蓝色，较细，更长
                tail.lineStyle(1, 0x88ccff, 0.4);
                tail.beginPath();
                tail.moveTo(0, 0);
                tail.lineTo(length * 0.5, -length * 0.8);    // 延伸更远
                tail.strokePath();

                // 更新尾巴位置
                tail.x = shootingStar.x;
                tail.y = shootingStar.y;
            },

            // 动画完成时清理
            onComplete: () => {
                head.destroy();   // 销毁头部
                tail.destroy();   // 销毁尾巴

                // 从数组中移除
                const index = this.shootingStars.indexOf(shootingStar);
                if (index > -1) {
                    this.shootingStars.splice(index, 1);
                }
            }
        });
    }

    /**
     * spawnEnemy 方法 - 生成一架普通敌机
     *
     * 由定时器每隔一定时间调用
     * 敌机从屏幕顶部随机位置出现，向下飞行
     * 随着游戏进行，敌机速度和生成频率会增加
     */
    spawnEnemy() {
        // 游戏结束时不生成
        if (this.gameOver) return;

        // ==================== 创建敌机 ====================
        // 随机X位置（留出边距避免敌机贴边）
        const x = Phaser.Math.Between(50, 1230);

        // 使用预生成的敌机纹理创建精灵
        const enemy = this.add.image(x, -30, 'enemyShip');  // Y=-30：在屏幕上方生成
        enemy.setScale(0.7);                                 // 缩放到70%

        // 设置敌机血量（取整，因为火力升级后基础血量可能是小数）
        enemy.health = Math.ceil(this.enemyBaseHealth);
        enemy.maxHealth = enemy.health;

        // 添加到敌机物理组
        this.enemies.add(enemy);

        // 为敌机添加物理属性
        this.physics.add.existing(enemy);
        enemy.body.setVelocityY(this.enemySpeed);         // 设置下落速度
        enemy.body.setSize(70, 70);                        // 设置碰撞体大小

        // ==================== 难度递增逻辑 ====================
        // 每得100分，增加敌机速度和生成频率
        if (this.score > 0 && this.score % 100 === 0) {
            // 增加敌机速度（最高350像素/秒）
            this.enemySpeed = Math.min(this.enemySpeed + 15, 350);

            // 减少生成间隔（最短500毫秒）
            const newDelay = Math.max(this.enemyTimer.delay - 50, 500);

            // 重置定时器
            this.enemyTimer.reset({
                delay: newDelay,
                callback: this.spawnEnemy,
                callbackScope: this,
                loop: true
            });
        }
    }

    /**
     * spawnBoss 方法 - 生成BOSS
     *
     * 每击败20架敌机触发一次
     * BOSS出现时：
     * 1. 暂停普通敌机生成
     * 2. 清除屏幕上现有敌机
     * 3. 播放警告音效和显示警告文字
     * 4. 延迟1秒后BOSS入场
     */
    spawnBoss() {
        // 游戏结束或BOSS已存在时不生成
        if (this.gameOver || this.bossActive) return;

        // 标记BOSS存在
        this.bossActive = true;

        // ==================== 暂停普通敌机生成 ====================
        this.enemyTimer.paused = true;

        // ==================== 清除现有敌机 ====================
        // 遍历并销毁所有敌机，为BOSS战清场
        this.enemies.children.iterate((enemy) => {
            if (enemy) {
                enemy.destroy();
            }
        });

        // ==================== 警告效果 ====================
        // 播放BOSS警告音效（上升音调）
        this.audioManager.playBossWarning();

        // 显示警告文字
        const warningText = this.add.text(640, 300, '警告! BOSS来袭!', {
            fontSize: '48px',
            fill: '#ff0000',      // 红色警告
            fontFamily: 'Arial',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        // 警告文字淡出动画
        this.tweens.add({
            targets: warningText,
            alpha: 0,              // 透明度变为0
            duration: 2000,        // 持续2秒
            onComplete: () => warningText.destroy()  // 完成后销毁
        });

        // ==================== 延迟生成BOSS ====================
        // 1秒后BOSS入场（给玩家准备时间）
        this.time.delayedCall(1000, () => {
            // 防止在延迟期间游戏结束
            if (this.gameOver) return;

            // ---------- 创建BOSS ----------
            const boss = this.add.image(640, -80, 'bossShip');  // 从屏幕上方中央进入
            boss.setScale(1.0);                                   // 原始大小
            boss.isBoss = true;                                   // 标记为BOSS

            // BOSS血量 = 基础血量 × 血量倍数
            // 首次：1 × 8 = 8 血
            // 第二次：1.5 × 16 = 24 血（敌机血量已增加50%）
            boss.health = Math.ceil(this.enemyBaseHealth * this.bossHealthMultiplier);
            boss.maxHealth = boss.health;

            // 添加到BOSS物理组
            this.bosses.add(boss);
            this.physics.add.existing(boss);

            // BOSS移动速度较慢
            boss.body.setVelocityY(80);
            boss.body.setSize(140, 120);  // 较大的碰撞体

            // ---------- 创建BOSS血条 ----------
            // 血条背景：灰色矩形
            boss.healthBarBg = this.add.rectangle(640, 60, 300, 20, 0x333333);

            // 血条：紫色矩形（会根据血量缩放）
            boss.healthBar = this.add.rectangle(640, 60, 300, 16, 0xff00ff);

            // 血条标签
            boss.healthText = this.add.text(640, 60, 'BOSS', {
                fontSize: '14px',
                fill: '#ffffff',
                fontFamily: 'Arial'
            }).setOrigin(0.5);

            // ---------- 创建BOSS射击定时器 ----------
            // BOSS每1.5秒发射一次子弹
            boss.fireTimer = this.time.addEvent({
                delay: 1500,                      // 射击间隔：1.5秒
                callback: () => this.bossFireBullet(boss),  // 射击回调
                callbackScope: this,
                loop: true                        // 循环执行
            });
        });
    }

    /**
     * bossFireBullet 方法 - BOSS发射子弹
     *
     * @param {Phaser.GameObjects.Image} boss - BOSS对象
     *
     * BOSS射击模式：
     * - 向玩家方向发射3发散射子弹
     * - 子弹速度适中，给玩家躲避时间
     * - 子弹带有紫色能量弹外观
     */
    bossFireBullet(boss) {
        // 游戏结束或BOSS不存在时不发射
        if (this.gameOver || !boss || !boss.active) return;

        // BOSS还在下降阶段时不发射（等到开始左右移动后再射击）
        if (!boss.moving) return;

        // ==================== 计算射击角度 ====================
        // 计算BOSS到玩家的角度
        const angleToPlayer = Phaser.Math.Angle.Between(
            boss.x, boss.y,
            this.player.x, this.player.y
        );

        // ==================== 发射散射子弹 ====================
        // 发射3发子弹：中间一发直射，左右各一发偏移15度
        const spreadAngles = [-0.26, 0, 0.26];  // -15度, 0度, +15度（弧度）
        const bulletSpeed = 350;                 // 子弹速度（像素/秒）

        spreadAngles.forEach(spread => {
            // 计算当前子弹的角度
            const bulletAngle = angleToPlayer + spread;

            // 创建子弹
            const bullet = this.add.image(boss.x, boss.y + 50, 'bossBullet');
            bullet.setScale(1.2);  // 略微放大便于识别

            // 添加到BOSS子弹组
            this.bossBullets.add(bullet);
            this.physics.add.existing(bullet);

            // 设置子弹速度（根据角度计算X和Y分量）
            bullet.body.setVelocity(
                Math.cos(bulletAngle) * bulletSpeed,
                Math.sin(bulletAngle) * bulletSpeed
            );

            // 设置碰撞体大小
            bullet.body.setSize(16, 24);

            // 设置子弹旋转角度（指向飞行方向）
            bullet.rotation = bulletAngle + Math.PI / 2;
        });

        // 播放BOSS射击音效（使用现有的射击音效，但音调不同）
        this.audioManager.playShoot();
    }

    /**
     * fireBullet 方法 - 发射子弹
     *
     * 自动射击机制：
     * - 每帧调用此方法
     * - 检查是否达到射击间隔
     * - 根据当前火力等级发射对应列数的子弹
     * - 播放射击音效
     */
    fireBullet() {
        // 获取当前时间
        const time = this.time.now;

        // 检查是否达到射击间隔（fireRate 毫秒）
        if (time > this.lastFired + this.fireRate) {
            // ==================== 计算子弹位置 ====================
            const spacing = 10;  // 多列子弹之间的间距（像素）

            // 计算所有子弹的总宽度
            const totalWidth = (this.bulletColumns - 1) * spacing;

            // 计算最左边子弹的X坐标（使子弹群居中于玩家）
            const startX = this.player.x - totalWidth / 2;

            // ==================== 发射子弹 ====================
            // 根据当前火力等级发射对应列数的子弹
            for (let i = 0; i < this.bulletColumns; i++) {
                // 计算当前子弹的X坐标
                const bulletX = startX + i * spacing;

                // 创建子弹精灵
                const bullet = this.add.image(bulletX, this.player.y - 40, 'bullet');
                bullet.setScale(1.5);   // 缩放到150%

                // 添加到子弹物理组
                this.bullets.add(bullet);
                this.physics.add.existing(bullet);

                // 设置子弹向上飞行（负Y速度）
                bullet.body.setVelocityY(-this.bulletSpeed);
            }

            // 记录发射时间
            this.lastFired = time;

            // 播放射击音效
            this.audioManager.playShoot();
        }
    }

    /**
     * bulletHitEnemy 方法 - 子弹击中普通敌机的回调
     *
     * @param {Phaser.GameObjects.Image} bullet - 击中的子弹
     * @param {Phaser.GameObjects.Image} enemy - 被击中的敌机
     *
     * 处理逻辑：
     * 1. 销毁子弹
     * 2. 减少敌机血量
     * 3. 如果敌机血量为0，播放爆炸效果，增加分数
     * 4. 检查是否触发BOSS
     */
    bulletHitEnemy(bullet, enemy) {
        // 销毁击中的子弹
        bullet.destroy();

        // 减少敌机血量
        enemy.health -= 1;

        if (enemy.health <= 0) {
            // ==================== 敌机被击毁 ====================
            // 创建爆炸视觉效果
            this.createExplosion(enemy.x, enemy.y);

            // 播放爆炸音效
            this.audioManager.playEnemyExplosion();

            // 销毁敌机
            enemy.destroy();

            // 增加分数：普通敌机 +10 分
            this.score += 10;

            // 击败敌机计数（用于触发BOSS）
            this.enemiesDefeated += 1;

            // 更新分数显示
            this.scoreText.setText('分数: ' + this.score);

            // ---------- 检查是否触发BOSS ----------
            // 每击败20架敌机生成BOSS
            if (this.enemiesDefeated % 20 === 0 && !this.bossActive) {
                this.spawnBoss();
            }
        } else {
            // ==================== 敌机受伤但未被击毁 ====================
            // 显示受伤闪烁效果
            this.tweens.add({
                targets: enemy,
                alpha: 0.5,          // 透明度变为50%
                duration: 50,        // 持续50毫秒
                yoyo: true           // 来回（自动恢复）
            });
        }
    }

    /**
     * bulletHitBoss 方法 - 子弹击中BOSS的回调
     *
     * @param {Phaser.GameObjects.Image} bullet - 击中的子弹
     * @param {Phaser.GameObjects.Image} boss - 被击中的BOSS
     *
     * 处理逻辑：
     * 1. 销毁子弹
     * 2. 减少BOSS血量
     * 3. 更新血条显示
     * 4. 如果BOSS被击败，触发奖励逻辑
     */
    bulletHitBoss(bullet, boss) {
        // 销毁击中的子弹
        bullet.destroy();

        // 减少BOSS血量
        boss.health -= 1;

        // ==================== 更新血条显示 ====================
        // 计算当前血量百分比
        const healthPercent = boss.health / boss.maxHealth;

        // 缩放血条（只缩放X轴，保持高度不变）
        boss.healthBar.setScale(healthPercent, 1);

        // 调整血条位置（使血条从左向右减少）
        boss.healthBar.x = 640 - (300 * (1 - healthPercent)) / 2;

        if (boss.health <= 0) {
            // ==================== BOSS被击败 ====================
            // 创建大型爆炸效果
            this.createBossExplosion(boss.x, boss.y);

            // 播放BOSS爆炸音效
            this.audioManager.playBossExplosion();

            // ---------- 清理BOSS相关对象 ----------
            // 停止BOSS射击定时器
            if (boss.fireTimer) {
                boss.fireTimer.remove();
            }

            // 清除所有BOSS子弹
            this.bossBullets.children.iterate((bullet) => {
                if (bullet) {
                    bullet.destroy();
                }
            });

            boss.healthBarBg.destroy();   // 血条背景
            boss.healthBar.destroy();     // 血条
            boss.healthText.destroy();    // 血条标签
            boss.destroy();               // BOSS本体

            // 重置BOSS状态
            this.bossActive = false;
            this.bossDefeated += 1;

            // 增加分数：BOSS +100 分
            this.score += 100;
            this.scoreText.setText('分数: ' + this.score);

            // ---------- 恢复敌机生成 ----------
            this.enemyTimer.paused = false;

            // ---------- 下一个BOSS血量翻倍 ----------
            this.bossHealthMultiplier *= 2;

            // ---------- 火力升级逻辑 ----------
            // 击杀BOSS后增加一列子弹（最多4列）
            if (this.bulletColumns < 4) {
                this.bulletColumns += 1;

                // 同时增加敌机血量50%（保持难度平衡）
                this.enemyBaseHealth *= 1.5;

                // 播放升级音效
                this.audioManager.playUpgrade();

                // 显示升级提示
                const upgradeText = this.add.text(640, 360, 'BOSS击杀! 火力升级!\n' + this.bulletColumns + '列子弹', {
                    fontSize: '36px',
                    fill: '#ff00ff',    // 紫色
                    fontFamily: 'Arial',
                    align: 'center'
                }).setOrigin(0.5);

                // 升级提示向上淡出
                this.tweens.add({
                    targets: upgradeText,
                    alpha: 0,            // 淡出
                    y: 280,              // 向上移动
                    duration: 2000,      // 持续2秒
                    onComplete: () => upgradeText.destroy()
                });
            }
        } else {
            // ==================== BOSS受伤但未被击败 ====================
            // 显示受伤闪烁效果
            this.tweens.add({
                targets: boss,
                alpha: 0.5,          // 透明度变为50%
                duration: 100,       // 持续100毫秒（比普通敌机长）
                yoyo: true           // 来回
            });
        }
    }

    /**
     * createBossExplosion 方法 - 创建BOSS爆炸效果
     *
     * @param {number} x - 爆炸中心X坐标
     * @param {number} y - 爆炸中心Y坐标
     *
     * BOSS爆炸比普通敌机更大、更华丽：
     * - 30个粒子（普通敌机12个）
     * - 更大的粒子尺寸
     * - 更大的扩散范围
     * - 更长的持续时间
     * - 包含紫色和青色（BOSS配色）
     */
    createBossExplosion(x, y) {
        // BOSS配色：紫色、青色、白色、红色
        const colors = [0xff00ff, 0x8800ff, 0x00ffff, 0xffffff, 0xff4444];

        // 创建30个爆炸粒子
        for (let i = 0; i < 30; i++) {
            // 计算粒子飞行角度（均匀分布在360度）
            const angle = (i / 30) * Math.PI * 2;

            // 创建粒子（较大的圆形，6-16像素）
            const particle = this.add.circle(x, y, Phaser.Math.Between(6, 16), Phaser.Utils.Array.GetRandom(colors));

            // 粒子飞行动画
            this.tweens.add({
                targets: particle,
                // 向外扩散（60-150像素距离）
                x: x + Math.cos(angle) * Phaser.Math.Between(60, 150),
                y: y + Math.sin(angle) * Phaser.Math.Between(60, 150),
                alpha: 0,            // 淡出
                scale: 0.2,          // 缩小
                duration: 1600,      // 持续1600毫秒
                onComplete: () => particle.destroy()  // 完成后销毁
            });
        }
    }

    /**
     * createExplosion 方法 - 创建普通敌机爆炸效果
     *
     * @param {number} x - 爆炸中心X坐标
     * @param {number} y - 爆炸中心Y坐标
     *
     * 使用多个彩色圆形粒子向外扩散模拟爆炸效果
     */
    createExplosion(x, y) {
        // 爆炸颜色：红色、橙色、黄色、白色（火焰配色）
        const colors = [0xff4444, 0xff8844, 0xffff44, 0xffffff];

        // 创建12个爆炸粒子
        for (let i = 0; i < 12; i++) {
            // 计算粒子飞行角度（均匀分布在360度）
            const angle = (i / 12) * Math.PI * 2;

            // 创建粒子（小圆形，4-8像素）
            const particle = this.add.circle(x, y, Phaser.Math.Between(4, 8), Phaser.Utils.Array.GetRandom(colors));

            // 粒子飞行动画
            this.tweens.add({
                targets: particle,
                // 向外扩散（30-60像素距离）
                x: x + Math.cos(angle) * Phaser.Math.Between(30, 60),
                y: y + Math.sin(angle) * Phaser.Math.Between(30, 60),
                alpha: 0,            // 淡出
                scale: 0.3,          // 缩小
                duration: 800,       // 持续800毫秒
                onComplete: () => particle.destroy()  // 完成后销毁
            });
        }
    }

    /**
     * playerHit 方法 - 玩家被击中的回调
     *
     * 处理游戏结束逻辑：
     * 1. 播放爆炸效果和音效
     * 2. 停止背景音乐
     * 3. 隐藏玩家战机
     * 4. 停止所有移动物体
     * 5. 显示游戏结束界面
     */
    playerHit() {
        // 防止重复触发
        if (this.gameOver) return;

        // 标记游戏结束
        this.gameOver = true;

        // ==================== 玩家死亡效果 ====================
        // 创建爆炸视觉效果
        this.createExplosion(this.player.x, this.player.y);

        // 播放玩家死亡音效
        this.audioManager.playPlayerDeath();

        // 停止背景音乐
        this.audioManager.stopBgm();

        // 隐藏玩家战机（而不是销毁，便于重启时恢复）
        this.player.setVisible(false);

        // 移除敌机生成定时器
        this.enemyTimer.remove();

        // ==================== 停止所有移动物体 ====================
        // 停止所有敌机
        this.enemies.children.iterate((enemy) => {
            if (enemy && enemy.body) {
                enemy.body.setVelocity(0);
            }
        });

        // 停止所有BOSS及其射击定时器
        this.bosses.children.iterate((boss) => {
            if (boss && boss.body) {
                boss.body.setVelocity(0);
                // 停止BOSS射击
                if (boss.fireTimer) {
                    boss.fireTimer.remove();
                }
            }
        });

        // 停止所有玩家子弹
        this.bullets.children.iterate((bullet) => {
            if (bullet && bullet.body) {
                bullet.body.setVelocity(0);
            }
        });

        // 停止所有BOSS子弹
        this.bossBullets.children.iterate((bullet) => {
            if (bullet && bullet.body) {
                bullet.body.setVelocity(0);
            }
        });

        // ==================== 显示游戏结束界面 ====================
        this.gameOverText.setVisible(true);   // "游戏结束!"
        this.restartText.setVisible(true);    // "按 R 键重新开始"
        this.menuText.setVisible(true);       // "按 M 键返回菜单"

        // 显示最终战绩
        this.add.text(640, 360, '击落敌机: ' + Math.floor(this.score / 10) + ' 架', {
            fontSize: '36px',
            fill: '#ffff00',     // 黄色
            fontFamily: 'Arial'
        }).setOrigin(0.5);
    }

    /**
     * update 方法 - 每帧调用的更新函数
     *
     * Phaser 会在每一帧自动调用此方法（通常60fps）
     *
     * 更新内容：
     * 1. 星星背景动画
     * 2. 游戏结束时的按键检测
     * 3. 玩家移动
     * 4. 自动射击
     * 5. 清理屏幕外的对象
     * 6. BOSS移动逻辑
     */
    update() {
        // ==================== 更新星星背景 ====================
        // 让星星持续向下移动，创造飞行效果
        this.stars.forEach(star => {
            // 根据星星速度向下移动
            star.y += star.speed;

            // 当星星移出屏幕底部时，重置到顶部
            if (star.y > 720) {
                star.y = 0;                                  // 重置Y坐标
                star.x = Phaser.Math.Between(0, 1280);      // 随机新X坐标
            }
        });

        // ==================== 游戏结束状态处理 ====================
        if (this.gameOver) {
            // ---------- 按R键重新开始 ----------
            if (Phaser.Input.Keyboard.JustDown(this.keyR)) {
                // 重新启动BGM
                if (this.audioManager && !this.audioManager.isMuted) {
                    this.audioManager.startBgm();
                }
                // 重启当前场景（所有状态会重置）
                this.scene.restart();
            }

            // ---------- 按M键返回菜单 ----------
            if (Phaser.Input.Keyboard.JustDown(this.keyM)) {
                // 清理AudioManager（完全释放音频资源）
                if (this.audioManager) {
                    this.audioManager.stopBgm();    // 停止BGM
                    this.audioManager.destroy();    // 销毁AudioManager
                    this.registry.set('audioManager', null);  // 从注册表移除
                }
                // 返回菜单场景
                this.scene.start('Menu');
            }

            // 游戏结束时不执行后续游戏逻辑
            return;
        }

        // ==================== 玩家移动控制 ====================
        const speed = 630;  // 玩家移动速度（像素/秒）

        // 检测左移按键（左方向键或A键）
        if (this.cursors.left.isDown || this.keyA.isDown) {
            this.player.body.setVelocityX(-speed);  // 向左移动
        }
        // 检测右移按键（右方向键或D键）
        else if (this.cursors.right.isDown || this.keyD.isDown) {
            this.player.body.setVelocityX(speed);   // 向右移动
        }
        // 没有按键时停止移动
        else {
            this.player.body.setVelocityX(0);
        }

        // ==================== 自动发射子弹 ====================
        // 每帧调用，内部会检查射击间隔
        this.fireBullet();

        // ==================== 清理屏幕外的敌机 ====================
        // 敌机移出屏幕底部后销毁，释放内存
        this.enemies.children.iterate((enemy) => {
            if (enemy && enemy.y > 780) {  // 留出一点缓冲距离
                enemy.destroy();
            }
        });

        // ==================== BOSS移动逻辑 ====================
        // BOSS到达指定位置后会在屏幕上方左右移动
        this.bosses.children.iterate((boss) => {
            if (boss) {
                // ---------- 检查是否到达停止下降的位置 ----------
                // BOSS下降到Y=120时停止，开始左右移动
                if (boss.y >= 120 && !boss.moving) {
                    boss.body.setVelocityY(0);      // 停止下降
                    boss.body.setVelocityX(100);    // 开始向右移动
                    boss.moving = true;              // 标记已开始左右移动
                }

                // ---------- 左右边界反弹 ----------
                if (boss.moving) {
                    // 到达左边界，改为向右
                    if (boss.x <= 100) {
                        boss.body.setVelocityX(100);
                    }
                    // 到达右边界，改为向左
                    else if (boss.x >= 1180) {
                        boss.body.setVelocityX(-100);
                    }
                }

                // ---------- 清理意外移出屏幕的BOSS ----------
                // 正常情况下BOSS不会移出屏幕，这是一个安全检查
                if (boss.y > 800) {
                    // 停止BOSS射击定时器
                    if (boss.fireTimer) boss.fireTimer.remove();
                    // 销毁BOSS及其血条
                    if (boss.healthBarBg) boss.healthBarBg.destroy();
                    if (boss.healthBar) boss.healthBar.destroy();
                    if (boss.healthText) boss.healthText.destroy();
                    boss.destroy();
                    this.bossActive = false;  // 重置BOSS状态
                }
            }
        });

        // ==================== 清理屏幕外的子弹 ====================
        // 子弹飞出屏幕顶部后销毁，释放内存
        this.bullets.children.iterate((bullet) => {
            if (bullet && bullet.y < -20) {  // Y < -20 表示已完全飞出
                bullet.destroy();
            }
        });

        // ==================== 清理屏幕外的BOSS子弹 ====================
        // BOSS子弹可能向任何方向飞行，检查所有边界
        this.bossBullets.children.iterate((bullet) => {
            if (bullet) {
                // 检查是否移出屏幕边界（上下左右都要检查）
                if (bullet.y > 780 || bullet.y < -30 ||
                    bullet.x < -30 || bullet.x > 1310) {
                    bullet.destroy();
                }
            }
        });
    }

    /**
     * shutdown 方法 - 场景关闭时调用
     *
     * 注意：这里不主动清理AudioManager
     * - 重启时：保持AudioManager，BGM继续播放
     * - 返回菜单时：在按M键的处理逻辑中显式清理
     *
     * 这样设计是为了避免重启游戏时BGM中断
     */
    shutdown() {
        // AudioManager的清理在返回菜单时的按键处理中显式执行
        // 这里留空是有意为之
    }

}
