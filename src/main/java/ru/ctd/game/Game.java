package ru.ctd.game;

import fox.FoxRender;
import fox.player.FoxPlayer;
import fox.utils.MediaCache;
import kuusisto.TinySound;
import lombok.extern.slf4j.Slf4j;
import ru.ctd.config.ApplicationProperties;
import ru.ctd.config.Constant;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.Random;

import static fox.utils.MediaCache.DATA_TYPE;

@Slf4j
public class Game extends JFrame implements KeyListener, ComponentListener, WindowListener {
    private final ApplicationProperties props = new ApplicationProperties();
    private final FoxRender render = new FoxRender();
    private final MediaCache cache = new MediaCache().getInstance();
    private final JFrame mainFrame;
    private final JPanel game_field;
    private final String ESC_TEXT = "[ESC] > options";
    private final String PAUSE_TEXT = "[ENTER] > pause";
    private final Random r = new Random();
    private JDialog modalOptions;
    private Thread optionsThread = null;
    private BufferedImage backImage, back2Image, back3Image, back4Image, gameoverImage, dropImage, pauseGameImage, lifeImage;
    private Graphics2D g2D;
    private float drop_left, drop_top = -128f, drop_fall_speed = 50, delta_time;
    private long last_frame_time, current_time;
    private Boolean paused = true, gameIsOver = false, isLifeUpSplashShowed = false, isThunderSplashShowed = false;
    private int score = 0, lives = 3, thunderAppearPercent = 5;
    private String text0 = "Score " + score;
    private String text1 = "Lives " + lives;
    private int t0w, t2w;
    private short greenScreenShowCycles = 3, whiteScreenShowCycles = 6;

    public Game() {
        mainFrame = this;

        loadResources();
        init();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Поймай каплю! " + props.getVersion());
        setMinimumSize(new Dimension(1024, 768));
        setPreferredSize(new Dimension(1024, 768));

        game_field = new JPanel() {
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!paused) {
                            int x = e.getX(), y = e.getY();

                            float dropRight = drop_left + dropImage.getWidth(null);
                            float dropBottom = drop_top + dropImage.getHeight(null);

                            boolean isAimed = x >= drop_left && x <= dropRight && y >= drop_top && y <= dropBottom;
                            if (isAimed) {
                                Constant.soundPlayer.play("correct");
                                drop_top = -dropImage.getHeight();
                                drop_left = (int) (Math.random() * (game_field.getWidth() - dropImage.getWidth(null)));
                                drop_fall_speed += 20;
                                score++;
                                if (score % 100 == 0) {
                                    Constant.soundPlayer.play("lvlup");
                                    lives++;
                                    isLifeUpSplashShowed = true;
                                }
                                text0 = "Score %s".formatted(score);
                                text1 = "Miss %s".formatted(lives);
                            }
                        }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                onRepaint(g);
            }
        };

        add(game_field);
        addKeyListener(this);
        addComponentListener(this);
        addWindowListener(this);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        new Thread(() -> {
            Constant.musicPlayer.play("music", true);
            long was = System.currentTimeMillis();

            while (true) {
                if (System.currentTimeMillis() - was > 1_000) {
                    was = System.currentTimeMillis();
                    if (r.nextInt(1, 101) <= thunderAppearPercent) {
                        isThunderSplashShowed = true;
                        Constant.soundPlayer.play("thunder");
                    }
                }
                repaint();
                Thread.yield();
            }
        }).start();
    }

    private void init() {
        backImage = cache.getBufferedImage("back");
        back2Image = cache.getBufferedImage("back2");
        back3Image = cache.getBufferedImage("back3");
        back4Image = cache.getBufferedImage("back4");
        pauseGameImage = cache.getBufferedImage("pause");
        gameoverImage = cache.getBufferedImage("gameover");
        lifeImage = cache.getBufferedImage("life");

        BufferedImage preDrop = cache.getBufferedImage("drop");
        if (preDrop != null) {
            BufferedImage postDrop = new BufferedImage(preDrop.getWidth(), preDrop.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2D = postDrop.createGraphics();
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT, 0.65f));
            g2D.drawImage(preDrop, 0, 0, postDrop.getWidth(), postDrop.getHeight(), this);
            g2D.dispose();

            dropImage = postDrop;
        } else {
            dropImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        }

        FoxPlayer.getVolumeConverter().setMinimum(-30);

        Constant.musicPlayer.setGlobalVolumePercent(100);

        Constant.musicPlayer.mute(false);
        Constant.musicPlayer.setLooped(true);
        Constant.musicPlayer.setParallelPlayable(false);
        Constant.musicPlayer.setVolumeFlowEnabled(true);
        Constant.musicPlayer.setGlobalVolumePercent(props.getMusicVolumePercent());
        Constant.musicPlayer.setUseExperimentalQualityFormat(true);
        Constant.musicPlayer.setUseUnsignedFormat(true);

        Constant.soundPlayer.mute(false);
        Constant.soundPlayer.setLooped(false);
        Constant.soundPlayer.setParallelPlayable(true);
        Constant.soundPlayer.setVolumeFlowEnabled(false);
        Constant.soundPlayer.setGlobalVolumePercent(props.getSoundVolumePercent());
        Constant.soundPlayer.setUseExperimentalQualityFormat(true);
        Constant.soundPlayer.setUseUnsignedFormat(true);

        TinySound.init();
        Constant.soundPlayer.play("correct");
    }

    private void loadResources() {
        try {
            cache.addLocalIfAbsent("music", "/sounds/music", DATA_TYPE.WAV);
            cache.addLocalIfAbsent("error", "/sounds/error", DATA_TYPE.WAV);
            cache.addLocalIfAbsent("gameoverSound", "/sounds/gameover", DATA_TYPE.WAV);
            cache.addLocalIfAbsent("correct", "/sounds/correct", DATA_TYPE.WAV);
            cache.addLocalIfAbsent("lvlup", "/sounds/lvlup", DATA_TYPE.WAV);
            cache.addLocalIfAbsent("pauseSound", "/sounds/pause", DATA_TYPE.WAV);
            cache.addLocalIfAbsent("thunder", "/sounds/thunder", DATA_TYPE.WAV);

            cache.addLocalIfAbsent("16PNG", "/images/16", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("logo", "/images/logo", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("back", "/images/back", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("back2", "/images/back2", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("back3", "/images/back3", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("back4", "/images/back4", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("gameover", "/images/gameover", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("drop", "/images/drop", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("pause", "/images/pause", DATA_TYPE.PNG);
            cache.addLocalIfAbsent("life", "/images/heart", DATA_TYPE.PNG);
        } catch (Exception e) {
            log.error("Media sound exception: {}", e.getMessage());
        }
    }

    private void onRepaint(Graphics g) {
        if (game_field == null || mainFrame == null || pauseGameImage == null) {
            return;
        }

        g2D = (Graphics2D) g;
        render.setRender(g2D, FoxRender.RENDER.MED, true, false);

        try {
            if (lives <= 0) {
                drawGameOver();
            } else if (paused) {
                drawPaused();
            } else {
                current_time = System.nanoTime();
                delta_time = (current_time - last_frame_time) * 0.00000000075f;
                last_frame_time = System.nanoTime();

                if (isLifeUpSplashShowed) {
                    greenScreenShowCycles--;
                    if (greenScreenShowCycles == 0) {
                        isLifeUpSplashShowed = false;
                        greenScreenShowCycles = 3;
                    }

                    g2D.drawImage(back3Image, 0, 0, getWidth(), getHeight(), game_field);
                } else if (isThunderSplashShowed) {
                    if (whiteScreenShowCycles % 3 == 0) {
                        g2D.drawImage(back4Image, 0, 0, getWidth(), getHeight(), game_field);
                    } else {
                        g2D.drawImage(backImage, 0, 0, getWidth(), getHeight(), game_field);
                    }

                    whiteScreenShowCycles--;
                    if (whiteScreenShowCycles == 0) {
                        isThunderSplashShowed = false;
                        whiteScreenShowCycles = 6;
                    }
                } else {
                    g2D.drawImage(backImage, 0, 0, getWidth(), getHeight(), game_field);
                }

                drop_top += drop_fall_speed * delta_time; // запускаем движение капли!
                g2D.drawImage(dropImage, (int) drop_left, (int) drop_top, dropImage.getWidth(), dropImage.getHeight(), game_field);

                drawScore(g2D);

                if (drop_top > getHeight()) {
                    Constant.soundPlayer.play("error");
                    newDrop();
                    lives--;
                    if (lives <= 0) {
                        Constant.soundPlayer.play("gameoverSound");
                        gameIsOver = true;
                    }
                    g2D.drawImage(back2Image, 0, 0, getWidth(), getHeight(), game_field);
                    drawScore(g2D);
                }
            }
        } catch (Exception e) {
            log.error("Media sound exception: {}", e.getMessage());
        }
    }

    private void drawPaused() {
        g2D.drawImage(backImage, 0, 0, getWidth(), getHeight(), game_field);
        g2D.drawImage(pauseGameImage,
                mainFrame.getWidth() / 2 - pauseGameImage.getWidth(game_field) / 2,
                mainFrame.getHeight() / 2 - pauseGameImage.getHeight(game_field) / 2, game_field);
    }

    private void drawGameOver() {
        g2D.drawImage(backImage, 0, 0, getWidth(), getHeight(), game_field);
        g2D.drawImage(gameoverImage, getWidth() / 4, getHeight() / 4, game_field);
    }

    private void drawScore(Graphics2D g2D) {
        g2D.setFont(Constant.f0);
        if (t0w == 0) {
            reInitTOW(g2D);
        }

        text0 = "Score: " + score;
        text1 = "Lives: " + lives;

        g2D.setColor(Color.GRAY);
        g2D.drawString(text0, 20, 42);
        g2D.setColor(Color.ORANGE);
        g2D.drawString(text0, 18, 40);

        g2D.setColor(Color.GRAY);
        g2D.drawString(text1, 20, 74);
        g2D.setColor(Color.ORANGE);
        g2D.drawString(text1, 18, 70);
        t0w = (int) (Constant.ffb.getStringBounds(g2D, text1).getWidth());
        g2D.drawImage(lifeImage, t0w + 25, 48, 30, 30, null);

        g2D.setFont(Constant.f1);

        g2D.setColor(Color.GRAY);
        g2D.drawString(ESC_TEXT, getWidth() - t2w - 23, 42);
        g2D.setColor(Color.ORANGE);
        g2D.drawString(ESC_TEXT, getWidth() - t2w - 25, 40);

        g2D.setColor(Color.GRAY);
        g2D.drawString(PAUSE_TEXT, getWidth() - t2w - 23, 72);
        g2D.setColor(Color.ORANGE);
        g2D.drawString(PAUSE_TEXT, getWidth() - t2w - 25, 70);
    }

    private void reInitTOW(Graphics2D g2D) {
        t0w = (int) (Constant.ffb.getStringBounds(g2D, text1).getWidth());
        t2w = (int) (Constant.ffb.getStringBounds(g2D, PAUSE_TEXT).getWidth());
    }

    private void newDrop() {
        drop_fall_speed = (float) (Math.random() * 100 + 50);
        drop_left = (float) (Math.random() * (getWidth() - 100));
        drop_top = -dropImage.getHeight();
    }

    private void showOptionsDialog() {
        paused = true;
        modalOptions = new JDialog();

        Runnable options = () -> {
            modalOptions.setTitle("Настройки игры:");
            modalOptions.setModal(true);
            modalOptions.setFocusable(false);
            modalOptions.setModalExclusionType(JDialog.ModalExclusionType.NO_EXCLUDE);
            modalOptions.setMinimumSize(new Dimension(game_field.getWidth() / 3, (int) (game_field.getHeight() / 2.5f)));
            modalOptions.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            modalOptions.add(new JPanel(new BorderLayout()) {
                {
                    setBorder(new EmptyBorder(3, 3, 3, 3));
                    setFocusable(true);

                    add(new JButton("Музыка вкл/выкл") {
                        {
                            setFocusPainted(false);
                            setPreferredSize(new Dimension(0, 30));
                            setForeground(Constant.musicPlayer.isMuted() ? Color.WHITE : null);
                            setBackground(Constant.musicPlayer.isMuted() ? Color.DARK_GRAY : null);
                            addActionListener(_ -> {
                                Constant.musicPlayer.mute(!Constant.musicPlayer.isMuted());
                                setForeground(Constant.musicPlayer.isMuted() ? Color.WHITE : null);
                                setBackground(Constant.musicPlayer.isMuted() ? Color.DARK_GRAY : null);
                            });
                        }
                    }, BorderLayout.NORTH);

                    add(new JPanel(new GridLayout(3, 0, 12, 12)) {
                        {
                            setFocusable(false);
                            setPreferredSize(new Dimension(0, 300));

                            JSlider volumeSlider = new JSlider(0, 100, Constant.musicPlayer.getPlayerVolumePercent()) {
                                {
                                    setBorder(BorderFactory.createCompoundBorder(
                                            BorderFactory.createTitledBorder(
                                                    BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true),
                                                    "Музыка:", 0, 2, Constant.f2, Color.BLACK),
                                            new EmptyBorder(24, 0, 0, 0)));
                                    setFocusable(false);
                                    setMajorTickSpacing(10);
                                    setMinorTickSpacing(2);
                                    setPaintTicks(true);
                                    setPaintTrack(true);
                                    setPaintLabels(true);
                                    addChangeListener(_ -> Constant.musicPlayer.setGlobalVolumePercent(getValue()));
                                }
                            };

                            JSlider soundSlider = new JSlider(0, 100, Constant.soundPlayer.getPlayerVolumePercent()) {
                                {
                                    setBorder(BorderFactory.createCompoundBorder(
                                            BorderFactory.createTitledBorder(
                                                    BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true),
                                                    "Звуки:", 0, 2, Constant.f2, Color.BLACK),
                                            new EmptyBorder(24, 0, 0, 0)));
                                    setFocusable(false);
                                    setMajorTickSpacing(10);
                                    setMinorTickSpacing(2);
                                    setPaintTicks(true);
                                    setPaintTrack(true);
                                    setPaintLabels(true);
                                    addChangeListener(_ -> Constant.soundPlayer.setGlobalVolumePercent(getValue()));
                                }
                            };

                            add(volumeSlider);
                            add(soundSlider);
                        }
                    }, BorderLayout.CENTER);

                    add(new JPanel(new BorderLayout(0, 0)) {
                        {
                            setFocusable(false);
                            setPreferredSize(new Dimension(0, 30));

                            add(new JButton("Вернуться") {
                                {
                                    setFocusable(false);
                                    setFocusPainted(false);
                                    addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            modalOptions.dispose();
                                        }
                                    });
                                }
                            }, BorderLayout.CENTER);

                            add(new JButton("Выход") {
                                {
                                    setFocusable(false);
                                    setFocusPainted(false);
                                    setBackground(Color.red.darker().darker().darker());
                                    setForeground(Color.WHITE);
                                    setPreferredSize(new Dimension(75, 0));
                                    addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            modalOptions.dispose();
                                            windowClosing(null);
                                            dispose();
                                            System.exit(0);
                                        }
                                    });
                                }
                            }, BorderLayout.EAST);
                        }
                    }, BorderLayout.SOUTH);

                    addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                modalOptions.dispose();
                            }
                        }
                    });
                }
            });

            modalOptions.pack();
            modalOptions.setLocationRelativeTo(null);
            modalOptions.setVisible(true);

            optionsThread.interrupt();
            last_frame_time = System.nanoTime();
            paused = false;
            game_field.repaint();
        };

        optionsThread = new Thread(options);
        optionsThread.start();
    }

    @Override
    public void keyPressed(KeyEvent k) {
        if (k.getKeyCode() == KeyEvent.VK_ENTER) {
            if (paused) {
                last_frame_time = System.nanoTime();
            }

            if (gameIsOver) {
                return;
            }

            paused = !paused;
            Constant.soundPlayer.play("pauseSound");
            game_field.repaint();
        }

        if (k.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (gameIsOver) {
                score = 0;
                lives = 3;
                newDrop();
                gameIsOver = false;
                game_field.repaint();
            } else {
                showOptionsDialog();
            }
        }
    }

    public void keyReleased(KeyEvent k) {
    }

    public void keyTyped(KeyEvent k) {
    }

    public void componentResized(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        log.info("Завершение работы...");
        TinySound.shutdown();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
