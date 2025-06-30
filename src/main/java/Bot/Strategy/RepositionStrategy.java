package Bot.Strategy;

import Bot.Actions.BotAction;
import Bot.Actions.MoveAction;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.algorithm.PathUtils; // Assuming PathUtils is correct

public class RepositionStrategy implements IStrategy {
    private static final double EXPLORE_DISTANCE_THRESHOLD = 50.0; // Khoảng cách tối thiểu để khám phá

    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        try {
            // Ưu tiên 1: Chạy vào bo nếu đang ở ngoài bo
            if (!PathUtils.checkInsideSafeArea(ctx.player, ctx.gameMap.getMapSize(), ctx.gameMap.getSafeZone())) {
                return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Chay vao bo");
            }

            // Ưu tiên 2: Tìm vị trí ẩn nấp/chiến lược tốt nhất trong bo
            Node strategicPosition = BotUtils.findBestHidingSpot(ctx.player, ctx.gameMap);
            // Nếu không tìm thấy chỗ ẩn nấp tốt, hoặc đã ở chỗ ẩn nấp
            if (strategicPosition != null && BotUtils.getDistance(ctx.player, strategicPosition) > 1.0) { // Nếu chưa ở chỗ ẩn nấp tốt
                return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, strategicPosition, ctx.nodesToAvoid, "Di toi cho an nap tot nhat");
            }

            // Ưu tiên 3: Khám phá góc bản đồ chưa được thăm dò (để tìm loot ẩn)
            Node unexploredCorner = BotUtils.findUnexploredCorner(ctx.gameMap, ctx.player);
            if (unexploredCorner != null && BotUtils.getDistance(ctx.player, unexploredCorner) > EXPLORE_DISTANCE_THRESHOLD) {
                return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, unexploredCorner, ctx.nodesToAvoid, "Kham pha goc ban do");
            }

            // Mặc định: Di chuyển về trung tâm bo an toàn (nếu không có mục tiêu chiến lược khác)
            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Tai dinh vi ve trung tam bo");

        } catch (Exception e) {
            System.err.println("Error in RepositionStrategy: " + e.getMessage());
            return null;
        }
    }
}