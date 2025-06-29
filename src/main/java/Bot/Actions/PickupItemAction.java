package Bot.Actions;

import jsclub.codefest.sdk.Hero;

import java.util.List;

public class PickupItemAction extends BotAction {
    private final Hero hero;

    public PickupItemAction(Hero hero, String reason) {
        this.hero = hero;
        this.reason = reason;
    }

    @Override
    public List<String> execute() {
        try {
            hero.pickupItem();
        } catch (Exception e) {
        }
        return null;
    }
}
