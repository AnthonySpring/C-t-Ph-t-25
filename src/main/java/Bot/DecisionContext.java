package Bot;

import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import java.util.List;
import jsclub.codefest.sdk.base.Node;

public class DecisionContext {
    public final Player player;
    public final GameMap gameMap;
    public final Hero hero;
    public final List<Node> nodesToAvoid;
    public final Node safeZoneCenter;
    public final int mapSize;
    public final int safeZoneRadius;

    public DecisionContext(Player player, GameMap gameMap, Hero hero) {
        this.player = player;
        this.gameMap = gameMap;
        this.hero = hero;
        this.nodesToAvoid = BotUtils.getNodesToAvoid(gameMap, player);
        this.mapSize = gameMap.getMapSize();
        this.safeZoneRadius = gameMap.getSafeZone();
        this.safeZoneCenter = new Node(mapSize / 2, mapSize / 2);
    }
}