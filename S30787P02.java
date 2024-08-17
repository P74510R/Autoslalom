package src;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class S30787P02 {
    public static void main(String[] args) {
        new WindowGenerator();
    }
}
class WindowGenerator{
    public WindowGenerator(){
        Window window = Window.getInstance();
        window.setVisible(true);
    }
}
//class Board is used for creating board and making it possible to control the car position
class Board  implements KeyListener {
    private final int[] board = new int[7];
    private int carposition = 2; // Initial car position in the middle of the first row
    private final Random random = new Random();
    private int EmptyRows = 2;

    public Board() {
        generateInitialBoard();
    }

    private void generateInitialBoard() {
        // Initialize the first row to represent the car position
        board[0] = carposition;

        // Generate the rest of the board starting from the second row
        int prevObstaclePos = -1; // No obstacle in the first row
        for (int i = 1+EmptyRows; i < board.length; i+=EmptyRows+1) {
            int obstaclePos;
            do {
                obstaclePos = random.nextInt(7);
            } while (obstaclePos == prevObstaclePos);
            board[i] = obstaclePos;
            prevObstaclePos = obstaclePos;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyChar()){
            case 'a':
                if(GameThread.getInstance().isRunning()){
                    if (carposition < 4){
                        carposition*=2;
                        updateCarPosition();
                    }
                }
                break;
            case 'd':
                if(GameThread.getInstance().isRunning()) {
                    if (carposition > 1) {
                        carposition /= 2;
                        updateCarPosition();
                    }
                }
                break;
            case 's':
                if(!GameThread.getInstance().isStarted()){
                    GameThread.getInstance().start();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                if(GameThread.getInstance().isStarted()){
                    if (GameThread.getInstance().isRunning()) {
                        GameThread.getInstance().stopThread();
                    } else {
                        GameThread.getInstance().start();
                    }
                }
                break;
        }
    }
    private void updateCarPosition() {
        board[0] = carposition; // Update the car position
    }
    public boolean checkLoseCondition() {
        String carPositionBinary = String.format("%03d", Integer.parseInt(Integer.toBinaryString(board[0]))); // Convert car position to binary
        String obstaclePositionBinary = String.format("%03d", Integer.parseInt(Integer.toBinaryString(board[1]))); // Convert obstacle position to binary

        // Check if car and obstacle are in the same position
        for (int i = 0; i < carPositionBinary.length(); i++) {
            if (carPositionBinary.charAt(i) == '1' && obstaclePositionBinary.charAt(i) == '1') {
                return true;
            }else{
                return false;
            }
        }

        return false;
    }
    public void updateBoardOnTick() {
        if (checkLoseCondition()){
            GameThread.getInstance().lose();
        }
        System.arraycopy(board, 2, board, 1, board.length - 2);
        int er = EmptyRows;
        if (er > 0) {
            for (int i = board.length-2; i > 0; i--) {
                if (board[i] == 0) {
                    er--;
                } else {
                    break;
                }
            }
            if (er <= 0) {
                int obstaclePos;
                do {
                    obstaclePos = random.nextInt(7);
                } while (obstaclePos == board[board.length - 2]);
                board[board.length - 1] = obstaclePos;
            } else {
                board[board.length - 1] = 0;
            }
        } else {
            int obstaclePos;
            do {
                obstaclePos = random.nextInt(7);
            } while (obstaclePos == board[board.length - 2]);
            board[board.length - 1] = obstaclePos;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public int[] getBoard() {
        return board;
    }
    public void decrementEmptyRows(){
        EmptyRows--;
    }
}
//singleton class GameThread is used for creating a thread that will be responsible for the game
class GameThread extends Thread{
    private static GameThread instance;
    private Thread thread;
    private int interval = 1000;
    private boolean running;
    private boolean started = false;
    private List<TickListener> ticklisteners = new ArrayList<>();

    private GameThread(){}

    public static synchronized GameThread getInstance() {
        if(instance==null){
            instance = new GameThread();
        }
        return instance;
    }
    public void start(){
        started = true;
        if(thread == null || !running){
            thread = new Thread(this::run);
            thread.start();
        }
    }
    public void addTickListener(TickListener listener){
        ticklisteners.add(listener);
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run(){
        running = true;
        int tickcounter=0;

        while (running){
            TickEvent event = new TickEvent(this);
            synchronized (ticklisteners) {
                for (TickListener listener : ticklisteners.toArray(new TickListener[0])) {
                    listener.onTick(event);
                }
            }
            tickcounter++;
            if(tickcounter == 20){
                if(interval>100){
                    interval-=50;
                }
                tickcounter = 0;
            }
            try{
                Thread.sleep(interval);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    public void stopThread() {
        running = false;
    }
    public void onGameEvent(){
        stopThread();
    }
    public void win(){
        running = false;
        JOptionPane.showMessageDialog(null, "Wygrales", "Gratulacje", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
    public void lose(){
        JOptionPane.showMessageDialog(null, "Przegrales", "Niestety", JOptionPane.INFORMATION_MESSAGE);
        new WindowGenerator();
    }

    public boolean isStarted() {
        return started;
    }
}
class TickEvent {
    private Object source;

    public TickEvent(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }
}
interface TickListener {
    void onTick(TickEvent event);
}
//p02.pres
class Car {
    public HashMap carpositions = new HashMap<Integer,int[]>();
    Car() {
        this.carpositions = new HashMap<>();
        carpositions.put(1, new int[]{325, 285});
        carpositions.put(2, new int[]{200, 285});
        carpositions.put(4, new int[]{70, 285});
    }
}
class GamePanel extends JPanel {
    private final List<BufferedImage> backgrounds;
    private final List<BufferedImage> obstacles;
    private Obstacle obstacle;
    private int currentBackgroundIndex = 0;
    private Board board;
    private ScorePanel sc;
    private BufferedImage car;
    private int x;

    public GamePanel(ScorePanel sc, Board board){
        obstacle = new Obstacle();
        this.sc=sc;
        this.board = board;
        this.addKeyListener(board);
        this.setFocusable(true);
        this.requestFocusInWindow();
        backgrounds = new ArrayList<>();
        try {
            backgrounds.add(ImageIO.read(getClass().getResource("/src/pres/tla/tlo1.jpg")));
            backgrounds.add(ImageIO.read(getClass().getResource("/src/pres/tla/tlo2.jpg")));
            backgrounds.add(ImageIO.read(getClass().getResource("/src/pres/tla/tlo3.jpg")));
            backgrounds.add(ImageIO.read(getClass().getResource("/src/pres/tla/tlo4.jpg")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        GameThread.getInstance().addTickListener(this::onTickEvent);
        try{
            car=ImageIO.read(getClass().getResource("/src/pres/samochod/brumbrum.png"));
        }
        catch(IOException e){
            e.printStackTrace();
        }
        obstacles = new ArrayList<>();
        try {
            obstacles.add(ImageIO.read(getClass().getResource("/src/pres/przeszkody/przeszkoda1.png")));
            obstacles.add(ImageIO.read(getClass().getResource("/src/pres/przeszkody/przeszkoda2.png")));
            obstacles.add(ImageIO.read(getClass().getResource("/src/pres/przeszkody/przeszkoda3.png")));
            obstacles.add(ImageIO.read(getClass().getResource("/src/pres/przeszkody/przeszkoda4.png")));
            obstacles.add(ImageIO.read(getClass().getResource("/src/pres/przeszkody/przeszkoda5.png")));
            obstacles.add(ImageIO.read(getClass().getResource("/src/pres/przeszkody/przeszkoda6.png")));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgrounds.get(currentBackgroundIndex), 0, 0, this.getWidth(), this.getHeight(), this);
        Car car1 = new Car();
        int[] boardArray = board.getBoard();
        int[] carPosition = (int[]) car1.carpositions.get(boardArray[0]);
        g.drawImage(car,carPosition[0],carPosition[1],car.getWidth(),car.getHeight(),this);
        for (int i = 1; i < boardArray.length; i++) {
            int row = i;
            int val = boardArray[i];
            BufferedImage obstacleimg = obstacles.get(row-1);
            if(row<obstacle.obstaclePositions.size() && row < obstacles.size()) {
                if (val == 1 || val == 2 || val == 4) {
                    Position p = obstacle.obstaclePositions.get(row).get(val);
                    g.drawImage(obstacleimg, p.getX(), p.getY(),obstacleimg.getWidth(),obstacleimg.getHeight(), this);
                } else if (val == 0) {
                } else {
                    if ( val == 3 || val == 5) {
                        Position first = obstacle.obstaclePositions.get(row).get(1);
                        Position second = obstacle.obstaclePositions.get(row).get(val - 1);
                        g.drawImage(obstacleimg, first.getX(), first.getY(),obstacleimg.getWidth(),obstacleimg.getHeight(), this);
                        g.drawImage(obstacleimg, second.getX(), second.getY(), obstacleimg.getWidth(),obstacleimg.getHeight(),this);
                    } else if (val == 6) {
                        Position first = obstacle.obstaclePositions.get(row).get(2);
                        Position second = obstacle.obstaclePositions.get(row).get(val - 2);
                        g.drawImage(obstacleimg, first.getX(), first.getY(),obstacleimg.getWidth(),obstacleimg.getHeight(), this);
                        g.drawImage(obstacleimg, second.getX(), second.getY(), obstacleimg.getWidth(), obstacleimg.getHeight(),
                                this);
                    }
                }
            }
        }
        GameThread gameThread = GameThread.getInstance();
        if(gameThread.isStarted() && !gameThread.isRunning()){
            g.setColor(new Color(0,0,0,127));
            g.fillRect(0,0,getWidth(),getHeight());

            g.setColor(Color.WHITE);
            g.setFont(new Font("Mistral", Font.BOLD, 30));
            g.drawString("PAUSE", 300, 200);
        } else if (!gameThread.isStarted()){
            BufferedImage tytul;
            try {
                tytul = ImageIO.read(getClass().getResource("/src/pres/tla/tytul.jpg"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            g.drawImage(tytul, 0, 0, this.getWidth(), this.getHeight(), this);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Mistral", Font.BOLD, 30));
            g.drawString("PRESS S TO START", 200, 200);
        }
        repaint();
    }

    public void onTickEvent(TickEvent event) {
        System.out.println();
        board.checkLoseCondition();
        board.updateBoardOnTick();
        if(!board.checkLoseCondition()){
            sc.increment();
            x++;
        }
        currentBackgroundIndex = (currentBackgroundIndex + 1) % backgrounds.size();
        repaint();
    }

    public Board getBoard() {
        return board;
    }
}
class Obstacle {
    public Map<Integer, Map<Integer,Position>> obstaclePositions = new HashMap<>();
    public Map<Integer,Position> map1 = new HashMap<>();
    public Map<Integer,Position> map2 = new HashMap<>();
    public Map<Integer,Position> map3 = new HashMap<>();
    public Map<Integer,Position> map4 = new HashMap<>();
    public Map<Integer,Position> map5 = new HashMap<>();
    public Map<Integer,Position> map6 = new HashMap<>();
    Obstacle() {
        map1.put(1, new Position(425, 252));
        map1.put(2, new Position(330, 252));
        map1.put(4, new Position(235, 252));
        obstaclePositions.put(1, map1);

        map2.put(1,new Position(455, 213));
        map2.put(2,new Position(360, 213));
        map2.put(4,new Position(265, 213));
        obstaclePositions.put(2, map2);

        map3.put(1, new Position(495, 143));
        map3.put(2, new Position(425, 143));
        map3.put(4, new Position(355, 143));
        obstaclePositions.put(3, map3);

        map4.put(1, new Position(525, 103));
        map4.put(2, new Position(470, 103));
        map4.put(4, new Position(415, 103));
        obstaclePositions.put(4, map4);

        map5.put(1, new Position(563, 45));
        map5.put(2, new Position(530, 45));
        map5.put(4, new Position(497, 45));
        obstaclePositions.put(5, map5);

        map6.put(1, new Position(595, 15));
        map6.put(2, new Position(563, 15));
        map6.put(4, new Position(531, 15));
        obstaclePositions.put(6, map6);
    }
}
class ObstacleTable extends JLabel {
    private final Board board;
    ObstacleTable(Board board){
        this.board = board;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
class Position {
    private int x;
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
}
class ScorePanel extends JPanel {
    private SevenSegmentDigit setki;
    private SevenSegmentDigit dziesiatki;
    private SevenSegmentDigit jednosci;
    private Board board;
    private int x = 0;
    private int y = 0;

    public ScorePanel(Board board) {
        this.board = board;
        jednosci = new SevenSegmentDigit();
        jednosci.setOpaque(false);
        dziesiatki = new SevenSegmentDigit();
        dziesiatki.setOpaque(false);
        setki = new SevenSegmentDigit();
        setki.setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(setki);
        add(dziesiatki);
        add(jednosci);
    }
    public void reset(){
        setki.reset();
        dziesiatki.reset();
        jednosci.reset();
    }
    public void increment(){
        jednosci.increment();
        if(jednosci.getValue() == 0){
            dziesiatki.increment();
            if(x==0) {
                board.decrementEmptyRows();
                x++;
            }
            if(dziesiatki.getValue() == 0){
                setki.increment();
                if (y==0){
                    board.decrementEmptyRows();
                    y++;
                }
                if(setki.getValue() == 0){
                    GameThread.getInstance().win();
                }
            }
        }
    }
}
class SevenSegmentDigit extends JPanel {
    private int value;
    public SevenSegmentDigit(){
        this.value = 0;
    }
    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        g.setColor(Color.BLACK);

        int[] segmentValues = {
                0b1111110, // 0
                0b0000110, // 1
                0b1101011, // 2
                0b1001111, // 3
                0b0010111, // 4
                0b1011101, // 5
                0b1111101, // 6
                0b0001110, // 7
                0b1111111, // 8
                0b1011111  // 9
        };
        int value = segmentValues[this.value];

        int width = getWidth() / 10;
        int height = getHeight() / 20;

        for (int i = 0; i < 7; i++) {
            if ((value & (1 << i)) != 0) {
                switch (i) {
                    case 0: g.fillRect(width * 2, height * 9, width * 6, height); break; // Down
                    case 1: g.fillRect(width * 8, height, width, height * 8); break; // Lower Left
                    case 2: g.fillRect(width * 8, height * 11, width, height * 8); break; // Upper Left
                    case 3: g.fillRect(width * 2, 0, width * 6, height); break; // Top
                    case 4: g.fillRect(0, height, width, height * 8); break; // Lower Right
                    case 5: g.fillRect(0, height * 11, width, height * 8); break; // Upper Right
                    case 6: g.fillRect(width * 2, height * 19, width * 6, height); break; // Middle
                }
            }
        }
    }
    public void reset() {
        this.value = 0;
        repaint();
    }

    public void increment(){
        this.value = (this.value+1) % 10;
        repaint();
    }
    public int getValue(){
        return this.value;
    }
}
class Window extends JFrame {
    private static Window instance;
    private Window() {
        super("Autoslalom");
        super.setIconImage(new ImageIcon(getClass().getResource("/src/pres/ikona/ussr.png")).getImage());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(656,430));
        Board board = new Board();
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setBounds(0,0,656,395);
        ScorePanel sc = new ScorePanel(board);
        sc.setBounds(25,25,80,40);
        sc.setOpaque(false);
        layeredPane.add(sc, JLayeredPane.PALETTE_LAYER);
        GamePanel gp = new GamePanel(sc, board);
        gp.setBounds(0,0,656,395);
        layeredPane.add(gp, JLayeredPane.DEFAULT_LAYER);
        add(layeredPane);
        pack();
        setVisible(true);
    }
    public static Window getInstance() {
        if(instance!=null){
            instance.dispose();
            instance = new Window();
        }else {
            instance = new Window();
        }
        return instance;
    }
}