package Bot.Actions;

import jsclub.codefest.sdk.Hero;

import java.util.List;

public class AttackAction extends BotAction {
    private final Hero hero;
    private final String direction;

    public AttackAction(Hero hero, String direction, String reason) {
        this.hero = hero;
        this.direction = direction;
        this.reason = reason;
    }

    @Override
    public List<String> execute() {
        try {
            if (direction != null) hero.attack(direction);
        } catch (Exception e) {
        }
        return null;
    }
}
