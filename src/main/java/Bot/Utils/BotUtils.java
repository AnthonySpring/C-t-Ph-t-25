package Bot.Utils;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.obstacles.ObstacleTag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Utility class for the bot to perform common actions and calculations.
 * This code is strictly based on the provided SDK documentation and file structure,
 * and includes error handling for robustness.
 * All "INCOMPATIBLE TYPES" errors are fixed by explicitly mapping lists to List<Node>.
 * Assumes Node/Element has getX() and getY() as per API documentation.
 */
public class BotUtils {

    public static <T extends Node> T findNearest(Node startNode, List<T> nodes) {
        if (startNode == null || nodes == null || nodes.isEmpty()) {
            return null;
        }
        try {
            return nodes.stream()
                    .min(Comparator.comparingDouble(node -> getDistance(startNode, node)))
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Error finding nearest node: " + e.getMessage());
            return null;
        }
    }

    public static double getDistance(Node n1, Node n2) {
        if (n1 == null || n2 == null) {
            return Double.MAX_VALUE;
        }
        try {
            return Math.hypot(n1.getX() - n2.getX(), n1.getY() - n2.getY());
        } catch (Exception e) {
            System.err.println("Error calculating distance: " + e.getMessage());
            return Double.MAX_VALUE;
        }
    }

    public static String getDirectionString(Node from, Node to) {
        if (from == null || to == null) {
            return null;
        }
        try {
            int dx = (int) (to.getX() - from.getX());
            int dy = (int) (to.getY() - from.getY());
            if (Math.abs(dx) > Math.abs(dy)) {
                return dx > 0 ? "R" : "L";
            }
            if (dy != 0) {
                return dy > 0 ? "D" : "U";
            }
            return null; // Same point
        } catch (Exception e) {
            System.err.println("Error getting direction string: " + e.getMessage());
            return null;
        }
    }

    public static String getOppositeDirection(String dir) {
        if (dir == null) return null;
        return switch (dir) {
            case "U" -> "D";
            case "D" -> "U";
            case "L" -> "R";
            case "R" -> "L";
            default -> null;
        };
    }

    public static Node getRetreatPoint(Node from, Node target) {
        if (from == null || target == null) return null;
        try {
            return new Node((int) (from.getX() - (target.getX() - from.getX())), (int) (from.getY() - (target.getY() - from.getY())));
        } catch (Exception e) {
            System.err.println("Error getting retreat point: " + e.getMessage());
            return null;
        }
    }

    public static List<Node> getNodesToAvoid(GameMap gm, Player p) {
        try {
            Stream<Node> indestructibleStream = (gm.getListIndestructibles() != null) ? gm.getListIndestructibles().stream().map(o -> (Node) o) : Stream.empty();
            Stream<Node> otherPlayersStream = (gm.getOtherPlayerInfo() != null) ? gm.getOtherPlayerInfo().stream().map(pl -> (Node) pl) : Stream.empty();
            Stream<Node> enemyStream = (gm.getListEnemies() != null) ? gm.getListEnemies().stream().map(e -> (Node) e) : Stream.empty();
            Stream<Node> trapsStream = (gm.getListTraps() != null) ? gm.getListTraps().stream().map(t -> (Node) t) : Stream.empty();

            Stream<Node> allPotentialObstaclesStream = Stream.concat(
                    Stream.concat(indestructibleStream, otherPlayersStream),
                    Stream.concat(enemyStream, trapsStream)
            );

            List<Obstacle> canGoThrough = null;
            try {
                canGoThrough = gm.getObstaclesByTag("CAN_GO_THROUGH");
            } catch (IllegalArgumentException e) {
                // Tag not found, so no passable obstacles to filter out
            }

            Set<Node> passableNodes = new HashSet<>();
            if (canGoThrough != null) {
                passableNodes.addAll(canGoThrough.stream().map(o -> (Node)o).collect(Collectors.toList()));
            }

            return allPotentialObstaclesStream
                    .filter(node -> !Objects.equals(node, p)) // Exclude current player
                    .filter(node -> !passableNodes.contains(node)) // Exclude passable obstacles
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting nodes to avoid: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static Obstacle findSpecialLoot(GameMap gm, String id) {
        if (gm.getListObstacles() == null) return null;
        try {
            return gm.getListObstacles().stream().filter(o -> o.getId().contains(id)).findFirst().orElse(null);
        }
        catch (Exception e) {
            System.err.println("Error finding special loot: " + e.getMessage());
            return null;
        }
    }

    public static Node findGeneralLoot(Player p, GameMap gm) {
        try {
            List<Obstacle> chests = gm.getListChests();
            List<Weapon> guns = gm.getAllGun();

            List<Node> chestNodes = (chests != null) ? chests.stream().map(o -> (Node) o).collect(Collectors.toList()) : new ArrayList<>();
            List<Node> gunNodes = (guns != null) ? guns.stream().map(w -> (Node) w).collect(Collectors.toList()) : new ArrayList<>();

            Node nearestChest = findNearest(p, chestNodes); // p is already a Node
            Node nearestWeapon = findNearest(p, gunNodes); // p is already a Node

            if (nearestChest == null) return nearestWeapon;
            if (nearestWeapon == null) return nearestChest;

            return getDistance(p, nearestChest) < getDistance(p, nearestWeapon) ? nearestChest : nearestWeapon;
        } catch (Exception e) {
            System.err.println("Error finding general loot: " + e.getMessage());
            return null;
        }
    }

    public static Node findNearestTarget(Player p, GameMap gm) {
        try {
            List<Player> otherPlayers = gm.getOtherPlayerInfo();
            List<Enemy> enemies = gm.getListEnemies();

            List<Node> otherPlayerNodes = (otherPlayers != null) ? otherPlayers.stream().map(pl -> (Node) pl).collect(Collectors.toList()) : new ArrayList<>();
            List<Node> enemyNodes = (enemies != null) ? enemies.stream().map(e -> (Node) e).collect(Collectors.toList()) : new ArrayList<>();

            List<Node> allTargetNodes = new ArrayList<>();
            allTargetNodes.addAll(otherPlayerNodes);
            allTargetNodes.addAll(enemyNodes);

            return findNearest(p, allTargetNodes);
        } catch (Exception e) {
            System.err.println("Error finding nearest target: " + e.getMessage());
            return null;
        }
    }

    public static Enemy findNearestEnemy(Player p, GameMap gm) {
        try {
            List<Enemy> enemies = gm.getListEnemies();
            List<Node> enemyNodes = (enemies != null) ? enemies.stream().map(e -> (Node) e).collect(Collectors.toList()) : new ArrayList<>();

            Node nearestEnemyNode = findNearest(p, enemyNodes);
            if (nearestEnemyNode != null && enemies != null) {
                return enemies.stream().filter(e -> Objects.equals(e, nearestEnemyNode)).findFirst().orElse(null);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error finding nearest enemy: " + e.getMessage());
            return null;
        }
    }


    public static HealingItem findNearestHealthPack(Player player, GameMap gm) {
        if (gm.getListHealingItems() == null || gm.getListHealingItems().isEmpty()) return null;
        try {
            List<HealingItem> healingItems = gm.getListHealingItems();
            List<Node> healingItemNodes = healingItems.stream().map(item -> (Node) item).collect(Collectors.toList());
            Node nearestHealingItemNode = findNearest(player, healingItemNodes);
            if (nearestHealingItemNode != null) {
                return healingItems.stream().filter(h -> Objects.equals(h, nearestHealingItemNode)).findFirst().orElse(null);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error finding health pack: " + e.getMessage());
            return null;
        }
    }

    /**
     * MỚI & QUAN TRỌNG: Tìm Airdrop (Trứng rồng) dựa trên ID hoặc các đặc điểm khác.
     * Gỡ bỏ hoàn toàn việc sử dụng ObstacleTag.DRAGON_EGG để tránh lỗi runtime.
     * Bạn cần xác định chuỗi ID chính xác của trứng rồng nếu muốn sử dụng.
     * Ví dụ: Nếu ID của trứng rồng là "AIRDROP_CRATE_123", thì dùng "AIRDROP_CRATE".
     * Hoặc nếu trứng rồng là một loại "SPECIAL_CHEST", bạn có thể tìm kiếm dựa trên ID đó.
     */
    public static Obstacle findAirdrop(Node playerNode, GameMap gm) {
        // CÁC CHUỖI ID CÓ THỂ CÓ TRONG SDK CỦA BẠN: "AIRDROP_CRATE", "SUPPLY_CRATE", "DRAGON_EGG_ITEM", ...
        // HÃY KIỂM TRA MÃ NGUỒN SDK HOẶC THỬ CÁC ID KHÁC NẾU CẦN.
        String[] possibleAirdropIds = {"AIRDROP_CRATE", "SUPPLY_CRATE", "DRAGON_EGG_ITEM", "SPECIAL_LOOT_BOX"}; // <-- THAY THẾ CÁC CHUỖI NÀY BẰNG ID THỰC TẾ

        if (gm.getListObstacles() == null) return null;
        try {
            List<Obstacle> airdropObstacles = new ArrayList<>();
            for (Obstacle o : gm.getListObstacles()) {
                if (o.getId() != null) {
                    for (String idPart : possibleAirdropIds) {
                        if (o.getId().contains(idPart)) {
                            airdropObstacles.add(o);
                            break; // Found a match, move to next obstacle
                        }
                    }
                }
            }

            if (airdropObstacles.isEmpty()) return null;

            List<Node> airdropNodes = airdropObstacles.stream().map(o -> (Node) o).collect(Collectors.toList());
            Node nearestAirdropNode = findNearest(playerNode, airdropNodes);

            if (nearestAirdropNode != null) {
                return airdropObstacles.stream().filter(o -> Objects.equals(o, nearestAirdropNode)).findFirst().orElse(null);
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error finding airdrop by ID: " + e.getMessage());
            return null;
        }
    }

    // Gỡ bỏ hoàn toàn findNearestDragonEgg() cũ để tránh lỗi.
    // public static Obstacle findNearestDragonEgg(Node playerNode, GameMap gm) {...}

    public static Obstacle findNearestChest(Node playerNode, GameMap gm) {
        try {
            List<Obstacle> chests = gm.getListChests();
            List<Node> chestNodes = (chests != null) ? chests.stream().map(o -> (Node) o).collect(Collectors.toList()) : new ArrayList<>();
            Node nearestChestNode = findNearest(playerNode, chestNodes);
            if (nearestChestNode != null) {
                return chests.stream().filter(o -> Objects.equals(o, nearestChestNode)).findFirst().orElse(null);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error finding nearest chest: " + e.getMessage());
            return null;
        }
    }

    public static Weapon findNearestThrowable(Node playerNode, GameMap gm) {
        try {
            List<Weapon> throwables = gm.getAllThrowable();
            List<Node> throwableNodes = (throwables != null) ? throwables.stream().map(w -> (Node) w).collect(Collectors.toList()) : new ArrayList<>();
            Node nearestThrowableNode = findNearest(playerNode, throwableNodes);
            if (nearestThrowableNode != null) {
                return throwables.stream().filter(w -> Objects.equals(w, nearestThrowableNode)).findFirst().orElse(null);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error finding nearest throwable: " + e.getMessage());
            return null;
        }
    }

    public static boolean isNodeBlocked(Node node, GameMap gm, List<Node> nodesToAvoid) {
        if (node == null || gm == null || nodesToAvoid == null) return true;
        try {
            if (nodesToAvoid.contains(node)) return true;
            return node.getX() < 0 || node.getX() >= gm.getMapSize() || node.getY() < 0 || node.getY() >= gm.getMapSize();
        } catch (Exception e) {
            System.err.println("Error checking if node is blocked: " + e.getMessage());
            return true;
        }
    }

    public static boolean isTargetBehindObstacle(Node playerNode, Node targetNode, GameMap gm) {
        if (playerNode == null || targetNode == null || gm == null) return false;
        try {
            double step = 0.5;
            double dx = targetNode.getX() - playerNode.getX();
            double dy = targetNode.getY() - playerNode.getY();
            double totalDistance = Math.hypot(dx, dy);

            for (double i = step; i < totalDistance; i += step) {
                double ratio = i / totalDistance;
                int x = (int) Math.round(playerNode.getX() + dx * ratio);
                int y = (int) Math.round(playerNode.getY() + dy * ratio);
                Node checkNode = new Node(x, y);

                if (gm.getListIndestructibles() != null && gm.getListIndestructibles().stream().anyMatch(o -> Objects.equals(o, checkNode))) {
                    return true;
                }
                if (gm.getListObstacles() != null) {
                    for (Obstacle obstacle : gm.getListObstacles()) {
                        if (Objects.equals(obstacle, checkNode) && !obstacle.getId().contains("CAN_GO_THROUGH")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error checking obstacle between nodes: " + e.getMessage());
            return false;
        }
    }

    public static Node findClearPathToTarget(Node playerNode, Node targetNode, GameMap gm) {
        if (playerNode == null || targetNode == null || gm == null) return null;
        try {
            List<Node> surroundingNodes = new ArrayList<>();
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    surroundingNodes.add(new Node((int) playerNode.getX() + i, (int) playerNode.getY() + j));
                }
            }
            List<Node> nodesToAvoid = getNodesToAvoid(gm, gm.getCurrentPlayer());
            surroundingNodes.sort(Comparator.comparingDouble(n -> getDistance(n, targetNode)));
            for (Node node : surroundingNodes) {
                if (!isNodeBlocked(node, gm, nodesToAvoid) && !isTargetBehindObstacle(node, targetNode, gm)) {
                    return node;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error finding clear path: " + e.getMessage());
            return null;
        }
    }

    public static Node findBestEscapeRoute(Node startNode, GameMap gm, List<Node> nodesToAvoid) {
        if (startNode == null || gm == null || nodesToAvoid == null) return null;
        try {
            Queue<Node> queue = new LinkedList<>();
            Set<Node> visited = new HashSet<>();
            queue.offer(startNode);
            visited.add(startNode);
            Node bestEscapeNode = startNode;
            double maxDistance = 0.0;
            while (!queue.isEmpty()) {
                Node currentNode = queue.poll();
                double distance = getDistance(startNode, currentNode);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    bestEscapeNode = currentNode;
                }
                if (visited.size() > 500) break;
                List<Node> neighbors = getNeighbors(currentNode);
                for (Node neighbor : neighbors) {
                    if (!visited.contains(neighbor) && !isNodeBlocked(neighbor, gm, nodesToAvoid)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
            return bestEscapeNode;
        } catch (Exception e) {
            System.err.println("Error finding escape route: " + e.getMessage());
            return null;
        }
    }

    public static List<Node> getNeighbors(Node node) {
        if (node == null) return new ArrayList<>();
        try {
            List<Node> neighbors = new ArrayList<>();
            neighbors.add(new Node((int) node.getX(), (int) node.getY() - 1));
            neighbors.add(new Node((int) node.getX(), (int) node.getY() + 1));
            neighbors.add(new Node((int) node.getX() - 1, (int) node.getY()));
            neighbors.add(new Node((int) node.getX() + 1, (int) node.getY()));
            return neighbors;
        } catch (Exception e) {
            System.err.println("Error getting neighbors: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static Node findBestHidingSpot(Player player, GameMap gm) {
        if (player == null || gm == null || gm.getListIndestructibles() == null || gm.getListIndestructibles().isEmpty()) return null;
        try {
            List<Obstacle> indestructibles = gm.getListIndestructibles();
            List<Node> indestructibleNodes = indestructibles.stream().map(o -> (Node) o).collect(Collectors.toList());
            return findNearest(player, indestructibleNodes);
        } catch (Exception e) {
            System.err.println("Error finding hiding spot: " + e.getMessage());
            return null;
        }
    }

    public static Node findBestAmbushPoint(Player player, GameMap gm) {
        return new Node(0, 0); // Placeholder implementation
    }

    public static Node findStrategicPosition(Player player, GameMap gm) {
        if (player == null || gm == null) return null;
        try {
            return new Node(gm.getMapSize() / 2, gm.getMapSize() / 2);
        } catch (Exception e) {
            System.err.println("Error finding strategic position: " + e.getMessage());
            return null;
        }
    }

    public static Node findUnexploredCorner(GameMap gm, Player player) {
        if (gm == null || player == null) return null;
        try {
            Node[] corners = {
                    new Node(0, 0),
                    new Node(gm.getMapSize() - 1, 0),
                    new Node(0, gm.getMapSize() - 1),
                    new Node(gm.getMapSize() - 1, gm.getMapSize() - 1)
            };
            Node furthestCorner = null;
            double maxDistance = -1;

            for (Node corner : corners) {
                double distance = getDistance(player, corner);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    furthestCorner = corner;
                }
            }
            return furthestCorner;
        } catch (Exception e) {
            System.err.println("Error finding unexplored corner: " + e.getMessage());
            return null;
        }
    }
}