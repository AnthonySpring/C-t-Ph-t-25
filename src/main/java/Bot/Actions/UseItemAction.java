package Bot.Actions;

import jsclub.codefest.sdk.Hero;

import java.util.List;

public class UseItemAction extends BotAction {
    private final Hero hero;
    private final String itemId;

    public UseItemAction(Hero hero, String itemId) {
        this.hero = hero;
        this.itemId = itemId;
        this.reason = "Dung item hoi mau";
    }

    @Override
    public List<String> execute() {
        try {
            if (itemId != null) hero.useItem(itemId);
        } catch (Exception e) {
        }
        return null;
    }
}
