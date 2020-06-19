// import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
// import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
// import javafx.scene.control.Label;
import javafx.scene.image.PixelFormat;
// import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import org.dyn4j.dynamics.World;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.CollisionAdapter;
import org.dyn4j.collision.AxisAlignedBounds;
import org.dyn4j.collision.manifold.Manifold;
import org.dyn4j.collision.narrowphase.Penetration;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;


public class Break extends Application {
    final double WORLD_SCALE = 100.0;
    final Vector2 screen = new Vector2(640, 480);
    final Vector2 worldSize = new Vector2(screen.x/WORLD_SCALE, screen.y/WORLD_SCALE);
    private GraphicsContext context = null;
    private World world = new World();//new AxisAlignedBounds(screen.getX(), screen.getY()));

    private class PLAYER{}
    private PLAYER PLAYERTYPE = new PLAYER();
    public class Player {
        private Body body = new Body();
        final private Vector2 size;
        final private Vector2 halfsize;

        public Player(Vector2 pos, Vector2 size) {
            this.size = size;
            this.halfsize = size.copy().divide(2.0);
            body.setUserData(PLAYERTYPE);
            body.addFixture(Geometry.createRectangle(size.x, size.y));
            body.setMass(MassType.INFINITE);
            body.translate(worldSize.x/2, worldSize.y-size.y*2);
        }

        public Vector2 center() {
            return body.getTransform().getTranslation().copy();
        }

        public void draw() {
            final var pos = body.getTransform().getTranslation();
            rect(pos, size, halfsize, Color.DARKCYAN);
        }

        public void setPosition(double x) {

            final var oldPos = body.getTransform();
            final var oldPosX = oldPos.getTranslationX();
            final var newPosX = x-size.x/2;
            body.translate(newPosX - oldPosX, 0);
        }

        public Vector2 getTopCenter() {
            return center().add(0, size.y/2);
        }
    }

    public final int stonesHNum = 22;
    final private Vector2 stoneSize = new Vector2(worldSize.x/stonesHNum, 0.1);
    final private Vector2 stoneHalfsize = stoneSize.copy().divide(2.0);
    public interface HackStoneType{
        public enum StoneType {
            DEAD,
            Level1,
            Level2,
            Level3,
            Level4,
            Level5,
            Level6,
            Level7,
            Level8,
        }
    }
    private class STONE{}
    private STONE STONETYPE = new STONE();
    public class Stone implements HackStoneType {
        private Body body = new Body();
        private int health = 99;
        private StoneType[] stonetypes = StoneType.values();

		public Stone(double x, double y, int type) {
            health = type+1;

            body.setUserData(STONETYPE);
            body.addFixture(
                Geometry.createRectangle(stoneSize.x, stoneSize.y),
                100.0, 0.0, 1+type/100.0
            );
            body.translate(x, y);
            body.setMass(MassType.INFINITE);
		}

        @Override
        public boolean equals(Object obj) {
            return center().equals(((Stone)obj).center());
            // return super.equals(obj);
        }

        public Vector2 center() {
            return body.getTransform().getTranslation().copy();
        }

		public void draw() {
            var color = Color.RED;
            switch(stonetypes[health]){
                case Level1: color = Color.BLUEVIOLET; break;
                case Level2: color = Color.GREENYELLOW; break;
                case Level3: color = Color.PAPAYAWHIP; break;
                case Level4: color = Color.BURLYWOOD; break;
                case Level5: color = Color.AQUAMARINE; break;
                case Level6: color = Color.CHARTREUSE; break;
                case Level7: color = Color.DEEPPINK; break;
                case Level8: color = Color.FIREBRICK; break;
                default: color = Color.RED; break;
            };
            final var pos = body.getTransform().getTranslation();
            rect(pos, stoneSize, stoneHalfsize, color);
		}
    }

    private class BALL{}
    private BALL BALLTYPE = new BALL();
    public class Ball {
        private Body body = new Body();
        final private Vector2 size;
        final private Vector2 halfsize;
        private double initSpeed = 5.0;

        public Ball (Vector2 position, Vector2 size) {
            this.size = size;
            this.halfsize = size.copy().divide(2.0);
            body.setUserData(BALLTYPE);
            body.addFixture(
                Geometry.createRectangle(size.x, size.y),
                100.0, 0.0, 1.01
            );
            body.translate(position);
            // body.setLinearVelocity(10, 10);
            body.setMass(MassType.NORMAL);
        }

        public Vector2 center() {
            return body.getTransform().getTranslation().copy();
        }

        public void draw() {
            final var position = body.getTransform().getTranslation();
            if(position.x <= 0 || position.x >= worldSize.x
                    ||
                    position.y <= 0 || position.y >= worldSize.y
              ){
                System.out.println(String.format("CRAAAAAAAAAAAAAP!: %s %s", position.x, position.y));
                System.out.println("The Ball left the Building. There is no return...");

                stones.clear();
            }
            rect(position, size, halfsize, Color.BLUEVIOLET);
        }

        public void initVelocity() {
            var velocity = center().subtract(player.center());
            velocity.normalize();
            velocity = velocity.multiply(initSpeed);
            // System.out.println(String.format("%s %s", velocity.x, velocity.y));
            ball.body.applyImpulse(velocity);
        }
    }

    private class WALL{}
    private WALL WALLTYPE = new WALL();
    public class Wall {
        private Body body = new Body();
        private Vector2 size = new Vector2();
        private Vector2 halfsize = size.copy().divide(2.0);

        public Wall(Vector2 position, Vector2 size) {
            body.setUserData(WALLTYPE);
            body.addFixture(
                Geometry.createRectangle(size.x, size.y),
                100.0, 0.0, 1.03
            );
            this.size = size.copy();
            this.halfsize = size.copy().divide(2.0);
            body.setMass(MassType.INFINITE);
            body.translate(position);
        }

        public void draw() {
            final var t = body.getTransform().getTranslation();
            rect(t, size, halfsize, Color.RED);
        }
    }

    public Player player = null;
    public Ball ball = null;
    public ArrayList<Stone> stones = new ArrayList<Stone>();
    public Wall[] walls = new Wall[4];
    private long startedAt;
    private long clicks = 0;


    public static void main(String[] args) {
        System.out.println("Starting Break...");
        launch();
        System.out.println("Break done.");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Break");
        var root = new Group();
        var canvas = new Canvas(screen.x, screen.y);
        context = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);
        var scene = new Scene(root);
        primaryStage.setScene(scene);
        handleEvents(canvas);
        setup();
        createLevel();
        startTimer();
        startedAt = System.nanoTime();
        primaryStage.show();
    }

    public void setup() {
        player = new Player(
            new Vector2(worldSize.x/2, worldSize.y-0.05-0.1-0.1),
            new Vector2(0.3, 0.1));
        ball = new Ball(
            player.getTopCenter().subtract(0, 0.05),
            new Vector2(0.1, 0.1));

        world.setGravity(World.ZERO_GRAVITY);
        // world.setGravity(World.EARTH_GRAVITY.multiply(-1.0));
        world.addBody(player.body);
        world.addBody(ball.body);
    }

    public void createLevel() {
        var listStones = IntStream.range(1, (int)(screen.y/(stoneSize.y*WORLD_SCALE)-1-10))
        .mapToObj(iy ->
            IntStream.range(1, (int)(screen.x/(stoneSize.x*WORLD_SCALE)-1))
            .mapToObj(ix -> new int[]{ix, iy})
            .filter(o -> Math.random() > 0.5)
            .toArray()
            // .toArray(Integer[][]::new)
        )
        .flatMap(e -> Arrays.stream(e))
        .map(e -> (int[])e)
        .map(e -> new Stone(
                e[0]*stoneSize.x+stoneSize.x,
                e[1]*stoneSize.y+stoneSize.y,
                (int)Math.floor(Math.random()*Stone.StoneType.values().length-1)
            ))
        .collect(Collectors.toList());
        stones = new ArrayList<Stone>(listStones);
        //.toList(Stone[]::new);

        walls[0] = new Wall(
            new Vector2(worldSize.x/2, 0.05),
            new Vector2(worldSize.x, 0.1)
        );
        walls[1] = new Wall(
            new Vector2(worldSize.x/2, worldSize.y-0.05),
            new Vector2(worldSize.x, 0.1)
        );
        walls[2] = new Wall(
            new Vector2(0.05, worldSize.y/2),
            new Vector2(0.1, worldSize.y)
        );
        walls[3] = new Wall(
            new Vector2(worldSize.x-0.05, worldSize.y/2),
            new Vector2(0.1, worldSize.y)
        );
        Arrays.stream(walls)
        .forEach(w -> {
            world.addBody(w.body);
        });

        stones.stream()
        .forEach(s -> {
            world.addBody(s.body);
        });
    }

    private Optional<Stone> getStone(Vector2 pos) {
        for(var stone : stones){
            if(stone.center().equals(pos)){
                // System.out.println("Found Stone");
                return Optional.of(stone);
            }
        }
        return Optional.empty();
    }
    private void removeStone(Stone stone) {
        for(int i=0; i<stones.size(); ++i){
            if(stones.get(i).equals(stone)){
                // System.out.println("Rem Stone");
                world.removeBody(stones.get(i).body);
                stones.remove(i);
                return;
            }
        }
    }

    private void degradeStone(Body body) {
        // System.out.println(String.format("FIND: %s ", body));
        var stone = getStone(body.getTransform().getTranslation());
        stone.map(s -> {
            // System.out.println(String.format("COLL: %s ", s.health));
            s.health -= 1;
            if(s.health <= 0){
                deadStones.add(s);
            }
            return s;
        });
    }

    public void handleEvents(Canvas canvas) {
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>()
        {

            @Override
            public void handle(MouseEvent event) {
                // TODO player.setPosition(event.getX()/WORLD_SCALE);
            }

        });

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
                clicks += 1;
				ball.initVelocity();
			}
        });
        world.addListener(new CollisionAdapter() {

            @Override
            // public boolean collision(Body body1, BodyFixture fixture1, Body body2, BodyFixture fixture2, Manifold f) {
            public boolean collision(Body body1, BodyFixture fixture1, Body body2, BodyFixture fixture2, Penetration penetration) {
                var res =  super.collision(body1, fixture1, body2, fixture2);
                var type1 = body1.getUserData();
                var type2 = body2.getUserData();
                if(type1 instanceof STONE && type2 instanceof BALL){
                    degradeStone((Body)body1);
                    // System.out.println(String.format("COLL: %s %s : %s %s", body1, fixture1, body2, fixture2));
                }else if(type1 instanceof BALL && type2 instanceof STONE){
                    degradeStone((Body)body2);
                    // System.out.println(String.format("COLL: %s %s : %s %s", body1, fixture1, body2, fixture2));
                }
                return res;
            }

        });
    }

    public void startTimer() {
        var timer = new AnimationTimer() {
            long startTime = System.nanoTime();

            @Override
            public void handle(long now) {
                var endTime = System.nanoTime();
                var diffTime = endTime - startTime;
                startTime = endTime;
                world.update(diffTime/1_000_000_000.0, 10);
                update();
                draw();
            }
        };
        timer.start();
    }

    public class ColorRectID {
        public Color color = null;
        public Point2D size = null;

		public ColorRectID(Color color, Point2D size) {
            this.color = color;
            this.size = size;
		}

        @Override
        public int hashCode() {
            return color.hashCode() + size.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof ColorRectID){
                var crID = (ColorRectID)o;
                var sameColor = crID.color.equals(color);
                var sameSize = crID.size.equals(size);
                return sameColor && sameSize;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s %s", color, size);
        }
    }

    public class ColorRect{
        public ColorRectID id = null;
        static public final int colorSize = 4;
        ColorRect(Color color, Point2D size) {
            this(new ColorRectID(color, size));
        }
        ColorRect(ColorRectID id) {
            this.id = id;
        }
    }

    private HashMap<ColorRectID, ColorRect> rects = new HashMap<ColorRectID, ColorRect>();

    public void rect(Vector2 center, Vector2 size, Vector2 halfsize, Color color) {
        final var pos = center.subtract(halfsize);
        rect(
            new Point2D(pos.x, pos.y).multiply(WORLD_SCALE),
            new Point2D(size.x, size.y).multiply(WORLD_SCALE),
            color
        );
    }
    public void rect(Point2D position, Point2D size, Color color) {
        var bufferID = new ColorRectID(color, size);
        var buffer = rects.get(bufferID);
        if(buffer == null){
            // System.out.println(String.format("%s", bufferID));
            // System.out.print(String.format("ADD."));
            buffer = new ColorRect(bufferID);
            rects.put(buffer.id, buffer);
        }
        // System.out.println(String.format("%s", rects.size()));
        context.setFill(buffer.id.color);
        context.fillRect(
            (int)position.getX(), (int)position.getY(),
            (int)buffer.id.size.getX(), (int)buffer.id.size.getY()
        );
    }

    private ArrayList<Stone> deadStones = new ArrayList<Stone>();

    private void removeStones() {
        deadStones.stream()
        .forEach(s -> removeStone(s));
        deadStones.clear();
    }

    public void update() {
        removeStones();
        if(stones.size() == 0){
            var done = (System.nanoTime() - startedAt)/1_000_000_000.0;
            System.out.println(String.format("Success!"));
            System.out.println(String.format("You succeeded after %ss and %s clicks!", done, clicks));
            Platform.exit();
        }
    }

    public void draw() {
        context.clearRect(0, 0, screen.x, screen.y);
        Arrays.stream(walls).forEach(wall -> wall.draw());
        stones.stream()
        .forEach(stone -> stone.draw());
        player.draw();
        ball.draw();
    }
}
