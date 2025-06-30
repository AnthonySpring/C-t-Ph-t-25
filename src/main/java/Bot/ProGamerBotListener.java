package Bot;

import Bot.Actions.*;
import Bot.Utils.BotUtils;
import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.npcs.Enemy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

enum GamePhase {
    EARLY_GAME, // Giai đoạn "Shipper"
    MID_GAME,   // Giai đoạn "Rình rập"
    END_GAME    // Giai đoạn "Sát thủ"
}

public class ProGamerBotListener implements Emitter.Listener {
    private final Hero hero;
    private List<String> currentPath = null;
    private Node pathTarget = null;
    private String pathReason = "";

    public ProGamerBotListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;
            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player myPlayer = gameMap.getCurrentPlayer();
            if (myPlayer == null || myPlayer.getHealth() <= 0) {
                this.currentPath = null;
                return;
            }

            DecisionContext context = new DecisionContext(myPlayer, gameMap, hero);
            if (this.currentPath != null && !this.currentPath.isEmpty()) {
                if (isPathStillValid(context)) {
                    String nextMove = this.currentPath.remove(0);
                    hero.move(nextMove);
                    return;
                } else {
                    System.out.println("BOT: [Cảnh báo] Mục tiêu của đường đi không còn hợp lệ! Hủy bỏ và tính toán lại.");
                    this.currentPath = null;
                }
            }
            this.currentPath = null;
            this.pathTarget = null;
            this.pathReason = "";

            GamePhase currentPhase = getCurrentGamePhase(gameMap);
            List<BotAction> potentialActions = generatePotentialActions(context, currentPhase);
            BotAction bestAction = chooseBestAction(potentialActions, context, currentPhase);

            if (bestAction != null) {
                System.out.println(">>> LUA CHON (" + currentPhase + "): " + bestAction.reason);
                if (bestAction instanceof MoveAction) {
                    this.pathTarget = ((MoveAction) bestAction).getTargetNode();
                    this.pathReason = bestAction.reason;
                }
                List<String> path = bestAction.execute();
                if (path != null) {
                    this.currentPath = new ArrayList<>(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.currentPath = null;
        }
    }

    private BotAction chooseBestAction(List<BotAction> actions, DecisionContext context, GamePhase phase) {
        BotAction bestAction = null;
        double highestScore = -Double.MAX_VALUE;

        for (BotAction action : actions) {
            double score = calculateAdvancedScore(action, context, phase);
            System.out.println("Hanh dong: " + action.reason + " / Diem so: " + String.format("%.2f", score));
            if (score > highestScore) {
                highestScore = score;
                bestAction = action;
            }
        }
        return bestAction;
    }

    private boolean isPathStillValid(DecisionContext context) {
        if (this.pathTarget == null) return true;
        if (this.pathReason.contains("sung")) return context.gameMap.getAllGun().stream().anyMatch(gun -> gun.equals(this.pathTarget));
        if (this.pathReason.contains("mau")) return context.gameMap.getListHealingItems().stream().anyMatch(item -> item.equals(this.pathTarget));
        if (this.pathReason.contains("Trung Rong")) return context.gameMap.getListObstacles().stream().anyMatch(obs -> obs.equals(this.pathTarget));
        return true;
    }

    private List<BotAction> generatePotentialActions(DecisionContext context, GamePhase phase) {
        List<BotAction> actions = new ArrayList<>();
        Player player = context.player;
        GameMap gameMap = context.gameMap;

        // Hành động chiến đấu
        Player nearestPlayer = BotUtils.findNearest(player, gameMap.getOtherPlayerInfo());
        if (nearestPlayer != null) {
            actions.add(new ShootAction(context.hero, BotUtils.getDirectionString(player, nearestPlayer), "Tan cong Nguoi Choi " + nearestPlayer.getId()));
            actions.add(new MoveAction(context.hero, gameMap, player, BotUtils.getRetreatPoint(player, nearestPlayer), context.nodesToAvoid, "Rut lui khoi Nguoi Choi"));
        }
        Enemy nearestNPC = BotUtils.findNearest(player, gameMap.getListEnemies());
        if (nearestNPC != null) {
            actions.add(new ShootAction(context.hero, BotUtils.getDirectionString(player, nearestNPC), "Tan cong Quai " + nearestNPC.getId()));
        }

        // Hành động loot đồ và Trứng Rồng
        if (phase == GamePhase.EARLY_GAME || phase == GamePhase.MID_GAME) {
            gameMap.getAllGun().stream()
                    .min(Comparator.comparingDouble(gun -> BotUtils.getDistance(player, gun)))
                    .ifPresent(gun -> actions.add(new MoveAction(context.hero, gameMap, player, gun, context.nodesToAvoid, "Di nhat sung")));

            HealingItem healthPack = BotUtils.findNearestHealthPack(player, gameMap);
            if (healthPack != null) actions.add(new MoveAction(context.hero, gameMap, player, healthPack, context.nodesToAvoid, "Di nhat mau"));

            Obstacle chest = BotUtils.findNearestChest(player, gameMap);
            if (chest != null) actions.add(new MoveAction(context.hero, gameMap, player, chest, context.nodesToAvoid, "Di pha ruong"));

            // Tìm Trứng Rồng
            Obstacle airdrop = BotUtils.findAirdrop(player, gameMap);
            if (airdrop != null) actions.add(new MoveAction(context.hero, gameMap, player, airdrop, context.nodesToAvoid, "Tiep can Trung Rong"));
        }

        // Hành động vị trí
        if (!jsclub.codefest.sdk.algorithm.PathUtils.checkInsideSafeArea(player, gameMap.getMapSize(), gameMap.getSafeZone())) {
            actions.add(new MoveAction(context.hero, gameMap, player, context.safeZoneCenter, context.nodesToAvoid, "Chay vao bo"));
        } else {
            Node hidingSpot = BotUtils.findBestHidingSpot(player, gameMap);
            if (hidingSpot != null && BotUtils.getDistance(player, hidingSpot) > 1.0) {
                actions.add(new MoveAction(context.hero, gameMap, player, hidingSpot, context.nodesToAvoid, "Tim cho an nap"));
            }
        }
        return actions;
    }

    private double calculateAdvancedScore(BotAction action, DecisionContext context, GamePhase phase) {
        Player player = context.player;
        double currentHealth = player.getHealth();
        String reason = action.reason;
        double score = 0;

        // --- ĐIỂM CƠ BẢN ---
        if (reason.contains("Tan cong")) score = 60;
        if (reason.contains("Rut lui")) score = 50;
        if (reason.contains("sung")) score = 40;
        if (reason.contains("mau")) score = 65;
        if (reason.contains("ruong")) score = 30;
        if (reason.contains("Trung Rong")) score = 200; // Rất quan trọng
        if (reason.contains("bo")) score = 1000;
        if (reason.contains("an nap")) score = 35;

        // --- ĐIỂM THƯỞNG & PHẠT ---
        if (reason.contains("mau")) score += (100 - currentHealth);
        if (reason.contains("Rut lui")) score += (100 - currentHealth) * 0.5;
        if (reason.contains("Tan cong Nguoi Choi")) score += 20;
        if (reason.contains("Tan cong Quai")) score -= 30; // Ít ưu tiên hơn

        // --- XỬ LÝ KỊCH BẢN PHỨC TẠP: TRỨNG RỒNG ---
        if (reason.contains("Trung Rong")) {
            Node airdropLocation = ((MoveAction) action).getTargetNode();
            // Đếm số lượng địch (cả người và quái) gần Trứng Rồng
            long enemiesNearAirdrop = Stream.concat(
                    context.gameMap.getOtherPlayerInfo().stream(),
                    context.gameMap.getListEnemies().stream()
            ).filter(enemy -> BotUtils.getDistance(enemy, airdropLocation) < 12.0).count();

            if (enemiesNearAirdrop > 0) {
                System.out.println("BOT: [Phan tich] Co " + enemiesNearAirdrop + " ke dich gan Trung Rong.");
                score -= enemiesNearAirdrop * 75; // Mỗi kẻ địch gần đó trừ 75 điểm
            }
        }

        // --- HỆ SỐ NHÂN THEO GIAI ĐOẠN GAME ---
        switch (phase) {
            case EARLY_GAME: // Hóa thân thành "SHIPPER"
                if (reason.contains("sung") || reason.contains("ruong") || reason.contains("mau")) score *= 3.0;
                if (reason.contains("Tan cong")) score *= 0.1; // Rất ngại đánh nhau
                if (reason.contains("Trung Rong")) score *= 0.5; // Đầu game chưa nên vội
                break;
            case MID_GAME: // "Kẻ rình rập" cẩn trọng
                if (reason.contains("Tan cong")) score *= 0.7;
                if (reason.contains("an nap") || reason.contains("bo")) score *= 1.5;
                break;
            case END_GAME: // "Sát thủ" quyết đoán
                if (reason.contains("Tan cong") || reason.contains("Rut lui")) score *= 2.0;
                if (reason.contains("ruong") || reason.contains("sung")) score *= 0.05; // Gần như không loot nữa
                break;
        }

        return score;
    }

    private GamePhase getCurrentGamePhase(GameMap gameMap) {
        // Giả sử SDK có `gameMap.getRoundNumber()` để lấy vòng bo hiện tại
        // Nếu không có, bạn có thể dùng lại logic `safeZoneRadius`
        try {
            // Dùng reflection để thử gọi getRoundNumber nếu có
            java.lang.reflect.Method method = gameMap.getClass().getMethod("getRoundNumber");
            int round = (int) method.invoke(gameMap);
            if (round <= 1) return GamePhase.EARLY_GAME; // Vòng bo đầu tiên và lúc chưa bo
            if (round <= 3) return GamePhase.MID_GAME; // Vòng bo 2, 3
            return GamePhase.END_GAME;
        } catch (Exception e) {
            // Nếu không có getRoundNumber, quay về dùng tỉ lệ bán kính
            int mapSize = gameMap.getMapSize();
            int safeZoneRadius = gameMap.getSafeZone();
            double zoneRatio = (double) safeZoneRadius / mapSize;
            if (zoneRatio > 0.4) return GamePhase.EARLY_GAME;
            if (zoneRatio > 0.2) return GamePhase.MID_GAME;
            return GamePhase.END_GAME;
        }
    }
}