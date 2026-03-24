/**
 * ============================================================================
 * 音频管理器 (AudioManager)
 * ============================================================================
 *
 * 功能说明：
 * - 使用 Web Audio API 生成所有游戏音效和背景音乐
 * - 无需加载外部音频文件，所有声音都是程序合成的
 * - 支持静音控制和音量调节
 *
 * 包含的音效：
 * - 射击音效 (playShoot)
 * - 敌机爆炸音效 (playEnemyExplosion)
 * - BOSS爆炸音效 (playBossExplosion)
 * - 玩家死亡音效 (playPlayerDeath)
 * - 升级音效 (playUpgrade)
 * - BOSS警告音效 (playBossWarning)
 * - 科幻风格背景音乐 (startBgm)
 * ============================================================================
 */

export class AudioManager {
    /**
     * 构造函数 - 初始化音频管理器
     * @param {Phaser.Scene} scene - Phaser场景对象引用
     */
    constructor(scene) {
        this.scene = scene;                    // Phaser场景引用
        this.audioContext = null;              // Web Audio API 上下文
        this.masterGain = null;                // 主音量控制节点
        this.isMuted = false;                  // 静音状态标志
        this.bgmOscillators = [];              // 背景音乐振荡器数组（未使用，保留）
        this.bgmGain = null;                   // 背景音乐音量控制节点
        this.isPlayingBgm = false;             // 背景音乐播放状态标志

        // 初始化音频系统
        this.init();
    }

    /**
     * 初始化音频上下文
     * - 创建 AudioContext（兼容不同浏览器）
     * - 设置主音量节点
     * - 尝试恢复可能被浏览器暂停的音频上下文
     */
    init() {
        try {
            // 创建音频上下文（兼容旧版Safari使用webkitAudioContext）
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();

            // 创建主音量控制节点
            this.masterGain = this.audioContext.createGain();
            // 连接到音频输出（扬声器）
            this.masterGain.connect(this.audioContext.destination);
            // 设置主音量为50%
            this.masterGain.gain.value = 0.5;

            // 浏览器自动播放策略可能导致上下文被暂停，尝试恢复
            if (this.audioContext.state === 'suspended') {
                this.audioContext.resume();
            }

            console.log('AudioContext initialized, state:', this.audioContext.state);
        } catch (e) {
            console.warn('Web Audio API not supported:', e);
        }
    }

    /**
     * 恢复音频上下文
     * - 浏览器为防止自动播放，可能会暂停音频上下文
     * - 此方法尝试恢复到运行状态
     */
    resumeContext() {
        if (this.audioContext && this.audioContext.state === 'suspended') {
            this.audioContext.resume().catch(e => console.log('Audio resume failed:', e));
        }
    }

    /**
     * 检查音频是否就绪可用
     * @returns {boolean} 音频是否可以播放
     */
    isAudioReady() {
        return this.audioContext && this.audioContext.state === 'running';
    }

    /**
     * 设置静音状态
     * @param {boolean} muted - true为静音，false为取消静音
     */
    setMuted(muted) {
        this.isMuted = muted;
        if (this.masterGain) {
            // 静音时音量设为0，否则恢复到0.5
            this.masterGain.gain.value = muted ? 0 : 0.5;
        }
    }

    // ========================================================================
    // 游戏音效部分
    // ========================================================================

    /**
     * 播放射击音效
     * - 短促的激光声效果
     * - 使用方波振荡器，频率从880Hz快速下降到220Hz
     * - 持续时间：0.1秒
     */
    playShoot() {
        // 检查音频上下文是否可用，是否静音
        if (!this.audioContext || this.isMuted) return;
        this.resumeContext();

        // 创建振荡器（声音源）和增益节点（音量控制）
        const osc = this.audioContext.createOscillator();
        const gain = this.audioContext.createGain();

        // 连接音频节点：振荡器 -> 增益 -> 主音量
        osc.connect(gain);
        gain.connect(this.masterGain);

        // 设置方波波形，产生电子/复古游戏风格的声音
        osc.type = 'square';
        // 频率从880Hz（高音A5）开始
        osc.frequency.setValueAtTime(880, this.audioContext.currentTime);
        // 指数衰减到220Hz（低音A3），产生"滋"的下降效果
        osc.frequency.exponentialRampToValueAtTime(220, this.audioContext.currentTime + 0.1);

        // 音量包络：快速淡出
        gain.gain.setValueAtTime(0.06, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.1);

        // 开始播放并在0.1秒后停止
        osc.start(this.audioContext.currentTime);
        osc.stop(this.audioContext.currentTime + 0.1);
    }

    /**
     * 播放敌机爆炸音效
     * - 使用白噪音模拟爆炸
     * - 通过低通滤波器使声音逐渐变闷
     * - 持续时间：0.3秒
     */
    playEnemyExplosion() {
        if (!this.audioContext || this.isMuted) return;
        this.resumeContext();

        // 计算噪音缓冲区大小（0.3秒的采样数）
        const bufferSize = this.audioContext.sampleRate * 0.3;
        // 创建单声道音频缓冲区
        const buffer = this.audioContext.createBuffer(1, bufferSize, this.audioContext.sampleRate);
        // 获取缓冲区数据数组
        const data = buffer.getChannelData(0);

        // 生成白噪音，并应用衰减包络
        for (let i = 0; i < bufferSize; i++) {
            // Math.random() * 2 - 1 生成-1到1之间的随机数
            // Math.pow(1 - i / bufferSize, 2) 创建二次衰减曲线
            data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 2);
        }

        // 创建缓冲区源节点
        const noise = this.audioContext.createBufferSource();
        noise.buffer = buffer;

        // 创建音量和滤波器节点
        const gain = this.audioContext.createGain();
        const filter = this.audioContext.createBiquadFilter();

        // 设置低通滤波器，截止频率从1000Hz衰减到100Hz
        // 这样爆炸声开始时明亮，结束时变得沉闷
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(1000, this.audioContext.currentTime);
        filter.frequency.exponentialRampToValueAtTime(100, this.audioContext.currentTime + 0.3);

        // 连接音频节点链：噪音源 -> 滤波器 -> 增益 -> 主音量
        noise.connect(filter);
        filter.connect(gain);
        gain.connect(this.masterGain);

        // 设置音量包络
        gain.gain.setValueAtTime(0.2, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.3);

        // 开始播放
        noise.start(this.audioContext.currentTime);
    }

    /**
     * 播放BOSS爆炸音效
     * - 比普通爆炸更大、更长
     * - 包含3层错开的噪音爆炸
     * - 额外添加低频冲击波效果
     * - 总持续时间：约0.8秒
     */
    playBossExplosion() {
        if (!this.audioContext || this.isMuted) return;
        this.resumeContext();

        // 创建3层错开的爆炸效果，每层间隔100毫秒
        for (let i = 0; i < 3; i++) {
            setTimeout(() => {
                // 0.5秒的噪音缓冲区
                const bufferSize = this.audioContext.sampleRate * 0.5;
                const buffer = this.audioContext.createBuffer(1, bufferSize, this.audioContext.sampleRate);
                const data = buffer.getChannelData(0);

                // 生成带衰减的白噪音（使用1.5次方衰减，比普通爆炸更慢）
                for (let j = 0; j < bufferSize; j++) {
                    data[j] = (Math.random() * 2 - 1) * Math.pow(1 - j / bufferSize, 1.5);
                }

                const noise = this.audioContext.createBufferSource();
                noise.buffer = buffer;

                const gain = this.audioContext.createGain();
                const filter = this.audioContext.createBiquadFilter();

                // 低通滤波器设置
                filter.type = 'lowpass';
                filter.frequency.setValueAtTime(800, this.audioContext.currentTime);
                filter.frequency.exponentialRampToValueAtTime(50, this.audioContext.currentTime + 0.5);

                noise.connect(filter);
                filter.connect(gain);
                gain.connect(this.masterGain);

                gain.gain.setValueAtTime(0.25, this.audioContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.5);

                noise.start(this.audioContext.currentTime);
            }, i * 100);  // 每层间隔100毫秒
        }

        // 添加低频冲击波效果（模拟爆炸的震感）
        const osc = this.audioContext.createOscillator();
        const gain = this.audioContext.createGain();

        osc.connect(gain);
        gain.connect(this.masterGain);

        // 使用正弦波，频率从100Hz下降到20Hz（接近次声波）
        osc.type = 'sine';
        osc.frequency.setValueAtTime(100, this.audioContext.currentTime);
        osc.frequency.exponentialRampToValueAtTime(20, this.audioContext.currentTime + 0.8);

        gain.gain.setValueAtTime(0.3, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.8);

        osc.start(this.audioContext.currentTime);
        osc.stop(this.audioContext.currentTime + 0.8);
    }

    /**
     * 播放玩家死亡音效
     * - 下降的音调表示失败
     * - 使用锯齿波创造刺耳的警告感
     * - 配合爆炸噪音
     */
    playPlayerDeath() {
        if (!this.audioContext || this.isMuted) return;
        this.resumeContext();

        // 创建下降音调效果
        const osc = this.audioContext.createOscillator();
        const gain = this.audioContext.createGain();

        osc.connect(gain);
        gain.connect(this.masterGain);

        // 锯齿波产生较为刺耳的声音
        osc.type = 'sawtooth';
        // 频率从400Hz下降到50Hz，产生"坠落"感
        osc.frequency.setValueAtTime(400, this.audioContext.currentTime);
        osc.frequency.exponentialRampToValueAtTime(50, this.audioContext.currentTime + 0.8);

        gain.gain.setValueAtTime(0.25, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.8);

        osc.start(this.audioContext.currentTime);
        osc.stop(this.audioContext.currentTime + 0.8);

        // 同时播放爆炸噪音
        this.playEnemyExplosion();
    }

    /**
     * 播放升级音效
     * - 上升的琶音表示正面反馈
     * - 使用C大调和弦音符：C5, E5, G5, C6
     */
    playUpgrade() {
        if (!this.audioContext || this.isMuted) return;
        this.resumeContext();

        // C大调和弦的音符频率（Hz）
        const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6

        // 依次播放每个音符，间隔100毫秒
        notes.forEach((freq, i) => {
            setTimeout(() => {
                const osc = this.audioContext.createOscillator();
                const gain = this.audioContext.createGain();

                osc.connect(gain);
                gain.connect(this.masterGain);

                // 使用正弦波产生纯净的音色
                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, this.audioContext.currentTime);

                gain.gain.setValueAtTime(0.15, this.audioContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.2);

                osc.start(this.audioContext.currentTime);
                osc.stop(this.audioContext.currentTime + 0.2);
            }, i * 100);
        });
    }

    /**
     * 播放BOSS警告音效
     * - 3声短促的警报声
     * - 使用方波产生警报效果
     */
    playBossWarning() {
        if (!this.audioContext || this.isMuted) return;
        this.resumeContext();

        // 播放3次警报声，间隔300毫秒
        for (let i = 0; i < 3; i++) {
            setTimeout(() => {
                const osc = this.audioContext.createOscillator();
                const gain = this.audioContext.createGain();

                osc.connect(gain);
                gain.connect(this.masterGain);

                // 方波200Hz，典型的警报频率
                osc.type = 'square';
                osc.frequency.setValueAtTime(200, this.audioContext.currentTime);

                // 突然开始，突然结束的音量包络
                gain.gain.setValueAtTime(0.15, this.audioContext.currentTime);
                gain.gain.setValueAtTime(0, this.audioContext.currentTime + 0.15);

                osc.start(this.audioContext.currentTime);
                osc.stop(this.audioContext.currentTime + 0.15);
            }, i * 300);
        }
    }

    // ========================================================================
    // 背景音乐部分 - 科幻电子风格
    // ========================================================================

    /**
     * 开始播放背景音乐
     * - 检查各种状态后调用 beginBgm
     */
    startBgm() {
        // 检查音频上下文
        if (!this.audioContext) {
            console.log('BGM start blocked: no audio context');
            return;
        }

        // 防止重复播放
        if (this.isPlayingBgm) {
            console.log('BGM already playing');
            return;
        }

        // 静音状态不播放
        if (this.isMuted) {
            console.log('BGM blocked: muted');
            return;
        }

        console.log('Starting BGM, context state:', this.audioContext.state);

        // 开始播放BGM
        this.beginBgm();
    }

    /**
     * 实际开始BGM播放
     * - 如果音频上下文被暂停，会延迟重试
     */
    beginBgm() {
        if (this.isPlayingBgm || !this.audioContext) return;

        console.log('beginBgm called, context state:', this.audioContext.state);

        // 如果上下文仍然挂起，500毫秒后重试
        if (this.audioContext.state === 'suspended') {
            console.log('Context still suspended, will retry in 500ms');
            setTimeout(() => {
                if (!this.isPlayingBgm && !this.isMuted) {
                    this.beginBgm();
                }
            }, 500);
            return;
        }

        console.log('BGM actually starting now!');
        this.isPlayingBgm = true;

        // 创建BGM专用的音量控制节点
        this.bgmGain = this.audioContext.createGain();
        this.bgmGain.connect(this.masterGain);
        this.bgmGain.gain.value = 0.6;  // BGM音量

        // 初始化节拍计数器
        this.beatCount = 0;
        // 开始BGM循环
        this.playBgmLoop();
    }

    /**
     * BGM主循环
     * - 140 BPM的节奏
     * - 每个节拍播放不同的音乐元素
     */
    playBgmLoop() {
        // 检查是否应该继续播放
        if (!this.isPlayingBgm || this.isMuted || !this.audioContext) {
            return;
        }

        // 尝试恢复可能被暂停的上下文
        if (this.audioContext.state === 'suspended') {
            this.audioContext.resume();
        }

        // 音乐参数
        const bpm = 140;                        // 每分钟节拍数
        const beatDuration = 60 / bpm;          // 每拍持续时间（秒）

        // 低音贝斯音符模式（A小调）
        // A1=55Hz, D2=73.42Hz, C2=65.41Hz, E2=82.41Hz
        const bassPattern = [55, 55, 73.42, 65.41, 55, 82.41, 73.42, 65.41];
        const bassIndex = this.beatCount % bassPattern.length;

        // 每拍播放低音贝斯
        this.playSciFiBass(bassPattern[bassIndex], beatDuration * 0.8);

        // 每2拍播放合成器和弦垫音
        if (this.beatCount % 2 === 0) {
            this.playSciFiPad(beatDuration * 1.8);
        }

        // 每4拍播放琶音
        if (this.beatCount % 4 === 0) {
            this.playArpeggio(beatDuration);
        }

        // 每8拍播放科幻扫频效果
        if (this.beatCount % 8 === 0) {
            this.playSciFiEffect();
        }

        // 每拍播放鼓点
        this.playDrum(this.beatCount, beatDuration);

        // 增加节拍计数
        this.beatCount++;

        // 设置下一拍的定时器
        this.bgmTimeout = setTimeout(() => {
            if (this.isPlayingBgm) {
                this.playBgmLoop();
            }
        }, beatDuration * 1000);
    }

    /**
     * 播放科幻风格低音贝斯
     * - 双振荡器叠加产生厚实的低音
     * - 使用低通滤波器塑造音色
     * @param {number} freq - 音符频率
     * @param {number} duration - 持续时间
     */
    playSciFiBass(freq, duration) {
        if (!this.audioContext || !this.isPlayingBgm) return;

        // 创建两个振荡器叠加
        const osc = this.audioContext.createOscillator();
        const osc2 = this.audioContext.createOscillator();
        const gain = this.audioContext.createGain();
        const filter = this.audioContext.createBiquadFilter();

        // 连接音频节点
        osc.connect(filter);
        osc2.connect(filter);
        filter.connect(gain);
        gain.connect(this.bgmGain);

        // 振荡器1：锯齿波，主要音高
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(freq, this.audioContext.currentTime);

        // 振荡器2：方波，低八度（频率减半）
        osc2.type = 'square';
        osc2.frequency.setValueAtTime(freq * 0.5, this.audioContext.currentTime);

        // 低通滤波器：截止频率从200Hz衰减到80Hz
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(200, this.audioContext.currentTime);
        filter.frequency.exponentialRampToValueAtTime(80, this.audioContext.currentTime + duration);

        // 音量包络
        gain.gain.setValueAtTime(0.35, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + duration);

        // 启动和停止振荡器
        osc.start(this.audioContext.currentTime);
        osc2.start(this.audioContext.currentTime);
        osc.stop(this.audioContext.currentTime + duration);
        osc2.stop(this.audioContext.currentTime + duration);
    }

    /**
     * 播放科幻垫音（Pad）
     * - A小调和弦：A3, C4, E4
     * - 轻微失谐创造更宽广的立体声效果
     * @param {number} duration - 持续时间
     */
    playSciFiPad(duration) {
        if (!this.audioContext || !this.isPlayingBgm) return;

        // A小调和弦音符
        const chordFreqs = [220, 261.63, 329.63]; // A3, C4, E4

        chordFreqs.forEach((freq, i) => {
            const osc = this.audioContext.createOscillator();
            const gain = this.audioContext.createGain();
            const filter = this.audioContext.createBiquadFilter();

            osc.connect(filter);
            filter.connect(gain);
            gain.connect(this.bgmGain);

            osc.type = 'sine';
            // 轻微失谐：每个音符偏移0.2%，创造合唱效果
            osc.frequency.setValueAtTime(freq * (1 + (i - 1) * 0.002), this.audioContext.currentTime);

            filter.type = 'lowpass';
            filter.frequency.setValueAtTime(800, this.audioContext.currentTime);

            // ADSR包络：缓慢起音，持续，缓慢衰减
            gain.gain.setValueAtTime(0, this.audioContext.currentTime);
            gain.gain.linearRampToValueAtTime(0.08, this.audioContext.currentTime + 0.1);      // Attack
            gain.gain.linearRampToValueAtTime(0.05, this.audioContext.currentTime + duration * 0.5);  // Sustain
            gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + duration);    // Release

            osc.start(this.audioContext.currentTime);
            osc.stop(this.audioContext.currentTime + duration);
        });
    }

    /**
     * 播放琶音
     * - 快速上下行的音符序列
     * - 使用三角波产生柔和的音色
     * @param {number} beatDuration - 一拍的持续时间
     */
    playArpeggio(beatDuration) {
        if (!this.audioContext || !this.isPlayingBgm) return;

        // 琶音音符序列：上行后下行
        const notes = [440, 523.25, 659.25, 783.99, 659.25, 523.25]; // A4, C5, E5, G5, E5, C5
        const noteLength = beatDuration * 0.15;  // 每个音符很短

        notes.forEach((freq, i) => {
            setTimeout(() => {
                if (!this.isPlayingBgm || this.isMuted) return;

                const osc = this.audioContext.createOscillator();
                const gain = this.audioContext.createGain();

                osc.connect(gain);
                gain.connect(this.bgmGain);

                // 三角波产生柔和的声音
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(freq, this.audioContext.currentTime);

                gain.gain.setValueAtTime(0.12, this.audioContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + noteLength);

                osc.start(this.audioContext.currentTime);
                osc.stop(this.audioContext.currentTime + noteLength);
            }, i * noteLength * 1000);
        });
    }

    /**
     * 播放科幻扫频效果
     * - 高频到低频的快速滑音
     * - 增加科幻氛围
     */
    playSciFiEffect() {
        if (!this.audioContext || !this.isPlayingBgm) return;

        const osc = this.audioContext.createOscillator();
        const gain = this.audioContext.createGain();

        osc.connect(gain);
        gain.connect(this.bgmGain);

        // 正弦波从2000Hz快速下降到200Hz
        osc.type = 'sine';
        osc.frequency.setValueAtTime(2000, this.audioContext.currentTime);
        osc.frequency.exponentialRampToValueAtTime(200, this.audioContext.currentTime + 0.5);

        // 音量很低，作为背景氛围
        gain.gain.setValueAtTime(0.05, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.5);

        osc.start(this.audioContext.currentTime);
        osc.stop(this.audioContext.currentTime + 0.5);
    }

    /**
     * 播放鼓点
     * - 包含底鼓、军鼓和踩镲
     * @param {number} beat - 当前节拍数
     * @param {number} duration - 一拍的持续时间
     */
    playDrum(beat, duration) {
        if (!this.audioContext || !this.isPlayingBgm) return;

        // ========== 底鼓 ==========
        // 每2拍播放一次（强拍）
        if (beat % 2 === 0) {
            const osc = this.audioContext.createOscillator();
            const gain = this.audioContext.createGain();

            osc.connect(gain);
            gain.connect(this.bgmGain);

            // 正弦波模拟底鼓：快速从150Hz下降到30Hz
            osc.type = 'sine';
            osc.frequency.setValueAtTime(150, this.audioContext.currentTime);
            osc.frequency.exponentialRampToValueAtTime(30, this.audioContext.currentTime + 0.1);

            gain.gain.setValueAtTime(0.4, this.audioContext.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.15);

            osc.start(this.audioContext.currentTime);
            osc.stop(this.audioContext.currentTime + 0.15);
        }

        // ========== 电子军鼓 ==========
        // 在第2、6、10...拍播放（偏拍/反拍）
        if (beat % 4 === 2) {
            const bufferSize = this.audioContext.sampleRate * 0.1;
            const buffer = this.audioContext.createBuffer(1, bufferSize, this.audioContext.sampleRate);
            const data = buffer.getChannelData(0);

            // 生成带衰减的噪音
            for (let i = 0; i < bufferSize; i++) {
                data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 3);
            }

            const noise = this.audioContext.createBufferSource();
            noise.buffer = buffer;

            const gain = this.audioContext.createGain();
            const filter = this.audioContext.createBiquadFilter();

            // 高通滤波器保留高频，产生明亮的军鼓声
            filter.type = 'highpass';
            filter.frequency.setValueAtTime(1000, this.audioContext.currentTime);

            noise.connect(filter);
            filter.connect(gain);
            gain.connect(this.bgmGain);

            gain.gain.setValueAtTime(0.15, this.audioContext.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.1);

            noise.start(this.audioContext.currentTime);
        }

        // ========== 踩镲 ==========
        // 每拍都播放，提供节奏感
        const bufferSize = this.audioContext.sampleRate * 0.05;
        const buffer = this.audioContext.createBuffer(1, bufferSize, this.audioContext.sampleRate);
        const data = buffer.getChannelData(0);

        // 快速衰减的噪音
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 5);
        }

        const hihat = this.audioContext.createBufferSource();
        hihat.buffer = buffer;

        const gain = this.audioContext.createGain();
        const filter = this.audioContext.createBiquadFilter();

        // 高通滤波器只保留超高频，模拟金属踩镲
        filter.type = 'highpass';
        filter.frequency.setValueAtTime(5000, this.audioContext.currentTime);

        hihat.connect(filter);
        filter.connect(gain);
        gain.connect(this.bgmGain);

        gain.gain.setValueAtTime(0.06, this.audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.05);

        hihat.start(this.audioContext.currentTime);
    }

    /**
     * 停止背景音乐
     */
    stopBgm() {
        this.isPlayingBgm = false;
        // 清除定时器
        if (this.bgmTimeout) {
            clearTimeout(this.bgmTimeout);
        }
    }

    /**
     * 销毁音频管理器
     * - 停止BGM
     * - 关闭音频上下文释放资源
     */
    destroy() {
        this.stopBgm();
        if (this.audioContext) {
            this.audioContext.close();
        }
    }
}
