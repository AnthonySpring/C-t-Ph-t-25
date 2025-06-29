package Bot;

import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Hero;
import jsclub.codefest.sdk.model.Tile;
import java.util.*;

public class BotBrain {

    private static final int DANGER_ZONE_DISTANCE = 4;

    private final Hero hero;
    private final GameMap gameMap;

    public BotBrain(Hero hero) {
        this.hero = hero;
        this.gameMap = hero.getGameMap();
    }

    public void Action(Object mapData) {
        this.gameMap.updateOnUpdateMap(mapData);
        this.makeDecision();
    }

    private void makeDecision() {
        Tile myPosition = hero.getCurrentPosition();

        Tile nearestEnemy = findNearestTargetInList(myPosition, hero.getEnemies());
        if (nearestEnemy != null && getManhattanDistance(myPosition, nearestEnemy) <= DANGER_ZONE_DISTANCE) {
            fleeFrom(nearestEnemy);
            return;
        }

        Tile target = findNearestTargetInList(myPosition, gameMap.getChests());
        if (target != null) {
            moveTo(target);
            return;
        }

        target = findNearestTargetInList(myPosition, gameMap.getPowerUps());
        if (target != null) {
            moveTo(target);
            return;
        }

        target = findNearestTargetInList(myPosition, gameMap.getSpoilTiles());
        if (target != null) {
            moveTo(target);
            return;
        }

        moveRandomly();
    }

    private void moveTo(Tile target) {
        String nextMove = findPathBFS(hero.getCurrentPosition(), target);
        if (nextMove != null) {
            hero.move(nextMove);
        } else {
            moveRandomly();
        }
    }

    private void fleeFrom(Tile enemy) {
        Tile myPosition = hero.getCurrentPosition();
        int dx = myPosition.getX() - enemy.getX();
        int dy = myPosition.getY() - enemy.getY();

        String move;
        if (Math.abs(dx) > Math.abs(dy)) {
            move = (dx > 0) ? "RIGHT" : "LEFT";
        } else {
            move = (dy > 0) ? "DOWN" : "UP";
        }

        if (isWalkable(getTileInDirection(myPosition, move))) {
            hero.move(move);
        } else {
            moveRandomly();
        }
    }

    private void moveRandomly() {
        List<String> moves = new ArrayList<>(Arrays.asList("UP", "DOWN", "LEFT", "RIGHT"));
        Collections.shuffle(moves);
        for (String move : moves) {
            if (isWalkable(getTileInDirection(hero.getCurrentPosition(), move))) {
                hero.move(move);
                return;
            }
        }
    }

    private String findPathBFS(Tile start, Tile end) {
        Queue<Tile> queue = new LinkedList<>();
        Map<Tile, Tile> parentMap = new HashMap<>();
        Set<Tile> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        parentMap.put(start, null);

        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            if (current.isSamePosition(end)) {
                Tile step = current;
                while (parentMap.get(step) != null && !parentMap.get(step).isSamePosition(start)) {
                    step = parentMap.get(step);
                }
                return getDirectionFromTwoTiles(start, step);
            }

            for (String move : Arrays.asList("UP", "DOWN", "LEFT", "RIGHT")) {
                Tile neighbor = getTileInDirection(current, move);
                if (isWalkable(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        return null;
    }

    private Tile findNearestTargetInList(Tile start, List<Tile> targets) {
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        return targets.stream()
                .min(Comparator.comparingInt(target -> getManhattanDistance(start, target)))
                .orElse(null);
    }

    private boolean isWalkable(Tile tile) {
        return tile != null && tile.getTileType() != Tile.TileType.WALL;
    }

    private int getManhattanDistance(Tile t1, Tile t2) {
        return Math.abs(t1.getX() - t2.getX()) + Math.abs(t1.getY() - t2.getY());
    }

    private Tile getTileInDirection(Tile origin, String direction) {
        int x = origin.getX();
        int y = origin.getY();
        switch (direction) {
            case "UP": y--; break;
            case "DOWN": y++; break;
            case "LEFT": x--; break;
            case "RIGHT": x++; break;
        }
        return gameMap.getTile(y, x);
    }

    private String getDirectionFromTwoTiles(Tile from, Tile to) {
        if (to.getY() < from.getY()) return "UP";
        if (to.getY() > from.getY()) return "DOWN";
        if (to.getX() < from.getX()) return "LEFT";
        if (to.getX() > from.getX()) return "RIGHT";
        return null;
    }
}