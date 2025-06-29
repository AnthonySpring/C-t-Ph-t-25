package Bot;

import Bot.Actions.BotAction;
import Bot.Strategy.*;
import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;

import java.util.Arrays;
import java.util.List;

public class ProGamerBotListener implements Emitter.Listener {
    private final Hero hero;
    private final List<IStrategy> strategies;
    private List<String> currentPath = null;

    public ProGamerBotListener(Hero hero) {
        this.hero = hero;
        this.strategies = Arrays.asList(
                new SurvivalStrategy(),
                new AirdropStrategy(),
                new CombatStrategy(),
                new LootingStrategy(),
                new RepositionStrategy()
        );
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

            if (this.currentPath != null && !this.currentPath.isEmpty()) {
                String nextMove = this.currentPath.remove(0);
                hero.move(nextMove);
                return;
            }
            this.currentPath = null;

            DecisionContext context = new DecisionContext(myPlayer, gameMap, hero);

            for (IStrategy strategy : strategies) {
                BotAction action = strategy.recommendAction(context);
                if (action != null) {
                    System.out.println("BOT: [Quyết định] " + action.reason);
                    this.currentPath = action.execute();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.currentPath = null;
        }
    }
}