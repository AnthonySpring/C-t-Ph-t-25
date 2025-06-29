package Bot.Utils;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BotUtils {
    public static <T extends Node> T findNearest(Node startNode, List<T> nodes) { if (startNode==null||nodes==null||nodes.isEmpty()) return null; return nodes.stream().min(Comparator.comparingDouble(node -> getDistance(startNode, node))).orElse(null); }
    public static double getDistance(Node n1, Node n2) { if (n1==null||n2==null) return Double.MAX_VALUE; return Math.hypot(n1.getX()-n2.getX(), n1.getY()-n2.getY()); }
    public static String getDirectionString(Node from, Node to) { if (from==null||to==null) return null; int dx = to.getX()-from.getX(), dy = to.getY()-from.getY(); if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? "R" : "L"; if (dy != 0) return dy > 0 ? "D" : "U"; return null; }
    public static String getOppositeDirection(String dir) { if(dir==null) return null; return switch(dir){case "U"->"D";case "D"->"U";case "L"->"R";case "R"->"L";default->null;}; }
    public static Node getRetreatPoint(Player player, Node target) { return new Node(player.getX() - (target.getX() - player.getX()), player.getY() - (target.getY() - player.getY())); }
    public static List<Node> getNodesToAvoid(GameMap gm, Player p) { List<Node> n=new ArrayList<>(); if(gm.getListIndestructibles()!=null)n.addAll(gm.getListIndestructibles()); if(gm.getOtherPlayerInfo()!=null)n.addAll(gm.getOtherPlayerInfo().stream().filter(pl->!pl.getId().equals(p.getId())).collect(Collectors.toList())); if(gm.getListEnemies()!=null)n.addAll(gm.getListEnemies()); if(gm.getListObstacles()!=null)n.addAll(gm.getListObstacles().stream().filter(o->o.getId().contains("TRAP")).collect(Collectors.toList())); if(gm.getObstaclesByTag("CAN_GO_THROUGH")!=null)n.removeAll(gm.getObstaclesByTag("CAN_GO_THROUGH")); return n; }
    public static Obstacle findSpecialLoot(GameMap gm, String id) { if(gm.getListObstacles() == null) return null; return gm.getListObstacles().stream().filter(o -> o.getId().contains(id)).findFirst().orElse(null); }
    public static Node findGeneralLoot(Player p, GameMap gm) { List<Obstacle> chests = (gm.getListObstacles() == null) ? new ArrayList<>() : gm.getListObstacles().stream().filter(o->o.getId().contains("CHEST")).collect(Collectors.toList()); Obstacle nC = findNearest(p, chests); Weapon nW = findNearest(p, gm.getAllGun()); if (nC==null) return nW; if (nW==null) return nC; return getDistance(p, nC) < getDistance(p, nW) ? nC : nW; }
    public static Node findNearestTarget(Player p, GameMap gm) { List<Node> allTargets = new ArrayList<>(); if (gm.getOtherPlayerInfo() != null) allTargets.addAll(gm.getOtherPlayerInfo()); if (gm.getListEnemies() != null) allTargets.addAll(gm.getListEnemies()); return findNearest(p, allTargets); }
}