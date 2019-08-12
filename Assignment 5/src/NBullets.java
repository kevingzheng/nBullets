import java.awt.*;
import java.util.*;

import javalib.worldimages.*;
import tester.*;
import javalib.funworld.*;

//Class for constants
class Constants {
  final static int WIDTH = 500;
  final static int HEIGHT = 300;
  final static int SHIP_RADIUS = HEIGHT / 30;
  final static double ON_TICK_SPEED = 1.0 / 28.0;
  final static int BULLET_SPEED = 8;
  final static int SHIP_SPAWN_SPEED = 1;
  final static int SHIP_SPEED = BULLET_SPEED / 2;
  final static int ORIGINAL_BULLET_RADIUS = 2;
  final static Color BULLET_COLOR = Color.PINK;
  final static Color SHIP_COLOR = Color.CYAN;
  final static int FONT_SIZE = 13;
  final static Color FONT_COLOR = Color.BLACK;
  final static int FRACTIONAL_NOT_ALLOWED = 7;

}

// This class is synonymous to a Posn Class. The only difference is that 
// this class has 2 doubles, whereas the Posn class has 2 ints. The reason this was 
// done was to avoid rounding errors. 
// Creating the Velocity class
class Velocity {
  double x;
  double y;

  // Constructor for Velocity
  Velocity(double x, double y) {
    this.x = x;
    this.y = y;
  }
}

// Class for NBullets that extends the world class
class NBullets extends World {

  ILoShips listOfShips;
  int bullets;
  int time;
  ILoBullets listOfBullets;
  int numShips;

  NBullets(ILoShips listOfShips, int bullets, int time, ILoBullets listOfBullets, int numShips) {
    this.listOfShips = listOfShips;
    this.bullets = bullets;
    this.time = time;
    this.listOfBullets = listOfBullets;
    this.numShips = numShips;
  }

  // constructor that uses default values given a number of bullets
  NBullets(int bullets) {
    this(new MtLoShips(), bullets, 0, new MtLoBullets(), 0);
  }

  // draws the WorldScene
  public WorldScene makeScene() {
    TextImage textImg = new TextImage(
        "Bullets Left:" + this.bullets + " Ships Sunk: " + this.numShips, Constants.FONT_SIZE,
        Constants.FONT_COLOR);
    return this.listOfBullets
        .draw(this.listOfShips.draw(new WorldScene(Constants.WIDTH, Constants.HEIGHT)))
        .placeImageXY(textImg, Constants.WIDTH / 5, Constants.HEIGHT * 19 / 20);
  }

  // draws the WorldScene
  public WorldScene makeFinalScene() {
    TextImage textImg = new TextImage("Bullets Left:" + 0 + " Ships Sunk: " + this.numShips,
        Constants.FONT_SIZE, Constants.FONT_COLOR);
    return this.listOfBullets
        .draw(this.listOfShips.draw(new WorldScene(Constants.WIDTH, Constants.HEIGHT)))
        .placeImageXY(textImg, Constants.WIDTH / 5, Constants.HEIGHT * 19 / 20);
  }

  // manages each element of the World on each tick
  public World onTick() {
    ILoBullets newBullets = this.listOfBullets.checkCollisions(this.listOfShips);
    ILoShips newShips = this.listOfShips.checkCollisions(this.listOfBullets);
    int newNumShips = this.listOfShips.length() - newShips.length() + this.numShips;

    if (this.time % 15 == 0) {
      ILoShips moveShips = new ConsLoShips(new Ship(), newShips);
      return new NBullets(moveShips.move().removeOffScreen(), this.bullets, this.time + 1,
          newBullets.removeOffScreen().move(), newNumShips);
    } else {
      return new NBullets(newShips.move().removeOffScreen(), this.bullets, this.time + 1,
          newBullets.removeOffScreen().move(), newNumShips);
    }
  }

  // Alters the World state after a key is pressed
  public World onKeyEvent(String key) {
    if (key.equals(" ") && this.bullets > 0) {
      ConsLoBullets newListOfBullets = new ConsLoBullets(new Bullet(this.bullets - 1),
          this.listOfBullets);

      return new NBullets(this.listOfShips, this.bullets - 1, this.time, newListOfBullets,
          this.numShips);
    } else {
      return this;
    }
  }

  // calls the respective draw function on the world end to render the world
  public WorldEnd worldEnds() {
    if (this.bullets == 0 && this.listOfBullets.length() == 0) {
      return new WorldEnd(true, this.makeFinalScene());
    } else {
      return new WorldEnd(false, this.makeScene());
    }
  }
}

class Bullet {
  int numberOfExplosions;
  int numberOfBullets;
  Velocity velocity;
  int radius;
  Posn position;

  // Constructor for creating a bullet given just a position and a number of
  // bullets
  Bullet(int numberOfBullets, Posn position) {
    this.numberOfExplosions = 1;
    this.numberOfBullets = numberOfBullets;
    this.position = position;
    this.velocity = new Velocity(0, -1 * Constants.BULLET_SPEED);
    this.radius = Constants.ORIGINAL_BULLET_RADIUS;

  }

  // constructor that creates a bullet given only a number of bullets
  Bullet(int numberOfBullets) {
    this(numberOfBullets, new Posn(Constants.WIDTH / 2, Constants.HEIGHT - 1));

  }

  // creating a new bullet after an explosion
  Bullet(int numberOfBullets, Posn position, int numberOfExplosions, Velocity velocity) {
    this.numberOfExplosions = numberOfExplosions;
    this.numberOfBullets = numberOfBullets;
    this.position = position;
    this.velocity = velocity;
    if (this.numberOfExplosions == 0) {
      this.radius = Math.min(Constants.ORIGINAL_BULLET_RADIUS + 2 * this.numberOfExplosions, 10);
    } else {
      this.radius = Math.min(2 * this.numberOfExplosions, 10);
    }
  }

  // renders the bullets
  WorldScene draw(WorldScene acc) {
    return acc.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, Constants.BULLET_COLOR),
        this.position.x, this.position.y);
  }

  // returns whether or not this bullet is off screen
  boolean isOffScreen() {
    return (this.position.x > Constants.WIDTH || this.position.x < 0)
        || (this.position.y > Constants.HEIGHT - 1 || this.position.y < 0);

  }

  // returns whether or not this bullet has hit that ship
  boolean thisBulletHitThatShip(Ship that) {
    return (Math.sqrt((Math.pow(Math.abs(this.position.x - that.x), 2)
        + Math.pow(Math.abs(this.position.y - that.y), 2))) < this.radius + that.radius);
  }

  // creates a list of new bullets after this bullet has a collision
  ILoBullets newBulletsAfterCollision(int count) {

    if (count > this.numberOfExplosions) {
      return new MtLoBullets();
    }
    Bullet first = new Bullet(this.numberOfBullets, this.position, this.numberOfExplosions + 1,
        new Velocity(
            (Math.hypot(this.velocity.x, this.velocity.y)
                * Math.cos(count * 2 * Math.PI / (this.numberOfExplosions + 1))),
            (Math.hypot(this.velocity.x, this.velocity.y)
                * Math.sin(count * 2 * Math.PI / (this.numberOfExplosions + 1)))));
    return new ConsLoBullets(first, newBulletsAfterCollision(count + 1).removeOffScreen());

  }

  // moves the bullets
  Bullet move() {
    return new Bullet(this.numberOfBullets, new Posn((int) (this.position.x + this.velocity.x),
        (int) (this.position.y + this.velocity.y)), this.numberOfExplosions, this.velocity);
  }

}

interface ILoBullets {
  // renders the World Scene
  WorldScene draw(WorldScene original);

  // moves this ILoBullets
  ILoBullets move();

  // removes the ILoBullets that are off screen
  ILoBullets removeOffScreen();

  // appends the bullets together
  ILoBullets append(ILoBullets other);

  // checks collisions between this ILoBullets and a given ILoShips
  ILoBullets checkCollisions(ILoShips that);

  // checks to see if any of these ILoBullets are colliding with ships
  boolean anyCollidingWithShip(Ship that);

  // calculates the number of elements in this ILoBullets
  int length();

}

class MtLoBullets implements ILoBullets {

  // renders this WorldScene with this MtLoBullets onto the original
  public WorldScene draw(WorldScene original) {
    return original;
  }

  // moves this MtLoBullets
  public ILoBullets move() {
    return new MtLoBullets();
  }

  // removes these MtLoBullets
  public ILoBullets removeOffScreen() {
    return new MtLoBullets();
  }

  // appends this MtLoBullets to the other
  public ILoBullets append(ILoBullets other) {
    return other;
  }

  // checks the collisions with that against this MtLoBullets
  public ILoBullets checkCollisions(ILoShips that) {
    return new MtLoBullets();
  }

  // checks if any of these MtLoBullets are colliding with that shiup
  public boolean anyCollidingWithShip(Ship that) {
    return false;
  }

  // returns the length of this MtLoBullets
  public int length() {
    return 0;
  }

}

// class for ConsLoBullets that implements ILoBullets
class ConsLoBullets implements ILoBullets {
  Bullet first;
  ILoBullets rest;

  // Constructor for ConsLoBullets
  ConsLoBullets(Bullet first, ILoBullets rest) {
    this.first = first;
    this.rest = rest;
  }

  // renders this ConsLloBullets onto the original
  public WorldScene draw(WorldScene original) {
    return this.rest.draw(this.first.draw(original));
  }

  // moves this ConsLoBullets
  public ILoBullets move() {
    return new ConsLoBullets(this.first.move(), this.rest.move());
  }

  // removes the off sceen bullets in this ConsLoBullets
  public ILoBullets removeOffScreen() {
    if (this.first.isOffScreen()) {
      return this.rest.removeOffScreen();
    } else {
      return new ConsLoBullets(this.first, this.rest.removeOffScreen());
    }
  }

  // appends two ILoBullets
  public ILoBullets append(ILoBullets other) {
    return new ConsLoBullets(this.first, this.rest.append(other));
  }

  // adds any bullets that have collided with the current bullets
  public ILoBullets checkCollisions(ILoShips that) {
    if (this.first.isOffScreen()) {
      return this.rest.checkCollisions(that);
    }
    if (that.anyCollidingWithBullet(this.first)) {
      return this.rest.append(this.first.newBulletsAfterCollision(0));
    } else {
      return new ConsLoBullets(this.first, this.rest.checkCollisions(that));
    }
  }

  // returns if any of this list of bullets collides with that ship
  public boolean anyCollidingWithShip(Ship that) {
    return this.first.thisBulletHitThatShip(that) || this.rest.anyCollidingWithShip(that);
  }

  // returns the length of this ConsLoBullets
  public int length() {
    return 1 + this.rest.length();
  }

}

// Class for creating ships
class Ship {
  int radius = Constants.SHIP_RADIUS;
  int dx;
  int x;
  int y;
  Random rand;

  // Constructors with all the fields
  Ship(int dx, int x, int y, Random rand) {
    this.dx = dx;
    this.x = x;
    this.y = y;
    this.rand = rand;
  }

  // constructor with all default values set
  Ship() {
    this(0, 0, 0, new Random());
    int random = rand.nextInt(2);
    this.dx = 1 - 2 * random;
    this.y = rand.nextInt(5 * Constants.HEIGHT / Constants.FRACTIONAL_NOT_ALLOWED)
        + Constants.HEIGHT / Constants.FRACTIONAL_NOT_ALLOWED;
    this.x = random * Constants.WIDTH;
  }

  // draws a circle image with this radius representing a ship
  WorldScene draw(WorldScene acc) {
    return acc.placeImageXY(new CircleImage(this.radius, OutlineMode.SOLID, Constants.SHIP_COLOR),
        this.x, this.y);
  }

  // returns the position of the ship
  Posn getPosition() {
    return new Posn(this.x, this.y);
  }

  // moves the ship
  Ship move() {
    return new Ship(this.dx, this.dx + this.x, this.y, this.rand);
  }

  // is this ship off screen?
  boolean isOffScreen() {
    return (this.x > Constants.WIDTH + this.radius) || (this.x < -1 * this.radius)
        || (this.y > Constants.HEIGHT + this.radius || this.y < -1 * this.radius);
  }

  // do these two ships collide?
  boolean thisShipCollidingThat(Bullet that) {
    return that.thisBulletHitThatShip(this);
  }

}

// interface for ILoShips
interface ILoShips {

  // draws this ILoShips
  WorldScene draw(WorldScene original);

  // moves this ILoShips
  ILoShips move();

  // removes the ILoShips that are off screen
  ILoShips removeOffScreen();

  // removes the ILoShips that have been hit by a bullet
  ILoShips removeCollisions(Bullet that);

  // are any of these ILoShips colliding with the bullet?
  boolean anyCollidingWithBullet(Bullet that);

  // checks collisions of this ILoShips against an ILoBullets
  ILoShips checkCollisions(ILoBullets that);

  // calculates the length of this ILoShips
  int length();

}

// class for empty list of Ships
class MtLoShips implements ILoShips {

  // draws the MtLoShip onto the previous WorldScene
  public WorldScene draw(WorldScene original) {
    return original;
  }

  // moves this MtLoShips
  public ILoShips move() {
    return new MtLoShips();
  }

  // removes the ILoShips that are off screen in this MtLoShips
  public ILoShips removeOffScreen() {
    return new MtLoShips();
  }

  // removes the ships that have been hit by a bullet in this MtLoShips
  public ILoShips removeCollisions(Bullet that) {
    return new MtLoShips();
  }

  // are any of these ILoShips colliding with a bullet in this MtLoShips?
  public boolean anyCollidingWithBullet(Bullet that) {
    return false;
  }

  // checks collisions of this MtLoShips against an ILoBullets
  public ILoShips checkCollisions(ILoBullets that) {
    return new MtLoShips();
  }

  // returns the length of this MtLoShips
  public int length() {
    return 0;
  }

}

// class for ConsLoShip that implements ILoShips
class ConsLoShips implements ILoShips {
  Ship first;
  ILoShips rest;

  ConsLoShips(Ship first, ILoShips rest) {
    this.first = first;
    this.rest = rest;
  }

  // draws this ConsLoShip onto the previous WorldScene
  public WorldScene draw(WorldScene original) {
    return this.rest.draw(this.first.draw(original));
  }

  // moves this ConsLoShips
  public ILoShips move() {
    return new ConsLoShips(this.first.move(), this.rest.move());
  }

  // removes the off screen ships in this ConsLoShips
  public ILoShips removeOffScreen() {
    if (this.first.isOffScreen()) {
      return this.rest.removeOffScreen();
    } else {
      return new ConsLoShips(this.first, this.rest.removeOffScreen());
    }
  }

  // removes the ships in this ConsLoShips that have been hit by a bullet
  public ILoShips removeCollisions(Bullet that) {
    if (this.first.thisShipCollidingThat(that)) {
      return this.rest.removeCollisions(that);
    } else {
      return new ConsLoShips(this.first, this.rest.removeCollisions(that));
    }
  }

  // are any ships colliding with a bullet in this ConsLoShips
  public boolean anyCollidingWithBullet(Bullet that) {
    return this.first.thisShipCollidingThat(that) || this.rest.anyCollidingWithBullet(that);
  }

  // returns the remaining ships in a ConsLoShips that haven't yet been hit
  public ILoShips checkCollisions(ILoBullets that) {
    if (that.anyCollidingWithShip(this.first)) {
      return this.rest.checkCollisions(that);
    } else {
      return new ConsLoShips(this.first, this.rest.checkCollisions(that));
    }
  }

  // returns the length of this ConsLoShips
  public int length() {
    return 1 + this.rest.length();
  }

}

// Class examples
class ExamplesMyWorldProgram {

  // creating examples of bullets
  Bullet bullet1 = new Bullet(10, new Posn(50, 50));
  Bullet bullet2 = new Bullet(3, new Posn(0, 80));
  Bullet bullet3 = new Bullet(10);
  Bullet bullet4 = new Bullet(8, new Posn(10, 10), 4, new Velocity(20, 20));
  Bullet bullet5 = new Bullet(10, new Posn(-50, 60));
  Bullet bullet6 = new Bullet(10, new Posn(50, -60));
  Bullet bullet7 = new Bullet(1, new Posn(500, 10));
  Bullet bullet8 = new Bullet(1, new Posn(0, 10));
  Bullet bullet9 = new Bullet(1, new Posn(10, 300));
  Bullet bullet10 = new Bullet(1, new Posn(400, 0));

  // creating examples of ILoBullets
  ILoBullets mtlob = new MtLoBullets();
  ILoBullets lob1 = new ConsLoBullets(bullet1, mtlob);
  ILoBullets lob2 = new ConsLoBullets(bullet2, lob1);
  ILoBullets lob3 = new ConsLoBullets(bullet3, lob2);
  ILoBullets lob4 = new ConsLoBullets(bullet5, lob3);
  ILoBullets lob5 = new ConsLoBullets(bullet6, lob4);
  ILoBullets lob6 = new ConsLoBullets(bullet7, lob5);
  ILoBullets lob7 = new ConsLoBullets(bullet8, lob6);
  ILoBullets lob8 = new ConsLoBullets(bullet9, lob7);
  ILoBullets lob9 = new ConsLoBullets(bullet10, lob8);

  // creating examples of ships
  Random rand1 = new Random(7);
  Ship ship1 = new Ship();
  Ship ship2 = new Ship(10, 10, 10, rand1);
  Ship ship3 = new Ship(8, 50, 50, rand1);
  Ship ship4 = new Ship(10, -50, 60, rand1);
  Ship ship5 = new Ship(10, 50, -60, rand1);
  Ship ship6 = new Ship(10, -50, -60, rand1);
  Ship ship7 = new Ship(8, 500, 10, rand1);
  Ship ship8 = new Ship(8, 510, 10, rand1);
  Ship ship9 = new Ship(8, 511, 10, rand1);
  Ship ship10 = new Ship(8, -10, 10, rand1);
  Ship ship11 = new Ship(8, -11, 10, rand1);
  Ship ship12 = new Ship(8, 10, 310, rand1);
  Ship ship13 = new Ship(8, 10, 311, rand1);
  Ship ship14 = new Ship(8, 10, -10, rand1);
  Ship ship15 = new Ship(8, 10, -11, rand1);

  // creating examples of ILoShips
  ILoShips mtlos = new MtLoShips();
  ILoShips los1 = new ConsLoShips(ship2, mtlos);
  ILoShips los2 = new ConsLoShips(ship3, los1);
  ILoShips los3 = new ConsLoShips(ship4, los2);
  ILoShips los4 = new ConsLoShips(ship5, los3);
  ILoShips los5 = new ConsLoShips(ship6, los3);

  // creating examples of NBullets
  NBullets game1 = new NBullets(this.los1, 10, 20, this.lob1, 5);
  NBullets game2 = new NBullets(this.los1, 0, 20, this.mtlob, 5);

  // testing draw for Bullets
  boolean testDraw(Tester t) {
    return t.checkExpect(this.bullet1.draw(new WorldScene(500, 300)),
        new WorldScene(500, 300).placeImageXY(new CircleImage(2, OutlineMode.SOLID, Color.PINK), 50,
            50))
        && t.checkExpect(this.bullet2.draw(this.bullet1.draw(new WorldScene(500, 300))),
            new WorldScene(500, 300)
                .placeImageXY(new CircleImage(2, OutlineMode.SOLID, Color.PINK), 50, 50)
                .placeImageXY(new CircleImage(2, OutlineMode.SOLID, Color.PINK), 0, 80));
  }

  // testing isOffScreen
  boolean testIsOffScreen(Tester t) {
    return t.checkExpect(this.bullet1.isOffScreen(), false)
        && t.checkExpect(this.bullet2.isOffScreen(), false)
        && t.checkExpect(this.bullet3.isOffScreen(), false)
        && t.checkExpect(this.bullet4.isOffScreen(), false)
        && t.checkExpect(this.bullet5.isOffScreen(), true)
        && t.checkExpect(this.bullet6.isOffScreen(), true)
        && t.checkExpect(this.bullet7.isOffScreen(), false)
        && t.checkExpect(this.bullet8.isOffScreen(), false)
        && t.checkExpect(this.bullet9.isOffScreen(), true)
        && t.checkExpect(this.bullet10.isOffScreen(), false);
  }

  // testing thisBulletHitThatShip
  boolean testThisBulletHitThatShip(Tester t) {
    return t.checkExpect(this.bullet1.thisBulletHitThatShip(this.ship2), false)
        && t.checkExpect(this.bullet1.thisBulletHitThatShip(this.ship3), true);
  }

  // testing newBulletsAfterCollision
  boolean testnewBulletsAfterCollision(Tester t) {
    return t.checkExpect(this.bullet1.newBulletsAfterCollision(11), new MtLoBullets());
  }

  // testing move for Bullet
  boolean testMove(Tester t) {
    return t.checkExpect(this.bullet1.move(), new Bullet(10, new Posn(50, 42)))
        && t.checkExpect(this.bullet2.move(), new Bullet(3, new Posn(0, 72)));
  }

  // testing draw for ILoBullets
  boolean testDrawILoBullets(Tester t) {
    return t.checkExpect(this.mtlob.draw(new WorldScene(500, 300)), new WorldScene(500, 300))
        && t.checkExpect(this.lob1.draw(this.mtlob.draw(new WorldScene(500, 300))),
            this.mtlob.draw(this.bullet1.draw(this.mtlob.draw(new WorldScene(500, 300)))));
  }

  // testing move for ILoBullets
  boolean testMoveILoBullets(Tester t) {
    return t.checkExpect(this.mtlob.move(), new MtLoBullets())
        && t.checkExpect(this.lob1.move(),
            new ConsLoBullets(new Bullet(10, new Posn(50, 42)), this.mtlob))
        && t.checkExpect(this.lob2.move(), new ConsLoBullets(new Bullet(3, new Posn(0, 72)),
            new ConsLoBullets(new Bullet(10, new Posn(50, 42)), this.mtlob)));
  }

  // testing removeOffScreen
  boolean testremoveOffScreen(Tester t) {
    return t.checkExpect(this.mtlob.removeOffScreen(), new MtLoBullets())
        && t.checkExpect(this.lob1.removeOffScreen(), this.lob1)
        && t.checkExpect(this.lob2.removeOffScreen(), this.lob2)
        && t.checkExpect(this.lob4.removeOffScreen(), this.lob3)
        && t.checkExpect(this.lob5.removeOffScreen(), this.lob3)
        && t.checkExpect(this.lob6.removeOffScreen(), new ConsLoBullets(this.bullet7, this.lob3))
        && t.checkExpect(this.lob7.removeOffScreen(),
            new ConsLoBullets(this.bullet8, new ConsLoBullets(this.bullet7, this.lob3)));
  }

  // testing append
  boolean testAppend(Tester t) {
    return t.checkExpect(this.mtlob.append(lob1), this.lob1)
        && t.checkExpect(this.mtlob.append(this.mtlob), this.mtlob)
        && t.checkExpect(this.lob1.append(this.mtlob), this.lob1)
        && t.checkExpect(this.lob1.append(this.lob2),
            new ConsLoBullets(this.bullet1, new ConsLoBullets(this.bullet2, lob1)));
  }

  // testing checkCollisions
  boolean testCheckCollisions(Tester t) {
    return t.checkExpect(this.mtlob.checkCollisions(this.los1), new MtLoBullets())
        && t.checkExpect(this.lob1.checkCollisions(this.mtlos), this.lob1)
        && t.checkExpect(this.lob1.checkCollisions(this.los1), this.lob1);
  }

  // testing anyCollidingWithShip
  boolean testAnyCollidingWithShip(Tester t) {
    return t.checkExpect(this.mtlob.anyCollidingWithShip(this.ship2), false)
        && t.checkExpect(this.lob1.anyCollidingWithShip(this.ship2), false)
        && t.checkExpect(this.lob1.anyCollidingWithShip(this.ship3), true);
  }

  // testing length
  boolean testLength(Tester t) {
    return t.checkExpect(this.lob1.length(), 1) && t.checkExpect(this.mtlob.length(), 0)
        && t.checkExpect(this.lob2.length(), 2);
  }

  // testing draw for Ship
  boolean testDrawShip(Tester t) {
    return t.checkExpect(this.ship2.draw(new WorldScene(300, 500)),
        new WorldScene(300, 500).placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN),
            10, 10))
        && t.checkExpect(
            this.ship3.draw(new WorldScene(300, 500)
                .placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN), 10, 10)),
            new WorldScene(300, 500)
                .placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN), 10, 10)
                .placeImageXY(new CircleImage(10, OutlineMode.SOLID, Color.CYAN), 50, 50));
  }

  // testing getPosition
  boolean testGetPosition(Tester t) {
    return t.checkExpect(this.ship2.getPosition(), new Posn(10, 10))
        && t.checkExpect(this.ship3.getPosition(), new Posn(50, 50));
  }

  // testing move
  boolean testMoveShip(Tester t) {
    return t.checkExpect(this.ship2.move(), new Ship(10, 20, 10, this.rand1));
  }

  // testing isOffScreen
  boolean testIsOffScreenShip(Tester t) {
    return t.checkExpect(this.ship2.isOffScreen(), false)
        && t.checkExpect(this.ship4.isOffScreen(), true)
        && t.checkExpect(this.ship5.isOffScreen(), true)
        && t.checkExpect(this.ship6.isOffScreen(), true)
        && t.checkExpect(this.ship8.isOffScreen(), false)
        && t.checkExpect(this.ship9.isOffScreen(), true)
        && t.checkExpect(this.ship10.isOffScreen(), false)
        && t.checkExpect(this.ship11.isOffScreen(), true)
        && t.checkExpect(this.ship12.isOffScreen(), false)
        && t.checkExpect(this.ship13.isOffScreen(), true)
        && t.checkExpect(this.ship14.isOffScreen(), false)
        && t.checkExpect(this.ship15.isOffScreen(), true);

  }

  // testing thisShipCollidingThat
  boolean thisShipCollidingThat(Tester t) {
    return t.checkExpect(this.ship2.thisShipCollidingThat(bullet1), false)
        && t.checkExpect(this.ship3.thisShipCollidingThat(bullet1), true);
  }

  // testing draw for ILoShips
  boolean testDrawILoShips(Tester t) {
    return t.checkExpect(this.mtlos.draw(new WorldScene(500, 300)), new WorldScene(500, 300))
        && t.checkExpect(this.los1.draw(this.mtlos.draw(new WorldScene(500, 300))),
            this.mtlos.draw(this.ship2.draw((this.mtlos.draw(new WorldScene(500, 300))))));
  }

  // testing move for ILoShips
  boolean testMoveILoShips(Tester t) {
    return t.checkExpect(this.mtlos.move(), new MtLoShips()) && t.checkExpect(this.los1.move(),
        new ConsLoShips(new Ship(10, 20, 10, this.rand1), this.mtlos));
  }

  // testing removeOffScreen
  boolean testRemoveOffScreen(Tester t) {
    return t.checkExpect(this.mtlos.removeOffScreen(), new MtLoShips())
        && t.checkExpect(this.los1.removeOffScreen(), this.los1)
        && t.checkExpect(this.los3.removeOffScreen(), this.los2)
        && t.checkExpect(this.los4.removeOffScreen(), this.los2)
        && t.checkExpect(this.los5.removeOffScreen(), this.los2);
  }

  // testing removeCollisions
  boolean testRemoveCollisions(Tester t) {
    return t.checkExpect(this.mtlos.removeCollisions(this.bullet1), this.mtlos)
        && t.checkExpect(this.los1.removeCollisions(this.bullet1), this.los1)
        && t.checkExpect(this.los1.removeCollisions(this.bullet4), this.mtlos)
        && t.checkExpect(this.los2.removeCollisions(this.bullet1), this.los1);

  }

  // testing anyCollidingWithBullet
  boolean testAnyCollidingWithBullet(Tester t) {
    return t.checkExpect(this.mtlos.anyCollidingWithBullet(bullet1), false)
        && t.checkExpect(this.los1.anyCollidingWithBullet(bullet1), false)
        && t.checkExpect(this.los2.anyCollidingWithBullet(bullet1), true);
  }

  // testing checkCollisions
  boolean testCheckCollisionsILoShips(Tester t) {
    return t.checkExpect(this.mtlos.checkCollisions(this.mtlob), this.mtlos)
        && t.checkExpect(this.los1.checkCollisions(this.lob1), this.los1)
        && t.checkExpect(this.los2.checkCollisions(this.lob2), this.los1);
  }

  // testing length
  boolean testLengthILoShips(Tester t) {
    return t.checkExpect(this.mtlos.length(), 0) && t.checkExpect(this.los1.length(), 1)
        && t.checkExpect(this.los2.length(), 2);
  }

  // testing makeScene
  boolean testMakeScene(Tester t) {
    return t
        .checkExpect(this.game1.makeScene(),
            this.lob1
                .draw(
                    this.los1.draw(new WorldScene(500, 300).placeImageXY(
                        new TextImage("Bullets Left:" + 10 + " Ships Sunk: " + 5,
                            Constants.FONT_SIZE, Constants.FONT_COLOR),
                        Constants.WIDTH / 5, Constants.HEIGHT * 19 / 20))));
  }

  // testing makeFinalScene
  boolean testMakeFinalScene(Tester t) {
    return t
        .checkExpect(this.game1.makeFinalScene(),
            this.lob1
                .draw(
                    this.los1.draw(new WorldScene(500, 300).placeImageXY(
                        new TextImage("Bullets Left:" + 0 + " Ships Sunk: " + 5,
                            Constants.FONT_SIZE, Constants.FONT_COLOR),
                        Constants.WIDTH / 5, Constants.HEIGHT * 19 / 20))));
  }

  // testing onTick
  boolean testOnTick(Tester t) {
    return t.checkExpect(this.game1.onTick(), new NBullets(this.los1.move().removeOffScreen(), 10,
        21, this.lob1.removeOffScreen().move(), 5));
  }

  // testing onKeyEvent
  boolean testOnKeyEvent(Tester t) {
    return t.checkExpect(this.game1.onKeyEvent(" "),
        new NBullets(this.los1, 9, 20, new ConsLoBullets(new Bullet(9), this.lob1), 5))
        && t.checkExpect(this.game1.onKeyEvent("k"), this.game1);
  }

  // testing worldEnds
  boolean testWorldEnds(Tester t) {
    return t.checkExpect(this.game1.worldEnds(), new WorldEnd(false, this.game1.makeScene()))
        && t.checkExpect(this.game2.worldEnds(), new WorldEnd(true, this.game2.makeFinalScene()));
  }

  boolean testBigBang(Tester t) {

    int worldWidth = Constants.WIDTH;
    int worldHeight = Constants.HEIGHT;
    double tickRate = Constants.ON_TICK_SPEED;

    NBullets w = new NBullets(10);
    return w.bigBang(worldWidth, worldHeight, tickRate);
  }

}

