// File: Bot/Actions/MoveAction.java
package Bot.Actions;

import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;

import java.util.Arrays;
import java.util.List;

public class MoveAction extends BotAction {
    private final Hero hero;
    private final GameMap gameMap;
    private final List<Node> nodesToAvoid;
    private final Node start;
    private final Node end;

    public MoveAction(Hero hero, GameMap gameMap, Node start, Node end, List<Node> nodesToAvoid, String reason) {
        this.hero = hero;
        this.gameMap = gameMap;
        this.start = start;
        this.end = end;
        this.nodesToAvoid = nodesToAvoid;
        this.reason = reason;
    }

    @Override
    public List<String> execute() {
        try {
            String path = PathUtils.getShortestPath(gameMap, nodesToAvoid, start, end, false);
            if (path != null && !path.isEmpty()) return Arrays.asList(path.split(""));
        } catch (Exception e) {
        }
        return null;
    }
}
